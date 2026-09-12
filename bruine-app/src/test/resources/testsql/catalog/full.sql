
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

insert into category values ('FISH', 'Fish', 'Any of numerous cold-blooded aquatic vertebrates characteristically having fins, gills, and a streamlined body' );
insert into category values ('DOGS', 'Dogs', 'A domesticated carnivorous mammal related to the foxes and wolves and raised in a wide variety of breeds' );
insert into category values ('REPTILES', 'Reptiles', 'Any of various cold-blooded, usually egg-laying vertebrates, such as a snake, lizard, crocodile, turtle' );
insert into category values ('CATS', 'Cats', 'Small carnivorous mammal domesticated since early times as a catcher of rats and mice and as a pet and existing in several distinctive breeds and varieties' );
insert into category values ('BIRDS', 'Birds', 'Any of the class Aves of warm-blooded, egg-laying, feathered vertebrates with forelimbs modified to form wings' );

insert into product values ('FISW01', 'Angelfish', 'Saltwater fish from Australia', 'FISH');
insert into product values ('FISW02', 'Tiger Shark', 'Saltwater fish from Australia', 'FISH');
insert into product values ('FIFW01', 'Koi', 'Freshwater fish from Japan', 'FISH');
insert into product values ('FIFW02', 'Goldfish', 'Freshwater fish from China', 'FISH');
insert into product values ('K9BD01', 'Bulldog', 'Friendly dog from England', 'DOGS');
insert into product values ('K9PO02', 'Poodle', 'Cute dog from France', 'DOGS');
insert into product values ('K9DL01', 'Dalmation', 'Great dog for a fire station', 'DOGS');
insert into product values ('K9RT01', 'Golden Retriever', 'Great family dog', 'DOGS');
insert into product values ('K9RT02', 'Labrador Retriever', 'Great hunting dog', 'DOGS');
insert into product values ('K9CW01', 'Chihuahua', 'Great companion dog', 'DOGS');
insert into product values ('RPSN01', 'Rattlesnake', 'Doubles as a watch dog', 'REPTILES');
insert into product values ('RPLI02', 'Iguana', 'Friendly green friend', 'REPTILES');
insert into product values ('FLDSH01', 'Manx', 'Great for reducing mouse populations', 'CATS');
insert into product values ('FLDLH02', 'Persian', 'Friendly house cat, doubles as a princess', 'CATS');
insert into product values ('AVCB01', 'Amazon Parrot', 'Great companion for up to 75 years', 'BIRDS');
insert into product values ('AVSB02', 'Finch', 'Great stress reliever', 'BIRDS');


insert into item  values ('EST1', 'Large', '10.00', 'FISW01', 'fish1.jpg', 1);
insert into item  values ('EST2', 'Thootless', '10.00', 'FISW01', 'fish1.jpg', 1);
insert into item  values ('EST3', 'Spotted', '12.00', 'FISW02', 'fish4.jpg', 1);
insert into item  values ('EST4', 'Spotless', '12.00', 'FISW02', 'fish4.jpg', 1);
insert into item  values ('EST5', 'Male Adult', '12.00', 'FIFW01', 'fish3.jpg', 1);
insert into item  values ('EST6', 'Female Adult', '12.00', 'FIFW01', 'fish3.jpg', 1);
insert into item  values ('EST7', 'Male Puppy', '12.00', 'FIFW02', 'fish2.jpg', 1);
insert into item  values ('EST8', 'Female Puppy', '12.00', 'FIFW02', 'fish2.jpg', 1);
insert into item  values ('EST9', 'Spotless Male Puppy', '22.00', 'K9BD01', 'dog1.jpg', 1);
insert into item  values ('EST10', 'Spotless Female Puppy', '22.00', 'K9BD01', 'dog1.jpg', 1);
insert into item  values ('EST11', 'Spotted Male Puppy', '32.00', 'K9PO02', 'dog2.jpg', 1);
insert into item  values ('EST12', 'Spotted Female Puppy', '32.00', 'K9PO02', 'dog2.jpg', 1);
insert into item  values ('EST13', 'Tailed', '62.00', 'K9DL01', 'dog3.jpg', 1);
insert into item  values ('EST14', 'Tailless', '62.00', 'K9DL01', 'dog3.jpg', 1);
insert into item  values ('EST15', 'Tailed', '82.00', 'K9RT01', 'dog4.jpg', 1);
insert into item  values ('EST16', 'Tailless', '82.00', 'K9RT01', 'dog4.jpg', 1);
insert into item  values ('EST17', 'Tailed', '100.00', 'K9RT02', 'dog5.jpg', 1);
insert into item  values ('EST18', 'Tailless', '100.00', 'K9RT02', 'dog5.jpg', 1);
insert into item  values ('EST19', 'Female Adult', '100.00', 'K9CW01', 'dog6.jpg', 1);
insert into item  values ('EST20', 'Female Adult', '100.00', 'K9CW01', 'dog6.jpg', 1);
insert into item  values ('EST21', 'Female Adult', '20.00', 'RPSN01', 'snake1.jpg', 1);
insert into item  values ('EST22', 'Male Adult', '20.00', 'RPSN01', 'snake1.jpg', 1);
insert into item  values ('EST211', 'Female Adult', '20.00', 'RPLI02', 'lizard1.jpg', 1);
insert into item  values ('EST221', 'Male Adult', '20.00', 'RPLI02', 'lizard1.jpg', 1);
insert into item  values ('EST23', 'Male Adult', '120.00','FLDSH01', 'cat1.jpg', 1);
insert into item  values ('EST24', 'Female Adult', '120.00', 'FLDSH01', 'cat1.jpg', 1);
insert into item  values ('EST231', 'Male Adult', '75.00','FLDLH02', 'cat2.jpg', 1);
insert into item  values ('EST241', 'Female Adult', '80.00', 'FLDLH02', 'cat2.jpg', 1);
insert into item  values ('EST25', 'Male Adult', '120.00', 'AVCB01', 'bird2.jpg', 1);
insert into item  values ('EST26', 'Female Adult', '120.00', 'AVCB01', 'bird2.jpg', 1);
insert into item  values ('EST27', 'Male Adult', '80.00', 'AVSB02', 'bird1.jpg', 1);
insert into item  values ('EST28', 'Female Adult', '70.00', 'AVSB02', 'bird1.jpg', 1);
/* Quelques données */

insert into tag_table values ('NEW', 'Nouveauté');
insert into tag_table values ('PROMO', 'Promotion');
insert into tag_table values ('EXCLU', 'Exclusivité');
insert into tag_table values ('FRIEND', 'Affectueux');
insert into tag_table values ('PLAYFULL', 'Joueur');
insert into tag_table values ('CHEAP', 'Économique');

insert into category_tag values ('DOGS', 'FRIEND');
insert into category_tag values ('DOGS', 'PROMO');
insert into category_tag values ('REPTILES', 'NEW');
