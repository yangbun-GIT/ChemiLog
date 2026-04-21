# ChemiLog Swagger / OpenAPI 제출 가이드

이 문서는 과제 제출용 Swagger 자료를 준비하는 방법을 정리합니다.

## 1. Spring Boot(Main Service)

- Swagger UI: [http://localhost:18081/swagger-ui.html](http://localhost:18081/swagger-ui.html)
- OpenAPI JSON: [http://localhost:18081/api-docs](http://localhost:18081/api-docs)

주요 API 그룹:
- 인증/회원: `/api/v1/auth/*`, `/api/v1/users/*`
- 식품: `/api/v1/foods/*`
- 식단: `/api/v1/meals/*`
- AI 게이트웨이: `/api/v1/ai/*`
- 관리자: `/api/v1/admin/*`

### 제출 파일 저장
```powershell
Invoke-WebRequest -Uri http://localhost:18081/api-docs -OutFile .\docs\spring-openapi.json
```

## 2. FastAPI(AI Service)

기본 문서 경로:
- Docs: [http://localhost:8000/docs](http://localhost:8000/docs)
- ReDoc: [http://localhost:8000/redoc](http://localhost:8000/redoc)
- OpenAPI JSON: [http://localhost:8000/openapi.json](http://localhost:8000/openapi.json)

주의:
- 기본 `docker-compose.yml`은 FastAPI를 외부로 publish하지 않으므로 브라우저 직접 접근이 안 될 수 있습니다.
- 필요 시 `fastapi-service`에 `ports`를 임시 추가해 확인 후, 제출 전에 원복하는 것을 권장합니다.

### 제출 파일 저장 (포트 publish 환경)
```powershell
Invoke-WebRequest -Uri http://localhost:8000/openapi.json -OutFile .\docs\fastapi-openapi.json
```

## 3. Swagger를 "따로" 제출하는 방법

과제 제출 폴더에 아래를 별도 포함하면 됩니다.

- `docs/swagger.md` (이 설명 문서)
- `docs/spring-openapi.json`
- `docs/fastapi-openapi.json` (가능하면)
- Swagger UI 화면 캡처 이미지(권장)

권장 압축 구조 예:
```text
제출물/
├─ README.md
├─ docs/
│  ├─ swagger.md
│  ├─ spring-openapi.json
│  ├─ fastapi-openapi.json
│  └─ swagger-screenshot.png
└─ 소스코드/
```

## 4. 점검 체크리스트

- [ ] Swagger UI가 실제로 열리는지 확인
- [ ] OpenAPI JSON 파일이 비어있지 않은지 확인
- [ ] `README.md`에서 문서 위치/실행 절차를 바로 찾을 수 있는지 확인
- [ ] `.env`, `node_modules` 등 민감/불필요 파일이 제출물에 없는지 확인
