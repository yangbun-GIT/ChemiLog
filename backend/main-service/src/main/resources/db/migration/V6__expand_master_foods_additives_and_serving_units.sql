-- Serving unit normalization and master data expansion (Phase UX/Content upgrade)
UPDATE food_items SET name = '프라이드치킨 1인분' WHERE name = '프라이드치킨 2조각';
UPDATE food_items SET name = '페퍼로니 피자 1인분' WHERE name = '페퍼로니 피자 2조각';
UPDATE food_items SET name = '초밥 1인분' WHERE name = '초밥 10pcs';
UPDATE food_items SET name = '콜라 1캔' WHERE name = '콜라 1캔(355ml)';
UPDATE food_items SET name = '치킨텐더 1인분' WHERE name = '치킨텐더 5조각';

INSERT INTO additives (name, purpose, danger_level, daily_acceptable_intake) VALUES
('수크랄로스','감미료',4,'15mg/kg/day'),
('사카린나트륨','감미료',4,'5mg/kg/day'),
('아세설팜칼륨','감미료',4,'15mg/kg/day'),
('네오탐','감미료',4,'2mg/kg/day'),
('스테비올배당체','감미료',2,'4mg/kg/day'),
('안식향산나트륨','보존료',4,'5mg/kg/day'),
('안식향산칼륨','보존료',4,'5mg/kg/day'),
('소르빈산','보존료',3,'25mg/kg/day'),
('프로피온산칼슘','보존료',3,'N/A'),
('질산나트륨','발색제',5,'3.7mg/kg/day'),
('아질산칼륨','발색제',5,'0.07mg/kg/day'),
('파라옥시안식향산메틸','보존료',4,'N/A'),
('파라옥시안식향산프로필','보존료',4,'N/A'),
('BHA','산화방지제',4,'0.5mg/kg/day'),
('BHT','산화방지제',4,'0.3mg/kg/day'),
('TBHQ','산화방지제',4,'0.7mg/kg/day'),
('황색4호','착색료',4,'7.5mg/kg/day'),
('황색5호','착색료',4,'5mg/kg/day'),
('적색40호','착색료',4,'7mg/kg/day'),
('청색1호','착색료',4,'12mg/kg/day'),
('캐러멜색소','착색료',3,'N/A'),
('코치닐추출색소','착색료',3,'N/A'),
('홍국색소','착색료',2,'N/A'),
('이산화티타늄','착색료',5,'N/A'),
('이산화황','보존료',4,'0.7mg/kg/day'),
('아황산나트륨','보존료',4,'0.7mg/kg/day'),
('아황산수소나트륨','보존료',4,'0.7mg/kg/day'),
('폴리인산나트륨','산도조절제',3,'70mg/kg/day'),
('피로인산나트륨','산도조절제',3,'70mg/kg/day'),
('인산염','산도조절제',3,'70mg/kg/day'),
('잔탄검','증점제',2,'N/A'),
('알긴산나트륨','증점제',2,'N/A'),
('CMC','증점제',3,'N/A'),
('폴리소르베이트80','유화제',3,'25mg/kg/day'),
('글리세린지방산에스테르','유화제',2,'N/A'),
('레시틴','유화제',1,'N/A'),
('산탄검','증점제',2,'N/A'),
('빙초산','산도조절제',2,'N/A'),
('구연산','산도조절제',1,'N/A'),
('구연산삼나트륨','산도조절제',2,'N/A'),
('젖산','산도조절제',1,'N/A'),
('젖산칼슘','강화제',1,'N/A'),
('염화암모늄','산도조절제',3,'N/A'),
('합성착향료','향미증진제',3,'N/A'),
('인공향료','향미증진제',3,'N/A'),
('말토덱스트린','충전제',2,'N/A'),
('효모추출물','향미증진제',2,'N/A'),
('카라멜향','착향료',2,'N/A'),
('포도당시럽','감미료',2,'N/A'),
('과당','감미료',3,'N/A') ON CONFLICT (name) DO NOTHING;

