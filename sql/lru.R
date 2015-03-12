#!/usr/bin/Rscript

library(ggplot2)
library(reshape)
library(plyr)

# gets lru value as an input
#args <- commandArgs(trailingOnly = TRUE)
#lru <- args[1]

theme_set(theme_grey(base_size = 22)) 
#image_folder = "/Volumes/E/mit/vis/code/scalar-prefetch/docs/dbpaper/images/png/"
image_folder = "/Volumes/E/mit/vis/code/scalar-prefetch/code/sql/images/"

ngram_filter = c("ngram2","ngram3","ngram4","ngram5","ngram6","ngram7","ngram8","ngram9","ngram10")
markov_filter = c("markov2","markov3","markov4","markov5","markov6","markov7","markov8","markov9","markov10")
sig_filter = c("sift","dsift","normal","histogram")
existing_filter = c("momentum2","hotspot2")
best_filter = c("markov3","sift")
move_filter = c("markov3","momentum2","hotspot2")
#all_filter = c("markov3","sift","dsift","hotspot2","normal","histogram","momentum2")

#hotspot is rarely better than momentum
#sift is clearly better than dsift
#histogram and normal are noise
#all_filter = c("markov3","sift","dsift","normal","histogram","hotspot2","momentum2")
all_filter = c("markov3","sift","momentum2")
all_filter2 = c("markov3","sift")


rawdata =
read.delim("new_res/output/lru_move2.csv",sep="\t",header=FALSE,col.names=c("lval","userid","taskname","model","allocated","move","accuracy","total"))
all_lru_data <- data.frame(rawdata)

rawdata =
read.delim("new_res/output/lru0_accuracy.csv",sep="\t",header=FALSE,col.names=c("userid","taskname","model","allocated","accuracy","total"))
lru0_data <- data.frame(rawdata)
rawdata =
read.delim("new_res/output/lru0_move_accuracy.csv",sep="\t",header=FALSE,col.names=c("userid","taskname","model","allocated","move","accuracy","total"))
lru0_move_data <- data.frame(rawdata)
rawdata =
read.delim("new_res/output/lru0_phase_accuracy.csv",sep="\t",header=FALSE,col.names=c("userid","taskname","model","allocated","phase","accuracy","total"))
lru0_phase_data <- data.frame(rawdata)
lru0_obj <- list(data=lru0_data,move_data=lru0_move_data,phase_data=lru0_phase_data)


rawdata =
read.delim("new_res/output/lru4_accuracy.csv",sep="\t",header=FALSE,col.names=c("userid","taskname","model","allocated","accuracy","total"))
lru4_data <- data.frame(rawdata)
rawdata =
read.delim("new_res/output/lru4_move_accuracy.csv",sep="\t",header=FALSE,col.names=c("userid","taskname","model","allocated","move","accuracy","total"))
lru4_move_data <- data.frame(rawdata)
rawdata =
read.delim("new_res/output/lru4_phase_accuracy.csv",sep="\t",header=FALSE,col.names=c("userid","taskname","model","allocated","phase","accuracy","total"))
lru4_phase_data <- data.frame(rawdata)
lru4_obj <- list(data=lru4_data,move_data=lru4_move_data,phase_data=lru4_phase_data)


rawdata =
read.delim("new_res/output/lru8_accuracy.csv",sep="\t",header=FALSE,col.names=c("userid","taskname","model","allocated","accuracy","total"))
lru8_data <- data.frame(rawdata)
rawdata =
read.delim("new_res/output/lru8_move_accuracy.csv",sep="\t",header=FALSE,col.names=c("userid","taskname","model","allocated","move","accuracy","total"))
lru8_move_data <- data.frame(rawdata)
rawdata =
read.delim("new_res/output/lru8_phase_accuracy.csv",sep="\t",header=FALSE,col.names=c("userid","taskname","model","allocated","phase","accuracy","total"))
lru8_phase_data <- data.frame(rawdata)
lru8_obj <- list(data=lru8_data,move_data=lru8_move_data,phase_data=lru8_phase_data)


