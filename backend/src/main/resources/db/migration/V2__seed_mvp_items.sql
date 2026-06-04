INSERT INTO items (code, name, item_type, base_price, current_price, grow_seconds, output_quantity, bulk_quantity_threshold, bulk_amount_threshold)
VALUES
    ('WHEAT', '小麦', 'HARVEST', 25, 25, null, null, 5000, 100000),
    ('CORN', '玉米', 'HARVEST', 31, 31, null, null, 5000, 120000),
    ('EGG', '鸡蛋', 'HARVEST', 18, 18, null, null, 8000, 100000),
    ('MILK', '牛奶', 'HARVEST', 43, 43, null, null, 3000, 120000),
    ('BULK_TOKEN', '大宗交易令牌', 'TOKEN', 0, 0, null, null, null, null);

INSERT INTO items (code, name, item_type, base_price, current_price, grow_seconds, output_item_id, output_quantity, bulk_quantity_threshold, bulk_amount_threshold)
SELECT 'WHEAT_SEED', '小麦种子', 'SEED', 80, 80, 1800, id, 12, null, null
FROM items
WHERE code = 'WHEAT';

INSERT INTO items (code, name, item_type, base_price, current_price, grow_seconds, output_item_id, output_quantity, bulk_quantity_threshold, bulk_amount_threshold)
SELECT 'CORN_SEED', '玉米种子', 'SEED', 120, 120, 3600, id, 10, null, null
FROM items
WHERE code = 'CORN';

INSERT INTO items (code, name, item_type, base_price, current_price, grow_seconds, output_item_id, output_quantity, bulk_quantity_threshold, bulk_amount_threshold)
SELECT 'CHICKEN', '鸡苗', 'ANIMAL', 320, 320, 7200, id, 20, null, null
FROM items
WHERE code = 'EGG';

INSERT INTO items (code, name, item_type, base_price, current_price, grow_seconds, output_item_id, output_quantity, bulk_quantity_threshold, bulk_amount_threshold)
SELECT 'COW', '奶牛', 'ANIMAL', 1880, 1880, 21600, id, 30, null, null
FROM items
WHERE code = 'MILK';

