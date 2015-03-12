drop table if exists lru0_data, lru4_data, lru8_data, lru0_data2, lru4_data2, lru8_data2, old_lru0_data, old_lru4_data, old_lru8_data, ground_truth;

--sql script to generate accuracy data from loaded data files (ngram.csv, signatures.csv)
--drop the analysis tables and start fresh
drop table if exists
lru0_total,lru0_hits,lru0_accuracy,lru0_move_all,lru0_move_hits,lru0_move_cache,lru0_phase_cache,
lru0_phase_all,lru0_phase_hits;
drop table if exists
lru4_total,lru4_hits,lru4_accuracy,lru4_move_all,lru4_move_hits,lru4_move_cache,lru4_phase_cache,
lru4_phase_all,lru4_phase_hits;
drop table if exists
lru8_total,lru8_hits,lru8_accuracy,lru8_move_all,lru8_move_hits,lru8_move_cache,lru8_phase_cache,
lru8_phase_all,lru8_phase_hits;

create table ground_truth(
    old_phase varchar(19) not null,
    move  varchar(8) not null,
    userid integer not null,
    taskname char(5) not null,
    x integer not null,
    y integer not null,
    zoom integer not null,
    new_phase varchar(11) not null,
    next_phase varchar(11) not null,
    added_rows integer not null,
    deleted_rows integer not null,
    id integer not null
);

create table lru0_data(
    userid integer not null,
    taskname char(5) not null,
    model varchar(13) not null,
    allocated varchar(3) not null,
    zoom integer not null,
    x integer not null,
    y integer not null,
    move  char(1) not null,
    phase varchar(19) not null,
    cache varchar(4) not null,
    id integer not null
);

create table lru4_data(
    userid integer not null,
    taskname char(5) not null,
    model varchar(13) not null,
    allocated varchar(3) not null,
    zoom integer not null,
    x integer not null,
    y integer not null,
    move  char(1) not null,
    phase varchar(19) not null,
    cache varchar(4) not null,
    id integer not null
);

create table lru8_data(
    userid integer not null,
    taskname char(5) not null,
    model varchar(13) not null,
    allocated varchar(3) not null,
    zoom integer not null,
    x integer not null,
    y integer not null,
    move  char(1) not null,
    phase varchar(19) not null,
    cache varchar(4) not null,
    id integer not null
);

\copy ground_truth from '/Volumes/E/mit/vis/code/scalar-prefetch/gt_updated.csv' delimiter E'\t' csv header;
\copy lru4_data from '/Volumes/E/mit/vis/code/scalar-prefetch/code/sql/new_res/lru/lru4_updated.csv' delimiter E'\t' csv;
\copy lru0_data from '/Volumes/E/mit/vis/code/scalar-prefetch/code/sql/new_res/lru/lru0_updated.csv' delimiter E'\t' csv;
\copy lru8_data from '/Volumes/E/mit/vis/code/scalar-prefetch/code/sql/new_res/lru/lru8_updated.csv' delimiter E'\t' csv;

create table lru0_data2 as select a.userid, a.taskname, a.model, a.allocated, a.zoom, a.x, a.y, b.move, b.new_phase as phase, a.cache, a.id from lru0_data as a left outer join ground_truth as b on (a.id=b.id and a.userid = b.userid and a.taskname = b.taskname and a.zoom = b.zoom) order by model,allocated,userid,taskname,id;
create table lru4_data2 as select a.userid, a.taskname, a.model, a.allocated, a.zoom, a.x, a.y, b.move, b.new_phase as phase, a.cache, a.id from lru4_data as a left outer join ground_truth as b on (a.id=b.id and a.userid = b.userid and a.taskname = b.taskname and a.zoom = b.zoom) order by model,allocated,userid,taskname,id;
create table lru8_data2 as select a.userid, a.taskname, a.model, a.allocated, a.zoom, a.x, a.y, b.move, b.new_phase as phase, a.cache, a.id from lru4_data as a left outer join ground_truth as b on (a.id=b.id and a.userid = b.userid and a.taskname = b.taskname and a.zoom = b.zoom) order by model,allocated,userid,taskname,id;

alter table lru0_data rename to old_lru0_data;
alter table lru4_data rename to old_lru4_data;
alter table lru8_data rename to old_lru8_data;

alter table lru0_data2 rename to lru0_data;
alter table lru4_data2 rename to lru4_data;
alter table lru8_data2 rename to lru8_data;

--this counts all the requests per model
select userid,taskname,model,allocated,count(*) as total into lru0_total from lru0_data group by
userid,taskname,model,allocated;
-- this counts all the hits per model
select userid,taskname,model,allocated,count(*) as hits into lru0_hits from lru0_data where
cache='hit' group by userid,taskname,model,allocated;
-- this joins the above tables and calculates the accuracy per model
select a.userid,a.taskname,a.model,a.allocated,1.0*coalesce(b.hits,0)/a.total as accuracy, a.total into
lru0_accuracy  from lru0_total as a left outer join lru0_hits as b on
a.userid=b.userid and a.taskname=b.taskname and a.model=b.model and a.allocated=b.allocated;
copy lru0_accuracy to
'/Volumes/E/mit/vis/code/scalar-prefetch/code/sql/new_res/output/lru0_accuracy.csv' with delimiter E'\t'
csv;

