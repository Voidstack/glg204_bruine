-- Table des récompenses du mini-jeu gacha
CREATE TABLE gacha_reward
(
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    emoji       VARCHAR(10)  NOT NULL,
    rarity      VARCHAR(20)  NOT NULL,
    description VARCHAR(255) NULL,
    name        VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_gacha_reward_rarity (rarity)
) ENGINE = InnoDB;

INSERT INTO gacha_reward (emoji, name, rarity, description)
VALUES ('⭐', 'Étoile Légendaire', 'legendary', 'Signe de prestige absolu'),
       ('🏆', 'Champion Suprême', 'legendary', 'Le titre ultime des grands joueurs'),
       ('🌠', 'Étoile Filante', 'legendary', 'Aussi rare qu''une nuit parfaite');

INSERT INTO gacha_reward (emoji, name, rarity, description)
VALUES ('💎', 'Cristal Épique', 'epic', 'Gemme réservée aux joueurs d''élite'),
       ('🔥', 'Flamme de la Victoire', 'epic', 'Symbole de puissance et de persévérance'),
       ('🦅', 'Aigle Royal', 'epic', 'Majesté et précision dans chaque partie'),
       ('🎖️', 'Médaille d''Honneur', 'epic', 'Décernée aux guerriers du classement');

INSERT INTO gacha_reward (emoji, name, rarity, description)
VALUES ('🔷', 'Diamant Rare', 'rare', 'Difficile à obtenir, précieux à garder'),
       ('🎯', 'Tireur d''Élite', 'rare', 'Pour ceux qui visent toujours juste'),
       ('🌟', 'Éclat de Gloire', 'rare', 'Souvenir de tes meilleures parties'),
       ('⚡', 'Éclair Rapide', 'rare', 'Réflexes hors du commun'),
       ('🗡️', 'Lame Acérée', 'rare', 'Tranchante comme ton skill');

INSERT INTO gacha_reward (emoji, name, rarity, description)
VALUES ('🟢', 'Badge Vert', 'uncommon', 'Pour les joueurs réguliers et constants'),
       ('🎮', 'Manette d''Argent', 'uncommon', 'Pour les joueurs assidus'),
       ('🛡️', 'Bouclier Solide', 'uncommon', 'Tu encaisses sans broncher'),
       ('🎵', 'Note Discordante', 'uncommon', 'Une touche de style unique'),
       ('🌊', 'Vague Bleue', 'uncommon', 'Comme une pluie fine.. une Bruine');

INSERT INTO gacha_reward (emoji, name, rarity, description)
VALUES ('⚪', 'Pierre Grise', 'common', 'La récompense de base'),
       ('🎲', 'Dé Commun', 'common', 'La chance est le premier pas'),
       ('📦', 'Caisse Standard', 'common', 'Qui sait ce qu''il y a dedans ?'),
       ('🍀', 'Trèfle', 'common', 'Peut-être le début de quelque chose de grand'),
       ('🔩', 'Écrou Rouillé', 'common', 'Humble début d''une longue aventure');