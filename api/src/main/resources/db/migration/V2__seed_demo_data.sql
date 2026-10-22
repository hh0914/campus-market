-- 校园集 V2：演示数据
-- 所有演示账号密码均为 Campus@2026（BCrypt cost 10）
-- 时间统一使用相对 NOW() 的表达式，保证任何时间运行都能看到近 14 天趋势

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------------
-- 用户
-- ---------------------------------------------------------------------------
INSERT INTO app_user (id, student_no, email, nickname, password_hash, bio, campus, role, status, verified, last_login_at, created_at) VALUES
(1, 'platform', 'platform@campus.edu', '平台运营', '$2a$10$Z1PnRXA0.peA9Li40eIWfupE36pqltwM2OEXoyBFYwo.1Qhp1K9By', '平台官方账号，手续费结算账户', NULL, 'ADMIN', 'ACTIVE', 1, DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_SUB(NOW(), INTERVAL 120 DAY)),
(2, 'admin', 'admin@campus.edu', '张明远', '$2a$10$Z1PnRXA0.peA9Li40eIWfupE36pqltwM2OEXoyBFYwo.1Qhp1K9By', '学生会生活部指导老师，负责平台合规', '主校区', 'ADMIN', 'ACTIVE', 1, DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_SUB(NOW(), INTERVAL 110 DAY)),
(3, 'mod001', 'mod001@campus.edu', '陈默', '$2a$10$Z1PnRXA0.peA9Li40eIWfupE36pqltwM2OEXoyBFYwo.1Qhp1K9By', '平台审核员，处理商品审核与举报工单', '主校区', 'MODERATOR', 'ACTIVE', 1, DATE_SUB(NOW(), INTERVAL 30 MINUTE), DATE_SUB(NOW(), INTERVAL 90 DAY)),
(4, '2021001', 'linxiao@campus.edu', '林晓', '$2a$10$Z1PnRXA0.peA9Li40eIWfupE36pqltwM2OEXoyBFYwo.1Qhp1K9By', '数码爱好者，出闲置必附实拍图，支持当面验机', '东校区', 'STUDENT', 'ACTIVE', 1, DATE_SUB(NOW(), INTERVAL 25 MINUTE), DATE_SUB(NOW(), INTERVAL 60 DAY)),
(5, '2021002', 'suqing@campus.edu', '苏晴', '$2a$10$Z1PnRXA0.peA9Li40eIWfupE36pqltwM2OEXoyBFYwo.1Qhp1K9By', '文学院研一，教材和衣物居多，价格好商量', '东校区', 'STUDENT', 'ACTIVE', 1, DATE_SUB(NOW(), INTERVAL 3 HOUR), DATE_SUB(NOW(), INTERVAL 55 DAY)),
(6, '2021003', 'lihang@campus.edu', '李航', '$2a$10$Z1PnRXA0.peA9Li40eIWfupE36pqltwM2OEXoyBFYwo.1Qhp1K9By', '机械学院大三，常出生活电器与运动装备', '西校区', 'STUDENT', 'ACTIVE', 1, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 48 DAY)),
(7, '2021004', 'wangzi@campus.edu', '王梓', '$2a$10$Z1PnRXA0.peA9Li40eIWfupE36pqltwM2OEXoyBFYwo.1Qhp1K9By', '刚注册，还没完成校园认证', '南校区', 'STUDENT', 'ACTIVE', 0, NULL, DATE_SUB(NOW(), INTERVAL 3 DAY)),
(8, '2021005', 'zhaoxin@campus.edu', '赵昕', '$2a$10$Z1PnRXA0.peA9Li40eIWfupE36pqltwM2OEXoyBFYwo.1Qhp1K9By', '艺术学院，偶尔出乐器与闲置美妆', '北校区', 'STUDENT', 'WARNED', 1, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 40 DAY));

