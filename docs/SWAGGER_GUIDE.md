# Swagger 산출물 가이드

본 프로젝트 제출용 Swagger/OpenAPI 산출물은 아래 2개입니다.

- `docs/spring-openapi.json` (Spring Main API)
- `docs/fastapi-openapi.json` (AI FastAPI)

## 1) Spring OpenAPI 갱신
서비스 실행 후:

```powershell
# 로그인으로 access token 획득
$loginBody = @{ username='admin@chemilog.com'; password='Admin1234!' } | ConvertTo-Json
$loginResp = Invoke-RestMethod -Method Post -Uri 'http://localhost:18081/api/v1/auth/login' -ContentType 'application/json' -Body $loginBody
$token = $loginResp.data.accessToken

# OpenAPI JSON 저장
$headers = @{ Authorization = "Bearer $token" }
Invoke-RestMethod -Method Get -Uri 'http://localhost:18081/v3/api-docs' -Headers $headers |
  ConvertTo-Json -Depth 100 |
  Set-Content -Path 'docs/spring-openapi.json' -Encoding utf8
```

## 2) FastAPI OpenAPI 갱신
FastAPI 컨테이너 내부 네트워크 경유:

```powershell
docker exec chemilog-spring-main-1 sh -lc "wget -qO- http://fastapi-service:8000/openapi.json" > docs/fastapi-openapi.json
```

## 3) Swagger UI 확인
- Spring: `http://localhost:18081/swagger-ui/index.html`
- FastAPI: `http://localhost:8000/docs` (호스트 포트 노출 환경에서만 직접 접근 가능)
