SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE reference_user_links;
TRUNCATE TABLE user_links;
TRUNCATE TABLE reference;
TRUNCATE TABLE links;
TRUNCATE TABLE users;
SET FOREIGN_KEY_CHECKS = 1;

-- 1. 유저 생성 (패스워드: password)
INSERT INTO users (user_id, username, password, nickname, email, role_type, status, is_lock, is_social, created_at, updated_at) VALUES
(1, 'user1', '{bcrypt}$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07IxqE7R2NW3qcizaQ', '유저1', 'user1@example.com', 'USER', 'ACTIVE', 0, 0, NOW(), NOW()),
(2, 'user2', '{bcrypt}$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07IxqE7R2NW3qcizaQ', '유저2', 'user2@example.com', 'USER', 'ACTIVE', 0, 0, NOW(), NOW()),
(3, 'user3', '{bcrypt}$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07IxqE7R2NW3qcizaQ', '유저3', 'user3@example.com', 'USER', 'ACTIVE', 0, 0, NOW(), NOW()),
(4, 'user4', '{bcrypt}$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07IxqE7R2NW3qcizaQ', '유저4', 'user4@example.com', 'USER', 'ACTIVE', 0, 0, NOW(), NOW()),
(5, 'user5', '{bcrypt}$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07IxqE7R2NW3qcizaQ', '유저5', 'user5@example.com', 'USER', 'ACTIVE', 0, 0, NOW(), NOW());

-- 2. 링크 생성 (다양한 카테고리와 AI 요약 포함)
INSERT INTO links (link_id, url, url_hash, title, ai_summary, content, public_view_count, created_at, updated_at) VALUES
(1, 'https://n.news.naver.com/mnews/article/001/1', '0ffee85ba40dd218778b89004d87550994864cad11ef9da54876e90e676787fc', '뉴욕증시, 금리 인하 기대감에 상승 마감', '미 연준의 완화적 기조로 뉴욕 증시가 사상 최고치를 경신했습니다.', 'Economy', 612, NOW(), NOW()),
(2, 'https://www.hankyung.com/economy/article/1', '9a6e8a1e106b28f861b11c48ae890d1a5bd8735a6b7d25f0de6b911a0824fabd', '한국 반도체 수출 6개월 연속 흑자', '메모리 반도체 가격 회복과 AI 수요 증가로 실적 개선이 뚜렷합니다.', 'Economy', 938, NOW(), NOW()),
(3, 'https://www.mk.co.kr/news/economy/1', '3232eff05e59e03f627f4073e2f255c0311fea7dfc6e2472a678af53d2827ab3', '부동산 시장, 서울과 지방 양극화 심화', '서울 핵심 지역은 상승세지만 지방은 여전히 침체된 분위기입니다.', 'Economy', 322, NOW(), NOW()),
(4, 'https://finance.naver.com/marketindex/1', 'e7e362184458844f89a5354d2a8f07cc724f68ca9dd94e4287fc9330f9e13668', '원달러 환율 1300원대 등락 지속', '달러 강세가 이어지며 환율이 높은 수준에서 유지되고 있습니다.', 'Economy', 456, NOW(), NOW()),
(11, 'https://www.vogue.co.kr/fashion/trend/1', 'f3c33079a3886796a89915455b591424e8c472c9831f7a422965258d30a506ed', '2024 SS 패션 트렌드 총정리', '이번 시즌은 깔끔한 실루엣과 모노톤의 조화가 돋보입니다.', 'Fashion', 946, NOW(), NOW()),
(12, 'https://www.elle.co.kr/beauty/skin/1', 'a9d69430c2e9fe6505b7b5910ed3ed8f32db177118bec549321b8e27df10a07d', '환절기 피부 장벽을 지키는 5가지 루틴', '보습과 영양 공급을 중심으로 전문가가 추천하는 스킨케어법입니다.', 'Beauty', 410, NOW(), NOW()),
(16, 'https://www.10000recipe.com/recipe/1', '497632803bf0c9354e3d3f070bceb7b56c600e80b647a5e71893e95664d422c1', '자취생 필수, 스팸 김치볶음밥 황금레시피', '많은 사람이 인정한 간단하고 맛있는 볶음밥 비법 레시피입니다.', 'Cooking', 532, NOW(), NOW()),
(19, 'https://www.healthline.com/nutrition/1', '7960791987e189f8249a1ef8d935fe42f6225f934ce894a236ae55f40dd4c389', '간헐적 단식, 16:8 방법의 효과와 주의점', '체중 관리뿐만 아니라 대사 증후군 예방에도 효과적인 단식 방법입니다.', 'Health', 814, NOW(), NOW()),
(21, 'https://ko.wikipedia.org/wiki/Renaissance', '000ede3cceb96783b87a927693ec86ce0203d4edef3d576cb015ceff8cdb2af2', '르네상스, 인간 중심 사상의 부활', '유럽의 근대를 여는 토대가 된 위대한 문화 부흥 운동을 소개합니다.', 'Humanities', 372, NOW(), NOW()),
(23, 'https://www.linkedin.com/pulse/1', '2557ba1c2b8b1134e64c1bcfefe8842f8a737465352af2dbeb5b73c005bf5133', '성공적인 이직을 위한 링크드인 프로필 팁', '취업 준비생이나 이직자를 위한 전문적인 프로필 구성법입니다.', 'Career', 116, NOW(), NOW()),
(25, 'https://ohou.se/projects/1', 'add05764b4b89d98ad86a954832987a16e63efe98312f503642c378bffc02dac', '10평대 원룸, 좁은 공간 넓게 쓰는 배치', '가구 배치와 색감 활용을 통해 공간을 훨씬 넓게 쓰는 노하우입니다.', 'Living', 305, NOW(), NOW()),
(27, 'https://github.com', '996e1f714b08e971ec79e3bea686287e66441f043177999a13dbc546d8fe402a', 'GitHub', '전 세계 개발자들의 협업 성지입니다.', 'Career', 902, NOW(), NOW()),
(28, 'https://velog.io', '95d7f354805661b2126004bbfd419805a32e29f82459236e705209fbd82d7d45', 'Velog', '국내 개발자들이 가장 선호하는 블로그 서비스입니다.', 'Career', 963, NOW(), NOW());

