# ChemiLog Swagger / OpenAPI 가이드

## 1) Spring Boot(Main Service) Swagger

Spring Main Service는 `springdoc-openapi`를 사용합니다.

- Swagger UI: `http://localhost:18081/swagger-ui.html`
- OpenAPI JSON: `http://localhost:18081/api-docs`

주요 경로:
- 인증/유저: `/api/v1/auth/*`, `/api/v1/users/*`
- 식품: `/api/v1/foods/*`
- 식단: `/api/v1/meals/*`
- AI 게이트웨이: `/api/v1/ai/*`
- 관리자: `/api/v1/admin/*`

## 2) FastAPI(AI Service) Swagger

FastAPI 기본 문서:

- Docs: `http://localhost:8000/docs`
- Redoc: `http://localhost:8000/redoc`
- OpenAPI JSON: `http://localhost:8000/openapi.json`

참고:
- 기본 docker-compose 설정은 FastAPI 포트를 host에 publish하지 않습니다.
- 필요 시 포트 매핑을 추가해 문서를 확인하세요.

## 3) OpenAPI 파일 저장

### Spring OpenAPI JSON 저장

```powershell
Invoke-WebRequest -Uri http://localhost:18081/api-docs -OutFile .\docs\spring-openapi.json
```

### FastAPI OpenAPI JSON 저장(포트 공개 시)

```powershell
Invoke-WebRequest -Uri http://localhost:8000/openapi.json -OutFile .\docs\fastapi-openapi.json
```

## 4) 과제 제출용 권장 캡처

- Spring Swagger UI 첫 화면
- Spring API 엔드포인트 펼친 화면(1~2개)
- FastAPI Docs 화면(가능한 경우)
