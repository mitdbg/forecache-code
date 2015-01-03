library(ggplot2)
library(reshape)
library(plyr)

theme_set(theme_grey(base_size = 18)) 

image_folder="/Volumes/E/mit/vis/code/scalar-prefetch/docs/dbpaper/images/png/"

rawdata =
read.delim("new_res/output/move_phase_dist.csv",sep="\t",header=FALSE,col.names=c("userid","taskname","move","phase","total"))
move_phase_dist <- data.frame(rawdata)

#total requests
p <- ggplot(data=move_phase_dist, aes(x=taskname, y=total,fill=taskname))
p <- p + stat_summary(fun.y="mean", geom="bar",position=position_dodge()) #geom_bar(stat="identity",position=position_fill())
p <- p + xlab("Task") + ylab("Average Requests")
p <- p + ggtitle(paste("Average Number of Requests per Task"))
print(p)
ggsave(paste(image_folder,"request_total.png",sep=""),width=7,height=5)

#phase distribution
p <- ggplot(data=move_phase_dist, aes(x=taskname, y=total,fill=phase))
p <- p + stat_summary(fun.y="mean", geom="bar",position=position_fill()) #geom_bar(stat="identity",position=position_fill())
p <- p + xlab("Task") + ylab("Requests by Exploration Phase")
p <- p + ggtitle(paste("Distribution of Exploration Phases"))
print(p)
ggsave(paste(image_folder,"phase_dist.png",sep=""),width=7,height=5)

#move distribution
p <- ggplot(data=move_phase_dist, aes(x=taskname, y=total,fill=move))
p <- p + stat_summary(fun.y="mean", geom="bar",position=position_fill()) #geom_bar(stat="identity",position=position_fill())
p <- p + xlab("Task") + ylab("Requests by Move")
p <- p + ggtitle(paste("Distribution of Moves"))
print(p)
ggsave(paste(image_folder,"move_dist.png",sep=""),width=7,height=5)


##move vs phase distribution
#p <- ggplot(data=move_phase_dist, aes(x=phase, y=total,fill=move))
#p <- p + facet_grid(~taskname)
#p <- p + stat_summary(fun.y="mean", geom="bar",position=position_fill()) #geom_bar(stat="identity",position=position_fill())
#p <- p + xlab("Exploration Phase") + ylab("Total Requests by Move")
#p <- p + ggtitle(paste("Distribution of Moves Across Exploration Phases"))
#print(p)
#ggsave(paste("move_phase_dist.png",sep=""),width=10,height=5)

#move vs phase distribution, transpose
#p <- ggplot(data=move_phase_dist, aes(x=move, y=total,fill=phase))
#p <- p + facet_grid(~taskname)
#p <- p + stat_summary(fun.y="mean", geom="bar",position=position_stack()) #geom_bar(stat="identity",position=position_fill())
#p <- p + xlab("Move (I=zoom in,O=zoom out,P=pan)") + ylab("Total Requests")
#p <- p + ggtitle(paste("Distribution of Moves Across Exploration Phases"))
#print(p)
#ggsave(paste("move_phase_dist.png",sep=""),width=10,height=5)
