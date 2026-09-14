-- Un SteamUser possède au plus un Deck
CREATE TABLE deck
(
    id            BIGINT NOT NULL AUTO_INCREMENT,
    steam_user_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_deck_user (steam_user_id),
    CONSTRAINT fk_deck_user FOREIGN KEY (steam_user_id) REFERENCES steam_user (id) ON DELETE CASCADE
) ENGINE = InnoDB;