lru_obj <- list(lru0_obj,lru4_obj,lru8_obj)
fetch <- c(1,2,3,4,5,6,7,8)
lruvals <- c(0,4,8)
bounds <- c(0.0,1.0)
sizes <- c(12,5)
psize = 3
lsize = 25

##compares lru values
#line_data <- subset(all_lru_data,model %in% all_filter2)
#p <- ggplot(data=line_data, aes(x=factor(lval), y=accuracy,color=factor(allocated)))
#p <- p + facet_grid(~model)
#p <- p + stat_summary(fun.y="mean", geom="line",position=position_identity(), aes(group = allocated))
#p <- p + stat_summary(fun.y="mean", geom="point",position=position_identity(),size=psize)
#p <- p + xlab("Last n Moves Cached") + ylab("Accuracy")
#p <- p + ggtitle(paste("Accuracy With Additional Caching",sep=""))
#p <- p + coord_cartesian(ylim = c(0.0, bounds[2]))
#p <- p + theme(legend.text = element_text(size = lsize))+ guides(color=guide_legend(title="Top-k")) 
#print(p)
#ggsave(paste(image_folder,"lru_all.png",sep=""),width=sizes[1],height=sizes[2])


##compares lru values
#line_data <- subset(lru0_phase_data,model %in% all_filter2)
#p <- ggplot(data=line_data, aes(x=factor(allocated), y=accuracy,color=model))
#p <- p + facet_grid(~phase)
#p <- p + stat_summary(fun.y="mean", geom="line",position=position_identity(), aes(group = model))
#p <- p + stat_summary(fun.y="mean", geom="point",position=position_identity(),size=psize)
#p <- p + xlab("Top k Predictions") + ylab("Accuracy")
#p <- p + ggtitle(paste("Accuracy Per Analysis Phase, with Varying k",sep=""))
#p <- p + coord_cartesian(ylim = c(0.0, bounds[2]))
#p <- p + theme(legend.text = element_text(size = lsize))+ guides(color=guide_legend(title="Top-k")) 
#print(p)
#ggsave(paste(image_folder,"lru0_allk.png",sep=""),width=sizes[1],height=sizes[2])

#total requests
#p <- ggplot(data=lru0_data, aes(x=taskname, y=total,fill=taskname))
#p <- p + stat_summary(fun.y="mean", geom="bar",position=position_dodge())
#p <- p + xlab("Task") + ylab("Average Requests")
#p <- p + ggtitle(paste("Average Number of Requests per Task"))
#p <- p + theme(legend.text = element_text(size = lsize))
#print(p)
#ggsave(paste(image_folder,"request_total.png",sep=""),width=8,height=5)

#disribution of phases per task
#p <- ggplot(data=subset(lru0_phase_data,allocated=='1' & model=='sift'), aes(x=taskname,fill=phase))
#p <- p + geom_bar(position=position_fill())
#p <- p + xlab("Task") + ylab("Fraction of total requests")
#p <- p + ggtitle(paste("Distribution of Analysis Phases",sep=""))
#print(p)
#ggsave(paste(image_folder,"phase_dist.png",sep=""),width=8,height=4)

#disribution of phases per task
#p <- ggplot(data=subset(lru0_move_data,allocated=='1' & model=='sift'), aes(x=taskname,fill=move))
#p <- p + geom_bar(position=position_fill())
#p <- p + xlab("Task") + ylab("Fraction of total requests")
#p <- p + ggtitle(paste("Distribution of moves",sep=""))
#print(p)
#ggsave(paste(image_folder,"move_dist.png",sep=""),width=7,height=4)



##compares lru values
#line_data <- subset(lru0_phase_data,(model %in% all_filter2) & allocated=='4')
#p <- ggplot(data=line_data, aes(x=model, y=accuracy,fill=model))
#p <- p + facet_grid(taskname~phase)
#p <- p + stat_summary(fun.y="mean", geom="bar")
#p <- p + xlab("Model") + ylab("Accuracy")
#p <- p + ggtitle(paste("Prediction Accuracy per Task (k=4)",sep=""))
#p <- p + coord_cartesian(ylim = c(0.0, 1.0))
#print(p)
#ggsave(paste(image_folder,"markov_vs_sift_lru0k4.png",sep=""),width=sizes[1],height=sizes[2])


