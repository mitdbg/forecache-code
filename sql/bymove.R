library(ggplot2)
library(reshape)
library(plyr)

theme_set(theme_grey(base_size = 18)) 

image_folder = "/Volumes/E/mit/vis/code/scalar-prefetch/docs/dbpaper/images/png/"

#ngram5_p1_move_accuracy.csv  ngram5_p1_phase_accuracy.csv  ngram_p1_move_accuracy.csv
#ngram_p1_phase_accuracy.csv sig_p1_move_accuracy.csv
rawdata =
read.delim("new_res/output/ngram_p1_move_accuracy.csv",sep="\t",header=FALSE,col.names=c("userid","taskname","model","len","move","accuracy","total"))
ngram_move <- subset(data.frame(rawdata),len > 1)
best_ngram_move <- subset(ngram_move,len==5,select = -c(len))

rawdata =
read.delim("new_res/output/ngram_p1_phase_accuracy.csv",sep="\t",header=FALSE,col.names=c("userid","taskname","model","len","phase","accuracy","total"))
ngram_phase <- subset(data.frame(rawdata),len > 1)
best_ngram_phase <- subset(ngram_phase,len==5,select = -c(len))

rawdata =
read.delim("new_res/output/sig_p1_phase_accuracy.csv",sep="\t",header=FALSE,col.names=c("userid","taskname","model","phase","accuracy","total"))
sig_phase <- subset(data.frame(rawdata),model!="fhistogram" & model!="random")
best_sig_phase <- subset(sig_phase,model %in% c("sift","histogram"))

rawdata =
read.delim("new_res/output/sig_p1_move_accuracy.csv",sep="\t",header=FALSE,col.names=c("userid","taskname","model","move","accuracy","total"))
sig_move <- subset(data.frame(rawdata),model!="fhistogram" & model!="random")
best_sig_move <- subset(sig_move,model %in% c("sift"))#,"histogram"))

best_phase <- rbind(best_ngram_phase,best_sig_phase)
best_move <- rbind(best_ngram_move,best_sig_move)

rawdata =
read.delim("new_res/output/move_phase_dist.csv",sep="\t",header=FALSE,col.names=c("userid","taskname","move","phase","total"))
move_phase_dist <- data.frame(rawdata)


#accuracy by move
p <- ggplot(data=best_ngram_move, aes(x=move, y=accuracy,fill=move))
p <- p + facet_grid(~taskname)
p <- p + stat_summary(fun.y="mean", geom="bar",position=position_dodge()) #geom_bar(stat="identity",position=position_fill())
p <- p + xlab("Move (I=zoom-in, O=zoom-out, P=pan)") + ylab("Accuracy")
#p <- p + scale_fill_discrete(labels=c("Zoom in","Zoom out","Pan"))
p <- p + ggtitle(paste("Average Accuracy for Ngram by Move"))
print(p)
ggsave(paste(image_folder,"best_ngram_p1_move.png",sep=""),width=12,height=5)

#accuracy by move
p <- ggplot(data=sig_move, aes(x=move, y=accuracy,fill=move))
p <- p + facet_grid(~taskname)
p <- p + stat_summary(fun.y="mean", geom="bar",position=position_dodge()) #geom_bar(stat="identity",position=position_fill())
p <- p + xlab("Move (I=zoom-in, O=zoom-out, P=pan)") + ylab("Accuracy")
#p <- p + scale_fill_discrete(labels=c("Zoom in","Zoom out","Pan"))
p <- p + ggtitle(paste("Average Accuracy for Ngram by Move"))
print(p)
ggsave(paste(image_folder,"best_sig_p1_move.png",sep=""),width=12,height=5)

#accuracy
p <- ggplot(data=sig_move, aes(x=model, y=accuracy,fill=model))
p <- p + facet_grid(~taskname)
p <- p + stat_summary(fun.y="mean", geom="bar",position=position_dodge()) #geom_bar(stat="identity",position=position_fill())
p <- p + xlab("Recommendation Model") + ylab("Accuracy")
#p <- p + scale_fill_discrete(labels=c("Zoom in","Zoom out","Pan"))
p <- p + ggtitle(paste("Average Accuracy for Signature Models."))
print(p)
ggsave(paste(image_folder,"sig_p1_results.png",sep=""),width=14,height=5)

