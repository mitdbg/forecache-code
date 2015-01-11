library(ggplot2)
#0,[0, 0]
#vecpath = "/Volumes/E/mit/vis/code/scalar-prefetch/tilevecs/0/fabf634233b5a4518efdbe074188301b.csv"

#Task 1
#7, [13, 29]
t7_13_29 = "/Volumes/E/mit/vis/code/scalar-prefetch/tilevecs/7/75be363447906c89c2470eed414350cc.csv"
#7, [13, 28]
t7_13_28 = "/Volumes/E/mit/vis/code/scalar-prefetch/tilevecs/7/037691bda5a195901067a7772dee322a.csv"
#tile: [12,29],zoom level: 7
t7_12_29 = "/Volumes/E/mit/vis/code/scalar-prefetch/tilevecs/7/342d0694911cb2713c2b4a75bc481204.csv"
#tile: [12,28],zoom level: 7
t7_12_28 = "/Volumes/E/mit/vis/code/scalar-prefetch/tilevecs/7/10a5f483239b3c1c6d4e963c1cd3bf28.csv"
#tile: [11,29],zoom level: 7
t7_11_29 = "/Volumes/E/mit/vis/code/scalar-prefetch/tilevecs/7/f421fc0a9bf9fa39df3435ff144ec777.csv"
#tile: [11,28],zoom level: 7
t7_11_28 = "/Volumes/E/mit/vis/code/scalar-prefetch/tilevecs/7/4211fb52d1206ae9d86be9f687d14c14.csv"


#tile: [14,27],zoom level: 7
t7_14_27 = "/Volumes/E/mit/vis/code/scalar-prefetch/tilevecs/7/ec9dc534c35783d5e49cc76129cdc2ae.csv"
#tile: [15,27],zoom level: 7
t7_15_27 = "/Volumes/E/mit/vis/code/scalar-prefetch/tilevecs/7/c65ca4f3c2634cacf2dd177bc1dca90e.csv"
#tile: [14,26],zoom level: 7
t7_14_26 = "/Volumes/E/mit/vis/code/scalar-prefetch/tilevecs/7/4ec16ed42e7d150ee1433608689ef30a.csv"
#tile: [15,26],zoom level: 7
t7_15_26 = "/Volumes/E/mit/vis/code/scalar-prefetch/tilevecs/7/b6ed76df474369733627c8882cedc768.csv"

# near WA
#tilenames = c("t7_13_29", "t7_13_28", "t7_12_29", "t7_12_28", "t7_11_29", "t7_11_28")
#vecpaths = c(t7_13_29, t7_13_28, t7_12_29, t7_12_28, t7_11_29, t7_11_28)

#rockies
tilenames = c("t7_14_27", "t7_15_27", "t7_14_26", "t7_15_26")
vecpaths = c(t7_14_27, t7_15_27, t7_14_26, t7_15_26)

for (i in 1:length(tilenames)) {
  vecpath = vecpaths[i]
  figpath = paste("figs",tilenames[i],sep="/")
  cat(paste(vecpath,"\n",sep=""))
  cat(paste(figpath,"\n",sep=""))
  dat = read.csv(vecpath, header=FALSE, col.names=c("ndsi","lsmask"))

  p <- ggplot(dat,aes(x=ndsi))
  #p <- p + geom_histogram(aes(y=..density..),binwidth=.005) + coord_cartesian(xlim=c(-1,1),ylim=c(0,6))
  p <- p + geom_histogram(aes(y=..count..),binwidth=.005) + coord_cartesian(xlim=c(-1,1),ylim=c(0,2000))
  p <- p + facet_grid(lsmask ~ .)
  ggsave(paste(figpath,"maskall.png",sep="/"),width=10,height=4)

  #d0 = subset(dat,lsmask==0 | lsmask==7)
  #p <- ggplot(d0,aes(x=ndsi,fill=factor(lsmask)), environment=environment())
  #p <- p + geom_histogram(aes(y=..density..),binwidth=.05,position="dodge", environement=environment())
  #print(p)
  #ggsave(paste(figpath,"mask0_7.png",sep="/"))

  #d0 = subset(dat,lsmask==0 | lsmask==1)
  #p <- ggplot(d0,aes(x=ndsi,fill=factor(lsmask)))
  #p + geom_histogram(aes(y=..density..),binwidth=.05,position="dodge")+ coord_cartesian(xlim=c(-1,1))
  #ggsave(paste(figpath,"mask0_1.png",sep="/"))

  #d0 = subset(dat,lsmask==0 | lsmask==2)
  #p <- ggplot(d0,aes(x=ndsi,fill=factor(lsmask)))
  #p + geom_histogram(aes(y=..density..),binwidth=.05,position="dodge")+ coord_cartesian(xlim=c(-1,1))
  #ggsave(paste(figpath,"mask0_2.png",sep="/"))

  #d0 = subset(dat,lsmask==1 | lsmask==2)
  #p <- ggplot(d0,aes(x=ndsi,fill=factor(lsmask)))
  #p + geom_histogram(aes(y=..density..),binwidth=.05,position="dodge")+ coord_cartesian(xlim=c(-1,1))
  #ggsave(paste(figpath,"mask1_2.png",sep="/"))
}


