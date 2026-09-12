-- Un SteamUser possède au plus un Deck
CREATE TABLE deck
(
    id            BIGINT NOT NULL AUTO_INCREMENT,
    steam_user_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_deck_user (steam_user_id),
    CONSTRAINT fk_deck_user FOREIGN KEY (steam_user_id) REFERENCES steam_user (id) ON DELETE CASCADE
) ENGINE = InnoDB;

-- Une carte appartient à au plus un deck à la fois : c'est une relation 1-N portée par
-- user_card lui-même, pas une table de jointure N-N. NULL signifie « hors deck ».
-- ON DELETE SET NULL : si un deck disparaissait, ses cartes ne seraient pas détruites,
-- seulement détachées.
ALTER TABLE user_card
    ADD COLUMN deck_id BIGINT NULL,
    ADD CONSTRAINT fk_user_card_deck FOREIGN KEY (deck_id) REFERENCES deck (id) ON DELETE SET NULL;
