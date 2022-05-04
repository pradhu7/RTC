
drop table if exists blobmgr_deps;
drop table if exists blobmgr_meta;
drop table if exists blobmgr_blobs;

-- this file is specific to MariaDB, version 10.2.4 or later, as it requires
-- JSON functions

-- one row in this table for each blob that's managed.  design is to allow sub-blobs
-- ("parts") that are owned by a parent blob.  each part has a unique name within
-- the parent blob. current code puts metadata at parent and blob data on parts
create table blobmgr_blobs (
  blob_id    integer primary key auto_increment,

  -- these are the base metadata columns known about by blob manager code;
  -- all but extra_* are managed by blob mgr code
  created_at  timestamp      not null,      --
  created_by  varchar(128)   not null,      -- email address
  is_deleted  varchar(1)     not null,      -- boolean: [yn]; soft delete
  blob_uuid   varchar(48)    null,          -- xuuid; non null/empty only for top-most (parent) blobs
  blob_parent integer        null,          -- null for topmost/parent and non-null for part blobs
  blob_name   varchar(54)    null,          -- null for topmost/parent and non-null for part blobs
  s3_path     varchar(1024)  null,          -- e.g., s3://bucket/prefix/object
  md5_digest  varchar(40)    null,          -- auto-created digest of the uploaded byte[]; only 32 chars needed but wth
  mime_type   varchar(40)    null,          -- e.g., application/x-apixio-config, application/gzip
  extra_1     text           null,          -- defined and managed by layer(s) above BMS
  extra_2     text           null,          -- defined and managed by layer(s) above BMS

  -- MariaDB:  force it to check json validity
  CHECK(extra_1 IS NULL OR JSON_VALID(extra_1)),
  CHECK(extra_2 IS NULL OR JSON_VALID(extra_2))
  );

create unique index blobmgr_blobs_uuid_idx  on blobmgr_blobs (blob_uuid);
create unique index blobmgr_blobs_name_uidx on blobmgr_blobs (blob_uuid, blob_name);

-- one row per client-defined metadata field per blob managed; this is to hold
-- the key=value pairs of metadata for each blob, where the set of key=value pairs
-- is defined by a higher level system that uses blob mgr (e.g., Science Model Catalog).
-- while only string-type values are currently needed, if a new type is required in
-- the future, a new appropriately typed column must be added
create table blobmgr_meta (
  blob_id   integer      not null,
  key_name  varchar(128) not null,

  -- ?? possibly add visibility-type of flag here based on what Java-level metadata config is...??

  -- only 1 of the following can be non-null; this structure allows for type-specific queries (e.g., numeric less-than)
  str_val   varchar(1024) null,
  int_val   integer       null,
  bool_val  varchar(1)    null,  -- y/n
  dbl_val   double        null,
  -- date_val datetime null,

  foreign key (blob_id) references blobmgr_blobs (blob_id)
  );

create unique index blobmgr_meta_uidx  on blobmgr_meta (blob_id, key_name);


-- support for dependencies between blobs.
create table blobmgr_deps (

  -- conceptually, either blob_id or logid_left must be non-null and
  -- the non-null one has the stated rel_type with the logid_right value
  blob_id    integer       null,
  logid_left varchar(4000) null,

  -- y to prevent any rows w/ logid_left value being added/removed.  hacked way to do it and works
  --   because once an ID is locked, it remains locked.
  -- to set  locked on logID:  update blobmgr_deps set is_locked='y' where logid_left = :id
  -- to test locked on logID:  select 'y' from blobmgr_deps where logid_left = :id and is_locked = 'y'
  is_locked   char(1)      null,

  rel_type    enum('OWNS', 'USES') not null,        -- SHOULD be UPPERCASE to match RelationType.x.toString() on case-sensitive collations
  logid_right varchar(4000)        not null,


  foreign key (blob_id) references blobmgr_blobs (blob_id)
  );
