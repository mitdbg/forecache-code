library(ggplot2)

theme_set(theme_grey(base_size = 16)) 

datapath = "/Volumes/E/mit/vis/code/scalar-prefetch/results/zoom_info.csv"
figpath = "/Volumes/E/mit/vis/code/scalar-prefetch/code/R/move_analysis/"
tasknames = c("task1","task2","task3")
taskids = c(1,2,3)

rawdata= read.csv(datapath, header=TRUE, col.names=c("id","taskname","zoom"))
d <- data.frame(rawdata)
ids <- unique(d$id) # get unique user id's

for(i in ids) {
#for(i in c(123)) {
  cat(paste("retrieving data for user ",i,"\n",sep=""))
for(tid in taskids) {
  tn = tasknames[tid]
  cat(paste("task: '",tn,"'\n",sep=""))
  d2 <- subset(d,id==i & taskname==tn)
  #d2 <- subset(d,id==i)
  d2$x <- 1:length(d2$zoom)
  cat(paste("size of d2: ",length(d2$zoom),"\n",sep=""))
  #print(d2)
  p <- ggplot(d2,aes(x=x,y=zoom))
  #p <- ggplot(d2,aes(x=x,y=y,group=taskname,color=factor(taskname)))
  p <- p + geom_line()
  p<- p + geom_point() + scale_y_reverse() + xlab("Tile Request ID") + ylab("Zoom Level")
  p <- p + ggtitle(paste("Change in Zoom Level\nOver Time (User ",i,", Task ",tid,")",sep=""))
  #p <- p + ggtitle(paste("Change in Zoom Level\nOver Time (User 2, Task ",tid,")",sep=""))
  print(p)
  ggsave(paste(figpath,tn,"/user",i,"_",tn,".png",sep=""),width=6,height=4)
  #ggsave(paste(figpath,tn,"/user2_",tn,".png",sep=""),width=6,height=4)
}
}
#}
