drop table if exists test_fetch;

create table test_fetch(
  tile_size integer not null,
  zoom integer not null,
  x integer not null,
  y integer not null,
  duration_ms integer not null,
  bytes integer not null,
  cached char(1) not null
);

\copy test_fetch from '/Volumes/E/mit/vis/code/forecache-code/py/fetch_data.txt' delimiter E'\t' csv header;

