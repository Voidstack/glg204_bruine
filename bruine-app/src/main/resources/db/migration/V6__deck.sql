-- Un steam_user possède un deck
CREATE TABLE deck
(
    id            BIGINT NOT NULL AUTO_INCREMENT,
    steam_user_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_deck_user (steam_user_id),
    CONSTRAINT fk_deck_user FOREIGN KEY (steam_user_id) REFERENCES steam_user (id) ON DELETE CASCADE
) ENGINE = InnoDB;

-- Contenu du Deck. Que la carte appartienne au propriétaire du deck est vérifié dans DeckService.
CREATE TABLE deck_card
(
    deck_id      BIGINT NOT NULL,
    slot         INT    NOT NULL,
    user_card_id BIGINT NOT NULL,
    PRIMARY KEY (deck_id, slot),
    UNIQUE KEY uq_deck_card_card (user_card_id),
    CONSTRAINT chk_deck_card_slot CHECK (slot BETWEEN 0 AND 9),
    CONSTRAINT fk_deck_card_deck FOREIGN KEY (deck_id) REFERENCES deck (id) ON DELETE CASCADE,
    CONSTRAINT fk_deck_card_card FOREIGN KEY (user_card_id) REFERENCES user_card (id) ON DELETE CASCADE
) ENGINE = InnoDB;

-- Deck vide pour les joueurs de démonstration créés dans V3
INSERT INTO deck (steam_user_id)
SELECT id
FROM steam_user;
