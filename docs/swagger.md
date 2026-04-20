# ChemiLog Swagger / OpenAPI Guide

## 1) Spring Boot (Main Service)

The Spring service uses `springdoc-openapi`.

- Swagger UI: `http://localhost:18081/swagger-ui.html`
- OpenAPI JSON: `http://localhost:18081/api-docs`

Main API groups:
- Auth/User: `/api/v1/auth/*`, `/api/v1/users/*`
- Foods: `/api/v1/foods/*`
- Meals: `/api/v1/meals/*`
- AI Gateway: `/api/v1/ai/*`
- Admin: `/api/v1/admin/*`

## 2) FastAPI (AI Service)

FastAPI default docs:

- Docs: `http://localhost:8000/docs`
- Redoc: `http://localhost:8000/redoc`
- OpenAPI JSON: `http://localhost:8000/openapi.json`

Note:
- In the default compose setup, FastAPI is not directly published to the host.
- To open FastAPI docs in browser, publish the port temporarily.

## 3) Export OpenAPI Files

### Spring OpenAPI JSON

```powershell
Invoke-WebRequest -Uri http://localhost:18081/api-docs -OutFile .\docs\spring-openapi.json
```

### FastAPI OpenAPI JSON (when port is published)

```powershell
Invoke-WebRequest -Uri http://localhost:8000/openapi.json -OutFile .\docs\fastapi-openapi.json
```

## 4) Recommended Screenshots for Submission

- Spring Swagger UI main page
- One or two expanded Spring endpoints
- FastAPI docs page (if available)
