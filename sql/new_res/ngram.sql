drop table if exists ngram_data;
create table ngram_data(
    userid integer not null,
    taskname char(5) not null,
    model varchar(10) not null,
    length integer not null,
    predictions integer not null,
    zoom integer not null,
    x integer not null,
    y integer not null,
    move  char(1) not null,
    phase varchar(19) not null,
    cache varchar(4) not null
);

\copy ngram_data from '/Volumes/E/mit/vis/code/scalar-prefetch/code/sql/new_res/ngram.csv' delimiter E'\t' csv;
