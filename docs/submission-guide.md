# ChemiLog Submission Guide

## 1) Required Deliverables

- Requirement spec (written by student)
- Requirement spec (AI revision + student review)
- Source code (exclude `node_modules`)
- Runtime screenshots (2 web pages + real DB data screen)
- Documents (`Swagger` + `README.md`)

## 2) Source Code Submission: Remove node_modules Locally

Run:

```powershell
Remove-Item -Recurse -Force .\frontend\customer-web\node_modules
Remove-Item -Recurse -Force .\frontend\admin-web\node_modules
```

Check:

```powershell
git status
```

Notes:
- `.gitignore` already excludes `node_modules/`.
- Your submission zip should include source code only.

## 3) How to Capture Real DB Data (Easy Method)

### Method A: psql terminal capture (recommended)

1. Open PostgreSQL shell:
```powershell
docker exec -it chemilog-postgres-1 psql -U chemilog_user -d chemilog
```

2. Run queries:
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

3. Capture the terminal screen. This is accepted as real stored DB evidence.

### Method B: export CSV and capture in Excel

```powershell
docker exec chemilog-postgres-1 psql -U chemilog_user -d chemilog -c "COPY (SELECT meal_id, user_id, meal_date, meal_type, total_calories FROM meals ORDER BY meal_id DESC LIMIT 20) TO STDOUT WITH CSV HEADER" > .\docs\meal-sample.csv
```

Open `docs/meal-sample.csv` in Excel and capture.

## 4) Web Screenshot Guide

- Customer Web: dashboard or meal sync page (at least 1)
- Admin Web: dashboard or management page (at least 1)
- Suggested names:
  - `capture-customer-dashboard.png`
  - `capture-admin-dashboard.png`
  - `capture-db-psql.png`

## 5) Swagger Submission Guide

Spring Swagger:
- `http://localhost:18081/swagger-ui.html`

Export OpenAPI:

```powershell
Invoke-WebRequest -Uri http://localhost:18081/api-docs -OutFile .\docs\spring-openapi.json
```

See also: `docs/swagger.md`

## 6) Final Forbidden File Check Before Push

```powershell
git ls-files | Select-String -Pattern "(^|/)\.env$|(^|/)node_modules/|(^|/)\.venv/|\.idea/|__pycache__|\.pem$|\.key$"
```

If output is empty, no forbidden files are tracked.

## 7) AI API Test Guide (for later)

1. Set `OPENAI_API_KEY` in `.env`
2. Restart:
```powershell
docker compose up -d --build
```

3. Get token:
```powershell
$loginBody = @{ username='user@chemilog.com'; password='User1234!' } | ConvertTo-Json
$loginRes = Invoke-RestMethod -Uri "http://localhost:18081/api/v1/auth/login" -Method Post -ContentType "application/json" -Body $loginBody
$token = $loginRes.data.access_token
```

4. Call AI mentoring API:
```powershell
$aiBody = @{
  chat_history = @(@{ role = "user"; content = "Give feedback on my meals today." })
  current_cart = @(@{ food_id = 5; name = "Samgyeopsal 1 serving"; quantity = 1.0 })
} | ConvertTo-Json -Depth 5

Invoke-WebRequest -Uri "http://localhost:18081/api/v1/ai/mentoring" `
  -Method Post `
  -Headers @{ Authorization = "Bearer $token" } `
  -ContentType "application/json" `
  -Body $aiBody
```

Expected:
- HTTP 200
- Mentoring text or SSE response events
