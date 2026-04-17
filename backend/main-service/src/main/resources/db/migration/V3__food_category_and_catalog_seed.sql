ALTER TABLE food_items
    ADD COLUMN IF NOT EXISTS category VARCHAR(50) NOT NULL DEFAULT '기타';

CREATE INDEX IF NOT EXISTS idx_food_category ON food_items (category);

UPDATE food_items
SET category = '기타'
WHERE category IS NULL OR BTRIM(category) = '';

UPDATE food_items SET category = '면류' WHERE barcode = '8801073110509';
UPDATE food_items SET category = '건강식' WHERE barcode = '8809999000101';
UPDATE food_items SET category = '유제품' WHERE barcode = '8809999000102';
UPDATE food_items SET category = '간식' WHERE barcode = '8809999000103';

INSERT INTO additives (name, purpose, danger_level, daily_acceptable_intake)
VALUES
    ('아스파탐', '감미료', 4, '40mg/kg/day'),
    ('아질산나트륨', '발색제', 5, '0.07mg/kg/day'),
    ('카라기난', '증점제', 2, 'N/A'),
    ('MSG', '향미증진제', 1, 'N/A')
ON CONFLICT (name) DO NOTHING;

INSERT INTO food_items (name, category, manufacturer, barcode, image_url, calories, carbs, protein, fat, sugars, sodium, is_deleted)
VALUES
    ('삼겹살 구이 1인분', '한식', 'ChemiLog Sample', '8809900000001', NULL, 665.00, 3.00, 39.00, 56.00, 1.00, 520.00, FALSE),
    ('곱창구이 1인분', '한식', 'ChemiLog Sample', '8809900000002', NULL, 520.00, 12.00, 25.00, 39.00, 2.50, 680.00, FALSE),
    ('된장찌개 1인분', '한식', 'ChemiLog Sample', '8809900000003', NULL, 185.00, 14.00, 12.00, 9.00, 4.00, 890.00, FALSE),
    ('김치찌개 1인분', '한식', 'ChemiLog Sample', '8809900000004', NULL, 220.00, 10.00, 15.00, 13.00, 3.00, 980.00, FALSE),
    ('비빔밥 1인분', '한식', 'ChemiLog Sample', '8809900000005', NULL, 560.00, 84.00, 18.00, 16.00, 9.00, 710.00, FALSE),
    ('회덮밥 1인분', '한식', 'ChemiLog Sample', '8809900000006', NULL, 480.00, 72.00, 22.00, 11.00, 8.00, 640.00, FALSE),
    ('제육볶음 1인분', '한식', 'ChemiLog Sample', '8809900000007', NULL, 430.00, 18.00, 29.00, 26.00, 9.00, 760.00, FALSE),
    ('라면 1봉지', '면류', 'ChemiLog Sample', '8809900000008', NULL, 500.00, 77.00, 10.00, 17.00, 5.00, 1800.00, FALSE),
    ('떡볶이 1인분', '분식', 'ChemiLog Sample', '8809900000009', NULL, 380.00, 78.00, 7.00, 5.00, 18.00, 920.00, FALSE),
    ('김밥 1줄', '분식', 'ChemiLog Sample', '8809900000010', NULL, 420.00, 62.00, 12.00, 13.00, 6.00, 700.00, FALSE),
    ('치즈버거 1개', '패스트푸드', 'ChemiLog Sample', '8809900000011', NULL, 450.00, 34.00, 25.00, 24.00, 7.00, 860.00, FALSE),
    ('프라이드치킨 2조각', '패스트푸드', 'ChemiLog Sample', '8809900000012', NULL, 560.00, 26.00, 34.00, 37.00, 0.00, 960.00, FALSE),
    ('감자튀김 미디엄', '패스트푸드', 'ChemiLog Sample', '8809900000013', NULL, 365.00, 48.00, 4.00, 17.00, 0.50, 320.00, FALSE),
    ('페퍼로니 피자 2조각', '패스트푸드', 'ChemiLog Sample', '8809900000014', NULL, 620.00, 64.00, 28.00, 31.00, 8.00, 1180.00, FALSE),
    ('콜라 1캔(355ml)', '음료', 'ChemiLog Sample', '8809900000015', NULL, 150.00, 39.00, 0.00, 0.00, 39.00, 20.00, FALSE),
    ('아이스아메리카노', '음료', 'ChemiLog Sample', '8809900000016', NULL, 10.00, 1.00, 1.00, 0.00, 0.00, 5.00, FALSE),
    ('바닐라 아이스크림 1컵', '디저트', 'ChemiLog Sample', '8809900000017', NULL, 280.00, 31.00, 5.00, 16.00, 27.00, 140.00, FALSE),
    ('초코 도넛 1개', '디저트', 'ChemiLog Sample', '8809900000018', NULL, 320.00, 39.00, 4.00, 18.00, 20.00, 270.00, FALSE),
    ('닭가슴살 샐러드', '건강식', 'ChemiLog Sample', '8809900000019', NULL, 230.00, 14.00, 24.00, 8.00, 4.00, 390.00, FALSE),
    ('연어 샐러드', '건강식', 'ChemiLog Sample', '8809900000020', NULL, 290.00, 11.00, 22.00, 16.00, 3.00, 420.00, FALSE),
    ('오트밀 1그릇', '건강식', 'ChemiLog Sample', '8809900000021', NULL, 190.00, 32.00, 6.00, 4.00, 1.00, 120.00, FALSE),
    ('그릭요거트 무가당', '유제품', 'ChemiLog Sample', '8809900000022', NULL, 95.00, 4.00, 10.00, 4.00, 3.00, 50.00, FALSE),
    ('도시락(일반식)', '간편식', 'ChemiLog Sample', '8809900000023', NULL, 560.00, 74.00, 19.00, 20.00, 8.00, 980.00, FALSE),
    ('컵밥(제육)', '간편식', 'ChemiLog Sample', '8809900000024', NULL, 470.00, 68.00, 14.00, 16.00, 7.00, 890.00, FALSE)
ON CONFLICT (barcode) DO NOTHING;

INSERT INTO food_additives_map (food_id, additive_id, is_deleted)
SELECT f.food_id, a.additive_id, FALSE
FROM food_items f
JOIN additives a ON a.name = '아질산나트륨'
WHERE f.barcode = '8809900000014'
ON CONFLICT (food_id, additive_id) DO NOTHING;

INSERT INTO food_additives_map (food_id, additive_id, is_deleted)
SELECT f.food_id, a.additive_id, FALSE
FROM food_items f
JOIN additives a ON a.name = '아스파탐'
WHERE f.barcode = '8809900000015'
ON CONFLICT (food_id, additive_id) DO NOTHING;

INSERT INTO food_additives_map (food_id, additive_id, is_deleted)
SELECT f.food_id, a.additive_id, FALSE
FROM food_items f
JOIN additives a ON a.name = 'MSG'
WHERE f.barcode IN ('8809900000008', '8809900000024')
ON CONFLICT (food_id, additive_id) DO NOTHING;
