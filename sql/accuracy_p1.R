library(ggplot2)
library(reshape)
library(plyr)

theme_set(theme_grey(base_size = 18)) 

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
sig_phase <- subset(data.frame(rawdata),model!="fhistogram")
best_sig_phase <- subset(sig_phase,model %in% c("sift","histogram"))

rawdata =
read.delim("new_res/output/sig_p1_move_accuracy.csv",sep="\t",header=FALSE,col.names=c("userid","taskname","model","move","accuracy","total"))
sig_move <- subset(data.frame(rawdata),model!="fhistogram")
best_sig_move <- subset(sig_move,model %in% c("sift","histogram"))

best_phase <- rbind(best_ngram_phase,best_sig_phase)
best_move <- rbind(best_ngram_move,best_sig_move)

rawdata =
read.delim("new_res/output/move_phase_dist.csv",sep="\t",header=FALSE,col.names=c("userid","taskname","move","phase","total"))
move_phase_dist <- data.frame(rawdata)

#phase distribution
p <- ggplot(data=move_phase_dist, aes(x=taskname, y=total,fill=phase))
p <- p + stat_summary(fun.y="mean", geom="bar",position=position_fill()) #geom_bar(stat="identity",position=position_fill())
p <- p + xlab("Exploration Phase") + ylab("Requests by Exploration Phase")
p <- p + ggtitle(paste("Distribution of Moves Across Exploration Phases"))
print(p)
ggsave(paste("phase_dist.png",sep=""),width=10,height=5)

#move distribution
p <- ggplot(data=move_phase_dist, aes(x=taskname, y=total,fill=move))
p <- p + stat_summary(fun.y="mean", geom="bar",position=position_fill()) #geom_bar(stat="identity",position=position_fill())
p <- p + xlab("Exploration Phase") + ylab("Requests by Move")
p <- p + ggtitle(paste("Distribution of Moves Across Moves"))
print(p)
ggsave(paste("move_dist.png",sep=""),width=10,height=5)


#move vs phase distribution
p <- ggplot(data=move_phase_dist, aes(x=phase, y=total,fill=move))
p <- p + facet_grid(~taskname)
p <- p + stat_summary(fun.y="mean", geom="bar",position=position_fill()) #geom_bar(stat="identity",position=position_fill())
p <- p + xlab("Exploration Phase") + ylab("Total Requests by Move")
p <- p + ggtitle(paste("Distribution of Moves Across Exploration Phases"))
print(p)
ggsave(paste("move_phase_dist.png",sep=""),width=10,height=5)

#move vs phase distribution, transpose
p <- ggplot(data=move_phase_dist, aes(x=move, y=total,fill=phase))
p <- p + facet_grid(~taskname)
p <- p + stat_summary(fun.y="mean", geom="bar",position=position_stack()) #geom_bar(stat="identity",position=position_fill())
p <- p + xlab("Move (I=zoom in,O=zoom out,P=pan)") + ylab("Total Requests")
p <- p + ggtitle(paste("Distribution of Moves Across Exploration Phases"))
print(p)
ggsave(paste("move_phase_dist.png",sep=""),width=10,height=5)


#accuracy by exploration phase
p <- ggplot(data=sig_phase, aes(x=model, y=accuracy,fill=phase))
p <- p + facet_grid(~taskname)
p <- p + stat_summary(fun.y="mean", geom="bar",position=position_dodge()) #geom_bar(stat="identity",position=position_fill())
p <- p + xlab("Recommendation Model") + ylab("Accuracy")
p <- p + ggtitle(paste("General Accuracy for all Signatures by Exploration Phase"))
print(p)
ggsave(paste("sig_p1_phase.png",sep=""),width=12,height=8)

#accuracy by exploration phase 2
p <- ggplot(data=sig_phase, aes(x=model, y=accuracy,fill=model))
p <- p + facet_grid(phase~taskname)
p <- p + stat_summary(fun.y="mean", geom="bar",position=position_dodge()) #geom_bar(stat="identity",position=position_fill())
p <- p + xlab("Recommendation Model") + ylab("Accuracy")
p <- p + ggtitle(paste("General Accuracy for all Signatures by Exploration Phase"))
print(p)
ggsave(paste("sig_p1_phase2.png",sep=""),width=12,height=8)

#accuracy by move 2
p <- ggplot(data=sig_move, aes(x=model, y=accuracy,fill=model))
p <- p + facet_grid(move~taskname)
p <- p + stat_summary(fun.y="mean", geom="bar",position=position_dodge()) #geom_bar(stat="identity",position=position_fill())
p <- p + xlab("Recommendation Model") + ylab("Accuracy")
#p <- p + scale_fill_discrete(labels=c("Zoom in","Zoom out","Pan"))
p <- p + ggtitle(paste("General Accuracy for all Signatures by Move"))
print(p)
ggsave(paste("sig_p1_move.png",sep=""),width=12,height=8)

#accuracy by exploration phase
p <- ggplot(data=ngram_phase, aes(x=len, y=accuracy,fill=phase))
p <- p + facet_grid(phase~taskname)
p <- p + stat_summary(fun.y="mean", geom="bar",position=position_dodge()) #geom_bar(stat="identity",position=position_fill())
p <- p + xlab("Ngram Order") + ylab("Accuracy")
p <- p + ggtitle(paste("General Accuracy for all Ngram Models  by Exploration Phase"))
print(p)
ggsave(paste("ngram_p1_phase.png",sep=""),width=12,height=8)

