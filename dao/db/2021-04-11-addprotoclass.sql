
-- protoclass is used to hold the protoc-generated canonical Java classname that was used
-- to create the outputdata and that must be used to restore it.

alter table apx_data add protoclass varchar(512) null;



