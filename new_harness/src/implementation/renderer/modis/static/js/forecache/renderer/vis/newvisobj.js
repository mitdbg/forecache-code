// assumes that the ForeCache.Backend object is available
// assumes that the ForeCache.Tracker object is available
var ForeCache = ForeCache || {};
ForeCache.Backend = ForeCache.Backend || {};
ForeCache.Tracker = ForeCache.Tracker || {};
ForeCache.Renderer = ForeCache.Renderer || {};
ForeCache.Renderer.Vis = ForeCache.Renderer.Vis || {};

/************* Classes *************/

// VisObj is a super class for visualizations. All visualization types should be able to
// inherit from this class, and only fill in a small number of functions.
// THIS CLASS DOES NOT RENDER ANYTHING!!!! Note also
// that the chart parameter is a jquery object
ForeCache.Renderer.Vis.VisObj = function(chart, options) {
	var self = this;

  this.name = "newvisobj";
  this.doneRendering = {};
  this.previousRenderState = {};
  this.statesRenderObj = new StatesRenderer.renderObj();
  // bookkeeping data structures
  this.tileManager = null; // this object manages all tiles for the page
  this.dimensionality = -1; // how many dimensions the visObj uses for navigation
  this.useLegend = true; // whether or not to render the legend
  this.useUsMap = true; // whether or not to render the US state boundaries
  this.removeDuplicates = false // consolidate and remove duplicate points

	//default values
  this.viewportRatio = -1; // set later by tile manager
	this.xlabel = options.xlabel
	this.ylabel = options.ylabel
  this.inverted = options.inverted || {"x":false,"y":false,"color":false};
  this.scaleType = options.scaleType || {};
	this.fixYDomain = false;
	this.mousebusy = false;
  // list of color values
  this.colorRange = options.colorRange || colorbrewer.YlOrRd[9];

	this.options = options || {};
	this.chart = (chart.toArray())[0]; // get dom element from jquery
};

// adjust the width/height of the visible axes to be some fraction of the true range
ForeCache.Renderer.Vis.VisObj.prototype.adjustForViewportRatio = function(domain) {
  var diff = domain[1] - domain[0];
  var diff2 = diff * this.viewportRatio;
  var pad = (diff - diff2) / 2;
  var domain2 = [domain[0]+pad, domain[1]-pad];
  //console.log(["domain2",domain2]);
  return domain2;
};

// used to change/override the "this.color" function
ForeCache.Renderer.Vis.VisObj.prototype.modifyColor = function() {
/*
should be filled in by subclasses
*/
};

