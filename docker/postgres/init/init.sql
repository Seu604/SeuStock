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
                         FOREIGN KEY (space_id) REFERENCES spaces(id)
);

CREATE TABLE boxes (
                       id SERIAL PRIMARY KEY,
                       external_id UUID UNIQUE NOT NULL DEFAULT gen_random_uuid(),
    -- 박스는 특정 선반에 속해야 하므로 NOT NULL
                       shelf_id INT NOT NULL,
                       name VARCHAR(255) NOT NULL,
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       FOREIGN KEY (shelf_id) REFERENCES shelves(id)
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
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       FOREIGN KEY (user_id) REFERENCES users(id)
--                        FOREIGN KEY (group_id) REFERENCES groups(id) ON DELETE CASCADE
);

-- 역할 재정의: '실물 재고'와 '유연한 위치'를 관리
CREATE TABLE stocks (
                        id SERIAL PRIMARY KEY,
                        external_id UUID UNIQUE NOT NULL DEFAULT gen_random_uuid(),
    -- 어떤 아이템에 대한 재고인지 명시 (NOT NULL)
                        item_id INT NOT NULL,
    -- 최소한의 위치 정보는 필요하므로 space_id는 NOT NULL
                        space_id INT NOT NULL,
    -- 선반이나 박스는 아직 모를 수 있으므로 NULL 허용
                        shelf_id INT,
                        box_id INT,
                        quantity INT NOT NULL DEFAULT 0,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (item_id) REFERENCES items(id),
                        FOREIGN KEY (space_id) REFERENCES spaces(id),
                        FOREIGN KEY (shelf_id) REFERENCES shelves(id),
                        FOREIGN KEY (box_id) REFERENCES boxes(id),
                        CHECK (quantity >= 0)
    -- CHECK 제약이나 트리거를 통해 (box_id -> shelf_id -> space_id) 관계의 일관성을 보장할 수 있음
    -- 예: CHECK (box_id IS NULL OR shelf_id IS NOT NULL) -> 박스가 있으면 선반도 있어야 함
);

CREATE TABLE stock_transactions (
                                    id SERIAL PRIMARY KEY,
                                    external_id UUID UNIQUE NOT NULL DEFAULT gen_random_uuid(),
                                    stock_id INT NOT NULL,
                                    transaction_type VARCHAR(255) NOT NULL,
                                    quantity_delta INT NOT NULL,
                                    memo TEXT,
                                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                    FOREIGN KEY (stock_id) REFERENCES stocks(id)
);