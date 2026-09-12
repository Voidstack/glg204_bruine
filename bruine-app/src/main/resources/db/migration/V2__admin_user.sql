create table admin_user
(
    username     varchar(20),
    primary key (username),
    userpassword varchar(500) not null,
    mfa_secret   varchar(64)  null,
    mfa_enabled  boolean      not null default false
) engine = innodb;

insert into admin_user (username, userpassword)
values ('admin', '{noop}admin'),
       ('ada', '{noop}cnam'),
       ('charles', '{noop}cnam'),
       ('joe', '{noop}cnam');