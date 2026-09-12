CREATE TABLE market_listing (
    id                    BIGINT NOT NULL AUTO_INCREMENT,
    seller_id             BIGINT NOT NULL,
    card_id               BIGINT NOT NULL,
    price                 INT    NOT NULL,
    created_at            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_listing_card (card_id),
    CONSTRAINT fk_listing_seller FOREIGN KEY (seller_id) REFERENCES steam_user(id)  ON DELETE CASCADE,
    CONSTRAINT fk_listing_card   FOREIGN KEY (card_id)   REFERENCES user_card(id)   ON DELETE CASCADE
) ENGINE=InnoDB;
