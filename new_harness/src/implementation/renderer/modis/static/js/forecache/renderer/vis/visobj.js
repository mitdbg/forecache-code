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

  this.statesRenderObj = new StatesRenderer.renderObj();
  // bookkeeping data structures
  this.currentTiles = []; // used to keep track of the current tile keys
  this.tileMap = new ForeCache.Backend.Structures.TileMap();
  this.currentZoom = -1;
  this.cacheSize = options.cacheSize || 1;

	//default values
  this.viewportRatio = options.viewportRatio || 1.0;
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

  ForeCache.Backend.Request.getTileStructure(function(ts) {
    self.ts = ts; // store the tile structure
    console.log(["tile structure",self.ts]);
    var newTileWidths = options.tileWidths || ts.tileWidths;
    ForeCache.Backend.Request.updateTileWidths(ts, newTileWidths, function(changed) {
      if(changed) { // successfully updated tile widths
        self.ts.tileWidths = newTileWidths;
        console.log(["successfully changed tile widths to",newTileWidths]);
      }
      if(options.hasOwnProperty("startingPos")) {
        self.currentTiles = options.startingPos; // these better be tile keys
      } else {
        var dimindices = [];
        for(var i = 0; i < self.ts.numdims; i++) {
          dimindices.push(0);
        }
        //self.currentTiles = [new ForeCache.Backend.Structures.NewTileKey(dimindices,0)];
        self.currentTiles = [new ForeCache.Backend.Structures.MultiDimTileKey(dimindices,[0])];
      }
      console.log(["current tiles",self.currentTiles]);
      self.currentZoom = self.currentTiles[0].zoom;
      self.getStartingTiles(); // get the starting data
    });
  });
};

