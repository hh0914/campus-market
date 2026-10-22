-- 校园集 V1：初始化表结构
-- 约定：金额 DECIMAL(12,2)，时间 DATETIME(6)，枚举以 VARCHAR 存储并由 Java enum 校验

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------------
-- 用户与认证
-- ---------------------------------------------------------------------------
CREATE TABLE app_user (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    student_no      VARCHAR(24)  NOT NULL COMMENT '学号',
    email           VARCHAR(120) NOT NULL COMMENT '校内邮箱',
    nickname        VARCHAR(32)  NOT NULL,
    password_hash   VARCHAR(100) NOT NULL COMMENT 'BCrypt',
    avatar_url      VARCHAR(255) DEFAULT NULL,
    bio             VARCHAR(200) DEFAULT NULL,
    campus          VARCHAR(50)  DEFAULT NULL,
    role            VARCHAR(16)  NOT NULL DEFAULT 'STUDENT' COMMENT 'STUDENT/MODERATOR/ADMIN',
    status          VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/WARNED/BANNED',
    verified        TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '校园认证',
    banned_until    DATETIME(6)  DEFAULT NULL,
    last_login_at   DATETIME(6)  DEFAULT NULL,
    created_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_student_no (student_no),
    UNIQUE KEY uk_user_email (email),
    KEY idx_user_role_status (role, status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '用户';

CREATE TABLE student_verification (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    user_id       BIGINT       NOT NULL,
    student_no    VARCHAR(24)  NOT NULL,
    school_email  VARCHAR(120) NOT NULL,
    code          VARCHAR(8)   NOT NULL,
    attempt_count INT          NOT NULL DEFAULT 0,
    status        VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/VERIFIED/FAILED',
    expires_at    DATETIME(6)  NOT NULL,
    verified_at   DATETIME(6)  DEFAULT NULL,
    created_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_verification_user_status (user_id, status),
    CONSTRAINT fk_verification_user FOREIGN KEY (user_id) REFERENCES app_user (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '校园认证记录';

CREATE TABLE refresh_token (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    user_id     BIGINT      NOT NULL,
    token_hash  CHAR(64)    NOT NULL COMMENT 'SHA-256',
    expires_at  DATETIME(6) NOT NULL,
    revoked     TINYINT(1)  NOT NULL DEFAULT 0,
    user_agent  VARCHAR(200) DEFAULT NULL,
    created_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_refresh_hash (token_hash),
    KEY idx_refresh_user_revoked (user_id, revoked),
    CONSTRAINT fk_refresh_user FOREIGN KEY (user_id) REFERENCES app_user (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '刷新令牌';

-- ---------------------------------------------------------------------------
-- 分类与商品
-- ---------------------------------------------------------------------------
CREATE TABLE category (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    name       VARCHAR(40) NOT NULL,
    slug       VARCHAR(40) NOT NULL,
    icon       VARCHAR(255) DEFAULT NULL,
    sort_order INT         NOT NULL DEFAULT 0,
    enabled    TINYINT(1)  NOT NULL DEFAULT 1,
    parent_id  BIGINT      DEFAULT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_category_slug (slug),
    KEY idx_category_parent (parent_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '商品分类';

CREATE TABLE item (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    seller_id       BIGINT        NOT NULL,
    category_id     BIGINT        NOT NULL,
    title           VARCHAR(80)   NOT NULL,
    description     TEXT          NOT NULL,
    price           DECIMAL(12, 2) NOT NULL,
    original_price  DECIMAL(12, 2) DEFAULT NULL,
    condition_level VARCHAR(16)   NOT NULL COMMENT 'NEW/LIKE_NEW/GOOD/FAIR',
    campus          VARCHAR(50)   DEFAULT NULL,
    trade_method    VARCHAR(16)   NOT NULL DEFAULT 'MEETUP' COMMENT 'MEETUP/EXPRESS',
    tags            VARCHAR(200)  DEFAULT NULL,
    cover_url       VARCHAR(255)  DEFAULT NULL,
    status          VARCHAR(20)   NOT NULL DEFAULT 'DRAFT'
                    COMMENT 'DRAFT/PENDING_REVIEW/ON_SALE/REJECTED/LOCKED/SOLD/OFF_SHELF',
    reject_reason   VARCHAR(200)  DEFAULT NULL,
    view_count      INT           NOT NULL DEFAULT 0,
    favorite_count  INT           NOT NULL DEFAULT 0,
    published_at    DATETIME(6)   DEFAULT NULL,
    sold_at         DATETIME(6)   DEFAULT NULL,
    version         BIGINT        NOT NULL DEFAULT 0,
    created_at      DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_item_status_category_created (status, category_id, created_at),
    KEY idx_item_seller_status (seller_id, status),
    KEY idx_item_price (price),
    KEY idx_item_published (published_at),
    CONSTRAINT fk_item_seller FOREIGN KEY (seller_id) REFERENCES app_user (id),
    CONSTRAINT fk_item_category FOREIGN KEY (category_id) REFERENCES category (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '商品';

CREATE TABLE item_image (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    item_id    BIGINT       NOT NULL,
    url        VARCHAR(255) NOT NULL,
    sort_order INT          NOT NULL DEFAULT 0,
    created_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_item_image_item_sort (item_id, sort_order),
    CONSTRAINT fk_item_image_item FOREIGN KEY (item_id) REFERENCES item (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '商品图片';

CREATE TABLE favorite (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    user_id    BIGINT      NOT NULL,
    item_id    BIGINT      NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_favorite_user_item (user_id, item_id),
    KEY idx_favorite_item (item_id),
    CONSTRAINT fk_favorite_user FOREIGN KEY (user_id) REFERENCES app_user (id),
    CONSTRAINT fk_favorite_item FOREIGN KEY (item_id) REFERENCES item (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '收藏';

-- ---------------------------------------------------------------------------
-- 会话与消息
-- ---------------------------------------------------------------------------
CREATE TABLE conversation (
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    item_id         BIGINT      NOT NULL,
    buyer_id        BIGINT      NOT NULL,
    seller_id       BIGINT      NOT NULL,
    last_message_at DATETIME(6) DEFAULT NULL,
    created_at      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_conversation_item_buyer_seller (item_id, buyer_id, seller_id),
    KEY idx_conversation_buyer (buyer_id),
    KEY idx_conversation_seller (seller_id),
    CONSTRAINT fk_conversation_item FOREIGN KEY (item_id) REFERENCES item (id) ON DELETE CASCADE,
    CONSTRAINT fk_conversation_buyer FOREIGN KEY (buyer_id) REFERENCES app_user (id),
    CONSTRAINT fk_conversation_seller FOREIGN KEY (seller_id) REFERENCES app_user (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '议价会话';

CREATE TABLE message (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    conversation_id BIGINT        NOT NULL,
    sender_id       BIGINT        NOT NULL,
    type            VARCHAR(16)   NOT NULL DEFAULT 'TEXT' COMMENT 'TEXT/IMAGE/OFFER',
    content         VARCHAR(1000) NOT NULL,
    offer_amount    DECIMAL(12, 2) DEFAULT NULL,
    read_flag       TINYINT(1)    NOT NULL DEFAULT 0,
    created_at      DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_message_conversation (conversation_id, id),
    CONSTRAINT fk_message_conversation FOREIGN KEY (conversation_id) REFERENCES conversation (id) ON DELETE CASCADE,
    CONSTRAINT fk_message_sender FOREIGN KEY (sender_id) REFERENCES app_user (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '会话消息';

-- ---------------------------------------------------------------------------
-- 订单与资金
-- ---------------------------------------------------------------------------
CREATE TABLE orders (
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    order_no       VARCHAR(32)   NOT NULL,
    item_id        BIGINT        NOT NULL,
    buyer_id       BIGINT        NOT NULL,
    seller_id      BIGINT        NOT NULL,
    amount         DECIMAL(12, 2) NOT NULL,
    fee_amount     DECIMAL(12, 2) NOT NULL DEFAULT 0,
    seller_income  DECIMAL(12, 2) NOT NULL DEFAULT 0,
    status         VARCHAR(16)   NOT NULL DEFAULT 'PENDING_PAY'
                   COMMENT 'PENDING_PAY/PAID/SHIPPED/COMPLETED/CANCELLED/REFUNDING/REFUNDED',
    trade_method   VARCHAR(16)   NOT NULL DEFAULT 'MEETUP',
    pay_deadline   DATETIME(6)   NOT NULL,
    paid_at        DATETIME(6)   DEFAULT NULL,
    shipped_at     DATETIME(6)   DEFAULT NULL,
    completed_at   DATETIME(6)   DEFAULT NULL,
    cancelled_at   DATETIME(6)   DEFAULT NULL,
    cancel_reason  VARCHAR(200)  DEFAULT NULL,
    version        BIGINT        NOT NULL DEFAULT 0,
    created_at     DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at     DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_order_buyer_status (buyer_id, status),
    KEY idx_order_seller_status (seller_id, status),
    KEY idx_order_status_deadline (status, pay_deadline),
    CONSTRAINT fk_order_item FOREIGN KEY (item_id) REFERENCES item (id),
    CONSTRAINT fk_order_buyer FOREIGN KEY (buyer_id) REFERENCES app_user (id),
    CONSTRAINT fk_order_seller FOREIGN KEY (seller_id) REFERENCES app_user (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '订单';

CREATE TABLE order_status_log (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    order_id    BIGINT       NOT NULL,
    from_status VARCHAR(16)  NOT NULL,
    to_status   VARCHAR(16)  NOT NULL,
    operator_id BIGINT       DEFAULT NULL,
    remark      VARCHAR(200) DEFAULT NULL,
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_order_log_order (order_id, id),
    CONSTRAINT fk_order_log_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '订单状态变更日志';

CREATE TABLE wallet (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    user_id      BIGINT        NOT NULL,
    balance      DECIMAL(12, 2) NOT NULL DEFAULT 0,
    frozen       DECIMAL(12, 2) NOT NULL DEFAULT 0,
    total_income DECIMAL(12, 2) NOT NULL DEFAULT 0,
    total_spent  DECIMAL(12, 2) NOT NULL DEFAULT 0,
    version      BIGINT        NOT NULL DEFAULT 0,
    created_at   DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at   DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_wallet_user (user_id),
    CONSTRAINT fk_wallet_user FOREIGN KEY (user_id) REFERENCES app_user (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '钱包';

CREATE TABLE payment_transaction (
    id         BIGINT        NOT NULL AUTO_INCREMENT,
    trade_no   VARCHAR(32)   NOT NULL,
    order_id   BIGINT        DEFAULT NULL,
    user_id    BIGINT        NOT NULL,
    type       VARCHAR(16)   NOT NULL COMMENT 'ESCROW_IN/ESCROW_OUT/REFUND/INCOME/FEE/RECHARGE',
    amount     DECIMAL(12, 2) NOT NULL,
    status     VARCHAR(16)   NOT NULL DEFAULT 'SUCCESS' COMMENT 'SUCCESS/FAILED',
    remark     VARCHAR(200)  DEFAULT NULL,
    created_at DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_trade_no (trade_no),
    KEY idx_tx_order (order_id),
    KEY idx_tx_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '支付流水';

CREATE TABLE wallet_ledger (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    wallet_id     BIGINT        NOT NULL,
    user_id       BIGINT        NOT NULL,
    order_id      BIGINT        DEFAULT NULL,
    type          VARCHAR(16)   NOT NULL COMMENT 'RECHARGE/ESCROW_OUT/ESCROW_IN/REFUND/INCOME/FEE',
    amount        DECIMAL(12, 2) NOT NULL,
    balance_after DECIMAL(12, 2) NOT NULL,
    remark        VARCHAR(200)  DEFAULT NULL,
    created_at    DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_ledger_wallet (wallet_id, id),
    KEY idx_ledger_user_created (user_id, created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '钱包流水';

-- ---------------------------------------------------------------------------
-- 评价与信用
-- ---------------------------------------------------------------------------
CREATE TABLE review (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    order_id    BIGINT        NOT NULL,
    reviewer_id BIGINT        NOT NULL,
    reviewee_id BIGINT        NOT NULL,
    item_id     BIGINT        NOT NULL,
    direction   VARCHAR(20)   NOT NULL COMMENT 'BUYER_TO_SELLER/SELLER_TO_BUYER',
    rating      INT           NOT NULL COMMENT '1-5',
    tags        VARCHAR(200)  DEFAULT NULL,
    comment     VARCHAR(500)  DEFAULT NULL,
    created_at  DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_review_order_reviewer (order_id, reviewer_id),
    KEY idx_review_reviewee (reviewee_id, created_at),
    CONSTRAINT fk_review_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT fk_review_reviewer FOREIGN KEY (reviewer_id) REFERENCES app_user (id),
    CONSTRAINT fk_review_reviewee FOREIGN KEY (reviewee_id) REFERENCES app_user (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '评价';

CREATE TABLE reputation (
    id               BIGINT        NOT NULL AUTO_INCREMENT,
    user_id          BIGINT        NOT NULL,
    credit_score     INT           NOT NULL DEFAULT 80,
    badge            VARCHAR(20)   NOT NULL DEFAULT 'NORMAL' COMMENT 'ELITE/RELIABLE/NORMAL/WATCH',
    total_deals      INT           NOT NULL DEFAULT 0,
    completed_deals  INT           NOT NULL DEFAULT 0,
    cancelled_deals  INT           NOT NULL DEFAULT 0,
    refunded_deals   INT           NOT NULL DEFAULT 0,
    total_reviews    INT           NOT NULL DEFAULT 0,
    good_reviews     INT           NOT NULL DEFAULT 0,
    bad_reviews      INT           NOT NULL DEFAULT 0,
    dispute_count    INT           NOT NULL DEFAULT 0,
    report_count     INT           NOT NULL DEFAULT 0,
    avg_rating       DECIMAL(3, 2) NOT NULL DEFAULT 0,
    created_at       DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at       DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_reputation_user (user_id),
    CONSTRAINT fk_reputation_user FOREIGN KEY (user_id) REFERENCES app_user (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '信用档案';

-- ---------------------------------------------------------------------------
-- 风控与通知
-- ---------------------------------------------------------------------------
CREATE TABLE report (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    reporter_id   BIGINT       NOT NULL,
    target_type   VARCHAR(16)  NOT NULL COMMENT 'ITEM/USER',
    target_id     BIGINT       NOT NULL,
    reason_type   VARCHAR(20)  NOT NULL COMMENT 'COUNTERFEIT/SCAM/ABUSE/OTHER',
    description   VARCHAR(500) NOT NULL,
    evidence_url  VARCHAR(255) DEFAULT NULL,
    status        VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/PROCESSING/RESOLVED/REJECTED',
    handler_id    BIGINT       DEFAULT NULL,
    handle_remark VARCHAR(300) DEFAULT NULL,
    handled_at    DATETIME(6)  DEFAULT NULL,
    created_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_report_status_created (status, created_at),
    KEY idx_report_target (target_type, target_id),
    CONSTRAINT fk_report_reporter FOREIGN KEY (reporter_id) REFERENCES app_user (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '举报工单';

CREATE TABLE notification (
    id         BIGINT        NOT NULL AUTO_INCREMENT,
    user_id    BIGINT        NOT NULL,
    type       VARCHAR(20)   NOT NULL COMMENT 'ITEM_AUDIT/ORDER/REPORT/REVIEW/SYSTEM',
    title      VARCHAR(100)  NOT NULL,
    content    VARCHAR(500)  NOT NULL,
    link       VARCHAR(255)  DEFAULT NULL,
    read_flag  TINYINT(1)    NOT NULL DEFAULT 0,
    created_at DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_notification_user_read (user_id, read_flag),
    CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES app_user (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '站内通知';

CREATE TABLE audit_log (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    actor_id    BIGINT       DEFAULT NULL,
    action      VARCHAR(40)  NOT NULL,
    target_type VARCHAR(16)  NOT NULL,
    target_id   BIGINT       DEFAULT NULL,
    detail      VARCHAR(1000) DEFAULT NULL,
    ip          VARCHAR(64)  DEFAULT NULL,
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_audit_actor_created (actor_id, created_at),
    KEY idx_audit_target (target_type, target_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '审计日志';