INSERT INTO food_items (name, category, manufacturer, barcode, image_url, calories, carbs, protein, fat, sugars, sodium, is_deleted) VALUES
('김치볶음밥 1인분','한식','ChemiLog Data','8809900010001',NULL,560.00,63,28,21.8,9.4,779.2,FALSE),
('오므라이스 1인분','한식','ChemiLog Data','8809900010002',NULL,620.00,69.8,31,24.1,10.5,828.4,FALSE),
('닭볶음탕 1인분','한식','ChemiLog Data','8809900010003',NULL,480.00,54,24,18.7,8.1,713.6,FALSE),
('부대찌개 1인분','한식','ChemiLog Data','8809900010004',NULL,520.00,58.5,26,20.2,8.8,746.4,FALSE),
('청국장 1인분','한식','ChemiLog Data','8809900010005',NULL,280.00,31.5,14,10.9,4.7,549.6,FALSE),
('육개장 1인분','한식','ChemiLog Data','8809900010006',NULL,330.00,37.1,16.5,12.8,5.6,590.6,FALSE),
('콩나물국밥 1인분','한식','ChemiLog Data','8809900010007',NULL,420.00,47.2,21,16.3,7.1,664.4,FALSE),
('순대국밥 1인분','한식','ChemiLog Data','8809900010008',NULL,610.00,68.6,30.5,23.7,10.3,820.2,FALSE),
('갈비찜 1인분','한식','ChemiLog Data','8809900010009',NULL,540.00,60.8,27,21,9.1,762.8,FALSE),
('잡채 1인분','한식','ChemiLog Data','8809900010010',NULL,390.00,43.9,19.5,15.2,6.6,639.8,FALSE),
('감자탕 1인분','한식','ChemiLog Data','8809900010011',NULL,590.00,66.4,29.5,22.9,10,803.8,FALSE),
('닭개장 1인분','한식','ChemiLog Data','8809900010012',NULL,360.00,40.5,18,14,6.1,615.2,FALSE),
('생선구이 정식 1인분','한식','ChemiLog Data','8809900010013',NULL,510.00,57.4,25.5,19.8,8.6,738.2,FALSE),
('제육덮밥 1인분','한식','ChemiLog Data','8809900010014',NULL,670.00,75.4,33.5,26.1,11.3,869.4,FALSE),
('비빔국수 1인분','면류','ChemiLog Data','8809900010015',NULL,540.00,75.6,21.6,16.8,9.1,876.8,FALSE),
('잔치국수 1인분','면류','ChemiLog Data','8809900010016',NULL,430.00,60.2,17.2,13.4,7.2,775.6,FALSE),
('칼국수 1인분','면류','ChemiLog Data','8809900010017',NULL,590.00,82.6,23.6,18.4,9.9,922.8,FALSE),
('메밀소바 1인분','면류','ChemiLog Data','8809900010018',NULL,510.00,71.4,20.4,15.9,8.6,849.2,FALSE),
('탄탄면 1인분','면류','ChemiLog Data','8809900010019',NULL,720.00,100.8,28.8,22.4,12.1,1042.4,FALSE),
('크림파스타 1인분','면류','ChemiLog Data','8809900010020',NULL,780.00,109.2,31.2,24.3,13.1,1097.6,FALSE),
('토마토파스타 1인분','면류','ChemiLog Data','8809900010021',NULL,650.00,91,26,20.2,10.9,978,FALSE),
('알리오올리오 1인분','면류','ChemiLog Data','8809900010022',NULL,700.00,98,28,21.8,11.8,1024,FALSE),
('라볶이 1인분','면류','ChemiLog Data','8809900010023',NULL,660.00,92.4,26.4,20.5,11.1,987.2,FALSE),
('냉모밀 1인분','면류','ChemiLog Data','8809900010024',NULL,470.00,65.8,18.8,14.6,7.9,812.4,FALSE),
('유니짜장 1인분','중식','ChemiLog Data','8809900010025',NULL,760.00,87.4,34.2,30.4,14,1149.6,FALSE),
('차돌짬뽕 1인분','중식','ChemiLog Data','8809900010026',NULL,780.00,89.7,35.1,31.2,14.4,1168.8,FALSE),
('깐풍기 1인분','중식','ChemiLog Data','8809900010027',NULL,640.00,73.6,28.8,25.6,11.8,1034.4,FALSE),
('유산슬 1인분','중식','ChemiLog Data','8809900010028',NULL,490.00,56.4,22,19.6,9,890.4,FALSE),
('마파두부 1인분','중식','ChemiLog Data','8809900010029',NULL,430.00,49.4,19.3,17.2,7.9,832.8,FALSE),
('양장피 1인분','중식','ChemiLog Data','8809900010030',NULL,520.00,59.8,23.4,20.8,9.6,919.2,FALSE),
('고추잡채 1인분','중식','ChemiLog Data','8809900010031',NULL,450.00,51.8,20.2,18,8.3,852,FALSE),
('군만두 1인분','중식','ChemiLog Data','8809900010032',NULL,390.00,44.8,17.6,15.6,7.2,794.4,FALSE),
('볶음짬뽕 1인분','중식','ChemiLog Data','8809900010033',NULL,710.00,81.6,32,28.4,13.1,1101.6,FALSE),
('잡채밥 1인분','중식','ChemiLog Data','8809900010034',NULL,690.00,79.4,31,27.6,12.7,1082.4,FALSE),
('가츠동 1인분','일식','ChemiLog Data','8809900010035',NULL,690.00,82.8,34.5,24.5,11.6,925.8,FALSE),
('우나기덮밥 1인분','일식','ChemiLog Data','8809900010036',NULL,720.00,86.4,36,25.6,12.1,950.4,FALSE),
('사케동 1인분','일식','ChemiLog Data','8809900010037',NULL,560.00,67.2,28,19.9,9.4,819.2,FALSE),
('돈코츠라멘 1인분','일식','ChemiLog Data','8809900010038',NULL,760.00,91.2,38,27,12.8,983.2,FALSE),
('마제소바 1인분','일식','ChemiLog Data','8809900010039',NULL,710.00,85.2,35.5,25.2,11.9,942.2,FALSE),
('야키소바 1인분','일식','ChemiLog Data','8809900010040',NULL,640.00,76.8,32,22.8,10.8,884.8,FALSE),
('일본식카레라이스 1인분','일식','ChemiLog Data','8809900010041',NULL,730.00,87.6,36.5,26,12.3,958.6,FALSE),
('오야코동 1인분','일식','ChemiLog Data','8809900010042',NULL,620.00,74.4,31,22,10.4,868.4,FALSE),
('텐동 1인분','일식','ChemiLog Data','8809900010043',NULL,770.00,92.4,38.5,27.4,12.9,991.4,FALSE),
('규카츠 1인분','일식','ChemiLog Data','8809900010044',NULL,680.00,81.6,34,24.2,11.4,917.6,FALSE),
('치킨버거 1개','패스트푸드','ChemiLog Data','8809900010045',NULL,520.00,49.4,26,24.3,10.9,914,FALSE),
('더블치즈버거 1개','패스트푸드','ChemiLog Data','8809900010046',NULL,720.00,68.4,36,33.6,15,1104,FALSE),
('새우버거 1개','패스트푸드','ChemiLog Data','8809900010047',NULL,490.00,46.6,24.5,22.9,10.3,885.5,FALSE),
('핫윙 1인분','패스트푸드','ChemiLog Data','8809900010048',NULL,430.00,40.8,21.5,20.1,9,828.5,FALSE),
('치킨너겟 1인분','패스트푸드','ChemiLog Data','8809900010049',NULL,390.00,37,19.5,18.2,8.1,790.5,FALSE),
('콘치즈핫도그 1개','패스트푸드','ChemiLog Data','8809900010050',NULL,410.00,39,20.5,19.1,8.6,809.5,FALSE),
('불고기피자 1인분','패스트푸드','ChemiLog Data','8809900010051',NULL,680.00,64.6,34,31.7,14.2,1066,FALSE),
('콤비네이션피자 1인분','패스트푸드','ChemiLog Data','8809900010052',NULL,710.00,67.4,35.5,33.1,14.8,1094.5,FALSE),
('핫도그세트 1인분','패스트푸드','ChemiLog Data','8809900010053',NULL,650.00,61.8,32.5,30.3,13.6,1037.5,FALSE),
('버팔로윙 1인분','패스트푸드','ChemiLog Data','8809900010054',NULL,470.00,44.6,23.5,21.9,9.8,866.5,FALSE),
('참치김밥 1줄','간편식','ChemiLog Data','8809900010055',NULL,440.00,55,19.8,15.6,9.9,756,FALSE),
('불고기김밥 1줄','간편식','ChemiLog Data','8809900010056',NULL,480.00,60,21.6,17.1,10.8,792,FALSE),
('참치마요 삼각김밥 1개','간편식','ChemiLog Data','8809900010057',NULL,210.00,26.2,9.4,7.5,4.7,549,FALSE),
('즉석카레덮밥 1인분','간편식','ChemiLog Data','8809900010058',NULL,590.00,73.8,26.6,21,13.3,891,FALSE),
('컵라면 매운맛 1개','간편식','ChemiLog Data','8809900010059',NULL,470.00,58.8,21.2,16.7,10.6,783,FALSE),
('냉동볶음밥 1인분','간편식','ChemiLog Data','8809900010060',NULL,530.00,66.2,23.8,18.8,11.9,837,FALSE),
('닭가슴살 도시락 1팩','간편식','ChemiLog Data','8809900010061',NULL,420.00,52.5,18.9,14.9,9.4,738,FALSE),
('햄치즈 샌드위치 1개','간편식','ChemiLog Data','8809900010062',NULL,390.00,48.8,17.6,13.9,8.8,711,FALSE),
('치킨마요 덮밥 1인분','간편식','ChemiLog Data','8809900010063',NULL,640.00,80,28.8,22.8,14.4,936,FALSE),
('치즈김밥 1줄','간편식','ChemiLog Data','8809900010064',NULL,450.00,56.2,20.2,16,10.1,765,FALSE),
('퀴노아 샐러드 1인분','건강식','ChemiLog Data','8809900010065',NULL,310.00,27.1,31,8.6,5.4,468,FALSE),
('두부 샐러드 1인분','건강식','ChemiLog Data','8809900010066',NULL,260.00,22.8,26,7.2,4.6,428,FALSE),
('병아리콩 샐러드 1인분','건강식','ChemiLog Data','8809900010067',NULL,340.00,29.7,34,9.4,5.9,492,FALSE),
('현미 닭죽 1인분','건강식','ChemiLog Data','8809900010068',NULL,300.00,26.2,30,8.3,5.2,460,FALSE),
('소고기 포케 1인분','건강식','ChemiLog Data','8809900010069',NULL,520.00,45.5,52,14.4,9.1,636,FALSE),
('닭가슴살볼 1인분','건강식','ChemiLog Data','8809900010070',NULL,280.00,24.5,28,7.8,4.9,444,FALSE),
('아보카도 샐러드 1인분','건강식','ChemiLog Data','8809900010071',NULL,290.00,25.4,29,8.1,5.1,452,FALSE),
('연두부 비빔볼 1인분','건강식','ChemiLog Data','8809900010072',NULL,240.00,21,24,6.7,4.2,412,FALSE),
('단호박 샐러드 1인분','건강식','ChemiLog Data','8809900010073',NULL,270.00,23.6,27,7.5,4.7,436,FALSE),
('그릴드치킨볼 1인분','건강식','ChemiLog Data','8809900010074',NULL,360.00,31.5,36,10,6.3,508,FALSE),
('사이다 1캔','음료','ChemiLog Data','8809900010075',NULL,150.00,33.8,1.1,1.2,28.7,53,FALSE),
('에너지드링크 1캔','음료','ChemiLog Data','8809900010076',NULL,210.00,47.2,1.6,1.6,40.1,66.2,FALSE),
('이온음료 1병','음료','ChemiLog Data','8809900010077',NULL,120.00,27,0.9,0.9,23,46.4,FALSE),
('레몬에이드 1잔','음료','ChemiLog Data','8809900010078',NULL,190.00,42.8,1.4,1.5,36.4,61.8,FALSE),
('복숭아 아이스티 1잔','음료','ChemiLog Data','8809900010079',NULL,170.00,38.2,1.3,1.3,32.5,57.4,FALSE),
('녹차라떼 1잔','음료','ChemiLog Data','8809900010080',NULL,230.00,51.8,1.7,1.8,44,70.6,FALSE),
('바닐라라떼 1잔','음료','ChemiLog Data','8809900010081',NULL,240.00,54,1.8,1.9,45.9,72.8,FALSE),
('딸기스무디 1잔','음료','ChemiLog Data','8809900010082',NULL,280.00,63,2.1,2.2,53.6,81.6,FALSE),
('요구르트음료 1병','음료','ChemiLog Data','8809900010083',NULL,140.00,31.5,1,1.1,26.8,50.8,FALSE),
('탄산수 1캔','음료','ChemiLog Data','8809900010084',NULL,5.00,1.1,0,0,0.9,21.1,FALSE),
('카라멜마끼아또 1잔','음료','ChemiLog Data','8809900010085',NULL,260.00,58.5,2,2,49.7,77.2,FALSE),
('초코라떼 1잔','음료','ChemiLog Data','8809900010086',NULL,290.00,65.2,2.2,2.3,55.4,83.8,FALSE),
('망고주스 1잔','음료','ChemiLog Data','8809900010087',NULL,180.00,40.5,1.3,1.4,34.4,59.6,FALSE),
('수박주스 1잔','음료','ChemiLog Data','8809900010088',NULL,160.00,36,1.2,1.2,30.6,55.2,FALSE),
('바나나우유 1팩','음료','ChemiLog Data','8809900010089',NULL,210.00,47.2,1.6,1.6,40.1,66.2,FALSE),
('밀크쉐이크 1잔','음료','ChemiLog Data','8809900010090',NULL,380.00,85.5,2.8,3,72.7,103.6,FALSE),
('크루아상 1개','디저트','ChemiLog Data','8809900010091',NULL,320.00,44,6.4,13.2,24.2,224,FALSE),
('마카롱 1개','디저트','ChemiLog Data','8809900010092',NULL,110.00,15.1,2.2,4.5,8.3,129.5,FALSE),
('브라우니 1조각','디저트','ChemiLog Data','8809900010093',NULL,290.00,39.9,5.8,11.9,21.9,210.5,FALSE),
('와플 1인분','디저트','ChemiLog Data','8809900010094',NULL,430.00,59.1,8.6,17.7,32.5,273.5,FALSE),
('팬케이크 1인분','디저트','ChemiLog Data','8809900010095',NULL,410.00,56.4,8.2,16.9,31,264.5,FALSE),
('초코케이크 1조각','디저트','ChemiLog Data','8809900010096',NULL,460.00,63.3,9.2,18.9,34.8,287,FALSE),
('딸기케이크 1조각','디저트','ChemiLog Data','8809900010097',NULL,390.00,53.6,7.8,16,29.5,255.5,FALSE),
('쿠키 1개','디저트','ChemiLog Data','8809900010098',NULL,160.00,22,3.2,6.6,12.1,152,FALSE),
('시나몬롤 1개','디저트','ChemiLog Data','8809900010099',NULL,390.00,53.6,7.8,16,29.5,255.5,FALSE),
('츄러스 1개','디저트','ChemiLog Data','8809900010100',NULL,250.00,34.4,5,10.3,18.9,192.5,FALSE),
('푸딩 1개','디저트','ChemiLog Data','8809900010101',NULL,180.00,24.8,3.6,7.4,13.6,161,FALSE),
('모찌 1개','디저트','ChemiLog Data','8809900010102',NULL,150.00,20.6,3,6.2,11.3,147.5,FALSE),
('순대 1인분','분식','ChemiLog Data','8809900010103',NULL,360.00,46.8,12.6,13.6,9.4,702,FALSE),
('튀김모둠 1인분','분식','ChemiLog Data','8809900010104',NULL,470.00,61.1,16.5,17.8,12.2,806.5,FALSE),
('김말이튀김 1인분','분식','ChemiLog Data','8809900010105',NULL,320.00,41.6,11.2,12.1,8.3,664,FALSE),
('라면떡볶이 1인분','분식','ChemiLog Data','8809900010106',NULL,640.00,83.2,22.4,24.2,16.6,968,FALSE),
('어묵탕 1인분','분식','ChemiLog Data','8809900010107',NULL,220.00,28.6,7.7,8.3,5.7,569,FALSE),
('치즈떡볶이 1인분','분식','ChemiLog Data','8809900010108',NULL,590.00,76.7,20.7,22.3,15.3,920.5,FALSE),
('컵떡볶이 1인분','분식','ChemiLog Data','8809900010109',NULL,510.00,66.3,17.8,19.3,13.3,844.5,FALSE),
('핫바 1개','분식','ChemiLog Data','8809900010110',NULL,180.00,23.4,6.3,6.8,4.7,531,FALSE),
('순대볶음 1인분','분식','ChemiLog Data','8809900010111',NULL,520.00,67.6,18.2,19.6,13.5,854,FALSE),
('닭꼬치 1개','분식','ChemiLog Data','8809900010112',NULL,230.00,29.9,8,8.7,6,578.5,FALSE),
('계란말이김밥 1줄','분식','ChemiLog Data','8809900010113',NULL,430.00,55.9,15,16.2,11.2,768.5,FALSE),
('치즈볼 1인분','분식','ChemiLog Data','8809900010114',NULL,340.00,44.2,11.9,12.8,8.8,683,FALSE) ON CONFLICT (barcode) DO NOTHING;