// this part of the setup process requires information about the starting data
ForeCache.Renderer.Vis.VisObj.prototype.finishSetup = function() {
  this.updateOpts();

	this.cx = this.options.width;
	this.cy = this.options.height;
	this.blankStyle = "#fff";
	this.background = "#EBEBE0";
	this.fillStyle = "#993366";

	this.padding = this.options.padding;
	this.size = this.options.size;

	$(this.chart).css("position","relative");
	$(this.chart).css("width",this.cx);
	$(this.chart).css("height",this.cy);


	//var halfWidth = Math.ceil((xdomain[1]-xdomain[0])/2);

	// x-scale
	this.x = d3.scale.linear()
		.domain(this.options.xdomain)
		.range([0, this.size.width]);

	// y-scale
	this.y = d3.scale.linear()
		.domain(this.options.ydomain)
		.nice()
		.range([0,this.size.height])
		.nice();

/*
  if(this.scaleType.hasOwnProperty("y")) {
    this.oldy = this.y;
    if(this.scaleType.y === "log") {
      if(this.options.ydomain[0] == 0) { //zeros are bad for logs
        this.options.ydomain[0] = 1;
        this.options.ydomain[1] += 1;
      }
      this.y = d3.scale.log().base(10)
        .domain(this.options.ydomain)
        .nice()
        .range([0,this.size.height])
        .nice();
      console.log(["new ydomain",this.y.domain()]);
    }
  }
*/

  var zdom = null;
  if(this.options.hasOwnProperty("zdomain")) {
    // color scale
    // assumes numeric domain
    // TODO: remove hardcoded color setup here
    zdom = this.options.zdomain;
    this.colorScale = d3.scale.quantize()
      .domain(zdom)
      .range(this.colorRange);
      //.range(colorbrewer.Spectral[9]);
  }

  if(this.inverted.x) {
    this.x.range([this.size.width,0]);
  }
  if(this.inverted.y) {
    this.y.range([this.size.height,0]);
  }
  // TODO: remove hardcoded color setup here
  if(this.inverted.hasOwnProperty("color") && this.inverted.color) {
    this.colorScale.domain(this.colorScale.domain().reverse());
  }

  // use this function to do any fancy color stuff in other classes
  this.modifyColor();

	this.xAxis = d3.svg.axis().scale(this.x).orient("bottom");
	this.yAxis = d3.svg.axis().scale(this.y).orient("left");

  this.svg = d3.select(this.chart).append("svg")
		.attr("width",	this.cx)
		.attr("height", this.cy)
		.attr("class","forecache-vis");

	this.vis = this.svg.append("g")
			.attr("transform", "translate(" + this.padding.left + "," + this.padding.top + ")");
	
	this.plot = this.vis.append("rect")
			.attr("width", this.size.width)
			.attr("height", this.size.height)
			.style("fill", "#FFFFFF")
			.style("opacity",0)
		  .attr("class","pan-rect");

  this.busyRect = this.vis.append("rect")
			.attr("width", this.size.width)
			.attr("height", this.size.height)
			.style("fill", "#00000")
			.style("opacity",.4)
		  .attr("class","busy-rect hide");

	//this.plot.call(d3.behavior.zoom().scaleExtent([1,1]).x(this.x).y(this.y).on("zoom", this.redraw()));
	this.plot.call(d3.behavior.zoom().scaleExtent([1,1]).x(this.x).y(this.y)
      .on("zoomstart",this.recordDragStart())
      .on("zoom", this.redraw())
			.on("zoomend",this.afterZoom()));

	//this.plot.call(d3.behavior.zoom().x(this.x).y(this.y).on("zoom", this.redraw()));

  if(this.useLegend) {
	  this.legend = this.svg.append("g")
      .attr("transform", "translate(" + (this.padding.left+this.size.width) + "," + this.padding.top + ")")
      .attr("class", "forecache-legend");
  }


	// add Chart Title
	if (this.options.title) {
		this.vis.append("text")
				.attr("class", "forecache-axis")
				.text(this.options.title)
				.attr("x", this.size.width/2)
				.attr("dy","-0.8em")
				.style("text-anchor","middle");
	}

	//Add the x-axis label
	this.vis.append("text")
		.attr("class", "x forecache-axis")
		.text(this.xlabel)
		.attr("x", this.size.width/2)
		.attr("y", this.size.height+this.padding.top)
		.attr("dy","2.4em")
		.style("text-anchor","middle");

// add y-axis label
	if (this.options.ylabel) {
		this.vis.append("g").append("text")
			.attr("class", "y forecache-axis")
			.text(this.options.ylabel)
			.style("text-anchor","middle")
			.attr("transform","translate(" + (-this.padding.left+12) + " " + this.size.height/2+") rotate(-90)");
	}

  if(this.useLegend) {
    this.drawLegend(this.options.zlabel,this.padding.right,150,zdom,this.colorScale);
  }

	this.base = d3.select(this.chart);
	this.canvas = this.base.append("canvas")
		.attr("width",this.cx)
		.attr("height",this.cy);
	this.ctx = this.canvas.node().getContext("2d");
	this.buttonsDiv = $(
	"<div class='buttons'>"+
  	"<button data-zoom='1'>Zoom In</button>"+
  	"<button data-zoom='-1'>Zoom Out</button>"+
	"</div>").appendTo(this.chart);

	this.buttonsDiv.css("position","absolute");
	this.buttonsDiv.css("right",Number(this.padding.right)+5);
	this.buttonsDiv.css("top",Number(this.padding.top)+5);
	d3.selectAll(this.buttonsDiv.children().toArray()) // get the actual buttons
    .on("click", this.zoomClick());

 	// used to update the y domain, so the visualization looks better
	this.fixYDomain = true;
	this.redraw()();
};

