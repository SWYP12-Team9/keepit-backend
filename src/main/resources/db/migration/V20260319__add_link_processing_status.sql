ALTER TABLE links
    MODIFY COLUMN content TEXT NULL;

ALTER TABLE links
    ADD COLUMN processing_status VARCHAR(20) NULL;

UPDATE links
SET processing_status = CASE
    WHEN title IS NULL OR TRIM(title) = '' THEN 'PENDING'
    ELSE 'READY'
END;

ALTER TABLE links
    MODIFY COLUMN processing_status VARCHAR(20) NOT NULL DEFAULT 'PENDING';
