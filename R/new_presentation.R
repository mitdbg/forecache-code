library(ggplot2)
library(reshape)
library(plyr)

theme_set(theme_grey(base_size = 18)) 


rawdata =
read.delim("new_res/ngram.csv",sep="\t",header=TRUE,col.names=c("userid","taskname","model","len","predictions","zoom","x","y","move","phase","hitmiss"))
ngram <- data.frame(rawdata)
ngram5 <- subset(ngram,predictions==1 & len==5,select = -c(len,predictions))

#rawdata =
#read.delim("new_res/ngram5.csv",sep="\t",header=TRUE,col.names=c("userid","taskname","model","hitmiss","accuracy"))
#ngram5 <- data.frame(rawdata)

rawdata = read.delim("new_res/normal_hist.csv",
header=TRUE,col.names=c("userid","taskname","model","predictions","zoom","x","y","move","phase","hitmiss"))
signatures <- data.frame(rawdata)
signatures$model <- factor(signatures$model, levels = c("normal","histogram","fhistogram"))


sig <- subset(signatures,predictions==1,select = -predictions)
tc <- ddply(sig,.(taskname,phase), "nrow")
mc <- subset(ddply(sig,.(taskname,move),"nrow"),move %in% c("I","O","P"))
#doesn't work
hits1 <- ddply(subset(sig,hitmiss=="hit" & move %in% c("I","O","P")),.(userid,taskname,model,move),"nrow")
hitmiss1 <- ddply(subset(sig,move %in% c("I","O","P")),.(userid,taskname,model,move),"nrow")

p <- ggplot(data=tc, aes(x=taskname, y=nrow,fill=phase))
#p <- p + facet_grid(~task)
p <- p + stat_summary(fun.y="mean", geom="bar",position=position_fill()) #geom_bar(stat="identity",position=position_fill())
p <- p + xlab("Exploration Phase") + ylab("Total Requests")
p <- p + ggtitle(paste("Distribution of Exploration Phases"))
print(p)
ggsave(paste("excounts2.png",sep=""),width=6,height=4)

p <- ggplot(data=mc, aes(x=taskname, y=nrow, fill=move))
p <- p + stat_summary(fun.y="mean", geom="bar",position=position_fill()) #geom_bar(stat="identity",position=position_dodge())
p <- p + xlab("Movement (Pan or Zoom?)") + ylab("Percentage")
p <- p + ggtitle(paste("Distribution of Moves"))
print(p)
ggsave(paste("dircounts2.png",sep=""),width=12,height=8)

#doesn't work
p <- ggplot(data=sig, aes(x=model, fill=hitmiss))
p <- p + facet_grid(move~taskname)
#p <- p + geom_boxplot(position=position_dodge())
p <- p + geom_bar(aes(y = (..count..)/sum(..count..)),position=position_dodge())
p <- p + xlab("Recommendation Model") + ylab("Accuracy")
p <- p + ggtitle(paste("General Accuracy for All Signatures"))
print(p)
ggsave(paste("signatures_accuracy_bars2.png",sep=""),width=12,height=6)