#accuracy by move
p <- ggplot(data=ngram_move, aes(x=len, y=accuracy,fill=move))
p <- p + facet_grid(move~taskname)
p <- p + stat_summary(fun.y="mean", geom="bar",position=position_dodge()) #geom_bar(stat="identity",position=position_fill())
p <- p + xlab("Ngram Order") + ylab("Accuracy")
#p <- p + scale_fill_discrete(labels=c("Zoom in","Zoom out","Pan"))
p <- p + ggtitle(paste("General Accuracy for all Ngram Models  by Move"))
print(p)
ggsave(paste("ngram_p1_move.png",sep=""),width=12,height=8)

#accuracy by exploration phase
p <- ggplot(data=best_phase, aes(x=model, y=accuracy,fill=model))
p <- p + facet_grid(phase~taskname)
p <- p + stat_summary(fun.y="mean", geom="bar",position=position_dodge()) #geom_bar(stat="identity",position=position_fill())
p <- p + xlab("Recommendation Model") + ylab("Accuracy")
p <- p + ggtitle(paste("General Accuracy for SIFT and Ngram by Exploration Phase"))
print(p)
ggsave(paste("best_p1_phase.png",sep=""),width=12,height=8)

#accuracy by exploration phase 2
p <- ggplot(data=best_phase, aes(x=model, y=accuracy,fill=model))
p <- p + facet_grid(phase~taskname)
p <- p + geom_boxplot(position=position_dodge())
p <- p + xlab("Recommendation Model") + ylab("Accuracy")
p <- p + ggtitle(paste("General Accuracy for SIFT and Ngram by Exploration Phase"))
print(p)
ggsave(paste("best_p1_phase2.png",sep=""),width=12,height=8)

#accuracy
p <- ggplot(data=best_move, aes(x=model, y=accuracy,fill=model))
p <- p + facet_grid(~taskname)
p <- p + stat_summary(fun.y="mean", geom="bar",position=position_dodge()) #geom_bar(stat="identity",position=position_fill())
p <- p + xlab("Recommendation Model") + ylab("Accuracy")
#p <- p + scale_fill_discrete(labels=c("Zoom in","Zoom out","Pan"))
p <- p + ggtitle(paste("General Accuracy for SIFT, Histogram and Ngram"))
print(p)
ggsave(paste("best_p1.png",sep=""),width=12,height=8)

#accuracy by move
p <- ggplot(data=best_move, aes(x=model, y=accuracy,fill=model))
p <- p + facet_grid(move~taskname)
p <- p + stat_summary(fun.y="mean", geom="bar",position=position_dodge()) #geom_bar(stat="identity",position=position_fill())
p <- p + xlab("Recommendation Model") + ylab("Accuracy")
#p <- p + scale_fill_discrete(labels=c("Zoom in","Zoom out","Pan"))
p <- p + ggtitle(paste("General Accuracy for SIFT, Histogram and Ngram by Move"))
print(p)
ggsave(paste("best_p1_move.png",sep=""),width=12,height=8)

#accuracy by move 2
p <- ggplot(data=best_move, aes(x=model, y=accuracy,fill=model))
p <- p + facet_grid(move~taskname)
p <- p + geom_boxplot(position=position_dodge())
p <- p + xlab("Recommendation Model") + ylab("Accuracy")
#p <- p + scale_fill_discrete(labels=c("Zoom in","Zoom out","Pan"))
p <- p + ggtitle(paste("General Accuracy for SIFT, Histogram and Ngram by Move"))
print(p)
ggsave(paste("best_p1_move2.png",sep=""),width=12,height=8)

#accuracy by move
p <- ggplot(data=subset(best_move,userid==121), aes(x=model, y=accuracy,fill=model))
p <- p + facet_grid(move~taskname)
p <- p + stat_summary(fun.y="mean", geom="bar",position=position_dodge()) #geom_bar(stat="identity",position=position_fill())
p <- p + xlab("Recommendation Model") + ylab("Accuracy")
#p <- p + scale_fill_discrete(labels=c("Zoom in","Zoom out","Pan"))
p <- p + ggtitle(paste("General Accuracy for SIFT, Histogram and Ngram by Move for User 1"))
print(p)
ggsave(paste("best_p1_move_u121.png",sep=""),width=12,height=8)

#accuracy by phase
p <- ggplot(data=subset(best_phase,userid==121), aes(x=model, y=accuracy,fill=model))
p <- p + facet_grid(phase~taskname)
p <- p + stat_summary(fun.y="mean", geom="bar",position=position_dodge()) #geom_bar(stat="identity",position=position_fill())
p <- p + xlab("Recommendation Model") + ylab("Accuracy")
#p <- p + scale_fill_discrete(labels=c("Zoom in","Zoom out","Pan"))
p <- p + ggtitle(paste("General Accuracy for SIFT,Histogram and Ngram by Exploration Phase for User 1"))
print(p)
ggsave(paste("best_p1_phase_u121.png",sep=""),width=12,height=8)

#move vs phase distribution
p <- ggplot(data=subset(move_phase_dist,userid==121), aes(x=taskname, y=total,fill=move))
p <- p + stat_summary(fun.y="mean", geom="bar",position=position_fill()) #geom_bar(stat="identity",position=position_fill())
p <- p + xlab("Exploration Phase") + ylab("Requests by Move")
p <- p + ggtitle(paste("Distribution of Moves Across Tasks for User 1"))
print(p)
ggsave(paste("move_dist_u121.png",sep=""),width=10,height=5)