--this counts all the hits per model and move
select userid,taskname,model,allocated,move,count(*) as total into lru0_move_all from lru0_data where move
!='Start' group by userid,taskname,model,allocated,move;
-- this counts all the requests per move
select userid,taskname,model,allocated,move,count(*) as hits into lru0_move_hits from lru0_data where
cache='hit' and move !='Start' group by userid,taskname,model,allocated,move;
-- this joins the above tables and calculates the accuracy per move
select a.userid,a.taskname,a.model,a.allocated,a.move,1.0*coalesce(b.hits,0)/a.total as accuracy, a.total into
lru0_move_cache from lru0_move_all as a left outer join lru0_move_hits as b on
a.userid=b.userid and a.taskname=b.taskname and a.model=b.model and a.move=b.move and
a.allocated=b.allocated;
copy lru0_move_cache to
'/Volumes/E/mit/vis/code/scalar-prefetch/code/sql/new_res/output/lru0_move_accuracy.csv' with delimiter E'\t'
csv;

--this counts all the hits per model and phase
select userid,taskname,model,allocated,phase,count(*) as total into lru0_phase_all from lru0_data where move
!='Start' group by userid,taskname,model,allocated,phase;
-- this counts all the requests per phase
select userid,taskname,model,allocated,phase,count(*) as hits into lru0_phase_hits from lru0_data where
cache='hit' and move !='Start' group by userid,taskname,model,allocated,phase;
-- this joins the above tables and calculates the accuracy per phase
select a.userid,a.taskname,a.model,a.allocated,a.phase,1.0*coalesce(b.hits,0)/a.total as accuracy, a.total into
lru0_phase_cache from lru0_phase_all as a left outer join lru0_phase_hits as b on
a.userid=b.userid and a.taskname=b.taskname and a.model=b.model and a.phase=b.phase and
a.allocated=b.allocated;
copy lru0_phase_cache to
'/Volumes/E/mit/vis/code/scalar-prefetch/code/sql/new_res/output/lru0_phase_accuracy.csv' with delimiter E'\t'
csv;


--this counts all the requests per model
select userid,taskname,model,allocated,count(*) as total into lru4_total from lru4_data group by
userid,taskname,model,allocated;
-- this counts all the hits per model
select userid,taskname,model,allocated,count(*) as hits into lru4_hits from lru4_data where
cache='hit' group by userid,taskname,model,allocated;
-- this joins the above tables and calculates the accuracy per model
select a.userid,a.taskname,a.model,a.allocated,1.0*coalesce(b.hits,0)/a.total as accuracy, a.total into
lru4_accuracy  from lru4_total as a left outer join lru4_hits as b on
a.userid=b.userid and a.taskname=b.taskname and a.model=b.model and a.allocated=b.allocated;
copy lru4_accuracy to
'/Volumes/E/mit/vis/code/scalar-prefetch/code/sql/new_res/output/lru4_accuracy.csv' with delimiter E'\t'
csv;

--this counts all the hits per model and move
select userid,taskname,model,allocated,move,count(*) as total into lru4_move_all from lru4_data where move
!='Start' group by userid,taskname,model,allocated,move;
-- this counts all the requests per move
select userid,taskname,model,allocated,move,count(*) as hits into lru4_move_hits from lru4_data where
cache='hit' and move !='Start' group by userid,taskname,model,allocated,move;
-- this joins the above tables and calculates the accuracy per move
select a.userid,a.taskname,a.model,a.allocated,a.move,1.0*coalesce(b.hits,0)/a.total as accuracy, a.total into
lru4_move_cache from lru4_move_all as a left outer join lru4_move_hits as b on
a.userid=b.userid and a.taskname=b.taskname and a.model=b.model and a.move=b.move and
a.allocated=b.allocated;
copy lru4_move_cache to
'/Volumes/E/mit/vis/code/scalar-prefetch/code/sql/new_res/output/lru4_move_accuracy.csv' with delimiter E'\t'
csv;

--this counts all the hits per model and phase
select userid,taskname,model,allocated,phase,count(*) as total into lru4_phase_all from lru4_data where move
!='Start' group by userid,taskname,model,allocated,phase;
-- this counts all the requests per phase
select userid,taskname,model,allocated,phase,count(*) as hits into lru4_phase_hits from lru4_data where
cache='hit' and move !='Start' group by userid,taskname,model,allocated,phase;
-- this joins the above tables and calculates the accuracy per phase
select a.userid,a.taskname,a.model,a.allocated,a.phase,1.0*coalesce(b.hits,0)/a.total as accuracy, a.total into
lru4_phase_cache from lru4_phase_all as a left outer join lru4_phase_hits as b on
a.userid=b.userid and a.taskname=b.taskname and a.model=b.model and a.phase=b.phase and
a.allocated=b.allocated;
copy lru4_phase_cache to
'/Volumes/E/mit/vis/code/scalar-prefetch/code/sql/new_res/output/lru4_phase_accuracy.csv' with delimiter E'\t'
csv;


