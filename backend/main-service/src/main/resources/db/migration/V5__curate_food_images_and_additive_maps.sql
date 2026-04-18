-- Curated image URLs for major sample foods
UPDATE food_items
SET image_url = 'https://images.pexels.com/photos/5779368/pexels-photo-5779368.jpeg?auto=compress&cs=tinysrgb&w=1200'
WHERE barcode = '8809900000001';

UPDATE food_items
SET image_url = 'https://images.pexels.com/photos/1453767/pexels-photo-1453767.jpeg?auto=compress&cs=tinysrgb&w=1200'
WHERE barcode = '8809900000002';

UPDATE food_items
SET image_url = 'https://images.pexels.com/photos/4518838/pexels-photo-4518838.jpeg?auto=compress&cs=tinysrgb&w=1200'
WHERE barcode = '8809900000003';

UPDATE food_items
SET image_url = 'https://images.pexels.com/photos/4198019/pexels-photo-4198019.jpeg?auto=compress&cs=tinysrgb&w=1200'
WHERE barcode = '8809900000004';

UPDATE food_items
SET image_url = 'https://images.pexels.com/photos/723198/pexels-photo-723198.jpeg?auto=compress&cs=tinysrgb&w=1200'
WHERE barcode = '8809900000005';

UPDATE food_items
SET image_url = 'https://images.pexels.com/photos/3296279/pexels-photo-3296279.jpeg?auto=compress&cs=tinysrgb&w=1200'
WHERE barcode = '8809900000006';

UPDATE food_items
SET image_url = 'https://images.pexels.com/photos/884600/pexels-photo-884600.jpeg?auto=compress&cs=tinysrgb&w=1200'
WHERE barcode IN ('8809900000008', '8809900000035');

UPDATE food_items
SET image_url = 'https://images.pexels.com/photos/5638732/pexels-photo-5638732.jpeg?auto=compress&cs=tinysrgb&w=1200'
WHERE barcode IN ('8809900000023', '8809900000024');

UPDATE food_items
SET image_url = 'https://images.pexels.com/photos/1639562/pexels-photo-1639562.jpeg?auto=compress&cs=tinysrgb&w=1200'
WHERE barcode = '8809900000011';

UPDATE food_items
SET image_url = 'https://images.pexels.com/photos/2338407/pexels-photo-2338407.jpeg?auto=compress&cs=tinysrgb&w=1200'
WHERE barcode = '8809900000012';

UPDATE food_items
SET image_url = 'https://images.pexels.com/photos/1583884/pexels-photo-1583884.jpeg?auto=compress&cs=tinysrgb&w=1200'
WHERE barcode = '8809900000013';

UPDATE food_items
SET image_url = 'https://images.pexels.com/photos/825661/pexels-photo-825661.jpeg?auto=compress&cs=tinysrgb&w=1200'
WHERE barcode = '8809900000014';

UPDATE food_items
SET image_url = 'https://images.pexels.com/photos/2983101/pexels-photo-2983101.jpeg?auto=compress&cs=tinysrgb&w=1200'
WHERE barcode = '8809900000015';

UPDATE food_items
SET image_url = 'https://images.pexels.com/photos/257816/pexels-photo-257816.jpeg?auto=compress&cs=tinysrgb&w=1200'
WHERE barcode IN ('8809900000019', '8809900000020');

UPDATE food_items
SET image_url = 'https://images.pexels.com/photos/291528/pexels-photo-291528.jpeg?auto=compress&cs=tinysrgb&w=1200'
WHERE barcode IN ('8809900000017', '8809900000018');

-- Strengthen additive mapping for warning presentation
INSERT INTO food_additives_map (food_id, additive_id, is_deleted)
SELECT f.food_id, a.additive_id, FALSE
FROM food_items f
JOIN additives a ON a.name = '아질산나트륨'
WHERE f.barcode IN ('8809900000008', '8809900000014', '8809900000024')
ON CONFLICT (food_id, additive_id) DO UPDATE SET is_deleted = FALSE;

INSERT INTO food_additives_map (food_id, additive_id, is_deleted)
SELECT f.food_id, a.additive_id, FALSE
FROM food_items f
JOIN additives a ON a.name = '아스파탐'
WHERE f.barcode IN ('8809900000015', '8809900000053', '8809999000103')
ON CONFLICT (food_id, additive_id) DO UPDATE SET is_deleted = FALSE;

INSERT INTO food_additives_map (food_id, additive_id, is_deleted)
SELECT f.food_id, a.additive_id, FALSE
FROM food_items f
JOIN additives a ON a.name = 'MSG'
WHERE f.barcode IN ('8809900000008', '8809900000024', '8809900000037')
ON CONFLICT (food_id, additive_id) DO UPDATE SET is_deleted = FALSE;
