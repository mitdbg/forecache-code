QVis.HeatMap = function(rootid,opts) {
	QVis.Graph.call(this,rootid,opts);

	//unique to scatterplots
	this.rectcontainer = null;
}

//inherit Graph Object
QVis.HeatMap.prototype = new QVis.Graph();

//fix constructor reference
QVis.HeatMap.constructor = QVis.HeatMap;

//to get access to the original functions
QVis.HeatMap.base = QVis.Graph.prototype;

//add a new render function
//data format: {'data':[{},{},...],'names':["","",...],'types':{"":,"":,...}}
QVis.HeatMap.prototype.render_canvas = function(_data, _labels,_types, opts) {
	this.selectx = true;
	this.selecty = true;
	this.selectz = true;
	this.dimsonly = true;
	
	this.draw_obj = "rect";

	var self = this;
	//call the original render function
	self.labelsfrombase = QVis.HeatMap.base.render.call(this,_data,_labels,_types,opts);
	if(this.canvas){
		this.canvas.width = this.canvas.width;
	}

	// create x,y axis scales
	var xdimname = self.labelsfrombase.x_label;//.substring("dims.".length);//""+_labels.dimnames[0];
	var ydimname = self.labelsfrombase.y_label;//.substring("dims.".length);//""+_labels.dimnames[1];
	//console.log("dimnames: "+xdimname+','+ydimname);
	//console.log(['dimbases',_labels.dimbases[xdimname]]);
	console.log(['labels',_labels]);
	//console.log(_labels.dimwidths[xdimname]);
	//console.log(Number(_labels.dimwidths[xdimname]+_labels.dimbases[xdimname]));
	//console.log(_labels.dimbases[ydimname]);
	//console.log(_labels.dimwidths[ydimname]);
	//console.log(Number(_labels.dimwidths[ydimname]+_labels.dimbases[ydimname]));
	self.zscale = this.createScale(_data,_types,self.labelsfrombase.z_label,this.w,this.px,this.inv[2]/*true*/,true).range(colorbrewer.GnBu[9]);
	if((self.labelsfrombase.z_label in this.min) && (self.labelsfrombase.z_label in this.max)) {
		if(this.inv[2]){
			self.zscale.domain([this.max[self.labelsfrombase.z_label],this.min[self.labelsfrombase.z_label]]);
		} else {
			self.zscale.domain([this.min[self.labelsfrombase.z_label],this.max[self.labelsfrombase.z_label]]);
		}
	}
	//var xscale = d3.scale.linear().domain([Number(_labels.dimbases[xdimname]),Number(_labels.dimwidths[xdimname])+Number(_labels.dimbases[xdimname])]).range([this.px,this.w-this.px]);
	//var yscale = d3.scale.linear().domain([Number(_labels.dimwidths[ydimname])+Number(_labels.dimbases[ydimname]),Number(_labels.dimbases[ydimname])]).range([this.py,this.h-this.py]);
	var xscale = this.createScale(_data,_types,self.labelsfrombase.x_label,this.w,this.px,this.inv[0]/*true*/,false)
	//console.log("true range: "+Number(_labels.dimbases[xdimname])+","+(Number(_labels.dimwidths[xdimname])+Number(_labels.dimbases[xdimname])));
	var yscale = this.createScale(_data,_types,self.labelsfrombase.y_label,this.h,this.py,this.inv[1]/*true*/,false)	
	//console.log(xscale.domain());
	//console.log(yscale.domain());
	//console.log(xscale.range());
	//console.log(yscale.range());

	$('#'+self.rootid).css('height',this.h);

	this.canvas = $('#canvas');
	this.canvas.attr('width', this.w)
		.attr('height', this.h)
		.attr('class', 'show');

	this.svg = d3.selectAll(this.jsvg.get())
		.attr('width', this.w)
		.attr('height', this.h)
		.attr('class', 'g')
		.attr('id', 'svg_'+this.rootid);
				
	this.rectcontainer = this.svg.append('g')
		.attr("class", "rectcontainer")
		.attr("width", this.w-this.px)					
		.attr("height",  this.h-this.py)
		.attr("x", 0)
		.attr("y", 0);

	this.addStatsHist1(_data, _labels,_types, opts);
	this.addStatsHist2(_data, _labels,_types, opts);

	//console.log("width:"+(self.w-2*self.px)/_labels.dimwidths[xdimname]);
	//console.log("height:"+(self.h-2*self.py)/_labels.dimwidths[ydimname]);
	//just testing the rects function
	//console.log(["xwidth",_labels.dimwidths[xdimname]]);
	this.drawRectsCanvas(this.canvas[0].getContext('2d'),_data,_types,xscale,yscale,/*'dims.'+*/xdimname,/*'dims.'+*/ydimname,function(d){return Math.max(1,(self.w-2*self.px)/(_labels.dimwidths[xdimname]-1));},
		function(d){return Math.max(1,(self.h-2*self.py)/(_labels.dimwidths[ydimname]-1));},
		function(d) {return self.zscale(d[self.labelsfrombase.z_label]);});

	//this.add_brush(xscale,yscale,/*'dims.'+*/xdimname,/*'dims.'+*/ydimname,function(d) {return self.zscale(d[self.labelsfrombase.z_label]);},this.rectcontainer);

	// this adds an svg rectangle we can find the mouse clicks from
	this.rectcontainer.append('rect')
		.attr("id","mouseclick_rect")
		.attr("x", this.px)
		.attr("y", this.py)
		.attr("width", this.w-2*this.px+Math.max(1,(self.w-2*self.px)/(_labels.dimwidths[xdimname]-1)))				
		.attr("height", this.h-2*this.py+Math.max(1,(self.h-2*self.py)/(_labels.dimwidths[ydimname]-1)))
		.style("color","black")
		.style("opacity",0.0);
}

