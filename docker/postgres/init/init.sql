CREATE TABLE users (
                       id SERIAL PRIMARY KEY,
                       external_id UUID UNIQUE NOT NULL DEFAULT gen_random_uuid(),
                       username VARCHAR(255) UNIQUE NOT NULL,
                       password VARCHAR(255) NOT NULL,
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- CREATE TABLE groups (
--                         id SERIAL PRIMARY KEY,
--                         external_id UUID UNIQUE NOT NULL DEFAULT gen_random_uuid(),
--                         name VARCHAR(255) NOT NULL,
--                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
-- );
--
-- CREATE TABLE group_users (
--                              id SERIAL PRIMARY KEY,
--                              external_id UUID UNIQUE NOT NULL DEFAULT gen_random_uuid(),
--                              user_id INT NOT NULL,
--                              group_id INT NOT NULL,
--                              role VARCHAR(255) NOT NULL,
--                              created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
--                              FOREIGN KEY (user_id) REFERENCES users(id),
--                              FOREIGN KEY (group_id) REFERENCES workspaces(id),
--                              UNIQUE (user_id, group_id)
-- );

CREATE TABLE spaces (
                        id SERIAL PRIMARY KEY,
    -- 명칭 일관성을 위해 'external_id'로 통일하고 DEFAULT 값 추가
                        external_id UUID UNIQUE NOT NULL DEFAULT gen_random_uuid(),
--                         group_id INT NOT NULL,
                        user_id INT NOT NULL,
                        name VARCHAR(255) NOT NULL,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
--                         FOREIGN KEY (group_id) REFERENCES groups(id)
                        FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE shelves (
                         id SERIAL PRIMARY KEY,
                         external_id UUID UNIQUE NOT NULL DEFAULT gen_random_uuid(),
    -- 선반은 반드시 특정 공간에 속해야 하므로 NOT NULL
                         space_id INT NOT NULL,
                         name VARCHAR(255) NOT NULL,
                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                         FOREIGN KEY (space_id) REFERENCES spaces(id) ON DELETE CASCADE
);

CREATE TABLE boxes (
                       id SERIAL PRIMARY KEY,
                       external_id UUID UNIQUE NOT NULL DEFAULT gen_random_uuid(),
    -- 박스는 특정 선반에 속해야 하므로 NOT NULL
                       shelf_id INT NOT NULL,
                       name VARCHAR(255) NOT NULL,
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       FOREIGN KEY (shelf_id) REFERENCES shelves(id) ON DELETE CASCADE
);

-- 역할 재정의: 아이템의 '마스터 정보'만 관리 (위치, 재고 정보 없음)
CREATE TABLE items (
                       id SERIAL PRIMARY KEY,
                       external_id UUID UNIQUE NOT NULL DEFAULT gen_random_uuid(),
--                        group_id INT NOT NULL,
                       user_id INT NOT NULL,
                       name VARCHAR(255) NOT NULL,
--     -- 예를 들어 '바코드', '규격' 등의 컬럼이 추가될 수 있음
--     sku VARCHAR(100) UNIQUE, -- Stock Keeping Unit
                       description TEXT,
                       active BOOLEAN NOT NULL DEFAULT TRUE,
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       FOREIGN KEY (user_id) REFERENCES users(id)
--                        FOREIGN KEY (group_id) REFERENCES groups(id) ON DELETE CASCADE
);

-- stocks: row 1개 = 물리적 단위 1개
CREATE TABLE stocks (
                        id              SERIAL PRIMARY KEY,
                        external_id     UUID UNIQUE NOT NULL DEFAULT gen_random_uuid(),
                        item_id         INT NOT NULL,
                        space_id        INT NOT NULL,
                        shelf_id        INT,
                        box_id          INT,
                        serial_number   VARCHAR(255),
                        lot_number      VARCHAR(255),
                        expiration_date DATE,
                        memo            TEXT,
                        status          VARCHAR(20) NOT NULL DEFAULT 'IN_STOCK',
                        created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (item_id)  REFERENCES items(id),
                        FOREIGN KEY (space_id) REFERENCES spaces(id),
                        FOREIGN KEY (shelf_id) REFERENCES shelves(id) ON DELETE CASCADE,
                        FOREIGN KEY (box_id)   REFERENCES boxes(id)   ON DELETE CASCADE,
                        CONSTRAINT chk_box_requires_shelf CHECK (box_id IS NULL OR shelf_id IS NOT NULL),
                        CONSTRAINT chk_stock_status CHECK (status IN ('IN_STOCK', 'DISPATCHED', 'LOST', 'DAMAGED', 'DISPOSED'))
);

CREATE TABLE stock_transactions (
                                    id               SERIAL PRIMARY KEY,
                                    external_id      UUID UNIQUE NOT NULL DEFAULT gen_random_uuid(),
                                    stock_id         INT NOT NULL,
                                    transaction_type VARCHAR(20) NOT NULL,
                                    from_space_id    INT,
                                    from_shelf_id    INT,
                                    from_box_id      INT,
                                    to_space_id      INT,
                                    to_shelf_id      INT,
                                    to_box_id        INT,
                                    memo             TEXT,
                                    created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                    FOREIGN KEY (stock_id) REFERENCES stocks(id) ON DELETE CASCADE,
                                    FOREIGN KEY (from_space_id) REFERENCES spaces(id),
                                    FOREIGN KEY (from_shelf_id) REFERENCES shelves(id),
                                    FOREIGN KEY (from_box_id)   REFERENCES boxes(id),
                                    FOREIGN KEY (to_space_id)   REFERENCES spaces(id),
                                    FOREIGN KEY (to_shelf_id)   REFERENCES shelves(id),
                                    FOREIGN KEY (to_box_id)     REFERENCES boxes(id),
                                    CONSTRAINT chk_transaction_type CHECK (transaction_type IN ('IN', 'OUT', 'MOVE', 'ADJUST'))
);

CREATE TABLE images (
                        id                SERIAL PRIMARY KEY,
                        external_id       UUID UNIQUE NOT NULL DEFAULT gen_random_uuid(),
                        user_id           INT NOT NULL,
                        storage_path      TEXT NOT NULL,
                        original_filename VARCHAR(255),
                        content_type      VARCHAR(100),
                        size_bytes        BIGINT,
                        content_hash      VARCHAR(64),
                        created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                        CONSTRAINT uq_images_user_hash UNIQUE (user_id, content_hash)
);

CREATE TABLE item_images (
                             item_id       INT NOT NULL,
                             image_id      INT NOT NULL,
                             display_order INT NOT NULL DEFAULT 0,
                             is_primary    BOOLEAN NOT NULL DEFAULT FALSE,
                             created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             PRIMARY KEY (item_id, image_id),
                             FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE,
                             FOREIGN KEY (image_id) REFERENCES images(id) ON DELETE CASCADE
);

CREATE TABLE stock_images (
                              stock_id      INT NOT NULL,
                              image_id      INT NOT NULL,
                              display_order INT NOT NULL DEFAULT 0,
                              is_primary    BOOLEAN NOT NULL DEFAULT FALSE,
                              created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              PRIMARY KEY (stock_id, image_id),
                              FOREIGN KEY (stock_id) REFERENCES stocks(id) ON DELETE CASCADE,
                              FOREIGN KEY (image_id) REFERENCES images(id) ON DELETE CASCADE
);
