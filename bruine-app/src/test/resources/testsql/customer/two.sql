
delete from customer;
delete from yaps_user;

delete from sequence_id where table_name = 'customer';

insert into yaps_user values 
(
        'u1', 'CUST', '{noop}cnam'
);

insert into yaps_user values 
(
        'u2', 'CUST', '{noop}cnam'
);

insert into yaps_user values 
(
        'e1', 'EMP', '{noop}cnam'
);

insert into sequence_id values ('customer', 2);


insert into customer 
    (id, firstname, lastname, telephone, email,
     street1, street2, city, state, zipcode, country, username) 
    values ('1', 'fn1', 'ln1', '1', 'c1@cnam.fr', 
        's11', 's21', 'c1', 's1', 'z1', 'c1', 'u1');

insert into customer (id, firstname, lastname, telephone, email, 
    street1, street2, city, state, zipcode, country, username) 
    values ('2', 'fn2', 'ln2', '2', 'c2@cnam.fr',
     's12', 's22', 'c2', 's2', 'z2', 'c2', 'u2');



