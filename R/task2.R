library(ggplot2)
library(rjson)
rootpath = "/Volumes/E/mit/vis/code/scalar-prefetch/tilevecs/8"

#Task 2
#swiss alps
#tile: [76,55],zoom level: 8
t8_76_55 = paste(rootpath,"8a8e50633348242e92e7ec16aebe9d3a.csv",sep="/")
#tile: [77,55],zoom level: 8
t8_77_55 = paste(rootpath,"4f009735c511d50ef7b678df98134ecc.csv",sep="/")
#tile: [78,55],zoom level: 8
t8_78_55 = paste(rootpath,"5943c221518c276bfc57057de1b48b66.csv",sep="/")
#tile: [79,55],zoom level: 8
t8_79_55 = paste(rootpath,"4af1a3e58147a6e000949e8d253d05f5.csv",sep="/")

#tile: [76,56],zoom level: 8
t8_76_56 = paste(rootpath,"072b27657a3822c482a20b70abcfcef3.csv",sep="/")
#tile: [77,56],zoom level: 8
t8_77_56 = paste(rootpath,"45e8446e79be3f7065abebabedde244b.csv",sep="/")
#tile: [78,56],zoom level: 8
t8_78_56 = paste(rootpath,"e9c45b9ba57997093d494d1886f059c5.csv",sep="/")
#tile: [79,56],zoom level: 8
t8_79_56 = paste(rootpath,"a20a1087b8e802044d5560f1fb70c30c.csv",sep="/")

#Northern europe
#tile: [76,61], zoom level: 8
t8_76_61 = paste(rootpath,"17ceea70033a1f7c3553bce665e36be2.csv",sep="/")
#tile: [77,61],zoom level: 8
t8_77_61 = paste(rootpath,"0c5696944929a45737362b77e7618b0b.csv",sep="/")
#tile: [78,61],zoom level: 8
t8_78_61 = paste(rootpath,"4c273b4cc91c24f59498e6a3c02b8028.csv",sep="/")
#tile: [76,62], zoom level: 8
t8_76_62 = paste(rootpath,"709d83b77acd2983551224b04c79b144.csv",sep="/")
#tile: [77,62], zoom level: 8
t8_77_62 = paste(rootpath,"6f3e4aa1e31a790e2d94034fa9d63b67.csv",sep="/")
#tile: [78,62], zoom level: 8
t8_78_62 = paste(rootpath,"7a09d2feca9f49e982d66ea8fd8ae591.csv",sep="/")
#tile: [77,63],zoom level: 8 
t8_77_63 = paste(rootpath,"42be1fd329c787f955905960091a0bc0.csv",sep="/")
#tile: [78,63],zoom level: 8
t8_78_63 = paste(rootpath,"fff17b0ecb44ed087bfcac3ef3803f19.csv",sep="/")

#Spanish alps:
#tile: [73,54],zoom level: 8
t8_73_54 = paste(rootpath,"07f3c197ab6aaf11cff4a8123933637d.csv",sep="/")
#tile: [74,54],zoom level: 8
t8_74_54 = paste(rootpath,"e5893589131d15084f91956850c6a723.csv",sep="/")
#tile: [75,54],zoom level: 8
t8_75_54 = paste(rootpath,"6173c0c64c1aff99ed308653d27c86d4.csv",sep="/")
#tile: [74,55],zoom level: 8
t8_74_55 = paste(rootpath,"e2650ed3499cda2999e5fa8576003ba3.csv",sep="/")
#tile: [75,55],zoom level: 8
t8_75_55 = paste(rootpath,"9f071d0b4447da6bec76fcfd1b26dab1.csv",sep="/")

#tilenames = c("t8_76_55", "t8_77_55", "t8_78_55", "t8_79_55", "t8_76_56", "t8_77_56", "t8_78_56", "t8_79_56")
tilenames = c("t8_76_55", "t8_77_55", "t8_78_55", "t8_79_55", "t8_76_56", "t8_77_56", "t8_78_56", "t8_79_56", "t8_76_61", "t8_77_61", "t8_78_61", "t8_76_62", "t8_77_62", "t8_78_62", "t8_77_63", "t8_78_63", "t8_73_54", "t8_74_54", "t8_75_54", "t8_74_55", "t8_75_55")
vecpaths = c(t8_76_55, t8_77_55, t8_78_55, t8_79_55, t8_76_56, t8_77_56, t8_78_56, t8_79_56, t8_76_61, t8_77_61, t8_78_61, t8_76_62, t8_77_62, t8_78_62, t8_77_63, t8_78_63, t8_73_54, t8_74_54, t8_75_54, t8_74_55, t8_75_55)
#t8_76_55 t8_77_55 t8_78_55 t8_79_55 t8_76_56 t8_77_56 t8_78_56 t8_79_56 t8_76_61 t8_77_61 t8_78_61 t8_76_62 t8_77_62 t8_78_62 t8_77_63 t8_78_63 t8_73_54 t8_74_54 t8_75_54 t8_74_55 t8_75_55

for (i in 1:length(tilenames)) {
  vecpath = vecpaths[i]
  figpath = paste("figs",tilenames[i],sep="/")
  cat(paste(vecpath,"\n",sep=""))
  cat(paste(figpath,"\n",sep=""))
  dat = read.csv(vecpath, header=FALSE, col.names=c("ndsi","lsmask"))
  d <- subset(dat, lsmask == 1)

  p <- ggplot(d,aes(x=ndsi))
  #p <- p + geom_histogram(aes(y=..density..),binwidth=.005) + coord_cartesian(xlim=c(-1,1),ylim=c(0,6))
  p <- p + geom_histogram(aes(y=..count..),binwidth=.005) + coord_cartesian(xlim=c(-1,1),ylim=c(0,2000))
  #p <- p + geom_histogram(aes(y=..count..),binwidth=.01,origin=-1) + coord_cartesian(xlim=c(-1,1),ylim=c(0,6000))
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


