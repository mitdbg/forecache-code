library(ggplot2)
library(rjson)
sigrootpath = "/Volumes/E/mit/vis/code/scalar-prefetch/sigs/8"

#Task 2
#swiss alps
#tile: [76,55],zoom level: 8
t8_76_55 = paste(sigrootpath,"8a8e50633348242e92e7ec16aebe9d3a.histsig",sep="/")
#tile: [77,55],zoom level: 8
t8_77_55 = paste(sigrootpath,"4f009735c511d50ef7b678df98134ecc.histsig",sep="/")
#tile: [78,55],zoom level: 8
t8_78_55 = paste(sigrootpath,"5943c221518c276bfc57057de1b48b66.histsig",sep="/")
#tile: [79,55],zoom level: 8
t8_79_55 = paste(sigrootpath,"4af1a3e58147a6e000949e8d253d05f5.histsig",sep="/")

#tile: [76,56],zoom level: 8
t8_76_56 = paste(sigrootpath,"072b27657a3822c482a20b70abcfcef3.histsig",sep="/")
#tile: [77,56],zoom level: 8
t8_77_56 = paste(sigrootpath,"45e8446e79be3f7065abebabedde244b.histsig",sep="/")
#tile: [78,56],zoom level: 8
t8_78_56 = paste(sigrootpath,"e9c45b9ba57997093d494d1886f059c5.histsig",sep="/")
#tile: [79,56],zoom level: 8
t8_79_56 = paste(sigrootpath,"a20a1087b8e802044d5560f1fb70c30c.histsig",sep="/")

#Northern europe
#tile: [76,61], zoom level: 8
t8_76_61 = paste(sigrootpath,"17ceea70033a1f7c3553bce665e36be2.histsig",sep="/")
#tile: [77,61],zoom level: 8
t8_77_61 = paste(sigrootpath,"0c5696944929a45737362b77e7618b0b.histsig",sep="/")
#tile: [78,61],zoom level: 8
t8_78_61 = paste(sigrootpath,"4c273b4cc91c24f59498e6a3c02b8028.histsig",sep="/")
#tile: [76,62], zoom level: 8
t8_76_62 = paste(sigrootpath,"709d83b77acd2983551224b04c79b144.histsig",sep="/")
#tile: [77,62], zoom level: 8
t8_77_62 = paste(sigrootpath,"6f3e4aa1e31a790e2d94034fa9d63b67.histsig",sep="/")
#tile: [78,62], zoom level: 8
t8_78_62 = paste(sigrootpath,"7a09d2feca9f49e982d66ea8fd8ae591.histsig",sep="/")
#tile: [77,63],zoom level: 8 
t8_77_63 = paste(sigrootpath,"42be1fd329c787f955905960091a0bc0.histsig",sep="/")
#tile: [78,63],zoom level: 8
t8_78_63 = paste(sigrootpath,"fff17b0ecb44ed087bfcac3ef3803f19.histsig",sep="/")

#Spanish alps:
#tile: [73,54],zoom level: 8
t8_73_54 = paste(sigrootpath,"07f3c197ab6aaf11cff4a8123933637d.histsig",sep="/")
#tile: [74,54],zoom level: 8
t8_74_54 = paste(sigrootpath,"e5893589131d15084f91956850c6a723.histsig",sep="/")
#tile: [75,54],zoom level: 8
t8_75_54 = paste(sigrootpath,"6173c0c64c1aff99ed308653d27c86d4.histsig",sep="/")
#tile: [74,55],zoom level: 8
t8_74_55 = paste(sigrootpath,"e2650ed3499cda2999e5fa8576003ba3.histsig",sep="/")
#tile: [75,55],zoom level: 8
t8_75_55 = paste(sigrootpath,"9f071d0b4447da6bec76fcfd1b26dab1.histsig",sep="/")

#tilenames = c("t8_76_55", "t8_77_55", "t8_78_55", "t8_79_55", "t8_76_56", "t8_77_56", "t8_78_56", "t8_79_56")
tilenames = c("t8_76_55", "t8_77_55", "t8_78_55", "t8_79_55", "t8_76_56", "t8_77_56", "t8_78_56", "t8_79_56", "t8_76_61", "t8_77_61", "t8_78_61", "t8_76_62", "t8_77_62", "t8_78_62", "t8_77_63", "t8_78_63", "t8_73_54", "t8_74_54", "t8_75_54", "t8_74_55", "t8_75_55")
vecpaths = c(t8_76_55, t8_77_55, t8_78_55, t8_79_55, t8_76_56, t8_77_56, t8_78_56, t8_79_56, t8_76_61, t8_77_61, t8_78_61, t8_76_62, t8_77_62, t8_78_62, t8_77_63, t8_78_63, t8_73_54, t8_74_54, t8_75_54, t8_74_55, t8_75_55)
#t8_76_55 t8_77_55 t8_78_55 t8_79_55 t8_76_56 t8_77_56 t8_78_56 t8_79_56 t8_76_61 t8_77_61 t8_78_61 t8_76_62 t8_77_62 t8_78_62 t8_77_63 t8_78_63 t8_73_54 t8_74_54 t8_75_54 t8_74_55 t8_75_55

for (i in 1:length(tilenames)) {
  vecpath = vecpaths[i]
  figpath = paste("figs",tilenames[i],sep="/")
  cat(paste(vecpath,"\n",sep=""))
  cat(paste(figpath,"\n",sep=""))
  json_data <- fromJSON(paste(readLines(vecpath),collapse=""))
  d <- data.frame(json_data)
  d$x <- 1:length(d$histogram)
  d$x <- (d$x / 400) * 2 - 1.0

  p <- ggplot(d,aes(x=x,y=histogram))
  p <- p + geom_bar(stat='identity')+ coord_cartesian(ylim=c(0,.025))
  print(p)
  ggsave(paste(figpath,"hist.png",sep="/"),width=10,height=4)
}

