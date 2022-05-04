create table blobmgr_deps (

  -- conceptually, either blob_id or logid_left must be non-null and
  -- the non-null one has the stated rel_type with the logid_right value
  blob_id    integer       null,
  logid_left varchar(4000) null,

  rel_type    enum('OWNS', 'USES') not null,
  logid_right varchar(4000)        not null,
  
  foreign key (blob_id) references blobmgr_blobs (blob_id)
  );