-- 3. 유저별 카테고리 폴더 생성
INSERT INTO reference (reference_id, user_id, title, is_default, is_public, created_at, updated_at, color_code) VALUES
(10, 1, '미지정', 1, 0, NOW(), NOW(), NULL),
(11, 1, '경제/시사', 0, 1, NOW(), NOW(), '#2E7D32'),
(12, 1, '뷰티/패션', 0, 1, NOW(), NOW(), '#C2185B'),
(13, 1, '요리/식품', 0, 1, NOW(), NOW(), '#FBC02D'),
(14, 1, '운동/건강', 0, 1, NOW(), NOW(), '#1976D2'),
(15, 1, '인문/지식', 0, 1, NOW(), NOW(), '#5D4037'),
(16, 1, '직장/커리어', 0, 1, NOW(), NOW(), '#455A64'),
(17, 1, '홈/리빙', 0, 1, NOW(), NOW(), '#0097A7');

-- 4. 유저1의 개인 링크 데이터 삽입 (랜덤 조회수 및 상태 적용)
INSERT INTO user_links (user_link_id, user_id, link_id, status, view_count, why, memo, created_at, updated_at)
SELECT 
    link_id, 
    1, 
    link_id, 
    IF(RAND() > 0.5, 'READ', 'UNREAD'), -- 랜덤하게 상태 지정
    FLOOR(RAND() * 100), -- 0 ~ 99 사이의 랜덤 조회수
    CASE 
        WHEN link_id % 3 = 0 THEN '포트폴리오 참고용'
        WHEN link_id % 3 = 1 THEN '업무 관련 지식'
        ELSE '관심 있는 아티클'
    END,
    CASE 
        WHEN link_id % 2 = 0 THEN '나중에 다시 읽어보기'
        ELSE '중요한 내용 포함됨'
    END,
    NOW(), 
    NOW() 
FROM links;

-- 5. 유저1의 링크들을 카테고리별 폴더에 자동 배정
INSERT INTO reference_user_links (reference_id, user_link_id, created_at, updated_at)
SELECT 
    CASE 
        WHEN l.content = 'Economy' THEN 11
        WHEN l.content IN ('Fashion', 'Beauty') THEN 12
        WHEN l.content = 'Cooking' THEN 13
        WHEN l.content = 'Health' THEN 14
        WHEN l.content = 'Humanities' THEN 15
        WHEN l.content = 'Career' THEN 16
        WHEN l.content = 'Living' THEN 17
        ELSE 10
    END,
    ul.user_link_id, 
    NOW(), 
    NOW()
FROM user_links ul 
JOIN links l ON ul.link_id = l.link_id
WHERE ul.user_id = 1;

SET FOREIGN_KEY_CHECKS = 1;