ForeCache.Renderer.Vis.VisObj.prototype.updateOpts = function() {
  var newopts = this.options;
  this.xindex = this.tileManager.getIndex(this.options.xname);
  this.yindex = this.tileManager.getIndex(this.options.yname);
  //console.log(["indexes","x",this.xindex,"y",this.yindex]);
  // compute color domain
  if(this.options.hasOwnProperty("zname")) {
    this.zindex = this.tileManager.getIndex(this.options.zname);
    newopts.zdomain = this.tileManager.getDomain(this.zindex);
    //console.log(["zdom",newopts.zdomain]);
    //console.log(["indexes","x",this.xindex,"y",this.yindex,"z",this.zindex]);
  }

  // compute domains according to tile range, not according to the data in the tile (tile could be
  // sparse)
  newopts.xdomain = this.tileManager.getTileDomain(this.xindex);
  newopts.xdomain = this.adjustForViewportRatio(newopts.xdomain);
  if(this.dimensionality == 1) { // is this a 1D navigable visualization?
    newopts.ydomain = this.tileManager.getDomain(this.yindex); // don't treat y like a dimension
  } else if(this.dimensionality == 2) { // is this a 2D navigable visualization?
    newopts.ydomain = this.tileManager.getTileDomain(this.yindex);
    newopts.ydomain = this.adjustForViewportRatio(newopts.ydomain);
  }
	newopts.size = {
		"width":	newopts.width - newopts.padding.left - newopts.padding.right,
		"height": newopts.height - newopts.padding.top	- newopts.padding.bottom
	};

  //console.log(["width",newopts.size.width,"height",newopts.size.height]);
};

// don't let users try to interact with the interface when busy
ForeCache.Renderer.Vis.VisObj.prototype.disableInteractions = function() {
  this.plot.attr("class","pan-rect hide");
  d3.selectAll(this.buttonsDiv.children().toArray()) // make buttons inactive
    .on("click", function(){console.log("button disabled");});
};

// make interactions possible again
ForeCache.Renderer.Vis.VisObj.prototype.enableInteractions = function() {
  this.plot.attr("class","pan-rect");
  d3.selectAll(this.buttonsDiv.children().toArray()) // make buttons active
    .on("click", this.zoomClick());
};

ForeCache.Renderer.Vis.VisObj.prototype.makeBusy = function() {
  if(!this.mousebusy) {
    this.disableInteractions();
    this.mousebusy = true;
    this.busyRect.attr("class","busy-rect");
    $('body').css("cursor", "wait");
  }
};


ForeCache.Renderer.Vis.VisObj.prototype.doneRenderingTile = function(tile) {
  this.doneRendering[tile.id.name] = true;
  if(this.tileManager.checkDoneRendering()) {
    this.tileManager.makeNotBusy();
  }
};

ForeCache.Renderer.Vis.VisObj.prototype.makeNotBusy = function() {
  if(this.mousebusy) {// && this.tileManager.checkDoneRendering()) {
    this.mousebusy = false;
    this.busyRect.attr("class","busy-rect hide");
    $("body").css("cursor", "default");
    this.enableInteractions();
  }
};

// only called when one of the zoom buttons is clicked
ForeCache.Renderer.Vis.VisObj.prototype.zoomClick = function() {
	var self = this;
	return function () {
    self.tileManager.makeBusy();
    self.changed = true;
		var zoomDiff = Number(this.getAttribute("data-zoom"));
    // 1D case
    var dimIndices = [self.xindex];
    var diffs = [zoomDiff];
    if(self.dimensionality== 2) { // 2D case
      dimIndices = [self.xindex,self.yindex];
      diffs = [zoomDiff,zoomDiff];
    }
		self.tileManager.zoomClick(self.viewName,dimIndices,diffs);
	};
}

ForeCache.Renderer.Vis.VisObj.prototype.renderTile = function(tile) {
/*
should be filled in by subclasses
*/
};

ForeCache.Renderer.Vis.VisObj.prototype.canvasUpdate = function() {
	var self = this;
  var start = Date.now(); // in seconds

	this.ctx.beginPath();
	this.ctx.fillStyle = this.background;
	this.ctx.rect(0,0,this.cx,this.cy);
	this.ctx.fill();
	this.ctx.closePath();

  if(this.removeDuplicates) {
    var stuff = this.tileManager.getSpan(this);
    var rows = stuff[0];
    var xts = stuff[1];
    var yts = stuff[2];
    this.renderRowsHelper(rows,xts,yts);
  } else {
  var totalTiles = this.tileManager.totalTiles();
  for(var i = 0; i < totalTiles; i++) {
    var s = Date.now();
    var tile = this.tileManager.getTile(i);
    this.renderTile(tile);
    var e = Date.now();
    //console.log(["time to render tile",e-s]); // console statements take ~3ms of time
    ForeCache.globalTracker.appendToLog(ForeCache.Tracker.perTileLogName,{'action':'renderTile','tileId':tile.id.name,'start':s,'end':e,'viewName':this.name});
  };
  }
  if(this.useUsMap) {
    this.statesRenderObj.renderUsa(this.tileManager.getAggregationWindow(this.xindex),this.tileManager.getAggregationWindow(this.yindex),this.ctx,this.x,this.y,this.padding);
  }
   var end = Date.now(); // in seconds
  //console.log(["time to render all tiles",end - start]);
  //ForeCache.globalTracker.appendToLog(ForeCache.Tracker.perInteractionLogName,
  //  {'action':'render','totalTiles':totalTiles,'start':start,'end':end,'viewName':this.name});
 	
	this.ctx.beginPath();
	this.ctx.fillStyle=this.blankStyle;
	this.ctx.rect(0,0,this.cx,this.padding.top);//1
	this.ctx.fill();
	this.ctx.closePath();

	this.ctx.beginPath();
	this.ctx.fillStyle=this.blankStyle;
	this.ctx.rect(this.cx-this.padding.right,0,this.cx,this.cy);//2
	this.ctx.fill();
	this.ctx.closePath();

	this.ctx.beginPath();
	this.ctx.fillStyle=this.blankStyle;
	this.ctx.rect(0,this.cy-this.padding.bottom,this.cx,this.cy); //3
	this.ctx.fill();
	this.ctx.closePath();

	this.ctx.beginPath();
	this.ctx.fillStyle=this.blankStyle;
	this.ctx.rect(0,0,this.padding.left,this.cy); //4
	this.ctx.fill();
	this.ctx.closePath();
  //this.tileManager.makeNotBusy();
}

