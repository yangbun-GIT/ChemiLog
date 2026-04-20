# ChemiLog 과제 제출 가이드

## 1) 제출 항목

- 요구 명세서(본인 작성)
- 요구 명세서(AI 수정/첨삭 + 본인 검토)
- 소스 코드(`node_modules` 제외)
- 실행 화면 캡처(웹 2개 + DB 실제 저장 화면)
- 문서(Swagger + README.md)

## 2) 지금은 node_modules가 있어야 하나요?

네. **지금 개발/빌드 중에는 node_modules가 있어야 정상**입니다.  
제출 직전에만 제거하면 됩니다.

## 3) 제출 직전에 삭제해야 하는 node_modules 위치

ChemiLog 프로젝트에서 삭제 대상은 아래 2개입니다.

1. `frontend/customer-web/node_modules`
2. `frontend/admin-web/node_modules`

삭제 명령:

```powershell
Remove-Item -Recurse -Force .\frontend\customer-web\node_modules
Remove-Item -Recurse -Force .\frontend\admin-web\node_modules
```

삭제 후 확인:

```powershell
Test-Path .\frontend\customer-web\node_modules
Test-Path .\frontend\admin-web\node_modules
```

둘 다 `False`면 정상입니다.

## 4) 다시 개발해야 할 때 node_modules 복구

```powershell
npm --prefix .\frontend\customer-web install
npm --prefix .\frontend\admin-web install
```

빌드 확인:

```powershell
npm run build
```

## 5) DB 실제 저장 화면 캡처 방법(쉬운 방법)

### 방법 A: psql 터미널 화면 캡처(권장)

1. PostgreSQL 컨테이너 접속
```powershell
docker exec -it chemilog-postgres-1 psql -U chemilog_user -d chemilog
```

2. 아래 쿼리 실행
```sql
\dt
SELECT user_id, email, role, status FROM users ORDER BY user_id;
SELECT meal_id, user_id, meal_date, meal_type, total_calories
FROM meals
ORDER BY meal_id DESC
LIMIT 20;
SELECT food_id, name, category, calories
FROM food_items
ORDER BY food_id DESC
LIMIT 20;
```

3. 터미널 결과를 그대로 캡처하면 "실제 DB 저장 화면" 증빙이 됩니다.

### 방법 B: CSV로 뽑아서 Excel 캡처

```powershell
docker exec chemilog-postgres-1 psql -U chemilog_user -d chemilog -c "COPY (SELECT meal_id, user_id, meal_date, meal_type, total_calories FROM meals ORDER BY meal_id DESC LIMIT 20) TO STDOUT WITH CSV HEADER" > .\docs\meal-sample.csv
```

생성된 `docs/meal-sample.csv`를 Excel에서 열어 캡처해도 됩니다.

## 6) GitHub 업로드 금지 파일 최종 점검

```powershell
git ls-files | Select-String -Pattern "(^|/)\.env$|(^|/)node_modules/|(^|/)\.venv/|\.idea/|__pycache__|\.pem$|\.key$"
```

출력이 없으면 금지 파일이 추적되지 않은 상태입니다.

## 7) AI API 나중에 테스트하는 방법

1. `.env`에 `OPENAI_API_KEY` 입력
2. 컨테이너 재기동

```powershell
docker compose up -d --build
```

3. 로그인 후 토큰 발급

```powershell
$loginBody = @{ username='user@chemilog.com'; password='User1234!' } | ConvertTo-Json
$loginRes = Invoke-RestMethod -Uri "http://localhost:18081/api/v1/auth/login" -Method Post -ContentType "application/json" -Body $loginBody
$token = $loginRes.data.access_token
```

4. AI 멘토링 API 호출

```powershell
$aiBody = @{
  chat_history = @(@{ role = "user"; content = "오늘 식단 피드백해줘" })
  current_cart = @(@{ food_id = 5; name = "삼겹살 구이 1인분"; quantity = 1.0 })
} | ConvertTo-Json -Depth 5

Invoke-WebRequest -Uri "http://localhost:18081/api/v1/ai/mentoring" `
  -Method Post `
  -Headers @{ Authorization = "Bearer $token" } `
  -ContentType "application/json" `
  -Body $aiBody
```

정상 기준:
- HTTP 200
- 멘토링 응답 텍스트 또는 SSE 이벤트 확인
