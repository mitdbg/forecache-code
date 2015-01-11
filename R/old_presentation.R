library(ggplot2)

theme_set(theme_grey(base_size = 18)) 

datapath = "res"
figpath = "/Volumes/E/mit/vis/code/scalar-prefetch/code/R/move_analysis/"
files = c("ngram.csv","sift.csv","dsift.csv") # ,"normal.csv")
tasknames = c("task1","task2","task3")
taskids = c(1,2,3)

rawdata =
read.delim("res/ngram.csv",sep="\t",header=TRUE,col.names=c("userid","taskname","model","len","predictions","hitmiss","accuracy"))
ngram <- data.frame(rawdata)

#rawdata = read.delim("res/sift.csv", header=TRUE,col.names=c("userid","taskname","model","predictions","hitmiss","accuracy"))
#sift <- data.frame(rawdata)
#rawdata = read.delim("res/dsift.csv", header=TRUE,col.names=c("userid","taskname","model","predictions","hitmiss","accuracy"))
#dsift <- data.frame(rawdata)
#rawdata = read.delim("res/normal.csv", header=TRUE,col.names=c("userid","taskname","model","predictions","hitmiss","accuracy"))
#normal <- data.frame(rawdata)
#rawdata = read.delim("res/histogram.csv", header=TRUE,col.names=c("userid","taskname","model","predictions","hitmiss","accuracy"))
#hist <- data.frame(rawdata)
#rawdata = read.delim("res/fhistogram.csv", header=TRUE,col.names=c("userid","taskname","model","predictions","hitmiss","accuracy"))
#fhist <- data.frame(rawdata)

rawdata = read.delim("res/all.csv", header=TRUE,col.names=c("userid","taskname","model","predictions","hitmiss","accuracy"))
allmodels <- data.frame(rawdata)
#allmodels$model <- factor(allmodels$model, levels = c("normal","histogram","dsift","sift","ngram"))
allmodels$model <- factor(allmodels$model, levels = c("normal","sift","ngram"))

rawdata = read.delim("res/signatures.csv", header=TRUE,col.names=c("userid","taskname","model","predictions","hitmiss","accuracy"))
signatures <- data.frame(rawdata)
signatures$model <- factor(signatures$model, levels = c("normal","histogram","dsift","sift"))

rawdata = read.delim("res/task_stats.csv", header=FALSE,col.names=c("taskname","match","count"))
task_stats <- data.frame(rawdata)

p <- ggplot(data=task_stats, aes(x=match, y=count, fill=match))
p <- p + facet_grid(~taskname)
p <- p + geom_bar(stat="identity",position=position_dodge())
p <- p + xlab("How closely users matched") + ylab("Count of users")
p <- p + ggtitle(paste("Distribution of User Matches to Our Exploration Model"))
print(p)
ggsave(paste("user_matches.png",sep=""),width=12,height=6)


p <- ggplot(data=signatures, aes(x=model, y=accuracy, fill=model))
p <- p + facet_grid(taskname ~ predictions)
p <- p + geom_bar(stat="identity",position=position_dodge())
p <- p + xlab("Recommendation Model") + ylab("Average Accuracy")
p <- p + ggtitle(paste("General Accuracy for All Recommendation Models"))
print(p)
ggsave(paste("signatures_accuracy.png",sep=""),width=12,height=6)


p <- ggplot(data=ngram, aes(x=len, y=accuracy, fill=len))
p <- p + facet_grid(taskname ~ predictions)
p <- p + geom_bar(stat="identity",position=position_dodge()) +scale_x_discrete(limits=c(1,2,3,4,5,6,7,8,9,10))
p <- p + xlab("History Length") + ylab("Average Accuracy")
p <- p + ggtitle(paste("Average Accuracy for Markov Chains"))
print(p)
ggsave(paste("markov_chains.png",sep=""),width=12,height=6)


p <- ggplot(data=allmodels, aes(x=model, y=accuracy, fill=model))
p <- p + facet_grid(taskname ~ predictions)
p <- p + geom_bar(stat="identity",position=position_dodge())
p <- p + xlab("Recommendation Model") + ylab("Average Accuracy")
p <- p + ggtitle(paste("General Accuracy for Best Signature and Markov Chain Models"))
print(p)
ggsave(paste("all_models_accuracy.png",sep=""),width=12,height=6)


