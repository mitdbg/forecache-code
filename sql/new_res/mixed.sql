drop table if exists normal_mixed_data, sift_mixed_data, mixed_data;

create table normal_mixed_data(
    userid integer not null,
    taskname char(5) not null,
    model varchar(13) not null,
    allocated varchar(3) not null,
    zoom integer not null,
    x integer not null,
    y integer not null,
    move  char(1) not null,
    phase varchar(19) not null,
    cache varchar(4) not null
);

create table sift_mixed_data(
    userid integer not null,
    taskname char(5) not null,
    model varchar(13) not null,
    allocated varchar(3) not null,
    zoom integer not null,
    x integer not null,
    y integer not null,
    move  char(1) not null,
    phase varchar(19) not null,
    cache varchar(4) not null
);


\copy sift_mixed_data from '/Volumes/E/mit/vis/code/scalar-prefetch/code/sql/mixed/sift_ngram.txt' delimiter E'\t' csv;

\copy normal_mixed_data from '/Volumes/E/mit/vis/code/scalar-prefetch/code/sql/mixed/normal_ngram.txt' delimiter E'\t' csv;

--don't double count the ngram rows, since we compare with ngram for both normal *and* sift
select * into mixed_data from sift_mixed_data; --where model!='ngram5' or allocated='3' union all select * from normal_mixed_data;
