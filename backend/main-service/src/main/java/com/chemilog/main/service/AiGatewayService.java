package com.chemilog.main.service;

import com.chemilog.main.api.ai.AiMentoringRequest;
import com.chemilog.main.config.properties.InternalProperties;
import com.chemilog.main.domain.user.User;
import com.chemilog.main.domain.user.UserRole;
import com.chemilog.main.exception.ApiException;
import com.chemilog.main.repository.UserRepository;
import com.chemilog.main.security.AuthUser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@Service
public class AiGatewayService {

    private final InternalProperties internalProperties;
    private final ObjectMapper snakeCaseObjectMapper;
    private final UserRepository userRepository;
    private final HttpClient httpClient;

    public AiGatewayService(
            InternalProperties internalProperties,
            ObjectMapper objectMapper,
            UserRepository userRepository
    ) {
        this.internalProperties = internalProperties;
        this.snakeCaseObjectMapper = objectMapper.copy()
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        this.userRepository = userRepository;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public StreamingResponseBody streamMentoring(AuthUser authUser, AiMentoringRequest request) {
        if (authUser == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "AUTH-4001", "로그인이 필요합니다.");
        }
        User user = userRepository.findByUserIdAndDeletedFalse(authUser.userId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "AUTH-4003", "사용자를 찾을 수 없습니다."));

        return outputStream -> {
            try {
                HttpRequest downstreamRequest = buildDownstreamRequest(user, request);
                HttpResponse<java.io.InputStream> downstreamResponse = httpClient.send(
                        downstreamRequest,
                        HttpResponse.BodyHandlers.ofInputStream()
                );
                if (downstreamResponse.statusCode() >= 400) {
                    writeErrorEvent(outputStream, "AI 서비스 호출에 실패했습니다.");
                    return;
                }

                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(downstreamResponse.body(), StandardCharsets.UTF_8)
                )) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        outputStream.write((line + "\n").getBytes(StandardCharsets.UTF_8));
                        outputStream.flush();
                    }
                }
            } catch (Exception e) {
                writeErrorEvent(outputStream, "AI 서버 지연. 잠시 후 재시도해 주세요.");
            }
        };
    }

    private HttpRequest buildDownstreamRequest(User user, AiMentoringRequest request) {
        String payload;
        try {
            payload = snakeCaseObjectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "AI-4000", "AI 요청 페이로드 직렬화에 실패했습니다.");
        }

        String goal = getHealthProfileString(user.getHealthProfile(), "goal", "MAINTAIN");
        String strictness = getHealthProfileString(user.getHealthProfile(), "strictness", "MEDIUM");
        String allergies = getHealthProfileList(user.getHealthProfile(), "allergies");
        String tier = user.getRole() == UserRole.PREMIUM ? "PREMIUM" : "FREE";

        return HttpRequest.newBuilder()
                .uri(URI.create(internalProperties.fastapiBaseUrl() + "/api/v1/ai/mentoring"))
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream")
                .header("X-Internal-Secret", internalProperties.secret())
                .header("X-Internal-User-Id", String.valueOf(user.getUserId()))
                .header("X-Internal-User-Role", "ROLE_" + user.getRole().name())
                .header("X-Internal-User-Tier", tier)
                .header("X-Internal-User-Goal", goal)
                .header("X-Internal-User-Strictness", strictness)
                .header("X-Internal-User-Allergies", allergies)
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                .build();
    }

    @SuppressWarnings("unchecked")
    private String getHealthProfileList(Map<String, Object> healthProfile, String key) {
        if (healthProfile == null) {
            return "";
        }
        Object value = healthProfile.get(key);
        if (value instanceof List<?> list) {
            return list.stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .reduce((a, b) -> a + "," + b)
                    .orElse("");
        }
        return "";
    }

    private String getHealthProfileString(Map<String, Object> healthProfile, String key, String defaultValue) {
        if (healthProfile == null) {
            return defaultValue;
        }
        Object value = healthProfile.get(key);
        if (value == null) {
            return defaultValue;
        }
        return value.toString();
    }

    private void writeErrorEvent(OutputStream outputStream, String message) throws IOException {
        String event = "event: error\n" +
                "data: {\"status\":\"fallback\",\"message\":\"" + escapeJson(message) + "\"}\n\n";
        outputStream.write(event.getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
    }

    private String escapeJson(String value) {
        return value.replace("\"", "\\\"");
    }
}
