library(ggplot2)
library(reshape)
library(plyr)

theme_set(theme_grey(base_size = 18)) 

rawdata =
read.delim("new_res/output/mixed_accuracy.csv",sep="\t",header=FALSE,col.names=c("userid","taskname","model","allocated","accuracy","total"))
mixed_data <- data.frame(rawdata)
#mixed_data <- subset(data.frame(rawdata),allocated > 1 & allocated < 8 & model %in%
#c("ngram5","sift","sift,ngram5"))

p <- ggplot(data=mixed_data, aes(x=model, y=accuracy,fill=model))
p <- p + facet_grid(allocated~taskname)
p <- p + stat_summary(fun.y="mean", geom="bar",position=position_dodge())
p <- p + xlab("Recommendation Model") + ylab("Accuracy")
p <- p + ggtitle(paste("General Accuracy for SIFT,Histogram and Ngram, and Combinations"))
print(p)
ggsave(paste("images/mixed_accuracy.png",sep=""),width=20,height=8)

