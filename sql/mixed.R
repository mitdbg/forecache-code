library(ggplot2)
library(reshape)
library(plyr)

theme_set(theme_grey(base_size = 18)) 
image_folder = "/Volumes/E/mit/vis/code/scalar-prefetch/docs/dbpaper/images/png/"

rawdata =
read.delim("new_res/output/mixed_accuracy.csv",sep="\t",header=FALSE,col.names=c("userid","taskname","model","allocated","accuracy","total"))
mixed_data <- data.frame(rawdata)
#mixed_data <- subset(data.frame(rawdata),allocated > 1 & allocated < 8 & model %in%
#c("ngram5","sift","sift,ngram5"))

rawdata =
read.delim("new_res/output/mixed_move_accuracy.csv",sep="\t",header=FALSE,col.names=c("userid","taskname","model","allocated","move","accuracy","total"))
#best_move <- data.frame(rawdata)
ns1 <- subset(data.frame(rawdata),(model %in% c("ngram5","sift") & allocated == "1"))
ns2 <- subset(data.frame(rawdata),(model %in% c("ngram5","sift") & allocated == "2"))# | (allocated == "1,2"))
ns3 <- subset(data.frame(rawdata),(model %in% c("ngram5","sift") & allocated == "3") | (allocated == "2,1"))
ns4 <- subset(data.frame(rawdata),(model %in% c("ngram5","sift") & allocated == "4") | (allocated == "3,1"))

siftngram <- subset(data.frame(rawdata),(model %in% c("ngram5","sift") & allocated == "3"))
siftngram2 <- subset(data.frame(rawdata),(model %in% c("ngram5","sift") & allocated == "3" |
model=="sift,ngram5" & allocated == "2,1"))

rawdata =
read.delim("new_res/output/mixed_accuracy2.csv",sep="\t",header=FALSE,col.names=c("userid","taskname","model","accuracy","total"))
mixed_data2 <- data.frame(rawdata)


#p <- ggplot(data=mixed_data, aes(x=model, y=accuracy,fill=model))
#p <- p + facet_grid(allocated~taskname)
#p <- p + stat_summary(fun.y="mean", geom="bar",position=position_dodge())
#p <- p + xlab("Recommendation Model") + ylab("Accuracy")
#p <- p + ggtitle(paste("General Accuracy for SIFT,Histogram and Ngram, and Combinations"))
#print(p)
#ggsave(paste("images/mixed_accuracy.png",sep=""),width=20,height=8)

#p <- ggplot(data=mixed_data2, aes(x=model, y=accuracy,fill=model))
#p <- p + facet_grid(~taskname)
#p <- p + stat_summary(fun.y="mean", geom="bar",position=position_dodge())
#p <- p + xlab("Recommendation Model") + ylab("Accuracy")
#p <- p + ggtitle(paste("General Accuracy for SIFT, Order-5 Ngram, and Combinations"))
#print(p)
#ggsave(paste("images/mixed_accuracy2.png",sep=""),width=20,height=8)

##accuracy by move
#p <- ggplot(data=ns1, aes(x=model, y=accuracy,fill=model))
#p <- p + facet_grid(move~taskname)
#p <- p + stat_summary(fun.y="mean", geom="bar",position=position_dodge()) #geom_bar(stat="identity",position=position_fill())
#p <- p + xlab("Recommendation Model") + ylab("Accuracy")
##p <- p + scale_fill_discrete(labels=c("Zoom in","Zoom out","Pan"))
#p <- p + ggtitle(paste("General Accuracy by Move"))
#print(p)
#ggsave(paste("images/ns1.png",sep=""),width=12,height=8)


##accuracy by move
#p <- ggplot(data=ns1, aes(x=model, y=accuracy,fill=model))
#p <- p + facet_grid(move~taskname)
#p <- p + stat_summary(fun.y="mean", geom="bar",position=position_dodge()) #geom_bar(stat="identity",position=position_fill())
#p <- p + xlab("Recommendation Model") + ylab("Accuracy")
##p <- p + scale_fill_discrete(labels=c("Zoom in","Zoom out","Pan"))
#p <- p + ggtitle(paste("General Accuracy by Move"))
#print(p)
#ggsave(paste("images/ns1.png",sep=""),width=12,height=8)


