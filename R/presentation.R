library(ggplot2)
library(reshape)
library(plyr)

theme_set(theme_grey(base_size = 18)) 

datapath = "res"
figpath = "/Volumes/E/mit/vis/code/scalar-prefetch/code/R/move_analysis/"
files = c("ngram.csv","sift.csv","dsift.csv") # ,"normal.csv")
tasknames = c("task1","task2","task3")
taskids = c(1,2,3)

rawdata =
read.delim("res/p1/ngram.csv",sep="\t",header=TRUE,col.names=c("userid","taskname","model","len","hitmiss","accuracy"))
ngram <- data.frame(rawdata)

rawdata =
read.delim("res/p1/ngram5.csv",sep="\t",header=TRUE,col.names=c("userid","taskname","model","hitmiss","accuracy"))
ngram5 <- data.frame(rawdata)

rawdata =
read.delim("res/p1/dircounts.csv",sep="\t",header=FALSE,col.names=c("userid","taskname","movement","count"))
dircounts <- data.frame(rawdata)

rawdata =
read.delim("res/p1/user121.csv",sep="\t",header=FALSE,col.names=c("taskname","model","hitmiss","accuracy"))
user121 <- data.frame(rawdata)



#rawdata = read.delim("res/p1/sift.csv", header=TRUE,col.names=c("userid","taskname","model","hitmiss","accuracy"))
#sift <- data.frame(rawdata)
#rawdata = read.delim("res/p1/dsift.csv", header=TRUE,col.names=c("userid","taskname","model","hitmiss","accuracy"))
#dsift <- data.frame(rawdata)
#rawdata = read.delim("res/p1/normal.csv", header=TRUE,col.names=c("userid","taskname","model","hitmiss","accuracy"))
#normal <- data.frame(rawdata)
#rawdata = read.delim("res/p1/histogram.csv", header=TRUE,col.names=c("userid","taskname","model","hitmiss","accuracy"))
#hist <- data.frame(rawdata)
#rawdata = read.delim("res/p1/fhistogram.csv", header=TRUE,col.names=c("userid","taskname","model","hitmiss","accuracy"))
#fhist <- data.frame(rawdata)

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

p <- ggplot(data=user121, aes(x=model, y=accuracy,fill=model))
p <- p + geom_bar(stat="identity")
p <- p + xlab("Model") + ylab("Accuracy")
p <- p + ggtitle(paste("SIFT vs Ngram for User 1"))
print(p)
ggsave(paste("user121_accuracy.png",sep=""),width=6,height=4)


p <- ggplot(data=excounts2, aes(x=task, y=total,fill=task))
p <- p + geom_boxplot(position=position_dodge())
p <- p + xlab("Task") + ylab("Total Requests")
p <- p + ggtitle(paste("Average Count of Requests"))
print(p)
ggsave(paste("request_totals.png",sep=""),width=6,height=4)


p <- ggplot(data=excounts, aes(x=task, y=count,fill=phase))
#p <- p + facet_grid(~task)
p <- p + stat_summary(fun.y="mean", geom="bar",position=position_fill()) #geom_bar(stat="identity",position=position_fill())
p <- p + xlab("Exploration Phase") + ylab("Total Requests")
p <- p + ggtitle(paste("Distribution of Exploration Phases"))
print(p)
ggsave(paste("excounts.png",sep=""),width=6,height=4)


p <- ggplot(data=dircounts, aes(x=taskname, y=count, fill=movement))
p <- p + stat_summary(fun.y="mean", geom="bar",position=position_fill()) #geom_bar(stat="identity",position=position_dodge())
p <- p + xlab("Movement (Pan or Zoom?)") + ylab("Percentage")
p <- p + ggtitle(paste("Distribution of Moves"))
print(p)
ggsave(paste("dircounts.png",sep=""),width=12,height=8)


p <- ggplot(data=ngram5, aes(x=taskname, y=accuracy, fill=taskname))
p <- p + stat_summary(fun.y="mean", geom="bar") #+ geom_bar(stat="identity",position=position_dodge())
p <- p + xlab("Task") + ylab("Accuracy")
p <- p + ggtitle(paste("Accuracy for Order 4 Markov Chain Model"))
print(p)
ggsave(paste("ngram5.png",sep=""),width=12,height=4)


p <- ggplot(data=task_stats, aes(x=taskname, y=count, fill=match))
p <- p + stat_summary(fun.y="mean", geom="bar", position=position_fill()) # + geom_bar(stat="identity",position=position_dodge())
p <- p + xlab("How closely users matched") + ylab("Count of users")
p <- p + ggtitle(paste("Distribution of User Matches to Our Exploration Model"))
print(p)
ggsave(paste("user_matches.png",sep=""),width=8,height=4)


p <- ggplot(data=signatures, aes(x=model, y=accuracy, fill=model))
p <- p + facet_grid(~taskname)
p <- p + stat_summary(fun.y="mean", geom="bar") # + geom_bar(stat="identity",position=position_dodge())
p <- p + xlab("Recommendation Model") + ylab("Average Accuracy")
p <- p + ggtitle(paste("General Accuracy for All Signatures"))
print(p)
ggsave(paste("signatures_accuracy.png",sep=""),width=14,height=6)

p <- ggplot(data=signatures, aes(x=model, y=accuracy, fill=model))
p <- p + facet_grid(~taskname)
p <- p + geom_boxplot(position=position_dodge())
p <- p + xlab("Recommendation Model") + ylab("Average Accuracy")
p <- p + ggtitle(paste("General Accuracy for All Signatures"))
print(p)
ggsave(paste("signatures_accuracy_boxplot.png",sep=""),width=12,height=6)


p <- ggplot(data=ngram, aes(x=len, y=accuracy, fill=len))
p <- p + facet_grid(~taskname)
p <- p + stat_summary(fun.y="mean", geom="bar") + scale_x_discrete(limits=c(2,3,4,5,6,7,8,9,10)) #+ geom_bar(stat="identity",position=position_dodge()) 
p <- p + xlab("History Length") + ylab("Average Accuracy")
p <- p + ggtitle(paste("Average Accuracy for Markov Chains"))
print(p)
ggsave(paste("markov_chains.png",sep=""),width=12,height=4)


p <- ggplot(data=ngram, aes(x=len, y=accuracy, fill=as.factor(len)))
p <- p + facet_grid(~taskname)
p <- p + geom_boxplot(position=position_dodge())
p <- p + xlab("History Length") + ylab("Average Accuracy")
p <- p + ggtitle(paste("Accuracy for Markov Chains"))
print(p)
ggsave(paste("markov_chains_boxplot.png",sep=""),width=12,height=6)


p <- ggplot(data=allmodels, aes(x=model, y=accuracy, fill=model))
p <- p + facet_grid(~taskname)
p <- p + stat_summary(fun.y="mean", geom="bar") #+ geom_bar(stat="identity",position=position_dodge())
p <- p + xlab("Recommendation Model") + ylab("Average Accuracy")
p <- p + ggtitle(paste("General Accuracy for Best Signature and Markov Chain Models"))
print(p)
ggsave(paste("all_models_accuracy.png",sep=""),width=10,height=5)

p <- ggplot(data=allmodels, aes(x=model, y=accuracy, fill=model))
p <- p + facet_grid(~taskname)
p <- p + geom_boxplot(position=position_dodge())
p <- p + xlab("Recommendation Model") + ylab("Average Accuracy")
p <- p + ggtitle(paste("General Accuracy for Best Signature and Markov Chain Models"))
print(p)
ggsave(paste("all_models_accuracy_boxplot.png",sep=""),width=12,height=6)

