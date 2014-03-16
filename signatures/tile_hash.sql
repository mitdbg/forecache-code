drop table if exists tile_hash;
drop table if exists temp_hash;

--select * from ndsi_agg_7_18_2013        2a0cf5267692de290efac7e3b6d5a593        [0, 0]  fabf634233b5a4518efdbe074188301b        0       90000
create table temp_hash(
        query char(32) not null,
        query_hash char(32) not null,
        tile_id varchar(9) not null,
        tile_hash char(32) not null,
        zoom integer,
        threshold integer,
        primary key(query,tile_id,zoom,threshold)
);
copy temp_hash from '/home/leibatt/projects/user_study/scalar_backend/tile_hash.tsv' with delimiter E'\t';
select distinct tile_id, tile_hash into tile_hash from temp_hash;
alter table tile_hash add primary key (tile_id);