#make a for loop for the fetch values (1 through 8)
for (i in 1:1) {
  lval = lruvals[i]
  
  ##in each iteration, build the following plots
  #line_data <- subset(lru_obj[[i]]$data,model %in% all_filter)
  #p <- ggplot(data=line_data, aes(x=factor(allocated), y=accuracy,color=model))
  #p <- p + stat_summary(fun.y="mean", geom="line",position=position_identity(), aes(group = model))
  #p <- p + stat_summary(fun.y="mean", geom="point",position=position_identity(),size=psize)
  #p <- p + xlab("Top-K Tiles Predicted") + ylab("Accuracy")
  #p <- p + ggtitle(paste("General Accuracy (lru=",lval,")",sep=""))
  #p <- p + coord_cartesian(ylim = c(bounds[1], bounds[2]))
  #p <- p + theme(legend.text = element_text(size = lsize))
  #print(p)
  #ggsave(paste(image_folder,"n1l",lval,"_line_all.png",sep=""),width=sizes[1],height=sizes[2])
  #
  #line_data <- subset(lru_obj[[i]]$data,model %in% sig_filter)
  #p <- ggplot(data=line_data, aes(x=factor(allocated), y=accuracy,color=model))
  #p <- p + stat_summary(fun.y="mean", geom="line",position=position_identity(), aes(group = model))
  #p <- p + stat_summary(fun.y="mean", geom="point",position=position_identity(),size=psize)
  #p <- p + xlab("Top-K Tiles Predicted") + ylab("Accuracy")
  #p <- p + ggtitle(paste("General Accuracy for All Signatures (lru=",lval,")",sep=""))
  #p <- p + coord_cartesian(ylim = c(bounds[1], bounds[2]))
  #p <- p + theme(legend.text = element_text(size = lsize))
  #print(p)
  #ggsave(paste(image_folder,"n1l",lval,"_line_sig.png",sep=""),width=sizes[1],height=sizes[2])
  
  #line_data <- subset(lru_obj[[i]]$data,model %in% markov_filter)
  #p <- ggplot(data=line_data, aes(x=factor(allocated), y=accuracy,color=model))
  #p <- p + stat_summary(fun.y="mean", geom="line",position=position_identity(), aes(group = model))
  #p <- p + stat_summary(fun.y="mean", geom="point",position=position_identity(),size=psize)
  #p <- p + xlab("Top-K Tiles Predicted") + ylab("Accuracy")
  #p <- p + ggtitle(paste("General Accuracy for All Markov Orders (lru=",lval,")",sep=""))
  #p <- p + coord_cartesian(ylim = c(bounds[1], bounds[2]))
  #p <- p + theme(legend.text = element_text(size = lsize))
  #print(p)
  #ggsave(paste(image_folder,"n1l",lval,"_line_markov.png",sep=""),width=sizes[1],height=sizes[2])


  ##make same plot as above, but only for ngram and sift, and facet by move
  #line_move_data <- subset(lru_obj[[i]]$move_data,model %in% move_filter)
  #p <- ggplot(data=line_move_data, aes(x=factor(allocated), y=accuracy,color=model,shape=model))
  #p <- p + facet_grid(~move)
  #p <- p + stat_summary(fun.y="mean", geom="line",position=position_identity(), aes(group = model))
  #p <- p + stat_summary(fun.y="mean", geom="point",position=position_identity(),size=psize)
  #p <- p + xlab("Top-k tiles Considered") + ylab("Accuracy")
  #p <- p + ggtitle(paste("Accuracy of Markov models by Move",sep="")) #(lru=",lval,")
  #p <- p + coord_cartesian(ylim = c(bounds[1], bounds[2]))
  #p <- p + theme(legend.text = element_text(size = lsize))
  #print(p)
  #ggsave(paste(image_folder,"n1l",lval,"_line_markov_move.png",sep=""),width=sizes[1],height=sizes[2])

  #make same plot as above, but only for ngram and sift, and facet by phase
  #line_phase_data <- subset(lru_obj[[i]]$phase_data,model %in% move_filter)
  #p <- ggplot(data=line_phase_data, aes(x=factor(allocated), y=accuracy,color=model,shape=model))
  #p <- p + facet_grid(~phase)
  #p <- p + stat_summary(fun.y="mean", geom="line",position=position_identity(), aes(group = model))
  #p <- p + stat_summary(fun.y="mean", geom="point",position=position_identity(),size=psize)
  #p <- p + xlab("Top-k tiles Considered") + ylab("Accuracy")
  #p <- p + ggtitle(paste("Accuracy of Markov models by Analysis Phase",sep="")) # (lru=",lval,")
  #p <- p + coord_cartesian(ylim = c(bounds[1], bounds[2]))
  #p <- p + theme(legend.text = element_text(size = lsize))
  #print(p)
  #ggsave(paste(image_folder,"n1l",lval,"_line_markov_phase.png",sep=""),width=sizes[1],height=sizes[2])

  ##make same plot as above, but only for ngram and sift, and facet by move
  #line_move_data <- subset(lru_obj[[i]]$move_data,model %in% sig_filter)
  #p <- ggplot(data=line_move_data, aes(x=factor(allocated), y=accuracy,color=model,shape=model))
  #p <- p + facet_grid(~move)
  #p <- p + stat_summary(fun.y="mean", geom="line",position=position_identity(), aes(group = model))
  #p <- p + stat_summary(fun.y="mean", geom="point",position=position_identity(),size=psize)
  #p <- p + xlab("Top-k tiles Considered") + ylab("Accuracy")
  #p <- p + ggtitle(paste("Accuracy of Signature-Based Models by Move",sep="")) #(lru=",lval,")
  #p <- p + coord_cartesian(ylim = c(bounds[1], bounds[2]))
  #p <- p + theme(legend.text = element_text(size = lsize))
  #print(p)
  #ggsave(paste(image_folder,"n1l",lval,"_line_sig_move.png",sep=""),width=sizes[1],height=sizes[2])

  ##make same plot as above, but only for ngram and sift, and facet by phase
  #line_phase_data <- subset(lru_obj[[i]]$phase_data,model %in% sig_filter)
  #p <- ggplot(data=line_phase_data, aes(x=factor(allocated), y=accuracy,color=model,shape=model))
  #p <- p + facet_grid(~phase)
  #p <- p + stat_summary(fun.y="mean", geom="line",position=position_identity(), aes(group = model))
  #p <- p + stat_summary(fun.y="mean", geom="point",position=position_identity(),size=psize)
  #p <- p + xlab("Top-k tiles Considered") + ylab("Accuracy")
  #p <- p + ggtitle(paste("Accuracy of Signature-Based Models by Analysis Phase",sep="")) # (lru=",lval,")
  #p <- p + coord_cartesian(ylim = c(bounds[1], bounds[2]))
  #p <- p + theme(legend.text = element_text(size = lsize))
  #print(p)
  #ggsave(paste(image_folder,"n1l",lval,"_line_sig_phase.png",sep=""),width=sizes[1],height=sizes[2])

  #make same plot as above, but only for ngram and sift, and facet by move
  #line_move_data <- subset(lru_obj[[i]]$move_data,model %in% best_filter)
  #p <- ggplot(data=line_move_data, aes(x=factor(allocated), y=accuracy,color=model,shape=model))
  #p <- p + facet_grid(~move)
  #p <- p + stat_summary(fun.y="mean", geom="line",position=position_identity(), aes(group = model))
  #p <- p + stat_summary(fun.y="mean", geom="point",position=position_identity(),size=psize)
  #p <- p + xlab("Top-k tiles Considered") + ylab("Accuracy")
  #p <- p + ggtitle(paste("Markov vs. SIFT by Move",sep="")) # (lru=",lval,")
  #p <- p + coord_cartesian(ylim = c(bounds[1], bounds[2]))
  #p <- p + theme(legend.text = element_text(size = lsize))
  #print(p)
  #ggsave(paste(image_folder,"n1l",lval,"_line_best_move.png",sep=""),width=sizes[1],height=sizes[2])


  ##make same plot as above, but only for ngram and sift, and facet by phase
  #line_phase_data <- subset(lru_obj[[i]]$phase_data,model %in% best_filter)
  #p <- ggplot(data=line_phase_data, aes(x=factor(allocated), y=accuracy,color=model,shape=model))
  #p <- p + facet_grid(~phase)
  #p <- p + stat_summary(fun.y="mean", geom="line",position=position_identity(), aes(group = model))
  #p <- p + stat_summary(fun.y="mean", geom="point",position=position_identity(),size=psize)
  #p <- p + xlab("Top-k tiles Considered") + ylab("Accuracy")
  #p <- p + ggtitle(paste("Markov vs. SIFT by Analysis Phase",sep="")) # (lru=",lval,")
  #p <- p + coord_cartesian(ylim = c(bounds[1], bounds[2]))
  #p <- p + theme(legend.text = element_text(size = lsize))
  #print(p)
  #ggsave(paste(image_folder,"n1l",lval,"_line_best_phase.png",sep=""),width=sizes[1],height=sizes[2])

  #line_phase_data <- subset(lru_obj[[i]]$phase_data,model %in% best_filter & phase=='Sensemaking' & taskname %in% c('task2','task3'))
  #p <- ggplot(data=line_phase_data, aes(x=factor(allocated), y=accuracy,color=model,shape=model))
  #p <- p + facet_grid(~taskname)
  #p <- p + stat_summary(fun.y="mean", geom="line",position=position_identity(), aes(group = model))
  #p <- p + stat_summary(fun.y="mean", geom="point",position=position_identity(),size=psize)
  #p <- p + xlab("Top-k tiles Considered") + ylab("Accuracy")
  #p <- p + ggtitle(paste("Markov vs. SIFT for Sensemaking Phase",sep="")) # (lru=",lval,")
  #p <- p + coord_cartesian(ylim = c(bounds[1], bounds[2]))
  #p <- p + theme(legend.text = element_text(size = lsize))
  #print(p)
  #ggsave(paste(image_folder,"n1l",lval,"_line_best_phase_specific.png",sep=""),width=sizes[1],height=sizes[2])

  ##make same plot as above, but only for ngram and sift, and facet by phase
  #line_phase_data <- subset(lru_obj[[i]]$phase_data,(model %in% best_filter | model=='hybrid'))
  #p <- ggplot(data=line_phase_data, aes(x=factor(allocated), y=accuracy,color=model,shape=model))
  #p <- p + stat_summary(fun.y="mean", geom="line",position=position_identity(), aes(group = model))
  #p <- p + stat_summary(fun.y="mean", geom="point",position=position_identity(),size=psize)
  #p <- p + xlab("Top-k tiles Considered") + ylab("Accuracy")
  #p <- p + ggtitle(paste("Markov vs. SIFT vs. Hybrid Model",sep="")) # (lru=",lval,")
  #p <- p + coord_cartesian(ylim = c(bounds[1], bounds[2]))
  #p <- p + theme(legend.text = element_text(size = lsize))
  #print(p)
  #ggsave(paste(image_folder,"n1l",lval,"_line_mixed.png",sep=""),width=sizes[1],height=sizes[2])

   #make same plot as above, but only for ngram and sift, and facet by phase
  #line_phase_data <- subset(lru_obj[[i]]$phase_data,(model %in% best_filter | model=='hybrid'))
  #p <- ggplot(data=line_phase_data, aes(x=factor(allocated), y=accuracy,color=model,shape=model))
  #p <- p + facet_grid(~phase)
  #p <- p + stat_summary(fun.y="mean", geom="line",position=position_identity(), aes(group = model))
  #p <- p + stat_summary(fun.y="mean", geom="point",position=position_identity(),size=psize)
  #p <- p + xlab("Top-k tiles Considered") + ylab("Accuracy")
  #p <- p + ggtitle(paste("Markov vs. SIFT vs. Hybrid Model by Analysis Phase",sep="")) # (lru=",lval,")
  #p <- p + coord_cartesian(ylim = c(bounds[1], bounds[2]))
  #p <- p + theme(legend.text = element_text(size = lsize))
  #print(p)
  #ggsave(paste(image_folder,"n1l",lval,"_line_mixed_phase.png",sep=""),width=sizes[1],height=sizes[2])

  #make same plot as above, but only for ngram and sift, and facet by phase
  #line_phase_data <- subset(lru_obj[[i]]$phase_data,(model %in% existing_filter | model=='hybrid'))
  #line_phase_data$model <- factor(line_phase_data$model, c('hybrid','hotspot2','momentum2'))
  #p <- ggplot(data=line_phase_data, aes(x=factor(allocated), y=accuracy, color=model, shape=model))
  #p <- p + facet_grid(~phase)
  #p <- p + stat_summary(fun.y="mean", geom="line",position=position_identity(), aes(group = model))
  #p <- p + stat_summary(fun.y="mean", geom="point",position=position_identity(),size=psize)
  #p <- p + xlab("Top-k tiles Considered") + ylab("Accuracy")
  #p <- p + ggtitle(paste("Hybrid Model vs. Existing Techniques",sep="")) # (lru=",lval,")
  #p <- p + coord_cartesian(ylim = c(bounds[1], bounds[2]))
  #p <- p + theme(legend.text = element_text(size = lsize))
  #print(p)
  #ggsave(paste(image_folder,"n1l",lval,"_line_mixed_vs_existing_phase.png",sep=""),width=sizes[1],height=sizes[2])


  for (fval in fetch) {
    data <- subset(lru_obj[[i]]$data, allocated == fval)
    move_data <- subset(lru_obj[[i]]$move_data, allocated == fval)
    all_data <- subset(data,model %in% all_filter)
    markov_data <- subset(data,model %in% markov_filter)
    all_move_data <- subset(move_data,model %in% all_filter2)
    best_move_data <- subset(move_data,model %in% best_filter)

    
    #p <- ggplot(data=markov_data, aes(x=model, y=accuracy,fill=model))
    #p <- p + stat_summary(fun.y="mean", geom="bar",position=position_dodge())
    #p <- p + xlab("Recommendation Model") + ylab("Accuracy")
    #p <- p + ggtitle(paste("General Markov Fetch ",fval," Accuracy (lru=",lval,")",sep=""))
    #p <- p + coord_cartesian(ylim = c(bounds[1], bounds[2]))
    #p <- p + theme(legend.text = element_text(size = lsize))
    #print(p)
    #ggsave(paste(image_folder,"f",fval,"n1l",lval,"_markov.png",sep=""),width=sizes[1],height=sizes[2])
    
    
    #p <- ggplot(data=all_data, aes(x=model, y=accuracy,fill=model))
    ##p <- p + facet_grid(~taskname)
    #p <- p + stat_summary(fun.y="mean", geom="bar",position=position_dodge()) #geom_bar(stat="identity",position=position_fill())
    #p <- p + xlab("Recommendation Model") + ylab("Accuracy")
    ##p <- p + scale_fill_discrete(labels=c("Zoom in","Zoom out","Pan"))
    #p <- p + ggtitle(paste("General Fetch ",fval," Accuracy (lru=",lval,")",sep=""))
    #p <- p + coord_cartesian(ylim = c(bounds[1], bounds[2]))
    #p <- p + theme(legend.text = element_text(size = lsize))
    #print(p)
    #ggsave(paste(image_folder,"f",fval,"n1l",lval,".png",sep=""),width=sizes[1],height=sizes[2])
    #
    #
    #p <- ggplot(data=all_move_data, aes(x=model, y=accuracy,fill=model))
    #p <- p + facet_grid(~move)
    #p <- p + stat_summary(fun.y="mean", geom="bar",position=position_dodge()) #geom_bar(stat="identity",position=position_fill())
    #p <- p + xlab("Recommendation Model") + ylab("Accuracy")
    ##p <- p + scale_fill_discrete(labels=c("Zoom in","Zoom out","Pan"))
    #p <- p + ggtitle(paste("Accuracy by Move (k=",fval,",lru=",lval,")",sep=""))
    #p <- p + coord_cartesian(ylim = c(bounds[1], bounds[2]))
    #p <- p + theme(axis.text.x  = element_blank())
    #p <- p + theme(legend.text = element_text(size = lsize))
    #print(p)
    #ggsave(paste(image_folder,"f",fval,"n1l",lval,"_move.png",sep=""),width=sizes[1],height=sizes[2])
  }
}

