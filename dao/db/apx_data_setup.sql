
-- this table is necessary for getting a list of all available bits of data
-- based on things like source (including filtering via generatorid+wildcard).
-- there is one row in this table for each managed rowkey in cassandra.

create table apx_data (
    id            int auto_increment,
    grouping_id   varchar(512)  not null,      -- this is the cassandra rowkey, as supplied by client in putData()
    partition_id  varchar(128)  null,
    data_count    int           not null,
    total_size    int           null,                                 -- in bytes
    created_at    timestamp     default '1970-01-01 00:00:01',

    primary key (id),
    unique index apx_data_grouping_idx (grouping_id, partition_id)  -- total key length must be <= 3072 bytes
);

-- partitioning/querykeys
create table apx_querykeys (
    data_id       int references apx_data(id),
    query_key     varchar(512)  not null,
    int_value     int           null,
    str_value     varchar(1024) null,
    bool_value    int(1)        null,

    unique index apx_querykeys_unique(data_id, query_key),

    constraint foreign key (data_id) references apx_data (id)
      
);

