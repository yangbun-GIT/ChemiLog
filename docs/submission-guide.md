# ChemiLog 제출 가이드

## 1) 소스코드 제출 시 제외 항목

필수 제외:
- `node_modules/`
- `.venv/`
- `.idea/`
- `__pycache__/`
- `.env`, `.env.*` (단 `.env.example` 포함)
- `*.pem`, `*.key`, `*.crt`, `*.jks` 등 키/인증서

현재 프로젝트는 `.gitignore`에 반영되어 있습니다.

## 2) 로컬에서 제출용 압축 만들기 (권장)

Git 추적 파일만 압축하면 제외 항목이 자동으로 빠집니다.

```powershell
git archive --format=zip --output .\ChemiLog-submit.zip HEAD
```

이 방식은 `node_modules` 같은 로컬 생성물/민감파일이 포함될 위험이 낮습니다.

## 3) 실행 화면 캡처(웹 2개)

필수 캡처 예시:
- 고객용 웹: 대시보드/식단/마이페이지 중 핵심 화면 1장 이상
- 관리자 웹: 대시보드 + 식품/첨가물/사용자 관리 중 1장 이상

## 4) DB 실제 저장 화면 캡처 방법

아래 2가지 중 하나를 권장합니다.

### 방법 A: psql CLI 화면 캡처 (빠름)

```powershell
docker exec -it chemilog-postgres-1 psql -U chemilog_user -d chemilog
```

psql 안에서 예시 쿼리:

```sql
\dt
SELECT user_id, email, role, status FROM users ORDER BY user_id;
SELECT meal_id, user_id, meal_date, meal_type, total_calories FROM meals ORDER BY meal_id DESC LIMIT 20;
SELECT food_id, name, category, calories FROM food_items ORDER BY food_id DESC LIMIT 20;
```

터미널 결과 화면을 캡처하면 "실제 DB 저장" 증빙으로 사용할 수 있습니다.

### 방법 B: GUI 툴(DBeaver/pgAdmin) 화면 캡처

연결 정보:
- Host: `localhost`
- Port: `5432` (직접 publish하지 않았다면 컨테이너 내부/터널 방식 필요)
- DB: `chemilog`
- User: `chemilog_user`
- Password: `.env`의 `POSTGRES_PASSWORD`

테이블 데이터 그리드 화면을 캡처해서 제출하면 됩니다.

## 5) 제출 체크리스트

- [ ] 요구 명세서(본인 작성)
- [ ] 요구 명세서(AI 첨삭 + 본인 검토본)
- [ ] 소스코드(제외 항목 제거)
- [ ] 실행 화면(고객 웹 + 관리자 웹 + DB 실제 저장 화면)
- [ ] 문서(Swagger + README)
