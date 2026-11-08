CREATE TABLE app_user (
    id BIGINT NOT NULL AUTO_INCREMENT,
    email_normalized VARCHAR(254) COLLATE utf8mb4_0900_as_cs NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_app_user PRIMARY KEY (id),
    CONSTRAINT uk_app_user_email UNIQUE (email_normalized),
    CONSTRAINT ck_app_user_email_nonempty CHECK (CHAR_LENGTH(TRIM(email_normalized)) > 0),
    CONSTRAINT ck_app_user_password_nonempty CHECK (CHAR_LENGTH(TRIM(password_hash)) > 0)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