-- ---------------------------------------------------------------------------
-- 钱包（余额与流水保持一致）
-- ---------------------------------------------------------------------------
INSERT INTO wallet (id, user_id, balance, frozen, total_income, total_spent, created_at) VALUES
(1, 1, 12.80, 0.00, 12.80, 0.00, DATE_SUB(NOW(), INTERVAL 120 DAY)),
(2, 2, 0.00, 0.00, 0.00, 0.00, DATE_SUB(NOW(), INTERVAL 110 DAY)),
(3, 3, 0.00, 0.00, 0.00, 0.00, DATE_SUB(NOW(), INTERVAL 90 DAY)),
(4, 4, 1200.00, 0.00, 372.40, 0.00, DATE_SUB(NOW(), INTERVAL 60 DAY)),
(5, 5, 800.00, 520.00, 254.80, 900.00, DATE_SUB(NOW(), INTERVAL 55 DAY)),
(6, 6, 300.00, 0.00, 0.00, 260.00, DATE_SUB(NOW(), INTERVAL 48 DAY)),
(7, 7, 0.00, 0.00, 0.00, 0.00, DATE_SUB(NOW(), INTERVAL 3 DAY)),
(8, 8, 150.00, 0.00, 0.00, 0.00, DATE_SUB(NOW(), INTERVAL 40 DAY));

INSERT INTO wallet_ledger (wallet_id, user_id, order_id, type, amount, balance_after, remark, created_at) VALUES
(4, 4, NULL, 'RECHARGE', 827.60, 827.60, '演示环境充值', DATE_SUB(NOW(), INTERVAL 30 DAY)),
(4, 4, 1, 'INCOME', 372.40, 1200.00, '订单结算收入', DATE_SUB(NOW(), INTERVAL 7 DAY)),
(5, 5, NULL, 'RECHARGE', 1445.20, 1445.20, '演示环境充值', DATE_SUB(NOW(), INTERVAL 30 DAY)),
(5, 5, 1, 'ESCROW_OUT', -380.00, 1065.20, '托管付款', DATE_SUB(NOW(), INTERVAL 8 DAY)),
(5, 5, 2, 'INCOME', 254.80, 1320.00, '订单结算收入', DATE_SUB(NOW(), INTERVAL 4 DAY)),
(5, 5, 4, 'ESCROW_OUT', -520.00, 800.00, '托管付款', DATE_SUB(NOW(), INTERVAL 2 DAY)),
(6, 6, NULL, 'RECHARGE', 560.00, 560.00, '演示环境充值', DATE_SUB(NOW(), INTERVAL 30 DAY)),
(6, 6, 2, 'ESCROW_OUT', -260.00, 300.00, '托管付款', DATE_SUB(NOW(), INTERVAL 5 DAY)),
(8, 8, NULL, 'RECHARGE', 150.00, 150.00, '演示环境充值', DATE_SUB(NOW(), INTERVAL 20 DAY)),
(1, 1, 1, 'FEE', 7.60, 7.60, '平台交易手续费', DATE_SUB(NOW(), INTERVAL 7 DAY)),
(1, 1, 2, 'FEE', 5.20, 12.80, '平台交易手续费', DATE_SUB(NOW(), INTERVAL 4 DAY));

-- ---------------------------------------------------------------------------
-- 信用档案
-- ---------------------------------------------------------------------------
INSERT INTO reputation (user_id, credit_score, badge, total_deals, completed_deals, cancelled_deals, refunded_deals, total_reviews, good_reviews, bad_reviews, dispute_count, report_count, avg_rating) VALUES
(1, 100, 'ELITE', 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.00),
(2, 100, 'ELITE', 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.00),
(3, 96, 'ELITE', 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.00),
(4, 93, 'RELIABLE', 3, 3, 0, 0, 2, 2, 0, 0, 0, 4.50),
(5, 88, 'RELIABLE', 3, 2, 0, 1, 2, 2, 0, 0, 0, 4.00),
(6, 74, 'NORMAL', 2, 1, 1, 0, 1, 0, 1, 0, 0, 2.00),
(7, 80, 'NORMAL', 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.00),
(8, 68, 'WATCH', 2, 0, 2, 0, 0, 0, 0, 1, 1, 0.00);

