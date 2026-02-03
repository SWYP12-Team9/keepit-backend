-- ============================================================
-- 2단계: MySQL Full-Text Index 마이그레이션
-- ============================================================
--
-- 이 스크립트는 링크 검색 성능 향상을 위한 Full-Text Index를 생성합니다.
--
-- 장점:
-- - LIKE '%keyword%' 대비 10~100배 빠른 검색 속도
-- - 자연어 검색 모드 지원
-- - Boolean 검색 모드 지원 (+필수, -제외, "정확히" 등)
--
-- 단점:
-- - 한글 형태소 분석 제한적 (ngram 토크나이저로 보완)
-- - 최소 검색어 길이 제한 (기본 3자, 설정 변경 가능)
-- - 인덱스 업데이트 시 약간의 오버헤드
--
-- 주의사항:
-- - InnoDB 엔진 사용 시 MySQL 5.6 이상 필요
-- - 한글 검색을 위해 ngram 파서 사용
-- - 인덱스 생성 시 테이블 락 발생 (대용량 데이터 시 점검 시간에 실행 권장)
-- ============================================================

-- ============================================================
-- ngram 설정 확인 (my.cnf 또는 my.ini에서 설정 필요)
-- ============================================================
-- [mysqld]
-- ngram_token_size=2
--
-- 위 설정이 없으면 한글 검색이 제대로 동작하지 않을 수 있습니다.
-- 서버 재시작 필요

-- ============================================================
-- 1. user_links 테이블에 Full-Text Index 생성
-- ============================================================
-- 검색 대상: why (저장 이유), memo (메모)

-- 기존 인덱스가 있으면 삭제 (멱등성 보장)
DROP INDEX IF EXISTS ft_userlink_search ON user_links;

-- Full-Text Index 생성 (ngram 파서 사용 - 한글 지원)
CREATE FULLTEXT INDEX ft_userlink_search
    ON user_links (why, memo)
    WITH PARSER ngram;

-- ============================================================
-- 2. links 테이블에 Full-Text Index 생성
-- ============================================================
-- 검색 대상: title (제목), ai_summary (AI 요약), url (URL)

-- 기존 인덱스가 있으면 삭제
DROP INDEX IF EXISTS ft_link_search ON links;

-- Full-Text Index 생성 (ngram 파서 사용 - 한글 지원)
CREATE FULLTEXT INDEX ft_link_search
    ON links (title, ai_summary, url)
    WITH PARSER ngram;

-- ============================================================
-- 3. 개별 필드용 Full-Text Index (선택적)
-- ============================================================
-- 특정 필드만 검색할 때 더 빠른 검색을 위해 개별 인덱스 생성
-- 필요에 따라 주석 해제하여 사용

-- user_links.why 개별 인덱스
-- CREATE FULLTEXT INDEX ft_userlink_why ON user_links (why) WITH PARSER ngram;

-- user_links.memo 개별 인덱스
-- CREATE FULLTEXT INDEX ft_userlink_memo ON user_links (memo) WITH PARSER ngram;

-- links.title 개별 인덱스
-- CREATE FULLTEXT INDEX ft_link_title ON links (title) WITH PARSER ngram;

-- ============================================================
-- 검증 쿼리 (마이그레이션 후 확인용)
-- ============================================================
-- 인덱스 확인
-- SHOW INDEX FROM user_links WHERE Index_type = 'FULLTEXT';
-- SHOW INDEX FROM links WHERE Index_type = 'FULLTEXT';

-- 검색 테스트
-- SELECT * FROM user_links
-- WHERE MATCH(why, memo) AGAINST('검색어' IN NATURAL LANGUAGE MODE);

-- SELECT * FROM links
-- WHERE MATCH(title, ai_summary, url) AGAINST('검색어' IN NATURAL LANGUAGE MODE);
