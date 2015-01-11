library(ggplot2)
library(reshape)
library(plyr)

theme_set(theme_grey(base_size = 18)) 

rawdata =
read.delim("res/p1/ngram.csv",sep="\t",header=TRUE,col.names=c("userid","taskname","model","len","hitmiss","accuracy"))
ngram <- data.frame(rawdata)

rawdata =
read.delim("res/p1/ngram5_2.csv",sep="\t",header=TRUE,col.names=c("userid","taskname","model","hitmiss","accuracy"))
ngram5 <- data.frame(rawdata)

rawdata =
read.delim("res/p1/dircounts.csv",sep="\t",header=FALSE,col.names=c("userid","taskname","movement","count"))
dircounts <- data.frame(rawdata)

rawdata =
read.delim("res/p1/user121.csv",sep="\t",header=FALSE,col.names=c("taskname","model","hitmiss","accuracy"))
user121 <- data.frame(rawdata)

rawdata = read.delim("res/p1/all.csv", header=TRUE,col.names=c("userid","taskname","model","hitmiss","accuracy"))
allmodels <- data.frame(rawdata)
#allmodels$model <- factor(allmodels$model, levels = c("normal","histogram","dsift","sift","ngram"))
allmodels$model <- factor(allmodels$model, levels = c("random","normal","sift","ngram"))

rawdata = read.delim("res/p1/signatures.csv", header=TRUE,col.names=c("userid","taskname","model","hitmiss","accuracy"))
signatures <- data.frame(rawdata)
signatures$model <- factor(signatures$model, levels = c("random","normal","histogram","dsift","sift"))

rawdata = read.delim("res/task_stats.csv", header=FALSE,col.names=c("taskname","match","count"))
task_stats <- data.frame(rawdata)

rawdata = read.delim("res/p1/excounts2.csv", header=TRUE)
tmp <- data.frame(rawdata)
excounts <- melt(tmp[,c("task","identification","analysis","transition")] ,id.vars = 1)
excounts <- rename(excounts,c("variable"="phase","value"="count"))

rawdata = read.delim("res/p1/exploration_counts.csv", header=TRUE)
excounts2 <- data.frame(rawdata)

p <- ggplot(data=ngram5, aes(x=userid, y=accuracy))
p <- p + stat_summary(fun.y="mean", geom="bar")
p <- p + xlab("User ID") + ylab("Accuracy")
#p <- p + ggtitle(paste("Prediction Accuracy"))
print(p)
ggsave(paste("forecache-results2-lb.png",sep=""),width=6,height=4)