/*
only called on a pan. no longer called on zooms. this is taken care of 
by the tile manager.
*/
ForeCache.Renderer.Vis.VisObj.prototype.afterZoom = function() {
	var self = this;
	return function() {
    self.changed = true;
    self.tileManager.makeBusy();
		self.tileManager.afterZoom({"interactionType":"pan","state":{"dragStart":self.dragStart,"dragEnd":d3.mouse(this)},"viewName":self.viewName});
	};
}

/*
Draws the legend for the graph. Needs the container to draw the legend in,
desired dimensions of the legend, and the color scale.
*/
ForeCache.Renderer.Vis.VisObj.prototype.drawLegend = function(title,l_w,l_h,colorDomain,colorFunc) {
    var offsets={"x":5,"y":14};
    var numcolors = 9; // number of colors in color scale
    var color_domain = [Math.max.apply(null,colorDomain),Math.min.apply(null,colorDomain)];
    var scale =
d3.scale.linear().domain([color_domain[0],color_domain[1]]).range([offsets.y,l_h]);
    var ticks = [color_domain[0]];
    var step = 1.0 *(color_domain[1] - color_domain[0]) / numcolors; // divide domain by number of colors in the scale
    for(var i = 1; i < numcolors; i++) { // loop over number of colors in scale - 1 to get ticks, I only use 9 by default
        ticks.push(color_domain[0]+i*step);
    }
    ticks.push(color_domain[1]);
    //console.log(["ticks",ticks]);
    var axis = d3.svg.axis().scale(scale).orient("right").tickValues(ticks).tickFormat(d3.format(".2f"));
    this.legend.selectAll("rect")
        .data(ticks.slice(0,ticks.length-1))
        .enter().append("rect")
        .attr("x", 10)
        .attr("y", function(d) { return scale(d); })
        .attr("width",15)
        .attr("height",Math.round(l_h/numcolors))
        .attr("fill",function(d) { return colorFunc(d) });
    this.legend.append("text")
			.attr("class", "x forecache-axis")
      .text(title)
      .attr("x",l_w/2)
      .attr("y",offsets.y/2)
			.style("text-anchor","middle");

    this.legend.append("g").attr("class","legend-axis").attr("transform","translate("+25+",0)").call(axis);
}


