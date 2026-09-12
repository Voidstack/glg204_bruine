-- Historique des achats en boutique (visible par l'utilisateur concerné et par l'admin).
-- On conserve un snapshot du pack (nom, emoji) pour rester lisible après modification/suppression du pack.
CREATE TABLE shop_purchase
(
    id               BIGINT      NOT NULL AUTO_INCREMENT,
    steam_user_id    BIGINT      NOT NULL,
    pack_name        VARCHAR(60) NOT NULL,
    pack_emoji       VARCHAR(10) NOT NULL,
    points_credited  INT         NOT NULL,
    price_cents_paid INT         NOT NULL,
    promo_percent    INT         NOT NULL DEFAULT 0,
    -- Identifiant de la session Stripe Checkout ayant donné lieu à l'achat.
    -- Sert de garde-fou d'idempotence : une session payée ne crédite les points qu'une seule fois.
    stripe_session_id VARCHAR(255) NULL,
    created_at       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_shop_purchase_user (steam_user_id),
    CONSTRAINT uq_shop_purchase_stripe_session UNIQUE (stripe_session_id),
    CONSTRAINT fk_shop_purchase_user FOREIGN KEY (steam_user_id)
        REFERENCES steam_user (id) ON DELETE CASCADE
) ENGINE = InnoDB;
