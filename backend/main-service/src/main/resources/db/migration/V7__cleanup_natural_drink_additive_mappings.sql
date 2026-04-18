-- Post-expansion data cleanup: keep naturally additive-free drinks clean
UPDATE food_additives_map fam
SET is_deleted = TRUE
FROM food_items f, additives a
WHERE fam.food_id = f.food_id
  AND fam.additive_id = a.additive_id
  AND f.name IN ('아이스아메리카노', '탄산수 1캔', '망고주스 1잔', '수박주스 1잔')
  AND a.name IN ('아스파탐', '안식향산나트륨');