/* re-renders the x-axis and y-axis tick lines and labels*/
ForeCache.Renderer.Vis.VisObj.prototype.redraw = function() {
	var self = this;
	return function() {
    self.cancelNextFrames();
		var tx = function(d) { 
			return "translate(" + self.x(d) + ",0)"; 
		},
		ty = function(d) { 
			return "translate(0," + self.y(d) + ")";
		},
		stroke = function(d) { 
			return d ? "#ccc" : "#666"; 
		},
		fx = self.x.tickFormat(10),
		fy = self.y.tickFormat(10);

		//replace the xlabel to match current zoom level
		self.vis.selectAll("text.x.forecache-axis").remove();
		self.vis.append("text")
			.attr("class", "x forecache-axis")
			.text(self.xlabel)
			.attr("x", self.size.width/2)
			.attr("y", self.size.height)
			.attr("dy","2.4em")
			.style("text-anchor","middle");

		// Regenerate x-ticks…
		self.vis.selectAll("g.x.forecache-axis").remove();
		self.vis.append("g")
			.attr("class", "x forecache-axis")
			.attr("transform", "translate(0," + (self.size.height)+ ")")
			.call(self.xAxis);

    // only do in one-dimensional case
		if((self.dimensionality == 1) && self.fixYDomain) {
			self.fixYDomain = false;
			var points = self.points;
      var ydomain = self.tileManager.getDomain(self.yindex);
      if(ydomain.length > 0) {
			  var buffer = .15*(ydomain[1]-ydomain[0]);
			  if((ydomain.length == 0)||(buffer == 0)) buffer = 1;
			  self.y.domain([ydomain[0]-buffer,ydomain[1]+buffer])
      }
		}

		// Regenerate y-ticks…
		self.vis.selectAll("g.y.forecache-axis").remove();
		self.vis.append("g")
			.attr("class", "y forecache-axis")
			.call(self.yAxis);

		self.plot.call(d3.behavior.zoom().scaleExtent([1,1]).x(self.x).y(self.y)
      .on("zoomstart",self.recordDragStart())
      .on("zoom", self.redraw())
			.on("zoomend",self.afterZoom()));
		self.canvasUpdate();		
	}	
};

ForeCache.Renderer.Vis.VisObj.prototype.recordDragStart = function() {
  var self = this;
  return function() {
    self.dragStart = d3.mouse(this);
    console.log(["mouse coords",d3.mouse(this)]);
  };
};

ForeCache.Renderer.Vis.VisObj.prototype.cancelNextFrames = function() {
  var tileNames = Object.keys(this.previousRenderState);
  for(var i = 0; i < tileNames.length; i++) {
    window.cancelAnimationFrame(this.previousRenderState[tileNames[i]].nextAnimationRequest);
    delete this.previousRenderState[tileNames[i]];
  }
};

/****************** Helper Functions *********************/

//TODO: create a function to generate snapshot info for the current visualization

ForeCache.Renderer.Vis.VisObj.prototype.log10 = function(val) {
  return Math.log(val) / Math.log(10.0);
};

// computs max, min, and min dist between any two points for the given column
// computes stats across all current tiles
ForeCache.Renderer.Vis.VisObj.prototype.get_stats = function(index) {
  var stats = {};
  var totalTiles = this.tileManager.totalTiles();
  for(var i = 0; i < totalTiles; i++) {
    //console.log(["totalTiles",totalTiles,"index",index,"tile",this.tileManager.getTile(i)]);
    var col = this.tileManager.getTile(i).columns[index];
    var s = this.get_stats_helper(col);
    if(!stats.hasOwnProperty("min") || (stats.min > s.min)) {
      stats.min = s.min;
    }
    if(!stats.hasOwnProperty("max") || (stats.max < s.max)) {
      stats.max = s.max;
    }
    if(!stats.hasOwnProperty("mindist") || (stats.mindist > s.mindist)) {
      stats.mindist = s.mindist;
    }
    if(!stats.hasOwnProperty("maxdist") || (stats.maxdist > s.maxdist)) {
      stats.maxdist = s.maxdist;
    }
  }
  return stats;
};

// goes from '#000000' to (r,g,b)
ForeCache.Renderer.Vis.VisObj.prototype.hex_to_rgb = function(hexString) {
  var rstring = parseInt(x,16);
};

// compute stats for a single column for one tile
ForeCache.Renderer.Vis.VisObj.prototype.get_stats_helper = function(col) {
  var stats = {};
  if(col.length == 0) {
    return stats;
  }
  var temp = [];
  var seen = {};
  // unique values only
  for(var i = 0; i < col.length; i++) {
    var val = col[i];
    if(!seen.hasOwnProperty(val)) {
        seen[val] = true;
        temp.push(val);
    }
  }
  // sort the values
  temp.sort(function(a,b){return Number(a)-Number(b);});
  stats.min = temp[0];
  stats.max = temp[temp.length - 1];
  stats.mindist = -1;
  stats.maxdist = -1;
  if(temp.length > 1) {
    var prev = temp[0];
    var mindist = stats.max-stats.min;
    var maxdist = mindist;
    for(var i = 1; i < temp.length; i++) {
      var val = temp[i];
      var dist = val - prev;
      if(dist < mindist) {
        mindist = dist;
      }
      if(dist > maxdist) {
        maxdist = dist
      }
      prev = val;
    }
    stats.mindist = mindist;
    stats.maxdist = maxdist;
  }
  return stats;
};
