alter table blobmgr_deps add column is_locked char(1) null;  -- only y/Y resolves to true
