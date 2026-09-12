-- fichier à utiliser avec /testsql/catalog/small.sql

delete from order_line;
delete from order_table;

update sequence_id set 
    max_id = 1
where
    table_name = 'order_table';

update sequence_id set 
    max_id = 11
where
    table_name = 'order_line';

insert into order_table (id, order_date, customer_id) values 
    ('1', '2022-11-08', '1');

insert into order_line (id, order_id, item_id, is_processed)
    values ('10', '1', 'it1a', 0);

insert into order_line (id, order_id, item_id, is_processed)
    values ('11', '1', 'it3a', 0);
