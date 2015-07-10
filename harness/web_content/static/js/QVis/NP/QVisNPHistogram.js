QVis.Histogram = function(rootid,opts) {
	QVis.Graph.call(this,rootid,opts);

	//unique to scatterplots
	this.circlecontainer = null;
	this.r = opts.r || 1.5;
}

//inherit Graph Object
QVis.Historam.prototype = new QVis.Graph();

//fix constructor reference
QVis.Histogram.constructor = QVis.Histogram;

//to get access to the original functions
QVis.Histogram.base = QVis.Graph.prototype;

//add a new render function
//data format: {'data':[{},{},...],'names':["","",...],'types':{"":,"":,...}}
QVis.Historam.prototype.render = function(_data, _labels,_types, opts) {
	// user wants us to draw something, so we know we have data now
	//assumption: data is always presented with labels
	if (!_labels || typeof(_labels) == 'undefined') {
		QVis.error("Did not get any data to render!")
		return;
	}

	this.update_opts(opts); // if new options are passed, update the options

	//clear everything to get them ready for drawing
	this.jsvg = $("#"+this.rootid + " svg"),
	this.jlegend = $("#"+this.rootid+" .legend");
	this.xlabeldiv = $("#"+this.rootid+" .xlabel");	
	this.ylabeldiv = $("#"+this.rootid+" .ylabel");		
	this.jsvg.empty(); this.jlegend.empty(); this.xlabeldiv.empty(); this.ylabeldiv.empty();

	// you should know why this is necessary
	var self = this;

	//console.log("this.rootid: " + this.rootid+", self.rootid: "+self.rootid);
	//console.log("this == self?" + this === self);

	// _labels.aggs contains the columns that will be plotted on the y-axis
	// I iterate through each column and consolidate the points that would be rendered
	// This means that there could be overlapping points from two different columns
	var labels = _labels.aggs, 
		x_label = _labels.x,
		y_label = _labels.y,
		cscale = d3.scale.category10().domain(labels);  // color scale

	// create x,y axis scales
	var xscale,yscale;
	// THIS IS ONLY IN THE CONTEXT OF SCATTERPLOTS!!! MANY ASSUMPTIONS MADE ABOUT THE DATA HERE
	if(_types[x_label] === 'int32' || _types[x_label] === 'int64' || _types[x_label] === 'double') {
		minx = d3.min(_data.map(function(d){return d[x_label];}));
		maxx = d3.max(_data.map(function(d){return d[x_label];}));
		console.log("ranges: "+minx+","+maxx);
		xscale = d3.scale.linear().domain([this.get_data_obj(minx,_types[x_label]), this.get_data_obj(maxx,_types[x_label])]).range([this.px,this.w-this.px]);
	} else if (_types[x_label] === "datetime") {
		minx = d3.min(_data.map(function(d){return d[x_label];}));
		maxx = d3.max(_data.map(function(d){return d[x_label];}));
		console.log("ranges: "+minx+","+maxx);
		console.log("ranges: "+this.get_data_obj(minx)+","+this.get_data_obj(maxx));
		xscale = d3.time.scale().domain([this.get_data_obj(minx,_types[x_label]), this.get_data_obj(maxx,_types[x_label])]).range([this.px,this.w-this.px]);
	} else if (_types[x_label] === 'string') {
		self.strings = []
		_data.map(function(d){self.strings.push(d[x_label]);});
		self.strings = this.remove_dupes(self.strings);
		xscale = d3.scale.ordinal().domain(self.strings);
		var steps = (this.w-2*this.px)/(self.strings.length-1);
		var range = [];
		for(var i = 0; i < self.strings.length;i++){range.push(this.px+steps*i);}
		xscale.range(range);
	} else {
		console.log("unrecognized type: "+_types[x_label]);
		console.log("labels: "+x_label+","+y_label);
	}

	if(_types[y_label] === 'int32' || _types[y_label] === 'int64' || _types[y_label] === 'double') {
		miny = d3.min(_data.map(function(d){return d[y_label];}));
		maxy = d3.max(_data.map(function(d){return d[y_label];}));
		console.log("ranges: "+miny+","+maxy);
		yscale = d3.scale.linear().domain([miny,maxy]).range([this.h-this.py, this.py]);
	}
	// add the legend and color it appropriately
	var legend = d3.selectAll(this.jlegend.get()).selectAll('text')
			.data(labels)
		.enter().append('div')
			.style('float', 'left')
			.style('color', cscale)
			.text(String);		
	
	//
	// render x-axis select options
	var xaxisselect = this.xlabeldiv.append($("<select></select>")).find("select");
	var xaxislabel = d3.selectAll(xaxisselect.get()).selectAll("option")
			.data(_labels.gbs)
		.enter().append("option")
			.attr("value", String)
			.text(String);
	xaxisselect.val(x_label);
	console.log(_labels.gbs);
	//
	// render y-axis select options
	var yaxisselect = this.ylabeldiv.append($("<select></select>")).find("select");
	var yaxisattrselect = yaxisselect.append($('<optgroup label="attrs"></optgroup>')).find("optgroup");
	var yaxislabel = d3.selectAll(yaxisattrselect.get()).selectAll("option")
			.data(_labels.gbs)
		.enter().append("option")
			.attr("value", String)
			.text(String);
	yaxisselect.val(y_label);
	//
	// I create and execute this anonymous function so
	// selectedval will be private to and accessible by the .change() callback function
	// Manually set the new labels and call render_scatterplot again
	// 
	// notice that I use "self" instead of "this".
	//
	(function() {
		var selectedval = x_label;
		$("#"+self.rootid+" .xlabel select").change(function() {
			var val = $("#"+self.rootid+" .xlabel select").val();
			var yval = $("#"+self.rootid+" .ylabel select").val(); // should be the same as before
			console.log(["selected option", selectedval, val])				
			if (val == selectedval) return;
			selectedval = val;
			var newlabels = {"x" : val,"y": yval, "gbs" : _labels.gbs, "aggs" : _labels.aggs};

			self.render(_data, newlabels,_types, opts);
		});
	})();

	(function() {
		var selectedval = y_label;
		$("#"+self.rootid+" .ylabel select").change(function() {
			var val = $("#"+self.rootid+" .ylabel select").val();
			var xval = $("#"+self.rootid+" .xlabel select").val(); // should be the same as before
			console.log(["selected option", selectedval, val])				
			if (val == selectedval) return;
			selectedval = val;
			var newlabels = {"y" : val,"x": xval, "gbs" : _labels.gbs, "aggs" : _labels.aggs};

			self.render(_data, newlabels,_types, opts);
		});
	})();

	this.svg = d3.selectAll(this.jsvg.get())
		.attr('width', this.w)
		.attr('height', this.h)
		.attr('class', 'g')
		.attr('id', 'svg_'+this.rootid);
	this.add_axes(xscale, yscale,x_label,y_label, self.strings,_types);
}
