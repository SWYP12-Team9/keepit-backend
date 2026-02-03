SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- 1. 기존의 모든 테스트 패턴 데이터 삭제 (e% 패턴과 test% 패턴 모두 삭제)
DELETE FROM user_links WHERE link_id IN (SELECT link_id FROM links WHERE url LIKE 'https://news.naver.com/e%' OR url LIKE 'https://news.naver.com/test%');
DELETE FROM links WHERE url LIKE 'https://news.naver.com/e%' OR url LIKE 'https://news.naver.com/test%';

-- 2. 테스트용 사용자 생성 (이미 있다면 무시됨)
INSERT IGNORE INTO users (user_id, username, password, nickname, email, role_type, social_provider_type, is_social, is_lock, status, created_at, updated_at)
VALUES (1, '12345678', 'N/A', '테스터', 'test@example.com', 'USER', 'KAKAO', true, false, 'ACTIVE', NOW(), NOW());

-- 3. 새 목 데이터 20개 생성 (경제7 + 유사2 + 일반11)
INSERT INTO links (url, title, description, ai_summary, created_at, updated_at)
VALUES 
-- [경제/시사] - 7개
('https://news.naver.com/test1', '2026년 글로벌 거시경제 전망', '세계 은행이 발표한 주요국 경제 성장률 및 인플레이션 분석.', '전 세계적인 고금리 기조가 유지되나 하반기부터 완만한 회복세가 기대됩니다.', NOW(), NOW()),
('https://news.naver.com/test2', '미 연준 금리 동결과 시장 반응', '제롬 파월 의장의 기자회견 및 향후 금리 인하 시점 예측.', '인플레이션 둔화 속도가 예상보다 느려 금리 동결 결정이 내려졌습니다.', NOW(), NOW()),
('https://news.naver.com/test3', '한국 반도체 수출 실적 역대 최고', '메모리 반도체 및 HBM 수요 급증에 따른 무역 수지 흑자 기록.', 'AI 서버 수요 증가로 인해 한국산 반도체의 수출 단가가 크게 상승했습니다.', NOW(), NOW()),
('https://news.naver.com/test4', '소비자 물가 지수(CPI) 상승 원인 분석', '유가 및 식료품 가격 상승이 생활 물가에 미치는 영향.', '생활 물가가 3%대 상승률을 보이며 가계 소비 심리가 위축되고 있습니다.', NOW(), NOW()),
('https://news.naver.com/test5', '디지털 화폐(CBDC) 도입의 경제적 파급효과', '중앙은행 발행 디지털 화폐가 금융 시스템에 미치는 변화.', '결제 효율성 증대와 투명성 확보가 기대됩니다.', NOW(), NOW()),
('https://news.naver.com/test6', '청년 고용 시장 현황 및 실업률 지표', '최근 취업 준비생 비중 증가와 업종별 채용 트렌드.', '정보기술 및 바이오 분야 채용은 늘었으나 전통 제조업은 위축된 모습입니다.', NOW(), NOW()),
('https://news.naver.com/test7', '한-EU 자유무역협정(FTA) 개정 논의', '디지털 통상 및 환경 규제 준수를 위한 협정문 수정 사항.', '탄소 국경세 도입에 따른 국내 기업들의 대응 방안이 주요 쟁점입니다.', NOW(), NOW()),

-- [다른 분야지만 경제틱한거] - 2개
('https://news.naver.com/test8', 'AI 반도체 기술 패권 경쟁의 서막', '엔비디아를 추격하는 빅테크 기업들의 자체 칩 개발 현황.', '기술 주도권 확보가 곧 기업의 생존과 수익성으로 직결되는 시대입니다.', NOW(), NOW()),
('https://news.naver.com/test9', '도심 주택 공급 확대를 위한 부동산 대책', '재건축 규제 완화 및 용적률 상향 정책이 시장에 미치는 영향.', '정부의 공급 활성화 대책으로 인해 노후 단지들에 대한 기대감이 상승 중입니다.', NOW(), NOW()),

