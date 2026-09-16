CREATE TABLE steam_user
(
    id                       BIGINT       NOT NULL AUTO_INCREMENT,
    steam_id                 VARCHAR(50)  NOT NULL UNIQUE,
    username                 VARCHAR(100) NOT NULL,
    created_at               DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at            DATETIME     NULL,
    initial_playtime_minutes BIGINT       NULL,
    current_playtime_minutes BIGINT       NULL,
    score                    INT          NOT NULL DEFAULT 0,
    total_pulls              BIGINT       NOT NULL DEFAULT 0,
    total_experience         BIGINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
) ENGINE = InnoDB;

INSERT INTO steam_user (steam_id, username, initial_playtime_minutes, current_playtime_minutes, score)
VALUES ('76561198100881386', 'Voidstack', 0, 300, 500),
       ('76561198000000000', 'TestUser1', 120, 150, 500),
       ('76561198000000001', 'TestUser2', 300, 350, 500),
       ('76561198000000002', 'TestUser3', 50, 75, 500);