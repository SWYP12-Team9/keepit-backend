-- 1. 중복 Link의 user_links 참조를 최소 ID로 통합
UPDATE user_links ul
    JOIN links l ON ul.link_id = l.id
    JOIN (
    SELECT url, MIN(id) as keep_id
    FROM links
    GROUP BY url
    HAVING COUNT(*) > 1
    ) dup ON l.url = dup.url AND l.id != dup.keep_id
    SET ul.link_id = dup.keep_id;

-- 2. 참조 없는 중복 Link 삭제 (최소 ID만 보존)
DELETE l FROM links l
JOIN (
    SELECT url, MIN(id) as keep_id
    FROM links
    GROUP BY url
    HAVING COUNT(*) > 1
) dup ON l.url = dup.url AND l.id != dup.keep_id;

-- 3. url_hash 컬럼 추가
ALTER TABLE links ADD COLUMN url_hash VARCHAR(64) CHARACTER SET utf8mb4 NULL;

-- 4. 기존 데이터의 url_hash 채우기 (MySQL SHA2 함수 사용)
UPDATE links SET url_hash = SHA2(url, 256);

-- 5. NOT NULL 제약조건 추가
ALTER TABLE links MODIFY COLUMN url_hash VARCHAR(64) CHARACTER SET utf8mb4 NOT NULL;

-- 6. UNIQUE 인덱스 추가
CREATE UNIQUE INDEX uk_link_url_hash ON links (url_hash);