-- Table des packs de la boutique (€ → points Bruine)
-- Gérée par l'admin : ajout / modification / suppression, promos temporaires, pack "Populaire".
CREATE TABLE shop_pack
(
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    name          VARCHAR(60) NOT NULL,
    emoji         VARCHAR(10) NOT NULL,
    points        INT         NOT NULL,
    bonus_points  INT         NOT NULL DEFAULT 0,
    price_cents   INT         NOT NULL,
    popular       BOOLEAN     NOT NULL DEFAULT FALSE,
    sort_order    INT         NOT NULL DEFAULT 0,
    -- Promo temporaire : réduction en % appliquée entre promo_start et promo_end (bornes nullables = pas de limite)
    promo_percent INT         NOT NULL DEFAULT 0,
    promo_start   DATETIME    NULL,
    promo_end     DATETIME    NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB;

-- Packs par défaut
INSERT INTO shop_pack (name, emoji, points, bonus_points, price_cents, popular, sort_order)
VALUES ('Gouttelette', '💧', 100, 0, 99, FALSE, 1),
       ('Ondée', '🌧️', 300, 50, 249, FALSE, 2),
       ('Averse', '⛈️', 700, 150, 499, TRUE, 3),
       ('Déluge', '🌊', 1500, 400, 999, FALSE, 4),
       ('Tempête', '🌪️', 3500, 1000, 1999, FALSE, 5),
       ('Mousson', '🌏', 8000, 2500, 3999, FALSE, 6);
