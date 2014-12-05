--sql script to generate accuracy data from loaded data files (ngram.csv, signatures.csv)
--drop the analysis tables and start fresh
drop table if exists ngram5_p1, ngram5_p1_move_all, ngram5_p1_move_cache, ngram5_p1_move_hits,
ngram5_p1_phase_all, ngram5_p1_phase_cache, ngram5_p1_phase_hits, sig_p1, sig_p1_move_all,
sig_p1_move_cache, sig_p1_move_hits, sig_p1_phase_all, sig_p1_phase_hits, sig_p1_phase_cache,
move_phase_dist, ngram_p1, ngram_p1_move_all, ngram_p1_move_cache, ngram_p1_move_hits,
ngram_p1_phase_all, ngram_p1_phase_cache, ngram_p1_phase_hits;

--these tables only have predictions=1 values
select userid,taskname,model,zoom,x,y,move,phase,cache into sig_p1 from signature_data where
predictions=1 and move !='S';
select userid,taskname,model,zoom,x,y,move,phase,cache into ngram5_p1 from ngram_data where length=5
and predictions=1 and move !='S';
select userid,taskname,model,length,zoom,x,y,move,phase,cache into ngram_p1 from ngram_data where
predictions=1 and move !='S';


--this table counts the distribution of moves per phase and task
select userid,taskname,move,phase,count(*) as total into move_phase_dist from ngram5_p1 where move
!= 'S' group by userid,taskname,move,phase;
select taskname,phase,move,avg(total) as total from move_phase_dist group by taskname, phase, move
order by taskname, phase, move;
copy move_phase_dist to
'/Volumes/E/mit/vis/code/scalar-prefetch/code/sql/new_res/output/move_phase_dist.csv' with delimiter E'\t'
csv;

--this counts all the hits per model and phase
select userid,taskname,model,phase,count(*) as total into sig_p1_phase_all from sig_p1 group by
userid,taskname,model,phase;
-- this counts all the requests per phase
select userid,taskname,model,phase,count(*) as hits into sig_p1_phase_hits from sig_p1 where
cache='hit' group by userid,taskname,model,phase;
-- this joins the above tables and calculates the accuracy per phase
select a.userid,a.taskname,a.model,a.phase,1.0*coalesce(b.hits,0)/a.total as accuracy, a.total into
sig_p1_phase_cache from sig_p1_phase_all as a left outer join sig_p1_phase_hits as b on
a.userid=b.userid and a.taskname=b.taskname and a.model=b.model and a.phase=b.phase;
copy sig_p1_phase_cache to
'/Volumes/E/mit/vis/code/scalar-prefetch/code/sql/new_res/output/sig_p1_phase_accuracy.csv' with delimiter E'\t'
csv;
--this computes the average accuracy across all users
select taskname, model, phase, avg(accuracy) as accuracy ,avg(total) as total from
sig_p1_phase_cache group by taskname,model,phase;

--this counts all the hits per model and move
select userid,taskname,model,move,count(*) as total into sig_p1_move_all from sig_p1 where move
!='S' group by userid,taskname,model,move;
-- this counts all the requests per move
select userid,taskname,model,move,count(*) as hits into sig_p1_move_hits from sig_p1 where
cache='hit' and move !='S' group by userid,taskname,model,move;
-- this joins the above tables and calculates the accuracy per move
select a.userid,a.taskname,a.model,a.move,1.0*coalesce(b.hits,0)/a.total as accuracy, a.total into
sig_p1_move_cache from sig_p1_move_all as a left outer join sig_p1_move_hits as b on
a.userid=b.userid and a.taskname=b.taskname and a.model=b.model and a.move=b.move;
copy sig_p1_move_cache to
'/Volumes/E/mit/vis/code/scalar-prefetch/code/sql/new_res/output/sig_p1_move_accuracy.csv' with delimiter E'\t'
csv;
--this computes the average accuracy across all users
select taskname, model, move, avg(accuracy) as accuracy ,avg(total) as total from sig_p1_move_cache
group by taskname,model,move order by taskname,model,move;