ForeCache.Renderer.Vis.VisObj.prototype.getStartingTiles = function() {
  var self = this;
  var fetchStart = Date.now();
  // get the starting data, then finish doing setup
  ForeCache.Backend.Request.getTiles(self.currentTiles,function(tiles){
    var fetchEnd = Date.now();
    ForeCache.globalTracker.appendToLog(ForeCache.Tracker.perInteractionLogName,{'action':'fetch','totalTiles':tiles.length,'start':fetchStart,'end':fetchEnd});
    console.log(['time to fetch all tiles',fetchEnd-fetchStart]);

    // store the starting tiles
    self.tileMap.batchInsert(tiles);
    //finish doing setup
    self.finishSetup(tiles);
  });
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

ForeCache.Renderer.Vis.VisObj.prototype.getZoomDims = function() {
  return this.ts.numdims;
};

// this part of the setup process requires information about the starting data
ForeCache.Renderer.Vis.VisObj.prototype.finishSetup = function(startingTiles) {
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

  if(this.options.hasOwnProperty("zdomain")) {
    // color scale
    // assumes numeric domain
    // TODO: remove hardcoded color setup here
    var zdom = this.options.zdomain;
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
			.style("opacity",0);

	//this.plot.call(d3.behavior.zoom().scaleExtent([1,1]).x(this.x).y(this.y).on("zoom", this.redraw()));
	this.plot.call(d3.behavior.zoom().scaleExtent([1,1]).x(this.x).y(this.y).on("zoom", this.redraw())
			.on("zoomend",this.afterZoom()));

	//this.plot.call(d3.behavior.zoom().x(this.x).y(this.y).on("zoom", this.redraw()));


	this.legend = this.svg.append("g")
			.attr("transform", "translate(" + (this.padding.left+this.size.width) + "," + this.padding.top + ")")
			.attr("class", "forecache-legend");


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


  this.drawLegend(this.options.zlabel,this.padding.right,150,zdom,this.colorScale);

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
  var tile0 = this.tileMap.get(this.currentTiles[0]);
  this.xindex = tile0.getIndex(this.options.xname);
  this.yindex = tile0.getIndex(this.options.yname);
  //console.log(["indexes","x",this.xindex,"y",this.yindex]);
  if(this.options.hasOwnProperty("zname")) {
    this.zindex = tile0.getIndex(this.options.zname);
    newopts.zdomain = ForeCache.Backend.Structures.getDomain(this.tileMap.getTiles(),this.zindex);
  }

  // compute domains according to tile range, not according to the data in the tile (tile could be
  // sparse)
  //newopts.xdomain = ForeCache.Backend.Structures.getDomain(this.tileMap.getTiles(),this.xindex);
  newopts.xdomain = ForeCache.Backend.Structures.getTileDomain(this.tileMap.getTiles(),this.ts,this.xindex);
  newopts.xdomain = this.adjustForViewportRatio(newopts.xdomain);
  //newopts.ydomain = ForeCache.Backend.Structures.getDomain(this.tileMap.getTiles(),this.yindex);
  newopts.ydomain = ForeCache.Backend.Structures.getTileDomain(this.tileMap.getTiles(),this.ts,this.yindex);
  if(this.getZoomDims() == 2) {
    newopts.ydomain = this.adjustForViewportRatio(newopts.ydomain);
  }
  // compute color domain
	newopts.size = {
		"width":	newopts.width - newopts.padding.left - newopts.padding.right,
		"height": newopts.height - newopts.padding.top	- newopts.padding.bottom
	};

  //console.log(["width",newopts.size.width,"height",newopts.size.height]);
};


// compute the range of the xdomain
ForeCache.Renderer.Vis.VisObj.prototype.getXRangeForZoom = function(currzoom,newzoom) {
  var self = this;
  var xindex = 0;
	var xdomain = self.x.domain(); // raw data values for this zoom level
  var xdiff = xdomain[1]-xdomain[0];
	var xmid = xdomain[0] + 1.0*(xdiff)/2; // midpoint
	//console.log(["xmid",xmid,"xdomain",xdomain]);

  var newXmid = self.ts.getNewMid(currzoom,newzoom,xindex,xmid);
	var halfWidth = self.ts.tileWidths[xindex] * self.cacheSize / 2.0;
	var newXDomain = [newXmid-halfWidth,newXmid+halfWidth];
  //console.log(["x","old mid",xmid,"new mid",newXmid]);
		
  return newXDomain;
};

// compute the range of the xdomain
ForeCache.Renderer.Vis.VisObj.prototype.getYRangeForZoom = function(currzoom,newzoom) {
  var self = this;
  var yindex = 1;
	var ydomain = self.y.domain(); // raw data values for this zoom level
  var ydiff = ydomain[1]-ydomain[0];
	var ymid = ydomain[0] + 1.0*(ydiff)/2; // midpoint
	//console.log(["xmid",ymid,"xdomain",ydomain]);

  var newYmid = self.ts.getNewMid(currzoom,newzoom,yindex,ymid);
	var halfWidth = self.ts.tileWidths[yindex] * self.cacheSize / 2.0;
	var newYDomain = [newYmid-halfWidth,newYmid+halfWidth];
  //console.log(["y","old mid",ymid,"new mid",newYmid]);
		
  return newYDomain;
};

ForeCache.Renderer.Vis.VisObj.prototype.zoomClick = function() {
	var self = this;
	//return function () {};
	return function () {
		if(!self.mousebusy) {
			self.mousebusy = true;
			$('body').css("cursor", "wait");
		}
		var zoomDiff = Number(this.getAttribute("data-zoom"));
    var newZoom = self.ts.getNewZoomPosition(self.currentZoom,[self.xindex,self.yindex],[zoomDiff,zoomDiff]);
		console.log(["zoomDiff",zoomDiff,"currentZoom",self.currentZoom,"newZoom",newZoom]);

		if(!self.ts.zoomLevelChanged(self.currentZoom,newZoom)) { // no change
			self.mousebusy = false;
		  $("body").css("cursor", "default");
      return; // don't do anything
    }

		var xdomain = self.x.domain();
    var newXDomain = self.getXRangeForZoom(self.currentZoom,newZoom);

    if(self.getZoomDims() == 2) { // we are tracking more than one dimension, so update y domain
		  var ydomain = self.y.domain();
      var newYDomain = self.getYRangeForZoom(self.currentZoom,newZoom);
    }
		
		self.currentZoom = newZoom;
		self.tileMap.clear();
		self.currentTiles = [];
		self.x.domain(self.adjustForViewportRatio(newXDomain));
    if(self.getZoomDims() == 2) {
		  self.y.domain(self.adjustForViewportRatio(newYDomain));
    }
		self.fixYDomain = true;
		self.afterZoom()();
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

  for(var t = 0; t < this.currentTiles.length; t++) {
    var s = Date.now();
    var id = this.currentTiles[t];
    var tile = this.tileMap.get(id);
    this.renderTile(tile);
    var e = Date.now();
    console.log(["time to render tile",e-s]); // console statements take ~3ms of time
    //ForeCache.globalTracker.appendToLog(ForeCache.Tracker.perTileLogName,{'action':'renderTile','tileId':id.name,'start':s,'end':e});
  };
  this.statesRenderObj.renderUsa(this.ts.getAggregationWindow(this.currentZoom,this.xindex),this.ts.getAggregationWindow(this.currentZoom,this.yindex),this.ctx,this.x,this.y,this.padding);
   var end = Date.now(); // in seconds
  console.log(["time to render all tiles",end - start]);
  ForeCache.globalTracker.appendToLog(ForeCache.Tracker.perInteractionLogName,
    {'action':'render','totalTiles':this.currentTiles.length,'start':start,'end':end});
 	
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
	if(this.mousebusy) {
		this.mousebusy = false;
		$("body").css("cursor", "default");
	}
}

ForeCache.Renderer.Vis.VisObj.prototype.getXRange = function() {
  var self = this;
	var xdom = self.x.domain();
	var low = Math.max(0,parseInt(xdom[0],10));
	var high = Math.max(0,parseInt(xdom[1],10));

  //console.log(["x","low",low,"high",high]);
	var minID = Math.floor(low / self.ts.tileWidths[self.xindex]);
	var maxID = Math.floor(high / self.ts.tileWidths[self.xindex]);
	var newIDs = [];
	var newTileMap = {};
	var toFetch = [];
	for(var tileID = minID; tileID < maxID; tileID++) {
		newIDs.push(tileID);
	}
	newIDs.push(maxID);
  return newIDs;
};

ForeCache.Renderer.Vis.VisObj.prototype.getYRange = function() {
  var self = this;
	var ydom = self.y.domain();
	var low = Math.max(0,parseInt(ydom[0],10));
	var high = Math.max(0,parseInt(ydom[1],10));

  //console.log(["y","low",low,"high",high]);
	var minID = Math.floor(low / self.ts.tileWidths[self.yindex]);
	var maxID = Math.floor(high / self.ts.tileWidths[self.yindex]);
	var newIDs = [];
	var newTileMap = {};
	var toFetch = [];
	for(var tileID = minID; tileID < maxID; tileID++) {
		newIDs.push(tileID);
	}
	newIDs.push(maxID);
  return newIDs;
};

ForeCache.Renderer.Vis.VisObj.prototype.getFutureTiles = function() {
  var numdims = this.getZoomDims();
  var xtiles = this.getXRange();
  var futuretiles = [];
  if(numdims == 1) { // one dimension
    if(this.ts.numdims == 1) {
      for(var x = 0; x < xtiles.length; x++) {
        //var newkey = new ForeCache.Backend.Structures.NewTileKey([xtiles[x]],this.currentZoom);
        var newkey = new ForeCache.Backend.Structures.MultiDimTileKey([xtiles[x]],this.currentZoom);
        futuretiles.push(newkey); // push the pair
      }
    } else if (this.ts.numdims == 2) { // navigation along 1 dim doesn't mean there is only one dim
      for(var x = 0; x < xtiles.length; x++) {
        var di = [];
        for(var i = 0; i < this.ts.numdims; i++) {
          di.push(0);
        }
        di[this.xindex] = xtiles[x];
        //var newkey = new ForeCache.Backend.Structures.NewTileKey(di,this.currentZoom);
        var newkey = new ForeCache.Backend.Structures.MultiDimTileKey(di,this.currentZoom);
        //console.log(["new key",newkey]);
        futuretiles.push(newkey); // push the pair
      }
    }
  } else if (numdims == 2) { // two dimensions
    var ytiles = this.getYRange();
    for(var x = 0; x < xtiles.length; x++) {
      for(var y = 0; y < ytiles.length; y++) {
        var di = [];
        for(var i = 0; i < this.ts.numdims; i++) {
          di.push(0);
        }
        di[this.xindex] = xtiles[x];
        di[this.yindex] = ytiles[y];
        //var newkey = new ForeCache.Backend.Structures.NewTileKey(di,this.currentZoom);
        var newkey = new ForeCache.Backend.Structures.MultiDimTileKey(di,this.currentZoom);
        //console.log(["new key",newkey]);
        futuretiles.push(newkey);
      }
    }
  }
  //console.log(futuretiles);
  return futuretiles;
};

ForeCache.Renderer.Vis.VisObj.prototype.afterZoom = function() {
	var self = this;
	//return function() {};
	return function() {
		if(!self.mousebusy) {
			self.mousebusy = true;
			$('body').css("cursor", "wait");
		}
			var newIDs = self.getFutureTiles(); //NewTileKey objects
			var newTileMap = new ForeCache.Backend.Structures.TileMap();
			var toFetch = [];

			for(var i = 0; i < newIDs.length; i++) {
				var tileID = newIDs[i];
				if(!self.tileMap.containsKey(tileID)) {
					toFetch.push(tileID);
				} else {
					newTileMap.insert(self.tileMap.get(tileID));
				}
			}
			self.tileMap = newTileMap; //get rid of the stuff we don't need
			self.currentTiles = newIDs; // record the new list of tiles
			//console.log(["proposed tiles",newIDs]);
			//console.log(["to fetch",toFetch.length,toFetch]);
			//console.log(["current tiles",self.currentTiles.length,self.currentTiles,self.tileMap]);
			if(toFetch.length > 0) {
				//self.getTiles(toFetch,function() {self.redraw()();}); // get the missing tiles from the new list
        var fetchStart = Date.now();
        ForeCache.Backend.Request.getTiles(toFetch,function(tiles) {
          var fetchEnd = Date.now();
          ForeCache.globalTracker.appendToLog(ForeCache.Tracker.perInteractionLogName,{'action':'fetch','totalTiles':tiles.length,'start':fetchStart,'end':fetchEnd});
          console.log(['time to fetch all tiles',fetchEnd-fetchStart]);
          self.tileMap.batchInsert(tiles);
          self.redraw()();
        });
			} else {
				if(self.mousebusy) {
					self.mousebusy = false;
					$('body').css("cursor", "default");
				}
			}
	};
}

/*
Draws the legend for the graph. Needs the container to draw the legend in,
desired dimensions of the legend, and the color scale.
*/
ForeCache.Renderer.Vis.VisObj.prototype.drawLegend = function(title,l_w,l_h,colorDomain,colorFunc) {
    var offsets={"x":5,"y":14};
    var numcolors = 9; // number of colors in color scale
    var temp = this;
    var color_domain = [Math.max.apply(null,colorDomain),Math.min.apply(null,colorDomain)];
    var scale =
d3.scale.linear().domain([color_domain[0],color_domain[1]]).range([offsets.y,l_h]);
    var ticks = [color_domain[0]];
    var step = 1.0 *(color_domain[1] - color_domain[0]) / numcolors; // divide domain by number of colors in the scale
    for(var i = 1; i < numcolors; i++) { // loop over number of colors in scale - 1 to get ticks, I only use 9 by default
        ticks.push(color_domain[0]+i*step);
    }
    ticks.push(color_domain[1]);
    console.log(["ticks",ticks]);
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
		if((self.ts.numdims == 1) && self.fixYDomain) {
			self.fixYDomain = false;
			var points = self.points;
      var ydomain = ForeCache.Backend.Structures.getDomain(self.tileMap.getTiles(),self.yindex);
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

		self.plot.call(d3.behavior.zoom().scaleExtent([1,1]).x(self.x).y(self.y).on("zoom", self.redraw())
			.on("zoomend",self.afterZoom()));
		self.canvasUpdate();		
	}	
};

/****************** Helper Functions *********************/

ForeCache.Renderer.Vis.VisObj.prototype.log10 = function(val) {
  return Math.log(val) / Math.log(10.0);
}

