# ChemiLog Swagger / OpenAPI 가이드

## 1) Spring Boot(Main Service) Swagger

Spring Boot는 `springdoc-openapi`를 사용합니다.

- Swagger UI: `http://localhost:18081/swagger-ui.html`
- OpenAPI JSON: `http://localhost:18081/api-docs`

`docker compose up -d --build` 후 접속 가능합니다.

### 주요 그룹(경로 기준)
- 인증/유저: `/api/v1/auth/*`, `/api/v1/users/*`
- 식품: `/api/v1/foods/*`
- 식단: `/api/v1/meals/*`
- AI 게이트웨이: `/api/v1/ai/*`
- 관리자: `/api/v1/admin/*`

## 2) FastAPI(AI Service) Swagger

FastAPI는 기본 Docs를 제공합니다.

- Docs: `http://localhost:8000/docs`
- Redoc: `http://localhost:8000/redoc`
- OpenAPI JSON: `http://localhost:8000/openapi.json`

주의:
- 현재 compose 기본 설정은 `fastapi-service`를 host로 직접 publish하지 않습니다.
- 외부 브라우저에서 FastAPI Swagger를 직접 확인하려면 임시로 포트 매핑을 추가하거나, 컨테이너 내부/프록시 경로로 접근해야 합니다.

## 3) OpenAPI 파일 저장(제출용)

제출 자료에 OpenAPI JSON을 파일로 첨부하려면:

```powershell
Invoke-WebRequest -Uri http://localhost:18081/api-docs -OutFile .\docs\spring-openapi.json
```

FastAPI가 host에 열려있는 경우:

```powershell
Invoke-WebRequest -Uri http://localhost:8000/openapi.json -OutFile .\docs\fastapi-openapi.json
```

## 4) 스크린샷 권장 목록(과제 제출)

- Spring Swagger UI 첫 화면
- 주요 API 예시 1~2개 (예: `/auth/login`, `/foods/search`)
- FastAPI Docs 첫 화면 (가능한 경우)