//add a new render function
//data format: {'data':[{},{},...],'names':["","",...],'types':{"":,"":,...}}
QVis.HeatMap.prototype.render = function(_data, _labels,_types, opts) {
	this.selectx = true;
	this.selecty = true;
	this.selectz = true;
	this.dimsonly = true;
	
	this.draw_obj = "rect";

	var self = this;
	//call the original render function
	self.labelsfrombase = QVis.HeatMap.base.render.call(this,_data,_labels,_types,opts);

	// create x,y axis scales
	var xdimname = self.labelsfrombase.x_label;//.substring("dims.".length);//""+_labels.dimnames[0];
	var ydimname = self.labelsfrombase.y_label;//.substring("dims.".length);//""+_labels.dimnames[1];
	//console.log("dimnames: "+xdimname+','+ydimname);
	//console.log(['dimbases',_labels.dimbases[xdimname]]);
	console.log(['labels',_labels]);
	//console.log(_labels.dimwidths[xdimname]);
	//console.log(Number(_labels.dimwidths[xdimname]+_labels.dimbases[xdimname]));
	//console.log(_labels.dimbases[ydimname]);
	//console.log(_labels.dimwidths[ydimname]);
	//console.log(Number(_labels.dimwidths[ydimname]+_labels.dimbases[ydimname]));
	self.zscale = this.createScale(_data,_types,self.labelsfrombase.z_label,this.w,this.px,this.inv[2]/*true*/,true).range(colorbrewer.GnBu[9]);
	if((self.labelsfrombase.z_label in this.min) && (self.labelsfrombase.z_label in this.max)) {
		if(this.inv[2]){
			self.zscale.domain([this.max[self.labelsfrombase.z_label],this.min[self.labelsfrombase.z_label]]);
		} else {
			self.zscale.domain([this.min[self.labelsfrombase.z_label],this.max[self.labelsfrombase.z_label]]);
		}
	}
	//var xscale = d3.scale.linear().domain([Number(_labels.dimbases[xdimname]),Number(_labels.dimwidths[xdimname])+Number(_labels.dimbases[xdimname])]).range([this.px,this.w-this.px]);
	//var yscale = d3.scale.linear().domain([Number(_labels.dimwidths[ydimname])+Number(_labels.dimbases[ydimname]),Number(_labels.dimbases[ydimname])]).range([this.py,this.h-this.py]);
	var xscale = this.createScale(_data,_types,self.labelsfrombase.x_label,this.w,this.px,this.inv[0]/*true*/,false)
	//console.log("true range: "+Number(_labels.dimbases[xdimname])+","+(Number(_labels.dimwidths[xdimname])+Number(_labels.dimbases[xdimname])));
	var yscale = this.createScale(_data,_types,self.labelsfrombase.y_label,this.h,this.py,this.inv[1]/*true*/,false)	
	//console.log(xscale.domain());
	//console.log(yscale.domain());
	//console.log(xscale.range());
	//console.log(yscale.range());

	this.svg = d3.selectAll(this.jsvg.get())
		.attr('width', this.w)
		.attr('height', this.h)
		.attr('class', 'g')
		.attr('id', 'svg_'+this.rootid);
				
	this.rectcontainer = this.svg.append('g')
		.attr("class", "rectcontainer")
		.attr("width", this.w-this.px)					
		.attr("height",  this.h-this.py)
		.attr("x", 0)
		.attr("y", 0);

	//console.log("width:"+(self.w-2*self.px)/_labels.dimwidths[xdimname]);
	//console.log("height:"+(self.h-2*self.py)/_labels.dimwidths[ydimname]);
	//just testing the rects function
	//console.log(["xwidth",_labels.dimwidths[xdimname]]);
	this.drawRects(this.rectcontainer,_data,_types,xscale,yscale,/*'dims.'+*/xdimname,/*'dims.'+*/ydimname,function(d){return Math.max(1,(self.w-2*self.px)/(_labels.dimwidths[xdimname]-1));},
		function(d){return Math.max(1,(self.h-2*self.py)/(_labels.dimwidths[ydimname]-1));},
		function(d) {return self.zscale(d[self.labelsfrombase.z_label]);});

	//this.add_brush(xscale,yscale,/*'dims.'+*/xdimname,/*'dims.'+*/ydimname,function(d) {return self.zscale(d[self.labelsfrombase.z_label]);},this.rectcontainer);

	// this adds a rectangle we can find the mouse clicks from
	this.rectcontainer.append('rect')
		.attr("id","mouseclick_rect")
		.attr("x", this.px)
		.attr("y", this.py)
		.attr("width", this.w-2*this.px+Math.max(1,(self.w-2*self.px)/(_labels.dimwidths[xdimname]-1)))				
		.attr("height", this.h-2*this.py+Math.max(1,(self.h-2*self.py)/(_labels.dimwidths[ydimname]-1)))
		.style("color","black")
		.style("opacity",0.0);
}

