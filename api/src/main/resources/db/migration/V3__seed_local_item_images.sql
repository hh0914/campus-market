-- 校园集 V3：将演示商品图片替换为随仓库分发的本地插画
-- 每件商品统一补齐 3 张图片（封面 + 成色细节 + 配件一览），避免依赖外网图床。
-- 说明：V2 已执行且禁止修改，因此图片素材通过本迁移增量更新。

SET NAMES utf8mb4;

-- 封面统一取第一张图
UPDATE item SET cover_url = '/images/items/ipad-air-4-1.svg' WHERE id = 1;
UPDATE item SET cover_url = '/images/items/mx-master-3s-1.svg' WHERE id = 2;
UPDATE item SET cover_url = '/images/items/advanced-math-1.svg' WHERE id = 3;
UPDATE item SET cover_url = '/images/items/kaoyan-english-1.svg' WHERE id = 4;
UPDATE item SET cover_url = '/images/items/rice-cooker-1.svg' WHERE id = 5;
UPDATE item SET cover_url = '/images/items/badminton-set-1.svg' WHERE id = 6;
UPDATE item SET cover_url = '/images/items/north-face-jacket-1.svg' WHERE id = 7;
UPDATE item SET cover_url = '/images/items/kindle-paperwhite-1.svg' WHERE id = 8;
UPDATE item SET cover_url = '/images/items/keychron-k2-1.svg' WHERE id = 9;
UPDATE item SET cover_url = '/images/items/desk-lamp-1.svg' WHERE id = 10;
UPDATE item SET cover_url = '/images/items/giant-bike-1.svg' WHERE id = 11;
UPDATE item SET cover_url = '/images/items/yamaha-guitar-1.svg' WHERE id = 12;
UPDATE item SET cover_url = '/images/items/cosmetics-set-1.svg' WHERE id = 13;
UPDATE item SET cover_url = '/images/items/camping-tent-1.svg' WHERE id = 14;
UPDATE item SET cover_url = '/images/items/monitor-27-1.svg' WHERE id = 15;
UPDATE item SET cover_url = '/images/items/winter-coat-1.svg' WHERE id = 16;

-- 重建演示商品的图集（仅覆盖演示商品，不动用户自行上传的数据）
DELETE FROM item_image WHERE item_id BETWEEN 1 AND 16;

INSERT INTO item_image (item_id, url, sort_order) VALUES
(1, '/images/items/ipad-air-4-1.svg', 0),
(1, '/images/items/ipad-air-4-2.svg', 1),
(1, '/images/items/ipad-air-4-3.svg', 2),
(2, '/images/items/mx-master-3s-1.svg', 0),
(2, '/images/items/mx-master-3s-2.svg', 1),
(2, '/images/items/mx-master-3s-3.svg', 2),
(3, '/images/items/advanced-math-1.svg', 0),
(3, '/images/items/advanced-math-2.svg', 1),
(3, '/images/items/advanced-math-3.svg', 2),
(4, '/images/items/kaoyan-english-1.svg', 0),
(4, '/images/items/kaoyan-english-2.svg', 1),
(4, '/images/items/kaoyan-english-3.svg', 2),
(5, '/images/items/rice-cooker-1.svg', 0),
(5, '/images/items/rice-cooker-2.svg', 1),
(5, '/images/items/rice-cooker-3.svg', 2),
(6, '/images/items/badminton-set-1.svg', 0),
(6, '/images/items/badminton-set-2.svg', 1),
(6, '/images/items/badminton-set-3.svg', 2),
(7, '/images/items/north-face-jacket-1.svg', 0),
(7, '/images/items/north-face-jacket-2.svg', 1),
(7, '/images/items/north-face-jacket-3.svg', 2),
(8, '/images/items/kindle-paperwhite-1.svg', 0),
(8, '/images/items/kindle-paperwhite-2.svg', 1),
(8, '/images/items/kindle-paperwhite-3.svg', 2),
(9, '/images/items/keychron-k2-1.svg', 0),
(9, '/images/items/keychron-k2-2.svg', 1),
(9, '/images/items/keychron-k2-3.svg', 2),
(10, '/images/items/desk-lamp-1.svg', 0),
(10, '/images/items/desk-lamp-2.svg', 1),
(10, '/images/items/desk-lamp-3.svg', 2),
(11, '/images/items/giant-bike-1.svg', 0),
(11, '/images/items/giant-bike-2.svg', 1),
(11, '/images/items/giant-bike-3.svg', 2),
(12, '/images/items/yamaha-guitar-1.svg', 0),
(12, '/images/items/yamaha-guitar-2.svg', 1),
(12, '/images/items/yamaha-guitar-3.svg', 2),
(13, '/images/items/cosmetics-set-1.svg', 0),
(13, '/images/items/cosmetics-set-2.svg', 1),
(13, '/images/items/cosmetics-set-3.svg', 2),
(14, '/images/items/camping-tent-1.svg', 0),
(14, '/images/items/camping-tent-2.svg', 1),
(14, '/images/items/camping-tent-3.svg', 2),
(15, '/images/items/monitor-27-1.svg', 0),
(15, '/images/items/monitor-27-2.svg', 1),
(15, '/images/items/monitor-27-3.svg', 2),
(16, '/images/items/winter-coat-1.svg', 0),
(16, '/images/items/winter-coat-2.svg', 1),
(16, '/images/items/winter-coat-3.svg', 2);
