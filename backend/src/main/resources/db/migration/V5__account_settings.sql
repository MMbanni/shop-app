ALTER TABLE app_user
    ADD COLUMN pending_email varchar(254),
    ADD COLUMN verification_hash varchar(64),
 ADD COLUMN verification_expires timestamptz;
CREATE INDEX ix_orders_customer_history ON shop_order (user_id, id DESC);