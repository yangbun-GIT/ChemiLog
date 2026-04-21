# ChemiLog 과제 제출 가이드

## 제출 항목 체크

1. 요구 명세서 (본인 작성)
2. 요구 명세서 (AI 수정/첨삭 + 본인 검토)
3. 소스 코드 (`node_modules` 제외)
4. 실행 화면 캡처 (웹 2개 + 실제 DB 화면)
5. 문서 (`README.md`, Swagger 자료)

## 1) 소스 코드 제출 시 `node_modules` 제외

삭제 대상:
- `frontend/customer-web/node_modules`
- `frontend/admin-web/node_modules`

삭제 명령:
```powershell
Remove-Item -Recurse -Force .\frontend\customer-web\node_modules
Remove-Item -Recurse -Force .\frontend\admin-web\node_modules
```

## 2) 실제 DB 화면 캡처

PostgreSQL 접속:
```powershell
docker exec -it chemilog-postgres-1 psql -U chemilog_user -d chemilog
```

조회 예시:
```sql
\dt
SELECT user_id, email, role, status FROM users ORDER BY user_id;
SELECT meal_id, user_id, meal_date, meal_type, total_calories
FROM meals
ORDER BY meal_id DESC
LIMIT 20;
```

터미널 결과를 그대로 캡처하면 "실제 저장 DB 화면" 증빙으로 제출 가능합니다.

## 3) Swagger 제출

자세한 절차는 `docs/swagger.md`를 따릅니다.

Spring OpenAPI JSON 추출:
```powershell
Invoke-WebRequest -Uri http://localhost:18081/api-docs -OutFile .\docs\spring-openapi.json
```

## 4) 최종 금지 파일 점검

```powershell
git ls-files | Select-String -Pattern "(^|/)\.env$|(^|/)node_modules/|(^|/)\.venv/|\.idea/|__pycache__|\.pem$|\.key$|\.crt$|\.jks$"
```

출력이 없으면 정상입니다.