--this counts all the hits per model and phase for ngram5
select userid,taskname,model,phase,count(*) as total into ngram5_p1_phase_all from ngram5_p1 group
by userid,taskname,model,phase;
-- this counts all the requests per phase
select userid,taskname,model,phase,count(*) as hits into ngram5_p1_phase_hits from ngram5_p1 where
cache='hit' group by userid,taskname,model,phase;
-- this joins the above tables and calculates the accuracy per phase
select a.userid,a.taskname,a.model,a.phase,1.0*coalesce(b.hits,0)/a.total as accuracy, a.total into
ngram5_p1_phase_cache from ngram5_p1_phase_all as a left outer join ngram5_p1_phase_hits as b on
a.userid=b.userid and a.taskname=b.taskname and a.model=b.model and a.phase=b.phase;
copy ngram5_p1_phase_cache to
'/Volumes/E/mit/vis/code/scalar-prefetch/code/sql/new_res/output/ngram5_p1_phase_accuracy.csv' with delimiter E'\t'
csv;
--this computes the average accuracy across all users
select taskname, model, phase, avg(accuracy) as accuracy ,avg(total) as total from
ngram5_p1_phase_cache group by taskname,model,phase;

--this counts all the hits per model and move for ngram5
select userid,taskname,model,move,count(*) as total into ngram5_p1_move_all from ngram5_p1 where
move !='S' group by userid,taskname,model,move;
-- this counts all the requests per move
select userid,taskname,model,move,count(*) as hits into ngram5_p1_move_hits from ngram5_p1 where
cache='hit' and move !='S' group by userid,taskname,model,move;
-- this joins the above tables and calculates the accuracy per move
select a.userid,a.taskname,a.model,a.move,1.0*coalesce(b.hits,0)/a.total as accuracy, a.total into
ngram5_p1_move_cache from ngram5_p1_move_all as a left outer join ngram5_p1_move_hits as b on
a.userid=b.userid and a.taskname=b.taskname and a.model=b.model and a.move=b.move;
copy ngram5_p1_move_cache to
'/Volumes/E/mit/vis/code/scalar-prefetch/code/sql/new_res/output/ngram5_p1_move_accuracy.csv' with delimiter E'\t'
csv;
--this computes the average accuracy across all users
select taskname, model, move, avg(accuracy) as accuracy ,avg(total) as total from
ngram5_p1_move_cache group by taskname,model,move;

--this counts all the hits per model and phase for ngram
select userid,taskname,model,length,phase,count(*) as total into ngram_p1_phase_all from ngram_p1 group
by userid,taskname,model,length,phase;
-- this counts all the requests per phase
select userid,taskname,model,length,phase,count(*) as hits into ngram_p1_phase_hits from ngram_p1 where
cache='hit' group by userid,taskname,model,length,phase;
-- this joins the above tables and calculates the accuracy per phase
select a.userid,a.taskname,a.model,a.length,a.phase,1.0*coalesce(b.hits,0)/a.total as accuracy, a.total into
ngram_p1_phase_cache from ngram_p1_phase_all as a left outer join ngram_p1_phase_hits as b on
a.userid=b.userid and a.taskname=b.taskname and a.model=b.model and a.phase=b.phase and
a.length=b.length;
copy ngram_p1_phase_cache to
'/Volumes/E/mit/vis/code/scalar-prefetch/code/sql/new_res/output/ngram_p1_phase_accuracy.csv' with delimiter E'\t'
csv;
--this computes the average accuracy across all users
select taskname, model, length, phase, avg(accuracy) as accuracy ,avg(total) as total from
ngram_p1_phase_cache group by taskname,model,phase,length;

--this counts all the hits per model and move for ngram
select userid,taskname,model,length,move,count(*) as total into ngram_p1_move_all from ngram_p1 where
move !='S' group by userid,taskname,model,length,move;
-- this counts all the requests per move
select userid,taskname,model,length,move,count(*) as hits into ngram_p1_move_hits from ngram_p1 where
cache='hit' and move !='S' group by userid,taskname,model,length,move;
-- this joins the above tables and calculates the accuracy per move
select a.userid,a.taskname,a.model,a.length,a.move,1.0*coalesce(b.hits,0)/a.total as accuracy, a.total into
ngram_p1_move_cache from ngram_p1_move_all as a left outer join ngram_p1_move_hits as b on
a.userid=b.userid and a.taskname=b.taskname and a.model=b.model and a.move=b.move and
a.length=b.length;
copy ngram_p1_move_cache to
'/Volumes/E/mit/vis/code/scalar-prefetch/code/sql/new_res/output/ngram_p1_move_accuracy.csv' with delimiter E'\t'
csv;
--this computes the average accuracy across all users
select taskname, model, length, move, avg(accuracy) as accuracy ,avg(total) as total from
ngram_p1_move_cache group by taskname,model,length,move order by taskname,model,move,length;
