INSERT INTO users (email, password_hash, role, status, health_profile, is_deleted)
VALUES
    (
        'admin@chemilog.com',
        '$2b$12$KI9.qdN5Qi/4H5b3LIk8Bu5r8id2mEkl8Wec5D.wPkmGpRO35jWxu',
        'ADMIN',
        'ACTIVE',
        '{"goal":"MAINTAIN","strictness":"MEDIUM","allergies":[]}'::jsonb,
        FALSE
    ),
    (
        'user@chemilog.com',
        '$2b$12$PsQVhtx2wz9Xs1s2NROouOAf0DfJ8lsPw4iPECsfCurIn4hlIzkj.',
        'USER',
        'ACTIVE',
        '{"goal":"FAT_LOSS","strictness":"MEDIUM","allergies":["SHRIMP"]}'::jsonb,
        FALSE
    ),
    (
        'premium@chemilog.com',
        '$2b$12$oKNSkwTe9xwIVIRZSYhnWec9d9p.e56lGZPPLMnflayeUEzt1WHaK',
        'PREMIUM',
        'ACTIVE',
        '{"goal":"BULK_UP","strictness":"HIGH","allergies":["PEANUT"]}'::jsonb,
        FALSE
    )
ON CONFLICT (email) DO NOTHING;

INSERT INTO additives (name, purpose, danger_level, daily_acceptable_intake)
VALUES
    ('아스파탐', '감미료', 4, '40mg/kg/day'),
    ('아질산나트륨', '발색제', 5, '0.07mg/kg/day'),
    ('소르빈산칼륨', '보존료', 2, '25mg/kg/day'),
    ('구아검', '증점제', 1, 'N/A')
ON CONFLICT (name) DO NOTHING;

INSERT INTO food_items (name, manufacturer, barcode, image_url, calories, carbs, protein, fat, sugars, sodium, is_deleted)
VALUES
    ('불닭볶음면', '삼양식품', '8801073110509', '/minio/images/buldak.webp', 530.00, 84.00, 12.00, 16.00, 8.00, 1280.00, FALSE),
    ('닭가슴살 스테이크', '헬시푸드', '8809999000101', '/minio/images/chicken_steak.webp', 165.00, 4.00, 23.00, 4.00, 1.50, 320.00, FALSE),
    ('그릭요거트 무가당', '데일리밀크', '8809999000102', '/minio/images/greek_yogurt.webp', 92.00, 4.20, 10.00, 4.00, 3.80, 55.00, FALSE),
    ('프로틴 바 초코', '피트스낵', '8809999000103', '/minio/images/protein_bar.webp', 210.00, 18.00, 20.00, 6.00, 6.00, 170.00, FALSE)
ON CONFLICT (barcode) DO NOTHING;

INSERT INTO food_additives_map (food_id, additive_id, is_deleted)
SELECT f.food_id, a.additive_id, FALSE
FROM food_items f
JOIN additives a ON a.name = '아스파탐'
WHERE f.name = '프로틴 바 초코'
ON CONFLICT (food_id, additive_id) DO NOTHING;

INSERT INTO food_additives_map (food_id, additive_id, is_deleted)
SELECT f.food_id, a.additive_id, FALSE
FROM food_items f
JOIN additives a ON a.name = '아질산나트륨'
WHERE f.name = '불닭볶음면'
ON CONFLICT (food_id, additive_id) DO NOTHING;

INSERT INTO food_additives_map (food_id, additive_id, is_deleted)
SELECT f.food_id, a.additive_id, FALSE
FROM food_items f
JOIN additives a ON a.name = '소르빈산칼륨'
WHERE f.name = '불닭볶음면'
ON CONFLICT (food_id, additive_id) DO NOTHING;

INSERT INTO food_additives_map (food_id, additive_id, is_deleted)
SELECT f.food_id, a.additive_id, FALSE
FROM food_items f
JOIN additives a ON a.name = '구아검'
WHERE f.name = '그릭요거트 무가당'
ON CONFLICT (food_id, additive_id) DO NOTHING;
