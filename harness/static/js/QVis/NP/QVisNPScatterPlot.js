QVis.ScatterPlot = function(rootid,opts) {
	QVis.Graph.call(this,rootid,opts);

	//unique to scatterplots
	this.circlecontainer = null;
	this.r = opts.r || 1.5;
}

//inherit Graph Object
QVis.ScatterPlot.prototype = new QVis.Graph();

//fix constructor reference
QVis.ScatterPlot.constructor = QVis.ScatterPlot;

//to get access to the original functions
QVis.ScatterPlot.base = QVis.Graph.prototype;

//add a new render function
//data format: {'data':[{},{},...],'names':["","",...],'types':{"":,"":,...}}
QVis.ScatterPlot.prototype.render = function(_data, _labels,_types, opts) {
	this.selectx = true;
	this.selecty = true;
	this.selectz = true;

	this.draw_obj = "circle";

	var self = this;

	//call the original render function
	self.labelsfrombase = QVis.ScatterPlot.base.render.call(this,_data,_labels,_types,opts);
	console.log("z_label "+self.labelsfrombase.z_label);

	// create x,y axis scales
	var xscale = this.createScale(_data,_types,self.labelsfrombase.x_label,this.w,this.px,this.inv[0],false);
	var yscale = this.createScale(_data,_types,self.labelsfrombase.y_label,this.h,this.py,this.inv[1],false);
	var zscale = this.createScale(_data,_types,self.labelsfrombase.z_label,0,0,this.inv[2],true)
			.range(colorbrewer.Oranges[9]);
	if((self.labelsfrombase.z_label in this.min) && (self.labelsfrombase.z_label in this.max)) {
		if(this.inv[2]){
			zscale.domain([this.max[self.labelsfrombase.z_label],this.min[self.labelsfrombase.z_label]]);
		} else {
			zscale.domain([this.min[self.labelsfrombase.z_label],this.max[self.labelsfrombase.z_label]]);
		}
	}
	console.log("xscale domain: "+xscale.domain());
	console.log("zscale range: "+zscale.range());

	this.svg = d3.selectAll(this.jsvg.get())
		.attr('width', this.w)
		.attr('height', this.h)
		.attr('class', 'g')
		.attr('id', 'svg_'+this.rootid);
	this.add_axes(xscale, yscale,self.labelsfrombase.x_label,self.labelsfrombase.y_label, self.strings,_types);
				
	this.circlecontainer = this.svg.append('g')
		.attr("class", "circlecontainer")
		.attr("width", this.w-this.px)					
		.attr("height",  this.h-this.py)
		.attr("x", 0)
		.attr("y", 0);

	this.drawCircles(this.circlecontainer,_data,_types,xscale,yscale,self.labelsfrombase.x_label,self.labelsfrombase.y_label,this.defaultRadius,function(d) {return zscale(d[self.labelsfrombase.z_label]);});
	//just testing the rects function
	//this.drawRects(this.circlecontainer,_data,_types,xscale,yscale,self.labelsfrombase.x_label,self.labelsfrombase.y_label,this.defaultRadius,this.defaultRadius,this.defaultColor);

	this.add_brush(xscale,yscale,self.labelsfrombase.x_label,self.labelsfrombase.y_label,function(d) {return zscale(d[self.labelsfrombase.z_label]);},this.circlecontainer);

}
