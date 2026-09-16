-- Exemplaire physique d'une carte possédé par un SteamUser : un id par exemplaire,
-- pour permettre à un joueur d'avoir plusieurs fois le même reward
CREATE TABLE user_card
(
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    steam_user_id   BIGINT      NOT NULL,
    gacha_reward_id BIGINT      NOT NULL,
    -- nom d'une constante de ECardFinish (@Enumerated(STRING)), collation binaire sinon la CHECK ignore la casse
    finish          VARCHAR(20) COLLATE utf8mb4_bin NOT NULL DEFAULT 'NORMAL',
    PRIMARY KEY (id),
    CONSTRAINT chk_user_card_finish CHECK (finish IN ('NORMAL', 'HOLOGRAPHIC', 'FOIL', 'POLYCHROME', 'NEGATIVE')),
    CONSTRAINT fk_user_card_user FOREIGN KEY (steam_user_id) REFERENCES steam_user (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_card_reward FOREIGN KEY (gacha_reward_id) REFERENCES gacha_reward (id) ON DELETE CASCADE
) ENGINE = InnoDB;