-- ---------------------------------------------------------------------------
-- 校园认证记录
-- ---------------------------------------------------------------------------
INSERT INTO student_verification (user_id, student_no, school_email, code, attempt_count, status, expires_at, verified_at, created_at) VALUES
(4, '2021001', 'linxiao@campus.edu', '000000', 0, 'VERIFIED', DATE_SUB(NOW(), INTERVAL 59 DAY), DATE_SUB(NOW(), INTERVAL 60 DAY), DATE_SUB(NOW(), INTERVAL 60 DAY)),
(5, '2021002', 'suqing@campus.edu', '000000', 0, 'VERIFIED', DATE_SUB(NOW(), INTERVAL 54 DAY), DATE_SUB(NOW(), INTERVAL 55 DAY), DATE_SUB(NOW(), INTERVAL 55 DAY)),
(6, '2021003', 'lihang@campus.edu', '000000', 0, 'VERIFIED', DATE_SUB(NOW(), INTERVAL 47 DAY), DATE_SUB(NOW(), INTERVAL 48 DAY), DATE_SUB(NOW(), INTERVAL 48 DAY)),
(8, '2021005', 'zhaoxin@campus.edu', '000000', 1, 'VERIFIED', DATE_SUB(NOW(), INTERVAL 39 DAY), DATE_SUB(NOW(), INTERVAL 40 DAY), DATE_SUB(NOW(), INTERVAL 40 DAY));

-- ---------------------------------------------------------------------------
-- 分类
-- ---------------------------------------------------------------------------
INSERT INTO category (id, name, slug, icon, sort_order, enabled) VALUES
(1, '数码电子', 'digital', '💻', 10, 1),
(2, '教材书籍', 'books', '📚', 20, 1),
(3, '生活电器', 'appliance', '🔌', 30, 1),
(4, '运动户外', 'sports', '🏸', 40, 1),
(5, '服饰鞋包', 'fashion', '👕', 50, 1),
(6, '美妆个护', 'beauty', '💄', 60, 1),
(7, '乐器', 'instrument', '🎸', 70, 1),
(8, '其他闲置', 'others', '📦', 80, 1);

