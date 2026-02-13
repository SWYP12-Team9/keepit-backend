-- 0. 삭제 대상 user_links의 reference_user_links 먼저 삭제
DELETE FROM reference_user_links
WHERE user_link_id IN (
    SELECT user_link_id FROM (
                                 SELECT ul.user_link_id
                                 FROM user_links ul
                                          JOIN links l ON ul.link_id = l.link_id
                                          JOIN (
                                     SELECT url, MIN(link_id) as keep_id
                                     FROM links
                                     GROUP BY url
                                     HAVING COUNT(*) > 1
                                 ) dup ON l.url = dup.url AND l.link_id != dup.keep_id
                                 WHERE EXISTS (
                                     SELECT 1 FROM user_links ul2
                                     WHERE ul2.user_id = ul.user_id
                                   AND ul2.link_id = dup.keep_id
                                     )
                             ) tmp
);

-- 1. 통합 시 충돌할 user_links 삭제
DELETE FROM user_links
WHERE user_link_id IN (
    SELECT user_link_id FROM (
                                 SELECT ul.user_link_id
                                 FROM user_links ul
                                          JOIN links l ON ul.link_id = l.link_id
                                          JOIN (
                                     SELECT url, MIN(link_id) as keep_id
                                     FROM links
                                     GROUP BY url
                                     HAVING COUNT(*) > 1
                                 ) dup ON l.url = dup.url AND l.link_id != dup.keep_id
                                 WHERE EXISTS (
                                     SELECT 1 FROM user_links ul2
                                     WHERE ul2.user_id = ul.user_id
                                   AND ul2.link_id = dup.keep_id
                                     )
                             ) tmp
);

-- 2. 나머지 중복 Link의 user_links 참조를 최소 ID로 통합
UPDATE user_links ul
    JOIN links l ON ul.link_id = l.link_id
    JOIN (
    SELECT url, MIN(link_id) as keep_id
    FROM links
    GROUP BY url
    HAVING COUNT(*) > 1
    ) dup ON l.url = dup.url AND l.link_id != dup.keep_id
    SET ul.link_id = dup.keep_id;

-- 3. 참조 없는 중복 Link 삭제
DELETE l FROM links l
    JOIN (
        SELECT url, MIN(link_id) as keep_id
        FROM links
        GROUP BY url
        HAVING COUNT(*) > 1
    ) dup ON l.url = dup.url AND l.link_id != dup.keep_id;

-- 4. url_hash 컬럼 추가
ALTER TABLE links ADD COLUMN url_hash VARCHAR(64) CHARACTER SET utf8mb4 NULL;

-- 5. 기존 데이터의 url_hash 채우기
UPDATE links SET url_hash = SHA2(url, 256);

-- 6. NOT NULL 제약조건 추가
ALTER TABLE links MODIFY COLUMN url_hash VARCHAR(64) CHARACTER SET utf8mb4 NOT NULL;

-- 7. UNIQUE 인덱스 추가
CREATE UNIQUE INDEX uk_link_url_hash ON links (url_hash);