##accuracy by move
#p <- ggplot(data=ns2, aes(x=model, y=accuracy,fill=model))
#p <- p + facet_grid(move~taskname)
#p <- p + stat_summary(fun.y="mean", geom="bar",position=position_dodge()) #geom_bar(stat="identity",position=position_fill())
#p <- p + xlab("Recommendation Model") + ylab("Accuracy")
##p <- p + scale_fill_discrete(labels=c("Zoom in","Zoom out","Pan"))
#p <- p + ggtitle(paste("General Accuracy by Move"))
#print(p)
#ggsave(paste("images/ns2.png",sep=""),width=12,height=8)

##accuracy by move
#p <- ggplot(data=ns3, aes(x=model, y=accuracy,fill=model))
#p <- p + facet_grid(move~taskname)
#p <- p + stat_summary(fun.y="mean", geom="bar",position=position_dodge()) #geom_bar(stat="identity",position=position_fill())
#p <- p + xlab("Recommendation Model") + ylab("Accuracy")
##p <- p + scale_fill_discrete(labels=c("Zoom in","Zoom out","Pan"))
#p <- p + ggtitle(paste("General Accuracy by Move"))
#print(p)
#ggsave(paste("images/ns3.png",sep=""),width=12,height=8)

##accuracy by move
#p <- ggplot(data=ns4, aes(x=model, y=accuracy,fill=model))
#p <- p + facet_grid(move~taskname)
#p <- p + stat_summary(fun.y="mean", geom="bar",position=position_dodge()) #geom_bar(stat="identity",position=position_fill())
#p <- p + xlab("Recommendation Model") + ylab("Accuracy")
##p <- p + scale_fill_discrete(labels=c("Zoom in","Zoom out","Pan"))
#p <- p + ggtitle(paste("General Accuracy by Move"))
#print(p)
#ggsave(paste("images/ns4.png",sep=""),width=12,height=8)

#p <- ggplot(data=siftngram, aes(x=model, y=accuracy,fill=model))
#p <- p + facet_grid(~taskname)
#p <- p + stat_summary(fun.y="mean", geom="bar",position=position_dodge()) #geom_bar(stat="identity",position=position_fill())
#p <- p + xlab("Recommendation Model") + ylab("Accuracy")
##p <- p + scale_fill_discrete(labels=c("Zoom in","Zoom out","Pan"))
#p <- p + ggtitle(paste("Accuracy for SIFT and Markov Chains (or Ngram)."))
#print(p)
#ggsave(paste(image_folder,"sift_vs_ngram.png",sep=""),width=12,height=8)


#p <- ggplot(data=siftngram, aes(x=model, y=accuracy,fill=model))
#p <- p + facet_grid(move~taskname)
#p <- p + stat_summary(fun.y="mean", geom="bar",position=position_dodge()) #geom_bar(stat="identity",position=position_fill())
#p <- p + xlab("Recommendation Model") + ylab("Accuracy")
##p <- p + scale_fill_discrete(labels=c("Zoom in","Zoom out","Pan"))
#p <- p + ggtitle(paste("Accuracy (per Directional Move) for SIFT and Markov Chains (or Ngram)."))
#print(p)
#ggsave(paste(image_folder,"sift_vs_ngram_move.png",sep=""),width=12,height=8)

p <- ggplot(data=siftngram2, aes(x=model, y=accuracy,fill=model))
p <- p + facet_grid(~taskname)
p <- p + stat_summary(fun.y="mean", geom="bar",position=position_dodge()) #geom_bar(stat="identity",position=position_fill())
p <- p + xlab("Recommendation Model") + ylab("Accuracy")
#p <- p + scale_fill_discrete(labels=c("Zoom in","Zoom out","Pan"))
p <- p + ggtitle(paste("Accuracy for 2-1 Mixture, SIFT and Markov Chains (or Ngram)."))
print(p)
ggsave(paste(image_folder,"mix_vs_sift_vs_ngram.png",sep=""),width=12,height=8)

p <- ggplot(data=siftngram2, aes(x=model, y=accuracy,fill=model))
p <- p + facet_grid(move~taskname)
p <- p + stat_summary(fun.y="mean", geom="bar",position=position_dodge()) #geom_bar(stat="identity",position=position_fill())
p <- p + xlab("Recommendation Model") + ylab("Accuracy")
#p <- p + scale_fill_discrete(labels=c("Zoom in","Zoom out","Pan"))
p <- p + ggtitle(paste("Accuracy (per Directional Move) for 2-1 Mixture, SIFT and Markov Chains (or Ngram)."))
print(p)
ggsave(paste(image_folder,"mix_vs_sift_vs_ngram_move.png",sep=""),width=12,height=8)