-- ---------------------------------------------------------------------------
-- 商品
-- ---------------------------------------------------------------------------
INSERT INTO item (id, seller_id, category_id, title, description, price, original_price, condition_level, campus, trade_method, tags, cover_url, status, reject_reason, view_count, favorite_count, published_at, sold_at, created_at) VALUES
(1, 4, 1, 'iPad Air 4 64G 深空灰', '自用 iPad Air 4，64G 深空灰，电池健康度 91%，循环 180 次。屏幕无划痕，边框有一处极小磕碰（图三）。配原装充电器和保护壳，支持东校区当面验机，验机满意再付款。', 1980.00, 4399.00, 'LIKE_NEW', '东校区', 'MEETUP', '平板,苹果,考研', 'https://picsum.photos/seed/campus-1a/800/600', 'ON_SALE', NULL, 186, 12, DATE_SUB(NOW(), INTERVAL 6 DAY), NULL, DATE_SUB(NOW(), INTERVAL 6 DAY)),
(2, 4, 1, '罗技 MX Master 3S 鼠标', '办公神器，静音微动，用了一个学期，功能完好，附 Type-C 数据线。', 380.00, 699.00, 'GOOD', '东校区', 'MEETUP', '鼠标,罗技,办公', 'https://picsum.photos/seed/campus-2a/800/600', 'ON_SALE', NULL, 92, 6, DATE_SUB(NOW(), INTERVAL 11 DAY), NULL, DATE_SUB(NOW(), INTERVAL 11 DAY)),
(3, 5, 2, '高等数学 同济第七版 上下册', '上册有少量笔记，下册几乎全新，无缺页。考研复习刚用完，可拆卖。', 25.00, 89.00, 'GOOD', '东校区', 'MEETUP', '教材,高数,考研', 'https://picsum.photos/seed/campus-3a/800/600', 'ON_SALE', NULL, 240, 18, DATE_SUB(NOW(), INTERVAL 9 DAY), NULL, DATE_SUB(NOW(), INTERVAL 9 DAY)),
(4, 5, 2, '考研英语真题 2010-2025', '黄皮书套装，真题册有勾画，解析册干净。适合二刷查漏补缺。', 45.00, 168.00, 'GOOD', '东校区', 'EXPRESS', '考研,英语,真题', 'https://picsum.photos/seed/campus-4a/800/600', 'ON_SALE', NULL, 131, 9, DATE_SUB(NOW(), INTERVAL 4 DAY), NULL, DATE_SUB(NOW(), INTERVAL 4 DAY)),
(5, 6, 3, '小米电煮锅 1.2L', '宿舍神器，煮面煮粥都方便，带蒸笼。因宿舍换大功率电器出了。', 65.00, 149.00, 'GOOD', '西校区', 'MEETUP', '电煮锅,宿舍,小米', 'https://picsum.photos/seed/campus-5a/800/600', 'ON_SALE', NULL, 76, 4, DATE_SUB(NOW(), INTERVAL 14 DAY), NULL, DATE_SUB(NOW(), INTERVAL 14 DAY)),
(6, 6, 4, '迪卡侬羽毛球拍 对拍', '入门对拍套装，含 3 个球和拍套，打过四五次，握把换过新胶。', 90.00, 199.00, 'LIKE_NEW', '西校区', 'MEETUP', '羽毛球,迪卡侬,运动', 'https://picsum.photos/seed/campus-6a/800/600', 'ON_SALE', NULL, 58, 3, DATE_SUB(NOW(), INTERVAL 16 DAY), NULL, DATE_SUB(NOW(), INTERVAL 16 DAY)),
(7, 5, 5, '北面冲锋衣 男款 M 码', '正品北面三合一冲锋衣，男款 M，穿过两次，无污渍无破损，内胆可拆。', 420.00, 1299.00, 'LIKE_NEW', '东校区', 'EXPRESS', '冲锋衣,北面,户外', 'https://picsum.photos/seed/campus-7a/800/600', 'ON_SALE', NULL, 164, 11, DATE_SUB(NOW(), INTERVAL 3 DAY), NULL, DATE_SUB(NOW(), INTERVAL 3 DAY)),
(8, 4, 1, 'Kindle Paperwhite 4 8G', '经典款 Kindle，8G 容量，背光可调，电池正常。已贴膜带保护套。', 380.00, 998.00, 'GOOD', '东校区', 'MEETUP', 'Kindle,电纸书,阅读', 'https://picsum.photos/seed/campus-8a/800/600', 'SOLD', NULL, 210, 15, DATE_SUB(NOW(), INTERVAL 12 DAY), DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_SUB(NOW(), INTERVAL 12 DAY)),
(9, 5, 1, 'Keychron K2 机械键盘', '茶轴 84 键，蓝牙有线双模，Mac 键位。键帽无打油，附拔键器。', 260.00, 499.00, 'GOOD', '东校区', 'MEETUP', '键盘,Keychron,机械', 'https://picsum.photos/seed/campus-9a/800/600', 'SOLD', NULL, 143, 8, DATE_SUB(NOW(), INTERVAL 18 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 18 DAY)),
(10, 6, 3, '护眼台灯 国AA级', '国 AA 级照度，三档色温，触控调光，用了一学期。', 55.00, 159.00, 'GOOD', '西校区', 'MEETUP', '台灯,护眼,宿舍', 'https://picsum.photos/seed/campus-10a/800/600', 'PENDING_REVIEW', NULL, 12, 1, NULL, NULL, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(11, 4, 4, '捷安特山地车 21 速', '毕业出，21 速山地车，前后刹车刚换新，车锁一并送。仅限东校区自提。', 680.00, 1580.00, 'FAIR', '东校区', 'MEETUP', '自行车,捷安特,毕业', 'https://picsum.photos/seed/campus-11a/800/600', 'PENDING_REVIEW', NULL, 9, 2, NULL, NULL, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(12, 8, 7, '雅马哈民谣吉他 F310', '入门面单，音色干净，琴颈无变形，附琴包和变调夹。', 550.00, 1080.00, 'GOOD', '北校区', 'EXPRESS', '吉他,雅马哈,乐器', 'https://picsum.photos/seed/campus-12a/800/600', 'PENDING_REVIEW', NULL, 21, 3, NULL, NULL, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(13, 8, 6, '化妆品九件套 全新未拆', '朋友送的套装，不适合我的肤质，全新未拆封。', 120.00, 399.00, 'NEW', '北校区', 'EXPRESS', '美妆,套装', 'https://picsum.photos/seed/campus-13a/800/600', 'REJECTED', '疑似三无化妆品，请补充品牌授权或正规购买凭证', 34, 0, NULL, NULL, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(14, 4, 4, '露营帐篷 双人 防雨', '双人自动帐篷，防雨涂层完好，只用过两次，含地钉和收纳袋。', 260.00, 599.00, 'LIKE_NEW', '东校区', 'MEETUP', '露营,帐篷,户外', 'https://picsum.photos/seed/campus-14a/800/600', 'LOCKED', NULL, 47, 5, DATE_SUB(NOW(), INTERVAL 8 DAY), NULL, DATE_SUB(NOW(), INTERVAL 8 DAY)),
(15, 6, 1, '27 寸 2K 显示器 IPS', '27 寸 2K IPS 屏，75Hz，带 HDMI 线和支架，无坏点。', 520.00, 1099.00, 'GOOD', '西校区', 'EXPRESS', '显示器,2K,办公', 'https://picsum.photos/seed/campus-15a/800/600', 'LOCKED', NULL, 88, 7, DATE_SUB(NOW(), INTERVAL 10 DAY), NULL, DATE_SUB(NOW(), INTERVAL 10 DAY)),
(16, 5, 5, '冬季棉服 L 码 米白', '米白色长款棉服，L 码，去年冬天买的，洗过一次，无破损。', 180.00, 459.00, 'GOOD', '东校区', 'MEETUP', '棉服,冬装,女装', 'https://picsum.photos/seed/campus-16a/800/600', 'ON_SALE', NULL, 63, 4, DATE_SUB(NOW(), INTERVAL 2 DAY), NULL, DATE_SUB(NOW(), INTERVAL 2 DAY));

INSERT INTO item_image (item_id, url, sort_order) VALUES
(1, 'https://picsum.photos/seed/campus-1a/800/600', 0),
(1, 'https://picsum.photos/seed/campus-1b/800/600', 1),
(1, 'https://picsum.photos/seed/campus-1c/800/600', 2),
(2, 'https://picsum.photos/seed/campus-2a/800/600', 0),
(2, 'https://picsum.photos/seed/campus-2b/800/600', 1),
(3, 'https://picsum.photos/seed/campus-3a/800/600', 0),
(3, 'https://picsum.photos/seed/campus-3b/800/600', 1),
(4, 'https://picsum.photos/seed/campus-4a/800/600', 0),
(5, 'https://picsum.photos/seed/campus-5a/800/600', 0),
(5, 'https://picsum.photos/seed/campus-5b/800/600', 1),
(6, 'https://picsum.photos/seed/campus-6a/800/600', 0),
(7, 'https://picsum.photos/seed/campus-7a/800/600', 0),
(7, 'https://picsum.photos/seed/campus-7b/800/600', 1),
(8, 'https://picsum.photos/seed/campus-8a/800/600', 0),
(9, 'https://picsum.photos/seed/campus-9a/800/600', 0),
(10, 'https://picsum.photos/seed/campus-10a/800/600', 0),
(11, 'https://picsum.photos/seed/campus-11a/800/600', 0),
(12, 'https://picsum.photos/seed/campus-12a/800/600', 0),
(13, 'https://picsum.photos/seed/campus-13a/800/600', 0),
(14, 'https://picsum.photos/seed/campus-14a/800/600', 0),
(15, 'https://picsum.photos/seed/campus-15a/800/600', 0),
(16, 'https://picsum.photos/seed/campus-16a/800/600', 0);

INSERT INTO favorite (user_id, item_id, created_at) VALUES
(4, 3, DATE_SUB(NOW(), INTERVAL 8 DAY)),
(4, 5, DATE_SUB(NOW(), INTERVAL 6 DAY)),
(4, 9, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(5, 1, DATE_SUB(NOW(), INTERVAL 4 DAY)),
(5, 2, DATE_SUB(NOW(), INTERVAL 3 DAY)),
(6, 7, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(8, 1, DATE_SUB(NOW(), INTERVAL 1 DAY));

-- ---------------------------------------------------------------------------
-- 议价会话
-- ---------------------------------------------------------------------------
INSERT INTO conversation (id, item_id, buyer_id, seller_id, last_message_at, created_at) VALUES
(1, 1, 5, 4, DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_SUB(NOW(), INTERVAL 2 DAY)),
(2, 5, 4, 6, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY));

INSERT INTO message (id, conversation_id, sender_id, type, content, offer_amount, read_flag, created_at) VALUES
(1, 1, 5, 'TEXT', '同学你好，这台 iPad 电池健康度还有多少呀？', NULL, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(2, 1, 4, 'TEXT', '循环 180 次，健康度 91%，可以当面验机再决定。', NULL, 1, DATE_SUB(NOW(), INTERVAL 20 HOUR)),
(3, 1, 5, 'OFFER', '1750 可以的话我今天下午就到东校区取', 1750.00, 0, DATE_SUB(NOW(), INTERVAL 2 HOUR)),
(4, 2, 4, 'TEXT', '电煮锅还在吗？带蒸笼吗？', NULL, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(5, 2, 6, 'TEXT', '在的，蒸笼一起给，明天中午西门面交方便吗？', NULL, 1, DATE_SUB(NOW(), INTERVAL 23 HOUR));

-- ---------------------------------------------------------------------------
-- 订单与资金流水
-- ---------------------------------------------------------------------------
INSERT INTO orders (id, order_no, item_id, buyer_id, seller_id, amount, fee_amount, seller_income, status, trade_method, pay_deadline, paid_at, shipped_at, completed_at, cancelled_at, cancel_reason, created_at) VALUES
(1, 'CM202610180001', 8, 5, 4, 380.00, 7.60, 372.40, 'COMPLETED', 'MEETUP', DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 7 DAY), NULL, NULL, DATE_SUB(NOW(), INTERVAL 8 DAY)),
(2, 'CM202610190002', 9, 6, 5, 260.00, 5.20, 254.80, 'COMPLETED', 'MEETUP', DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY), NULL, NULL, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(3, 'CM202610210003', 14, 4, 6, 260.00, 0.00, 0.00, 'PENDING_PAY', 'MEETUP', DATE_ADD(NOW(), INTERVAL 25 MINUTE), NULL, NULL, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 10 MINUTE)),
(4, 'CM202610200004', 15, 5, 6, 520.00, 0.00, 0.00, 'SHIPPED', 'EXPRESS', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 2 DAY));

INSERT INTO order_status_log (order_id, from_status, to_status, operator_id, remark, created_at) VALUES
(1, 'PENDING_PAY', 'PENDING_PAY', 5, '创建订单，商品已锁定', DATE_SUB(NOW(), INTERVAL 8 DAY)),
(1, 'PENDING_PAY', 'PAID', 5, '买家完成托管付款', DATE_SUB(NOW(), INTERVAL 8 DAY)),
(1, 'PAID', 'SHIPPED', 4, '卖家已发货或已约定面交', DATE_SUB(NOW(), INTERVAL 8 DAY)),
(1, 'SHIPPED', 'COMPLETED', 5, '买家确认收货，平台扣除手续费 7.60 元', DATE_SUB(NOW(), INTERVAL 7 DAY)),
(2, 'PENDING_PAY', 'PENDING_PAY', 6, '创建订单，商品已锁定', DATE_SUB(NOW(), INTERVAL 5 DAY)),
(2, 'PENDING_PAY', 'PAID', 6, '买家完成托管付款', DATE_SUB(NOW(), INTERVAL 5 DAY)),
(2, 'PAID', 'SHIPPED', 5, '卖家已发货或已约定面交', DATE_SUB(NOW(), INTERVAL 5 DAY)),
(2, 'SHIPPED', 'COMPLETED', 6, '买家确认收货，平台扣除手续费 5.20 元', DATE_SUB(NOW(), INTERVAL 4 DAY)),
(3, 'PENDING_PAY', 'PENDING_PAY', 4, '创建订单，商品已锁定', DATE_SUB(NOW(), INTERVAL 10 MINUTE)),
(4, 'PENDING_PAY', 'PENDING_PAY', 5, '创建订单，商品已锁定', DATE_SUB(NOW(), INTERVAL 2 DAY)),
(4, 'PENDING_PAY', 'PAID', 5, '买家完成托管付款', DATE_SUB(NOW(), INTERVAL 2 DAY)),
(4, 'PAID', 'SHIPPED', 6, '卖家已发货或已约定面交', DATE_SUB(NOW(), INTERVAL 1 DAY));

INSERT INTO payment_transaction (trade_no, order_id, user_id, type, amount, status, remark, created_at) VALUES
('TX20261018000001', 1, 5, 'ESCROW_IN', 380.00, 'SUCCESS', '托管付款', DATE_SUB(NOW(), INTERVAL 8 DAY)),
('TX20261018000002', 1, 4, 'INCOME', 372.40, 'SUCCESS', '订单结算收入', DATE_SUB(NOW(), INTERVAL 7 DAY)),
('TX20261018000003', 1, 1, 'FEE', 7.60, 'SUCCESS', '平台交易手续费', DATE_SUB(NOW(), INTERVAL 7 DAY)),
('TX20261019000004', 2, 6, 'ESCROW_IN', 260.00, 'SUCCESS', '托管付款', DATE_SUB(NOW(), INTERVAL 5 DAY)),
('TX20261019000005', 2, 5, 'INCOME', 254.80, 'SUCCESS', '订单结算收入', DATE_SUB(NOW(), INTERVAL 4 DAY)),
('TX20261019000006', 2, 1, 'FEE', 5.20, 'SUCCESS', '平台交易手续费', DATE_SUB(NOW(), INTERVAL 4 DAY)),
('TX20261020000007', 4, 5, 'ESCROW_IN', 520.00, 'SUCCESS', '托管付款', DATE_SUB(NOW(), INTERVAL 2 DAY));

-- ---------------------------------------------------------------------------
-- 评价
-- ---------------------------------------------------------------------------
INSERT INTO review (order_id, reviewer_id, reviewee_id, item_id, direction, rating, tags, comment, created_at) VALUES
(1, 5, 4, 8, 'BUYER_TO_SELLER', 5, '描述相符,见面准时', '卖家人很好，提前把电量和划痕都拍给我看了，面交时还带了充电器。', DATE_SUB(NOW(), INTERVAL 7 DAY)),
(1, 4, 5, 8, 'SELLER_TO_BUYER', 4, '沟通顺畅,付款及时', '买家很爽快，约在校门口五分钟就完成了交易。', DATE_SUB(NOW(), INTERVAL 7 DAY)),
(2, 6, 5, 9, 'BUYER_TO_SELLER', 4, '成色不错', '键盘和描述一致，就是约的时间改了一次。', DATE_SUB(NOW(), INTERVAL 4 DAY)),
(2, 5, 6, 9, 'SELLER_TO_BUYER', 2, '发货慢', '约好中午面交，等了半小时才到，希望以后守时。', DATE_SUB(NOW(), INTERVAL 4 DAY));

-- ---------------------------------------------------------------------------
-- 举报工单
-- ---------------------------------------------------------------------------
INSERT INTO report (id, reporter_id, target_type, target_id, reason_type, description, evidence_url, status, handler_id, handle_remark, handled_at, created_at) VALUES
(1, 5, 'ITEM', 13, 'COUNTERFEIT', '这套化妆品没有中文标签，也查不到备案号，怀疑是三无产品。', NULL, 'RESOLVED', 3, '经核实为无正规来源化妆品，已下架商品并提醒卖家补充凭证。', DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY)),
(2, 6, 'ITEM', 16, 'OTHER', '和卖家沟通时对方说衣服有轻微泛黄，但商品描述里没写，希望平台核实。', NULL, 'PENDING', NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 6 HOUR)),
(3, 4, 'USER', 8, 'ABUSE', '在聊天里被对方反复辱骂，已经截图保存。', NULL, 'PROCESSING', 3, NULL, NULL, DATE_SUB(NOW(), INTERVAL 1 DAY));

-- ---------------------------------------------------------------------------
-- 站内通知
-- ---------------------------------------------------------------------------
INSERT INTO notification (user_id, type, title, content, link, read_flag, created_at) VALUES
(4, 'ORDER', '交易完成', '订单 CM202610180001 已完成，实收 372.40 元', '/orders/1', 1, DATE_SUB(NOW(), INTERVAL 7 DAY)),
(4, 'REVIEW', '收到新评价', '你收到一条 5 星评价', '/users/4/profile', 1, DATE_SUB(NOW(), INTERVAL 7 DAY)),
(4, 'ITEM_AUDIT', '商品审核通过', '你的商品《iPad Air 4 64G 深空灰》已通过审核并上架', '/items/1', 0, DATE_SUB(NOW(), INTERVAL 6 DAY)),
(5, 'ORDER', '买家申请退款', '订单 CM202610190002 买家申请退款，请及时处理', '/orders/2', 1, DATE_SUB(NOW(), INTERVAL 4 DAY)),
(5, 'REVIEW', '收到新评价', '你收到一条 2 星评价', '/users/5/profile', 0, DATE_SUB(NOW(), INTERVAL 4 DAY)),
(6, 'ORDER', '订单已付款', '订单 CM202610200004 买家已付款，请尽快发货或约定面交', '/orders/4', 1, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(6, 'ORDER', '订单已付款', '订单 CM202610210003 买家已付款，请尽快发货或约定面交', '/orders/3', 0, DATE_SUB(NOW(), INTERVAL 10 MINUTE)),
(8, 'REPORT', '收到平台警告', '你的账号因违规收到警告：商品描述与实际不符，请注意如实描述。', NULL, 0, DATE_SUB(NOW(), INTERVAL 3 DAY));

-- ---------------------------------------------------------------------------
-- 审计日志
-- ---------------------------------------------------------------------------
INSERT INTO audit_log (actor_id, action, target_type, target_id, detail, ip, created_at) VALUES
(3, 'ITEM_APPROVE', 'ITEM', 1, 'title=iPad Air 4 64G 深空灰', '127.0.0.1', DATE_SUB(NOW(), INTERVAL 6 DAY)),
(3, 'ITEM_REJECT', 'ITEM', 13, 'reason=疑似三无化妆品，请补充品牌授权或正规购买凭证', '127.0.0.1', DATE_SUB(NOW(), INTERVAL 5 DAY)),
(3, 'REPORT_RESOLVE', 'REPORT', 1, 'action=DOWN_ITEM, remark=经核实为无正规来源化妆品，已下架商品并提醒卖家补充凭证。', '127.0.0.1', DATE_SUB(NOW(), INTERVAL 4 DAY)),
(2, 'REPORT_RESOLVE', 'REPORT', 3, 'action=WARN, remark=聊天记录存在辱骂，给予警告。', '127.0.0.1', DATE_SUB(NOW(), INTERVAL 1 DAY));
