SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- 1. 기존 테스트 데이터 삭제
DELETE FROM user_links WHERE link_id IN (SELECT link_id FROM links WHERE url LIKE 'https://news.naver.com/v2%');
DELETE FROM links WHERE url LIKE 'https://news.naver.com/v2%';

-- 2. 테스트용 사용자 2명 생성 (이미 있다면 무시됨)
INSERT IGNORE INTO users (user_id, username, password, nickname, email, role_type, social_provider_type, is_social, is_lock, status, created_at, updated_at)
VALUES 
(1, 'user1_test', 'N/A', '테스터1', 'user1@test.com', 'USER', 'KAKAO', true, false, 'ACTIVE', NOW(), NOW()),
(2, 'user2_test', 'N/A', '테스터2', 'user2@test.com', 'USER', 'KAKAO', true, false, 'ACTIVE', NOW(), NOW());

-- 3. 검색 고도화 테스트를 위한 목 데이터 생성
INSERT INTO links (url, title, description, ai_summary, created_at, updated_at)
VALUES 
-- '명상' 키워드 테스트
('https://news.naver.com/v2-1', '스트레스 해소법', '현대인의 정신 건강 가이드', '명상은 뇌의 휴식을 돕고 스트레스를 완화합니다.', NOW(), NOW()),
-- '생산성' 키워드 테스트 (why/memo에 들어갈 예정)
('https://news.naver.com/v2-2', '자바 스프링 가이드', '백엔드 개발자를 위한 자바 실무', '스프링 부트의 핵심 기능을 설명합니다.', NOW(), NOW()),
-- '재테크' 키워드 테스트
('https://news.naver.com/v2-3', '2026 주식 시장', '주요 업종별 전망', '반도체와 2차전지 섹터가 유망할 것으로 보입니다.', NOW(), NOW());

-- 4. UserLink 등록 (why와 memo에 특수 키워드 삽입)
-- 유저 1의 링크
INSERT INTO user_links (user_id, link_id, status, is_public, why, memo, view_count, created_at, updated_at)
VALUES 
(1, (SELECT link_id FROM links WHERE url = 'https://news.naver.com/v2-1'), 'UNREAD', true, '정신 수양을 위해', '매일 아침 10분씩 실천하자', 0, NOW(), NOW()),
(1, (SELECT link_id FROM links WHERE url = 'https://news.naver.com/v2-2'), 'UNREAD', true, '생산성 향상을 위한 필수 기술', '회사 프로젝트에 적용해보기', 0, NOW(), NOW());

-- 유저 2의 링크 (유저 1의 검색 결과에 나와야 함)
INSERT INTO user_links (user_id, link_id, status, is_public, why, memo, view_count, created_at, updated_at)
VALUES 
(2, (SELECT link_id FROM links WHERE url = 'https://news.naver.com/v2-3'), 'UNREAD', true, '노후 자금 준비', '분할 매수로 접근하기. 재테크 공부용', 0, NOW(), NOW());
