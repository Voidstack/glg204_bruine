delete from customer;
delete from yaps_user;
delete from sequence_id where table_name = 'customer';
insert into sequence_id values ('customer', 0);

