--sql script to generate accuracy data from loaded data files (ngram.csv, signatures.csv)
--drop the analysis tables and start fresh
drop table if exists
mixed_total,mixed_hits,mixed_accuracy,mixed_move_all,mixed_move_hits,mixed_move_cache;

--this counts all the requests per model
select userid,taskname,model,allocated,count(*) as total into mixed_total from mixed_data group by
userid,taskname,model,allocated;
-- this counts all the hits per model
select userid,taskname,model,allocated,count(*) as hits into mixed_hits from mixed_data where
cache='hit' group by userid,taskname,model,allocated;
-- this joins the above tables and calculates the accuracy per model
select a.userid,a.taskname,a.model,a.allocated,1.0*coalesce(b.hits,0)/a.total as accuracy, a.total into
mixed_accuracy  from mixed_total as a left outer join mixed_hits as b on
a.userid=b.userid and a.taskname=b.taskname and a.model=b.model and a.allocated=b.allocated;
copy mixed_accuracy to
'/Volumes/E/mit/vis/code/scalar-prefetch/code/sql/new_res/output/mixed_accuracy.csv' with delimiter E'\t'
csv;

--this counts all the hits per model and move
select userid,taskname,model,allocated,move,count(*) as total into mixed_move_all from mixed_data where move
!='S' group by userid,taskname,model,allocated,move;
-- this counts all the requests per move
select userid,taskname,model,allocated,move,count(*) as hits into mixed_move_hits from mixed_data where
cache='hit' and move !='S' group by userid,taskname,model,allocated,move;
-- this joins the above tables and calculates the accuracy per move
select a.userid,a.taskname,a.model,a.allocated,a.move,1.0*coalesce(b.hits,0)/a.total as accuracy, a.total into
mixed_move_cache from mixed_move_all as a left outer join mixed_move_hits as b on
a.userid=b.userid and a.taskname=b.taskname and a.model=b.model and a.move=b.move and
a.allocated=b.allocated;
copy mixed_move_cache to
'/Volumes/E/mit/vis/code/scalar-prefetch/code/sql/new_res/output/mixed_move_accuracy.csv' with delimiter E'\t'
csv;

--calculates accuracy across all users
select taskname, model,allocated, avg(accuracy),stddev(accuracy) from mixed_accuracy where model='ngram5' or
model='sift' or model='sift,ngram5' group by taskname,model,allocated order by
taskname,model,allocated;
