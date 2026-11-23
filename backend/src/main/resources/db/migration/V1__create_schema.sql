CREATE TABLE users (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    created_at    DATETIME(6)  NOT NULL,
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE categories (
    id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT      NOT NULL,
    name    VARCHAR(50) NOT NULL,
    color   VARCHAR(7)  NOT NULL,
    CONSTRAINT uk_categories_user_name UNIQUE (user_id, name),
    CONSTRAINT fk_categories_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE expenses (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id        BIGINT         NOT NULL,
    category_id    BIGINT         NOT NULL,
    amount         DECIMAL(12, 2) NOT NULL,
    expense_date   DATE           NOT NULL,
    description    VARCHAR(255),
    payment_method VARCHAR(20)    NOT NULL,
    created_at     DATETIME(6)    NOT NULL,
    updated_at     DATETIME(6)    NOT NULL,
    CONSTRAINT fk_expenses_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_expenses_category FOREIGN KEY (category_id) REFERENCES categories (id)
);

-- Every list and report filters on the user and a date range
CREATE INDEX idx_expenses_user_date ON expenses (user_id, expense_date);

CREATE TABLE budgets (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT         NOT NULL,
    category_id  BIGINT         NOT NULL,
    budget_month VARCHAR(7)     NOT NULL,
    amount       DECIMAL(12, 2) NOT NULL,
    CONSTRAINT uk_budgets_user_category_month UNIQUE (user_id, category_id, budget_month),
    CONSTRAINT fk_budgets_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_budgets_category FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE CASCADE
);
