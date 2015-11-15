// assumes that the ForeCache.Backend object is available
var ForeCache = ForeCache || {};
ForeCache.Renderer = ForeCache.Renderer || {};
ForeCache.Renderer.Vis = ForeCache.Renderer.Vis || {};

/************* Classes *************/

// VisObj is a super class for visualizations. All visualization types should be able to
// inherit from this class, and only fill in a small number of functions.
// THIS CLASS DOES NOT RENDER ANYTHING!!!! Note also
// that the chart parameter is a jquery object
ForeCache.Renderer.Vis.VisObj = function(chart, FCBackend, options) {
	var self = this;
  this.backend = FCBackend;

  // bookkeeping data structures
  this.currentTiles = []; // used to keep track of the current tile keys
  this.tileMap = new ForeCache.Backend.TileMap();
  this.currentZoom = -1;
  this.cacheSize = 1;

	//default values
  this.viewportRatio = options.viewportRatio || 1.0;
	this.xlabel = options.xlabel
	this.ylabel = options.ylabel
  this.inverted = options.inverted || {"x":false,"y":false};
	this.fixYDomain = false;
	this.mousebusy = false;

	this.options = options || {};
	this.chart = (chart.toArray())[0]; // get dom element from jquery

  this.backend.getTileStructure(function(ts) {
    self.ts = ts; // store the tile structure
    if(options.hasOwnProperty("startingPos")) {
      self.currentTiles = options.startingPos; // these better be tile keys
    } else {
      var dimindices = [];
      for(var i = 0; i < self.ts.numdims; i++) {
        dimindices.push(0);
      }
      self.currentTiles = [new self.backend.NewTileKey(dimindices,0)];
    }
    self.currentZoom = self.currentTiles[0].zoom;
    self.getStartingTiles(); // get the starting data
  });
};

