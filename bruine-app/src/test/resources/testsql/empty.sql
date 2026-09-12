
-- We should probably avoid duplicating all those data.
-- A temporary solution would be to have a script shell for this.
-- but in the next iterations of the software, we will move toward 
-- a systematic use of flyway. 
-- besides, much of the data in the original SQL files don't belong there.
-- In an actual application, the very specific data about the available items
-- won't be hardcoded for instance.

-- Cleanup
delete from customer;
delete from item;
delete from product;
delete from category;
delete from sequence_id;
delete from yaps_user;

insert into sequence_id values ('customer', 0);
insert into sequence_id values ('item', 0);