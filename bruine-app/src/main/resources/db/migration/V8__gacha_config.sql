CREATE TABLE gacha_config
(
    id                  INT NOT NULL DEFAULT 1,
    rarity_legendary    INT NOT NULL DEFAULT 1,
    rarity_epic         INT NOT NULL DEFAULT 4,
    rarity_rare         INT NOT NULL DEFAULT 15,
    rarity_uncommon     INT NOT NULL DEFAULT 30,
    rarity_common       INT NOT NULL DEFAULT 50,
    finish_negative     INT NOT NULL DEFAULT 5,
    finish_polychrome   INT NOT NULL DEFAULT 15,
    finish_foil         INT NOT NULL DEFAULT 30,
    finish_holographic  INT NOT NULL DEFAULT 50,
    finish_normal       INT NOT NULL DEFAULT 900,
    xp_base_common      INT NOT NULL DEFAULT 10,
    xp_base_uncommon    INT NOT NULL DEFAULT 25,
    xp_base_rare        INT NOT NULL DEFAULT 75,
    xp_base_epic        INT NOT NULL DEFAULT 200,
    xp_base_legendary   INT NOT NULL DEFAULT 500,
    xp_mult_normal      INT NOT NULL DEFAULT 1,
    xp_mult_holographic INT NOT NULL DEFAULT 2,
    xp_mult_foil        INT NOT NULL DEFAULT 3,
    xp_mult_polychrome  INT NOT NULL DEFAULT 4,
    xp_mult_negative    INT NOT NULL DEFAULT 5,
    PRIMARY KEY (id),
    CONSTRAINT chk_single_row CHECK (id = 1)
) ENGINE = InnoDB;

INSERT INTO gacha_config (id)
VALUES (1);