-- High-risk additive mapping for processed foods (warning label quality)
INSERT INTO food_additives_map (food_id, additive_id, is_deleted)
SELECT f.food_id, a.additive_id, FALSE
FROM food_items f
JOIN additives a ON a.name = '아질산나트륨'
WHERE f.category IN ('패스트푸드','간편식','면류')
ON CONFLICT (food_id, additive_id) DO UPDATE SET is_deleted = FALSE;

INSERT INTO food_additives_map (food_id, additive_id, is_deleted)
SELECT f.food_id, a.additive_id, FALSE
FROM food_items f
JOIN additives a ON a.name = '아스파탐'
WHERE f.category IN ('음료','디저트')
ON CONFLICT (food_id, additive_id) DO UPDATE SET is_deleted = FALSE;

INSERT INTO food_additives_map (food_id, additive_id, is_deleted)
SELECT f.food_id, a.additive_id, FALSE
FROM food_items f
JOIN additives a ON a.name = '안식향산나트륨'
WHERE f.category IN ('음료','간편식','디저트')
ON CONFLICT (food_id, additive_id) DO UPDATE SET is_deleted = FALSE;

INSERT INTO food_additives_map (food_id, additive_id, is_deleted)
SELECT f.food_id, a.additive_id, FALSE
FROM food_items f
JOIN additives a ON a.name = '황색5호'
WHERE f.category IN ('디저트','분식')
ON CONFLICT (food_id, additive_id) DO UPDATE SET is_deleted = FALSE;

INSERT INTO food_additives_map (food_id, additive_id, is_deleted)
SELECT f.food_id, a.additive_id, FALSE
FROM food_items f
JOIN additives a ON a.name = '폴리인산나트륨'
WHERE f.category IN ('패스트푸드','간편식')
ON CONFLICT (food_id, additive_id) DO UPDATE SET is_deleted = FALSE;