--this counts all the requests per model
select userid,taskname,model,allocated,count(*) as total into lru8_total from lru8_data group by
userid,taskname,model,allocated;
-- this counts all the hits per model
select userid,taskname,model,allocated,count(*) as hits into lru8_hits from lru8_data where
cache='hit' group by userid,taskname,model,allocated;
-- this joins the above tables and calculates the accuracy per model
select a.userid,a.taskname,a.model,a.allocated,1.0*coalesce(b.hits,0)/a.total as accuracy, a.total into
lru8_accuracy  from lru8_total as a left outer join lru8_hits as b on
a.userid=b.userid and a.taskname=b.taskname and a.model=b.model and a.allocated=b.allocated;
copy lru8_accuracy to
'/Volumes/E/mit/vis/code/scalar-prefetch/code/sql/new_res/output/lru8_accuracy.csv' with delimiter E'\t'
csv;

--this counts all the hits per model and move
select userid,taskname,model,allocated,move,count(*) as total into lru8_move_all from lru8_data where move
!='Start' group by userid,taskname,model,allocated,move;
-- this counts all the requests per move
select userid,taskname,model,allocated,move,count(*) as hits into lru8_move_hits from lru8_data where
cache='hit' and move !='Start' group by userid,taskname,model,allocated,move;
-- this joins the above tables and calculates the accuracy per move
select a.userid,a.taskname,a.model,a.allocated,a.move,1.0*coalesce(b.hits,0)/a.total as accuracy, a.total into
lru8_move_cache from lru8_move_all as a left outer join lru8_move_hits as b on
a.userid=b.userid and a.taskname=b.taskname and a.model=b.model and a.move=b.move and
a.allocated=b.allocated;
copy lru8_move_cache to
'/Volumes/E/mit/vis/code/scalar-prefetch/code/sql/new_res/output/lru8_move_accuracy.csv' with delimiter E'\t'
csv;

--this counts all the hits per model and phase
select userid,taskname,model,allocated,phase,count(*) as total into lru8_phase_all from lru8_data where move
!='Start' group by userid,taskname,model,allocated,phase;
-- this counts all the requests per phase
select userid,taskname,model,allocated,phase,count(*) as hits into lru8_phase_hits from lru8_data where
cache='hit' and move !='Start' group by userid,taskname,model,allocated,phase;
-- this joins the above tables and calculates the accuracy per phase
select a.userid,a.taskname,a.model,a.allocated,a.phase,1.0*coalesce(b.hits,0)/a.total as accuracy, a.total into
lru8_phase_cache from lru8_phase_all as a left outer join lru8_phase_hits as b on
a.userid=b.userid and a.taskname=b.taskname and a.model=b.model and a.phase=b.phase and
a.allocated=b.allocated;
copy lru8_phase_cache to
'/Volumes/E/mit/vis/code/scalar-prefetch/code/sql/new_res/output/lru8_phase_accuracy.csv' with delimiter E'\t'
csv;

select model,allocated,phase,count(*) as total into lru0_pall from lru0_data where move
!='Start' group by model,allocated,phase;
-- this counts all the requests per phase
select model,allocated,phase,count(*) as hits into lru0_phits from lru0_data where
cache='hit' and move !='Start' group by model,allocated,phase;
-- this joins the above tables and calculates the accuracy per phase across all users
select a.allocated,a.phase,a.model,1.0*coalesce(b.hits,0)/a.total as accuracy from lru0_pall as a
left outer join lru0_phits as b on a.model=b.model and a.phase=b.phase and
a.allocated=b.allocated where (a.model='sift' or a.model='markov3') and a.allocated='4' order by
a.allocated,a.phase,a.model;

--calculates accuracy across all users
--select taskname, model,allocated, avg(accuracy),stddev(accuracy) from lru4_accuracy where
--model='markov3' or
--model='sift' or model='sift,markov3' group by taskname,model,allocated order by
--taskname,model,allocated;

--calculates accuracy across all users
--select taskname, model,allocated, avg(accuracy),stddev(accuracy) from lru8_accuracy where
--model='markov3' or
--model='sift' or model='sift,markov3' group by taskname,model,allocated order by
--taskname,model,allocated;

--calculates accuracy across all users
--select taskname, model,allocated, avg(accuracy),stddev(accuracy) from lru0_accuracy where model='markov3' or
--model='sift' or model='sift,markov3' group by taskname,model,allocated order by
--taskname,model,allocated;

