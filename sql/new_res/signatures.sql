drop table if exists signature_data;
create table signature_data(
    userid integer not null,
    taskname char(5) not null,
    model varchar(10) not null,
    predictions integer not null,
    zoom integer not null,
    x integer not null,
    y integer not null,
    move  char(1) not null,
    phase varchar(19) not null,
    cache varchar(4) not null
);

\copy signature_data from '/Volumes/E/mit/vis/code/scalar-prefetch/code/sql/new_res/signatures.csv' delimiter E'\t' csv;
