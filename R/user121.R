library(ggplot2)
library(reshape)
library(plyr)

theme_set(theme_grey(base_size = 18)) 

rawdata =
read.delim("res/p1/user121.csv",sep="\t",header=FALSE,col.names=c("taskname","model","hitmiss","accuracy"))
user121 <- data.frame(rawdata)
user121$model <- factor(user121$model, levels = c("sift","ngram"))

rawdata =
read.delim("res/p1/user121_overlap.csv",sep="\t",header=FALSE,col.names=c("taskname","model","hits"))
overlap <- data.frame(rawdata)
overlap$model <- factor(overlap$model, levels = c("sift","ngram","both"))


p <- ggplot(data=user121, aes(x=model, y=accuracy,fill=model))
p <- p + facet_grid(~taskname)
p <- p + geom_bar(stat="identity")
p <- p + xlab("Model") + ylab("Accuracy")
p <- p + ggtitle(paste("SIFT vs Ngram for User 1"))
print(p)
ggsave(paste("user121_accuracy.png",sep=""),width=6,height=4)

p <- ggplot(data=overlap, aes(x=taskname, y=hits,fill=model))
p <- p + geom_bar(stat="identity",position=position_fill())
p <- p + xlab("Task") + ylab("Count")
p <- p + ggtitle(paste("Overlap in successful predictions for User 1"))
print(p)
ggsave(paste("user121_overlap.png",sep=""),width=7,height=4)

