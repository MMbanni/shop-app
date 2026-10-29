ALTER TABLE product
    ADD COLUMN attributes JSONB NOT NULL DEFAULT '{}'::jsonb;