-- [완전 다른 분야] - 11개
('https://news.naver.com/test10', '2026년 봄/여름 메이크업 트렌드', '자연스러운 피부 표현과 파스텔 톤 컬러의 유행 예고.', '맑고 투명한 피부 광택을 살리는 네추럴 메이크업이 유행할 전망입니다.', NOW(), NOW()),
('https://news.naver.com/test11', '친환경 소재를 활용한 지속 가능한 패션', '비건 가죽 및 재생 폴리에스터를 사용한 브랜드 사례.', '패션 산업의 환경 오염 문제를 해결하기 위한 혁신적인 소재들이 등장하고 있습니다.', NOW(), NOW()),
('https://news.naver.com/test12', '바쁜 직장인을 위한 15분 간편 레시피', '에어프라이어와 전자레인지를 활용한 건강한 한 끼 요리.', '퇴근 후에도 부담 없이 요리할 수 있는 쉽고 맛있는 식단을 소개합니다.', NOW(), NOW()),
('https://news.naver.com/test13', '발효 음식의 효능과 김치 문화의 세계화', '유산균이 풍부한 한국 전통 음식이 면역력에 미치는 영향.', '김치가 건강식으로 인정받으며 전 세계적으로 소비량이 급증하고 있습니다.', NOW(), NOW()),
('https://news.naver.com/test14', '효과적인 다이어트를 위한 HIIT 운동법', '짧은 시간 동안 고강도로 진행하는 인터벌 트레이닝 가이드.', '바쁜 일상 속에서 최대의 칼로리 소모 효과를 볼 수 있는 운동 루틴입니다.', NOW(), NOW()),
('https://news.naver.com/test15', '현대인의 스트레스 관리와 명상의 힘', '불안감을 줄이고 집중력을 높이는 데일리 명상 기법.', '하루 10분의 명상만으로도 심리적 안정을 찾을 수 있습니다.', NOW(), NOW()),
('https://news.naver.com/test16', '고대 로마 제국의 부흥과 멸망의 역사', '로마의 정치 체제와 군사력이 세계사에 남긴 유산 분석.', '로마의 흥망성쇠를 통해 현대 사회의 거버넌스 문제를 다시금 고찰해 봅니다.', NOW(), NOW()),
('https://news.naver.com/test17', '현대 철학 입문: 실존주의란 무엇인가', '사르트르와 카뮈를 중심으로 본 인간 존재의 의미 탐구.', '세상에 던져진 인간이 스스로 가치를 창조해 나가는 과정을 설명합니다.', NOW(), NOW()),
('https://news.naver.com/test18', '직장 내 효율적인 커뮤니케이션 기술', '협업을 원활하게 하는 경청과 명확한 의사전달 방법.', '팀워크를 높이고 오해를 줄이는 비즈니스 대화법의 핵심 원칙을 공유합니다.', NOW(), NOW()),
('https://news.naver.com/test19', '작은 공간을 넓게 쓰는 스마트 홈 인테리어', '미니멀리즘 가구 배치와 조명을 활용한 공간 확장 팁.', '작은 평수의 집도 수납 정리를 통해 넓게 활용할 수 있습니다.', NOW(), NOW()),
('https://news.naver.com/test20', '초보자를 위한 반려식물 가드닝 기초', '햇빛 조절과 물 주기 등 식물 키우기에 필요한 핵심 지식.', '집안 분위기를 살리고 공기를 정화해 주는 반려식물 관리법을 알아봅니다.', NOW(), NOW());

-- 4. UserLink 등록
INSERT INTO user_links (user_id, link_id, status, is_public, view_count, created_at, updated_at)
SELECT 1, link_id, 'UNREAD', true, 0, NOW(), NOW()
FROM links
WHERE url LIKE 'https://news.naver.com/test%';
