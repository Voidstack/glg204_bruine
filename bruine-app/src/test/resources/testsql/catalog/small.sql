
delete from customer;
delete from item;
delete from product;
delete from category;
delete from yaps_user;
delete from item_bundle;
delete from tag_table;
delete from category_tag;



delete from sequence_id where table_name = 'customer';
delete from sequence_id where table_name = 'item';

insert into sequence_id values ('customer', 2);
insert into sequence_id values ('item', 0);


insert into yaps_user values 
(
        'u1', 'CUST', '{noop}cnam'
);

insert into customer 
    (id, firstname, lastname, telephone, email,
     street1, street2, city, state, zipcode, country, username) 
    values ('1', 'firstname1', 'lastname1', 'telephone1', 'email1', 
        'street1_1', 'street2_1', 'city1', 'state1', 'zipcode1', 'country1', 'u1');

insert into category values ('c1a', 'c1b', 'c1c' );
insert into category values ('c2a', 'c2b', 'c2c' );

insert into product values ('p1', 'p1a', 'p1b', 'c1a');
insert into product values ('p2', 'p2a', 'p2b', 'c1a');
insert into product values ('p3', 'p3a', 'p3b', 'c2a');

insert into item (id, name, unit_cost, product_fk, image_path) values ('it1a', 'it1b', '10.00', 'p2', 'it1.jpg');
insert into item  (id, name, unit_cost, product_fk, image_path) values ('it2a', 'it2b', '30.00', 'p2', 'it2.jpg');
insert into item  (id, name, unit_cost, product_fk, image_path) values ('it3a', 'it3b', '20.00', 'p3', 'it3.jpg');
insert into item  (id, name, unit_cost, product_fk, image_path, is_available) values ('it4a', 'it4b', '20.00', 'p3', 'it3.jpg', 0);


/* Quelques données */

insert into tag_table values ('NEW', 'Nouveauté');
insert into tag_table values ('PROMO', 'Promotion');
insert into tag_table values ('EXCLU', 'Exclusivité');
insert into tag_table values ('FRIEND', 'Affectueux');
insert into tag_table values ('PLAYFULL', 'Joueur');
insert into tag_table values ('CHEAP', 'Économique');

insert into category_tag values ('c1a', 'FRIEND');
insert into category_tag values ('c1a', 'PROMO');
insert into category_tag values ('c2a', 'NEW');
