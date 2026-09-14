-- Remplacement de la table de jointure : ajout d'un id auto-increment
-- pour permettre à un SteamUser d'avoir plusieurs fois le même reward

-- Une carte appartient à au plus un deck à la fois : c'est une relation 1-N portée par
-- user_card lui-même, pas une table de jointure N-N. deck_id NULL signifie « hors deck ».
-- ON DELETE SET NULL : si un deck disparaissait, ses cartes ne seraient pas détruites,
-- seulement détachées.

CREATE TABLE user_card (
    id               BIGINT NOT NULL AUTO_INCREMENT,
    steam_user_id    BIGINT NOT NULL,
    gacha_reward_id BIGINT NOT NULL,
    finish VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    deck_id          BIGINT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_user_card_user   FOREIGN KEY (steam_user_id)    REFERENCES steam_user(id)    ON DELETE CASCADE,
    CONSTRAINT fk_user_card_reward FOREIGN KEY (gacha_reward_id) REFERENCES gacha_reward(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_card_deck   FOREIGN KEY (deck_id)          REFERENCES deck(id)          ON DELETE SET NULL
) ENGINE=InnoDB;