QVis.HeatMap.prototype.mini_render_canvas = function(_data, _labels,_types, opts) {
	var self = this;
	this.draw_obj = "rect";
	
	//call the original mini_render function
	QVis.HeatMap.base.mini_render.call(this,_data,_labels,_types,opts);
	if(this.canvas){
		this.canvas.width = this.canvas.width; // clear the canvas
	}

	//console.log("got here");
	// create x,y axis scales
	var xdimname = self.labelsfrombase.x_label;//.substring("dims.".length);//""+_labels.dimnames[0];
	var ydimname = self.labelsfrombase.y_label;//.substring("dims.".length);//""+_labels.dimnames[1];
	//console.log("dimnames: "+xdimname+','+ydimname);
	//console.log(_labels.dimbases[xdimname]);
	//console.log(_labels.dimwidths[xdimname]);
	//console.log(Number(_labels.dimwidths[xdimname]+_labels.dimbases[xdimname]));
	//console.log(_labels.dimbases[ydimname]);
	//console.log(_labels.dimwidths[ydimname]);
	//console.log(Number(_labels.dimwidths[ydimname]+_labels.dimbases[ydimname]));
	self.zscale = this.createScale(_data,_types,self.labelsfrombase.z_label,this.w,this.px,this.inv[2]/*true*/,true).range(colorbrewer.GnBu[9]);
	if((self.labelsfrombase.z_label in this.min) && (self.labelsfrombase.z_label in this.max)) {
		if(this.inv[2]){
			self.zscale.domain([this.max[self.labelsfrombase.z_label],this.min[self.labelsfrombase.z_label]]);
		} else {
			self.zscale.domain([this.min[self.labelsfrombase.z_label],this.max[self.labelsfrombase.z_label]]);
		}
	}
	//var xscale = d3.scale.linear().domain([Number(_labels.dimbases[xdimname]),Number(_labels.dimwidths[xdimname])+Number(_labels.dimbases[xdimname])]).range([this.px,this.w-this.px]);
	//var yscale = d3.scale.linear().domain([Number(_labels.dimwidths[ydimname])+Number(_labels.dimbases[ydimname]),Number(_labels.dimbases[ydimname])]).range([this.py,this.h-this.py]);
	var xscale = this.createScale(_data,_types,self.labelsfrombase.x_label,this.w,this.px,this.inv[0]/*true*/,false)
	//console.log("true range: "+Number(_labels.dimbases[xdimname])+","+(Number(_labels.dimwidths[xdimname])+Number(_labels.dimbases[xdimname])));
	var yscale = this.createScale(_data,_types,self.labelsfrombase.y_label,this.h,this.py,this.inv[1]/*true*/,false)	
	//console.log(xscale.domain());
	//console.log(yscale.domain());
	//console.log(xscale.range());
	//console.log(yscale.range());

	$('#'+self.rootid).css('height',this.h);

	this.canvas = $('#canvas');
	this.canvas.attr('width', this.w)
		.attr('height', this.h)
		.attr('class', 'show');

	this.svg = d3.selectAll(this.jsvg.get())
		.attr('width', this.w)
		.attr('height', this.h)
		.attr('class', 'g')
		.attr('id', 'svg_'+this.rootid);
				
	this.rectcontainer = this.svg.append('g')
		.attr("class", "rectcontainer")
		.attr("width", this.w-this.px)					
		.attr("height",  this.h-this.py)
		.attr("x", 0)
		.attr("y", 0);


	this.addStatsHist1(_data, _labels,_types, opts);
	this.addStatsHist2(_data, _labels,_types, opts);

	//console.log("width:"+(self.w-2*self.px)/_labels.dimwidths[xdimname]);
	//console.log("height:"+(self.h-2*self.py)/_labels.dimwidths[ydimname]);
	//just testing the rects function
	//console.log(["xwidth",_labels.dimwidths[xdimname]]);
	this.drawRectsCanvas(this.canvas[0].getContext('2d'),_data,_types,xscale,yscale,/*'dims.'+*/xdimname,/*'dims.'+*/ydimname,function(d){return Math.max(1,(self.w-2*self.px)/(_labels.dimwidths[xdimname]-1));},
		function(d){return Math.max(1,(self.h-2*self.py)/(_labels.dimwidths[ydimname]-1));},
		function(d) {return self.zscale(d[self.labelsfrombase.z_label]);});

	//this.add_brush(xscale,yscale,/*'dims.'+*/xdimname,/*'dims.'+*/ydimname,function(d) {return self.zscale(d[self.labelsfrombase.z_label]);},this.rectcontainer);

	// this adds a rectangle we can find the mouse clicks from
	this.rectcontainer.append('rect')
		.attr("id","mouseclick_rect")
		.attr("x", this.px)
		.attr("y", this.py)
		.attr("width", this.w-2*this.px+Math.max(1,(self.w-2*self.px)/(_labels.dimwidths[xdimname]-1)))				
		.attr("height", this.h-2*this.py+Math.max(1,(self.h-2*self.py)/(_labels.dimwidths[ydimname]-1)))
		.style("color","black")
		.style("opacity",0.0);
}

