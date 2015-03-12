library(ggplot2)

rawdata = read.delim("res/task_stats.csv", header=FALSE,col.names=c("taskname","match","count"))
task_stats <- data.frame(rawdata)


p <- ggplot(data=task_stats, aes(x=match, y=count, fill=match))
p <- p + facet_grid(~taskname)
p <- p + geom_bar(stat="identity",position=position_dodge())
p <- p + xlab("How closely users matched") + ylab("Count of users")
p <- p + ggtitle(paste("Distribution of User Matches to Our Exploration Model"))
print(p)
ggsave(paste("user_matches.png",sep=""),width=9,height=4)