ForeCache.Renderer.Vis.VisObj.prototype.getStartingTiles = function() {
  var self = this;
  // get the starting data, then finish doing setup
  self.backend.getTiles(self.currentTiles,function(tiles){
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
  console.log(["domain2",domain2]);
  return domain2;
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

  if(this.inverted.x) {
    this.x.range([this.size.width,0]);
  }
  if(this.inverted.y) {
    this.y.range([this.size.height,0]);
  }

  if(this.options.hasOwnProperty("zdomain")) {
    // color scale
    // assumes numeric domain
    // TODO: remove hardcoded color setup here
    this.color = d3.scale.quantize()
      .domain(this.options.zdomain)
      .range(colorbrewer.YlOrRd[9]);
  }

	this.xAxis = d3.svg.axis().scale(this.x).orient("bottom");
	this.yAxis = d3.svg.axis().scale(this.y).orient("left");

	this.vis = d3.select(this.chart).append("svg")
		.attr("width",	this.cx)
		.attr("height", this.cy)
		.attr("class","forecache-vis")
		.append("g")
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

	this.svg = this.vis.append("svg")
			.attr("top", 0)
			.attr("left", 0)
			.attr("width", this.size.width)
			.attr("height", this.size.height)
			//.attr("viewBox", "0 0 "+this.size.width+" "+this.size.height)
			.attr("class", "line");

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
  if(this.options.hasOwnProperty("zname")) {
    this.zindex = tile0.getIndex(this.options.zname);
    newopts.zdomain = ForeCache.Backend.getDomain(this.tileMap.getTiles(),this.zindex);
  }

  // compute domains according to tile range, not according to the data in the tile (tile could be
  // sparse)
  //newopts.xdomain = ForeCache.Backend.getDomain(this.tileMap.getTiles(),this.xindex);
  newopts.xdomain = ForeCache.Backend.getTileDomain(this.tileMap.getTiles(),this.ts,this.xindex);
  newopts.xdomain = this.adjustForViewportRatio(newopts.xdomain);
  //newopts.ydomain = ForeCache.Backend.getDomain(this.tileMap.getTiles(),this.yindex);
  newopts.ydomain = ForeCache.Backend.getTileDomain(this.tileMap.getTiles(),this.ts,this.yindex);
  newopts.ydomain = this.adjustForViewportRatio(newopts.ydomain);
  // compute color domain
	newopts.size = {
		"width":	newopts.width - newopts.padding.left - newopts.padding.right,
		"height": newopts.height - newopts.padding.top	- newopts.padding.bottom
	};

  console.log(["width",newopts.size.width,"height",newopts.size.height]);
};


// compute the range of the xdomain
ForeCache.Renderer.Vis.VisObj.prototype.getXRangeForZoom = function(currzoom,newzoom) {
  var self = this;
  var xindex = 0;
	var xdomain = self.x.domain(); // raw data values for this zoom level
  var xdiff = xdomain[1]-xdomain[0];
	var xmid = xdomain[0] + 1.0*(xdiff)/2; // midpoint
	console.log(["xmid",xmid,"xdomain",xdomain]);

  // old aggregation window
	var oldWindow = self.ts.aggregationWindows[currzoom][xindex];
  // new aggregation window
	var newWindow = self.ts.aggregationWindows[newzoom][xindex];
  // translate this midpoint to the equivalent midpoint on the new zoom level
	var newXmid = 1.0*xmid * oldWindow / newWindow;
	var halfWidth = self.ts.tileWidths[xindex] * self.cacheSize / 2.0;
	var newXDomain = [newXmid-halfWidth,newXmid+halfWidth];
	console.log(["x","oldWindow",oldWindow,"newWindow",newWindow,"newXmid",newXmid,
									"halfWidth",halfWidth,"newXdomain",newXDomain]);
		
  return newXDomain;
};

// compute the range of the xdomain
ForeCache.Renderer.Vis.VisObj.prototype.getYRangeForZoom = function(currzoom,newzoom) {
  var self = this;
  var yindex = 1;
	var ydomain = self.y.domain(); // raw data values for this zoom level
  var ydiff = ydomain[1]-ydomain[0];
	var ymid = ydomain[0] + 1.0*(ydiff)/2; // midpoint
	console.log(["xmid",ymid,"xdomain",ydomain]);

  // old aggregation window
	var oldWindow = self.ts.aggregationWindows[currzoom][yindex];
  // new aggregation window
	var newWindow = self.ts.aggregationWindows[newzoom][yindex];
  // translate this midpoint to the equivalent midpoint on the new zoom level
	var newYmid = 1.0*ymid * oldWindow / newWindow;
	var halfWidth = self.ts.tileWidths[yindex] * self.cacheSize / 2.0;
	var newYDomain = [newYmid-halfWidth,newYmid+halfWidth];
	console.log(["y","oldWindow",oldWindow,"newWindow",newWindow,"newYmid",newYmid,
									"halfWidth",halfWidth,"newYdomain",newYDomain]);
		
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
		if(zoomDiff < 0) console.log("zoom out");
		else console.log("zoom in");
		var newZoom = Math.min(Math.max(0,self.currentZoom+zoomDiff),self.ts.totalLevels-1);
		console.log(["zoomDiff",zoomDiff,"currentZoom",self.currentZoom,"newZoom",newZoom]);

		if(newZoom == self.currentZoom) { // no change
			self.mousebusy = false;
		  $("body").css("cursor", "default");
      return; // don't do anything
    }

		var xdomain = self.x.domain();
    var newXDomain = self.getXRangeForZoom(self.currentZoom,newZoom);

    if(self.ts.numdims >= 2) { // we are tracking more than one dimension, so update y domain
		  var ydomain = self.y.domain();
      var newYDomain = self.getYRangeForZoom(self.currentZoom,newZoom);
    }
		
		self.currentZoom = newZoom;
		self.tileMap.clear();
		self.currentTiles = [];
		//self.x.domain(newXDomain);
		//self.y.domain(newYDomain);
		self.x.domain(self.adjustForViewportRatio(newXDomain));
    if(self.ts.numdims >= 2) {
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
	this.ctx.beginPath();
	this.ctx.fillStyle = this.background;
	this.ctx.rect(0,0,this.cx,this.cy);
	this.ctx.fill();
	this.ctx.closePath();

  for(var t = 0; t < this.currentTiles.length; t++) {
    var id = this.currentTiles[t];
    var tile = this.tileMap.get(id);
    this.renderTile(tile);
  };
	
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

  console.log(["x","low",low,"high",high]);
	var minID = Math.floor(low / self.ts.tileWidths[0]);
	var maxID = Math.floor(high / self.ts.tileWidths[0]);
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

  console.log(["y","low",low,"high",high]);
	var minID = Math.floor(low / self.ts.tileWidths[1]);
	var maxID = Math.floor(high / self.ts.tileWidths[1]);
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
  var numdims = this.ts.numdims;
  var xtiles = this.getXRange();
  var futuretiles = [];
  if(numdims == 1) { // one dimension
    for(var x = 0; x < xtiles.length; x++) {
        var newkey = new ForeCache.Backend.NewTileKey([xtiles[x]],this.currentZoom);
        futuretiles.push(newkey); // push the pair
    }
  } else if (numdims == 2) { // two dimensions
    var ytiles = this.getYRange();
    for(var x = 0; x < xtiles.length; x++) {
      for(var y = 0; y < ytiles.length; y++) {
        var newkey = new ForeCache.Backend.NewTileKey([xtiles[x],ytiles[y]],this.currentZoom);
        futuretiles.push(newkey);
      }
    }
  }
  console.log(futuretiles);
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
			var newTileMap = new ForeCache.Backend.TileMap();
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
			console.log(["to fetch",toFetch.length,toFetch]);
			console.log(["current tiles",self.currentTiles.length,self.currentTiles,self.tileMap]);
			if(toFetch.length > 0) {
				//self.getTiles(toFetch,function() {self.redraw()();}); // get the missing tiles from the new list
        self.backend.getTiles(toFetch,function(tiles) {
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
      var ydomain = ForeCache.Backend.getDomain(self.tileMap.getTiles(),self.yindex);
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