QVis.HeatMap.prototype.mini_render = function(_data, _labels,_types, opts) {
	var self = this;
	this.draw_obj = "rect";
	
	//call the original mini_render function
	QVis.HeatMap.base.mini_render.call(this,_data,_labels,_types,opts);
	//console.log("got here");
	// create x,y axis scales
	var xdimname = self.labelsfrombase.x_label;//.substring("dims.".length);//""+_labels.dimnames[0];
	var ydimname = self.labelsfrombase.y_label;//.substring("dims.".length);//""+_labels.dimnames[1];
	//console.log("dimnames: "+xdimname+','+ydimname);
	//console.log(_labels.dimbases[xdimname]);
	//console.log(_labels.dimwidths[xdimname]);
	//console.log(Number(_labels.dimwidths[xdimname]+_labels.dimbases[xdimname]));
	//console.log(_labels.dimbases[ydimname]);
	//console.log(_labels.dimwidths[ydimname]);
	//console.log(Number(_labels.dimwidths[ydimname]+_labels.dimbases[ydimname]));
	self.zscale = this.createScale(_data,_types,self.labelsfrombase.z_label,this.w,this.px,this.inv[2]/*true*/,true).range(colorbrewer.GnBu[9]);
	if((self.labelsfrombase.z_label in this.min) && (self.labelsfrombase.z_label in this.max)) {
		if(this.inv[2]){
			self.zscale.domain([this.max[self.labelsfrombase.z_label],this.min[self.labelsfrombase.z_label]]);
		} else {
			self.zscale.domain([this.min[self.labelsfrombase.z_label],this.max[self.labelsfrombase.z_label]]);
		}
	}
	//var xscale = d3.scale.linear().domain([Number(_labels.dimbases[xdimname]),Number(_labels.dimwidths[xdimname])+Number(_labels.dimbases[xdimname])]).range([this.px,this.w-this.px]);
	//var yscale = d3.scale.linear().domain([Number(_labels.dimwidths[ydimname])+Number(_labels.dimbases[ydimname]),Number(_labels.dimbases[ydimname])]).range([this.py,this.h-this.py]);
	var xscale = this.createScale(_data,_types,self.labelsfrombase.x_label,this.w,this.px,this.inv[0]/*true*/,false)
	//console.log("true range: "+Number(_labels.dimbases[xdimname])+","+(Number(_labels.dimwidths[xdimname])+Number(_labels.dimbases[xdimname])));
	var yscale = this.createScale(_data,_types,self.labelsfrombase.y_label,this.h,this.py,this.inv[1]/*true*/,false)	
	//console.log(xscale.domain());
	//console.log(yscale.domain());
	//console.log(xscale.range());
	//console.log(yscale.range());

	this.svg = d3.selectAll(this.jsvg.get())
		.attr('width', this.w)
		.attr('height', this.h)
		.attr('class', 'g')
		.attr('id', 'svg_'+this.rootid);
				
	this.rectcontainer = this.svg.append('g')
		.attr("class", "rectcontainer")
		.attr("width", this.w-this.px)					
		.attr("height",  this.h-this.py)
		.attr("x", 0)
		.attr("y", 0);

	//console.log("width:"+(self.w-2*self.px)/_labels.dimwidths[xdimname]);
	//console.log("height:"+(self.h-2*self.py)/_labels.dimwidths[ydimname]);
	//just testing the rects function
	//console.log(["xwidth",_labels.dimwidths[xdimname]]);
	this.drawRects(this.rectcontainer,_data,_types,xscale,yscale,/*'dims.'+*/xdimname,/*'dims.'+*/ydimname,function(d){return Math.max(1,(self.w-2*self.px)/(_labels.dimwidths[xdimname]-1));},
		function(d){return Math.max(1,(self.h-2*self.py)/(_labels.dimwidths[ydimname]-1));},
		function(d) {return self.zscale(d[self.labelsfrombase.z_label]);});

	//this.add_brush(xscale,yscale,/*'dims.'+*/xdimname,/*'dims.'+*/ydimname,function(d) {return self.zscale(d[self.labelsfrombase.z_label]);},this.rectcontainer);

	// this adds a rectangle we can find the mouse clicks from
	this.rectcontainer.append('rect')
		.attr("id","mouseclick_rect")
		.attr("x", this.px)
		.attr("y", this.py)
		.attr("width", this.w-2*this.px+Math.max(1,(self.w-2*self.px)/(_labels.dimwidths[xdimname]-1)))				
		.attr("height", this.h-2*this.py+Math.max(1,(self.h-2*self.py)/(_labels.dimwidths[ydimname]-1)))
		.style("color","black")
		.style("opacity",0.0);
}
