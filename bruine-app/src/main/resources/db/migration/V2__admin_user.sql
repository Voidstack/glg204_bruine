CREATE TABLE admin_user
(
    username     VARCHAR(20)  NOT NULL,
    userpassword VARCHAR(500) NOT NULL,
    mfa_secret   VARCHAR(64)  NULL,
    mfa_enabled  BOOLEAN      NOT NULL DEFAULT FALSE,
    PRIMARY KEY (username)
) ENGINE = InnoDB;

INSERT INTO admin_user (username, userpassword)
VALUES ('admin', '{noop}admin'),
       ('ada', '{noop}cnam'),
       ('charles', '{noop}cnam'),
       ('joe', '{noop}cnam');
