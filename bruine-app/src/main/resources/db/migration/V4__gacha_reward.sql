-- Table des récompenses du mini-jeu gacha
CREATE TABLE gacha_reward
(
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    emoji       VARCHAR(10)  NOT NULL,
    -- nom d'une constante de ECardRarity (@Enumerated(STRING)), collation binaire sinon la CHECK ignore la casse
    rarity      VARCHAR(20) COLLATE utf8mb4_bin NOT NULL,
    description VARCHAR(255) NULL,
    name        VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_gacha_reward_rarity (rarity),
    CONSTRAINT chk_gacha_reward_rarity CHECK (rarity IN ('COMMON', 'UNCOMMON', 'RARE', 'EPIC', 'LEGENDARY'))
) ENGINE = InnoDB;

INSERT INTO gacha_reward (emoji, name, rarity, description)
VALUES ('⭐', 'Étoile Légendaire', 'LEGENDARY', 'Signe de prestige absolu'),
       ('🏆', 'Champion Suprême', 'LEGENDARY', 'Le titre ultime des grands joueurs'),
       ('🌠', 'Étoile Filante', 'LEGENDARY', 'Aussi rare qu''une nuit parfaite');

INSERT INTO gacha_reward (emoji, name, rarity, description)
VALUES ('💎', 'Cristal Épique', 'EPIC', 'Gemme réservée aux joueurs d''élite'),
       ('🔥', 'Flamme de la Victoire', 'EPIC', 'Symbole de puissance et de persévérance'),
       ('🦅', 'Aigle Royal', 'EPIC', 'Majesté et précision dans chaque partie'),
       ('🎖️', 'Médaille d''Honneur', 'EPIC', 'Décernée aux guerriers du classement');

INSERT INTO gacha_reward (emoji, name, rarity, description)
VALUES ('🔷', 'Diamant Rare', 'RARE', 'Difficile à obtenir, précieux à garder'),
       ('🎯', 'Tireur d''Élite', 'RARE', 'Pour ceux qui visent toujours juste'),
       ('🌟', 'Éclat de Gloire', 'RARE', 'Souvenir de tes meilleures parties'),
       ('⚡', 'Éclair Rapide', 'RARE', 'Réflexes hors du commun'),
       ('🗡️', 'Lame Acérée', 'RARE', 'Tranchante comme ton skill');

INSERT INTO gacha_reward (emoji, name, rarity, description)
VALUES ('🟢', 'Badge Vert', 'UNCOMMON', 'Pour les joueurs réguliers et constants'),
       ('🎮', 'Manette d''Argent', 'UNCOMMON', 'Pour les joueurs assidus'),
       ('🛡️', 'Bouclier Solide', 'UNCOMMON', 'Tu encaisses sans broncher'),
       ('🎵', 'Note Discordante', 'UNCOMMON', 'Une touche de style unique'),
       ('🌊', 'Vague Bleue', 'UNCOMMON', 'Comme une pluie fine.. une Bruine');

INSERT INTO gacha_reward (emoji, name, rarity, description)
VALUES ('⚪', 'Pierre Grise', 'COMMON', 'La récompense de base'),
       ('🎲', 'Dé Commun', 'COMMON', 'La chance est le premier pas'),
       ('📦', 'Caisse Standard', 'COMMON', 'Qui sait ce qu''il y a dedans ?'),
       ('🍀', 'Trèfle', 'COMMON', 'Peut-être le début de quelque chose de grand'),
       ('🔩', 'Écrou Rouillé', 'COMMON', 'Humble début d''une longue aventure');