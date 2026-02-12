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

-- 3. Prefix UNIQUE 인덱스 추가
CREATE UNIQUE INDEX uk_link_url ON links (url(500));
