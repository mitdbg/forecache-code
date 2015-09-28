/*
histogram along x axis of original graph (above original graph
*/
QVis.Graph.prototype.addStatsHist1 = function(_data, _labels,_types, opts) {
	this.hist1_canvas = $("canvas#hist1");
	xlen = this.w;
	ylen = 200;
	x_label = this.labelsfrombase.x_label;
	y_label = this.labelsfrombase.y_label;
	xwidth = _labels.dimwidths[x_label];
	ywidth = _labels.dimwidths[y_label];
	xpadding = this.px;
	ypadding = 0;

	canvas = this.hist1_canvas;
	if(canvas) {
		canvas.width = canvas.width;
	}

	canvas.attr('width', xlen)
		.attr('height', ylen)
		.attr('class', 'show'); // get canvas ready

	xscale = d3.scale.linear().domain([0,xwidth]).range([xpadding,xlen-xpadding]);
	yscale = d3.scale.linear().domain([100,0]).range([ypadding,ylen-ypadding]);

	stdevs = new Array();
	console.log(["xwidth",xwidth]);
	for(var i= 0; i < xwidth; i++) {
		stdevs[i] = Math.floor(Math.random() * (100 - 0 + 1)) + 0;
	}

	ctx = canvas[0].getContext('2d');
	var start = Math.round((new Date().getTime())/1000);
	for(var drawindex = 0; drawindex < stdevs.length; drawindex++) {
		ctx.rect(xscale(drawindex),
			ylen - ypadding - yscale(stdevs[drawindex]),
			(xlen-xpadding)/xwidth,
			yscale(stdevs[drawindex])
		);
		ctx.fillStyle = "blue";
		ctx.fill();
		ctx.strokeStyle = 'black';
		ctx.lineWidth = 2;
		ctx.stroke();
	}
	var end = Math.round((new Date().getTime())/1000);
	console.log(["draw histogram duration",end-start]);
}

QVis.Graph.prototype.addStatsHist2 = function(_data, _labels,_types, opts) {
	this.hist2_canvas = $("canvas#hist2");
	xlen = 200;
	ylen = this.h;
	x_label = this.labelsfrombase.x_label;
	y_label = this.labelsfrombase.y_label;
	xwidth = _labels.dimwidths[x_label];
	ywidth = _labels.dimwidths[y_label];
	xpadding = 0;
	ypadding = this.py;

	canvas = this.hist2_canvas;
	if(canvas) {
		canvas.width = canvas.width;
	}

	canvas.attr('width', xlen)
		.attr('height', ylen)
		.attr('class', 'show'); // get canvas ready

	xscale = d3.scale.linear().domain([0,100]).range([xpadding,xlen-xpadding]);
	yscale = d3.scale.linear().domain([0,ywidth]).range([ypadding,ylen-ypadding]);

	stdevs = new Array();
	for(var i= 0; i < ywidth; i++) {
		stdevs[i] = Math.floor(Math.random() * (100 - 5 + 1)) + 5;
	}

	ctx = canvas[0].getContext('2d');
	var start = Math.round((new Date().getTime())/1000);
	for(var drawindex = 0; drawindex < stdevs.length; drawindex++) {
		ctx.rect(0,
			yscale(drawindex),
			xscale(stdevs[drawindex]),
			(ylen-ypadding)/ywidth
		);
		ctx.fillStyle = "blue";
		ctx.fill();
		ctx.strokeStyle = 'black';
		ctx.lineWidth = 2;
		ctx.stroke();
	}
	var end = Math.round((new Date().getTime())/1000);
	console.log(["draw histogram duration",end-start]);
}

QVis.Graph.prototype.removeStatsHists = function() {
	if (this.hist1_canvas) {
		this.hist1_canvas.removeClass('show');
	}
	if (this.hist2_canvas) {
		this.hist2_canvas.removeClass('show');
	}	
}
