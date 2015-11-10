var ForeCache = ForeCache || {};
ForeCache.Renderer = ForeCache.Renderer || {};
// all specialized renderers inherit from this base object
ForeCache.Renderer.BaseObj = {};

// used to create a unique identifier for visualization objects in the DOM
ForeCache.Renderer.BaseObj.getPrefix = function() { return BigDawgVis.getPrefix() + "forecache-baseobj-";};

// returns a pointer to a new div containing the rendered visualization.
// The new div has a unique identifier taking the form: "bigdawgvis-linechart-<UUID>".
// this function also appends the div to the given node in the dom (a.k.a. "root")
ForeCache.Renderer.BaseObj.getVis = function(root,options,FCBackend) {
  var name = ForeCache.Renderer.BaseObj.getPrefix()+BigDawgVis.uuid();
  var visDiv = $("<div id=\""+name+"\"></div>").appendTo(root);
  var graph = new ForeCache.Renderer.VisObj(visDiv,FCBackend,options);
  return visDiv;
};



/************* Classes *************/
/* chart parameter is a jquery object */
ForeCache.Renderer.VisObj = function(chart, FCBackend, options) {
	var self = this;
  this.backend = FCBackend;
  this.currentTiles = [];

	//default values
	this.xlabel = options.xlabel
	this.ylabel = options.ylabel
	this.fixYDomain = false;
	this.mousebusy = false;

	this.options = options || {};
	this.chart = (chart.toArray())[0]; // get dom element from jquery

  this.backend.getTileStructure(function(ts) {
    self.ts = ts; // store the tile structure
    if(options.hasOwnProperty("startingPos")) {
      self.startingPosition = options.startingPos;
    } else {
      var dimindices = [];
      for(var i = 0; i < self.ts.tileWidths.length; i++) {
        dimindices.push(0);
      }
      self.startingPosition = [new self.backend.NewTileKey(dimindices,0)];
    }
    self.getStartingTiles(); // get the starting data
  });
};

ForeCache.Renderer.VisObj.prototype.getStartingTiles = function() {
  var self = this;
  // get the starting data, then finish doing setup
  self.backend.getTiles(self.startingPosition,function(tiles){self.finishSetup(tiles);});
};


// this part of the setup process requires information about the starting data
ForeCache.Renderer.VisObj.prototype.finishSetup = function(startingTiles) {
  console.log("got here");
  this.currentTiles = startingTiles; // store the starting tiles
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
		.range([this.size.height,0])
		.nice();

  // color scale
  // assumes numeric domain
  // TODO: remove hardcoded color setup here
  this.color = d3.scale.quantize()
    .domain(this.options.zdomain)
    .range(colorbrewer.YlOrRd[9]);

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
			.attr("transform","translate(" + -40 + " " + this.size.height/2+") rotate(-90)");
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


// computs max, min, and min dist between any two points for the given column
// computes stats across all current tiles
ForeCache.Renderer.VisObj.prototype.get_stats = function(index) {
  var stats = {};
  for(var i = 0; i < this.currentTiles.length; i++) {
    var col = this.currentTiles[i].columns[index];
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
  }
  return stats;
};

// compute stats for a single column for one tile
ForeCache.Renderer.VisObj.prototype.get_stats_helper = function(col) {
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
  if(temp.length > 1) {
    var prev = temp[0];
    var mindist = stats.max-stats.min;
    for(var i = 1; i < temp.length; i++) {
      var val = temp[i];
      var dist = val - prev;
      if(dist < mindist) {
        mindist = dist;
      }
      prev = val;
    }
    stats.mindist = mindist;
  }
  return stats;
};

// adds to the default options set in BigDawgVis.updateOpts
ForeCache.Renderer.VisObj.prototype.updateOpts = function() {
  var newopts = this.options;
  this.xindex = this.currentTiles[0].getIndex(this.options.xname);
  this.yindex = this.currentTiles[0].getIndex(this.options.yname);
  this.zindex = this.currentTiles[0].getIndex(this.options.zname);

  // compute color domain
  newopts.zdomain = ForeCache.Backend.getDomain(this.currentTiles,this.zindex);
	newopts.size = {
		"width":	newopts.width - newopts.padding.left - newopts.padding.right,
		"height": newopts.height - newopts.padding.top	- newopts.padding.bottom
	};
  newopts.boxwidth = {};

  xstats = this.get_stats(this.xindex);
  console.log(xstats);
  newopts.xdomain = [xstats.max,xstats.min];
  var xm = xstats.mindist; // width of box
  var xd = xstats.max - xstats.min; // space for boxes in domain
  var xw = newopts.size.width; // space for boxes in range
  if(xm > 0) {
    var numboxes = Math.ceil(xd / xm) + 1;
    var boxwidth = Math.floor(1.0 * xw / numboxes);
    if(boxwidth < 1) { // can't have a fraction of a pixel!
      boxwidth = 1; // make it 1 pixel
    }
    newopts.boxwidth.x = boxwidth;
    newopts.size.width = numboxes*boxwidth; // make the width more realistic
    newopts.width = newopts.padding.left + newopts.padding.right + newopts.size.width;
    console.log([xstats,xm,xd,xw,numboxes,boxwidth]);
    newopts.getXPos = function(value) {
      return Math.floor((value - xstats.min)/xm)*boxwidth;
    }
  }

  ystats = this.get_stats(this.yindex);
  newopts.ydomain = [ystats.max,ystats.min];
  console.log(ystats);
  var ym = ystats.mindist; // height of box
  var yd = ystats.max - ystats.min; // space for boxes in domain
  var yw = newopts.size.height; // space for boxes in range
  if(ym > 0) {
    var numboxes = Math.ceil(yd / ym) + 1;
    var boxwidth = Math.floor(1.0*yw / numboxes);
    if(boxwidth < 1) { // can't have a fraction of a pixel!
      boxwidth = 1; // make it 1 pixel
    }
    newopts.boxwidth.y = boxwidth;
    newopts.size.height = numboxes*boxwidth; // make the height more realistic
    newopts.height = newopts.padding.top + newopts.padding.bottom + newopts.size.height;
    console.log([ystats,ym,yd,yw,numboxes,boxwidth]);
    newopts.getYPos = function(value) {
      return Math.floor((value - ystats.min)/ym)*boxwidth;
    }
  }
  console.log(["width",newopts.size.width,"height",newopts.size.height]);
};

//
// VisObj methods
//

ForeCache.Renderer.VisObj.prototype.zoomClick = function() {
	var self = this;
	return function () {};
/*
	return function () {
		if(!self.mousebusy) {
			self.mousebusy = true;
			$('body').css("cursor", "wait");
		}
		var zoomDiff = Number(this.getAttribute("data-zoom"));
		if(zoomDiff < 0) console.log("zoom out");
		else console.log("zoom in");
		var newZoom = Math.min(Math.max(0,self.currentZoom+zoomDiff),self.aggWindows.length-1);
		console.log(["zoomDiff",zoomDiff,"currentZoom",self.currentZoom,"newZoom",newZoom]);
		if(newZoom == self.currentZoom) return; // no change

		var xdomain = self.x.domain();
		var ydomain = self.y.domain();
		var xmid = xdomain[0] + 1.0*(xdomain[1]-xdomain[0])/2;
		console.log(["xmid",xmid,"xdomain",xdomain,"zoomDiff",zoomDiff,
									"currentZoom",self.currentZoom,"newZoom",newZoom]);
		var oldWindow = self.aggWindows[self.currentZoom];
		var newWindow = self.aggWindows[newZoom];
		var newXmid = 1.0*xmid * oldWindow / newWindow;
		var halfWidth = self.k * self.cacheSize / 2.0;
		var newXdomain = [newXmid-halfWidth,newXmid+halfWidth];
		console.log(["oldWindow",oldWindow,"newWindow",newWindow,"newXmid",newXmid,
									"halfWidth",halfWidth,"newXdomain",newXdomain]);
		
		self.currentZoom = newZoom;
		self.tileMap = {};
		self.currentTiles = [];
		self.x.domain(newXdomain);
		self.fixYDomain = true;
		self.afterZoom()();
	};
*/
}

ForeCache.Renderer.VisObj.prototype.renderTile = function(tile) {
  var rows = tile.getSize();
  var xw = this.options.boxwidth.x;
  var yw = this.options.boxwidth.y;
	for(var i=0; i < rows;i++) {
    var xval = tile.columns[this.xindex][i];
    var yval = tile.columns[this.yindex][i];
    var zval = tile.columns[this.zindex][i];
		var x = this.x(xval)+this.padding.left;
		var y = this.y(yval)+this.padding.top;
		
		this.ctx.beginPath();
 		this.ctx.fillStyle = this.color(zval);
		this.ctx.fillRect(x,y, xw, yw);
		this.ctx.closePath();
	}
};

ForeCache.Renderer.VisObj.prototype.canvasUpdate = function() {
	var self = this;
	this.ctx.beginPath();
	this.ctx.fillStyle = this.background;
	this.ctx.rect(0,0,this.cx,this.cy);
	this.ctx.fill();
	this.ctx.closePath();

  for(var t = 0; t < this.currentTiles.length; t++) {
    this.renderTile(this.currentTiles[t]);
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

ForeCache.Renderer.VisObj.prototype.afterZoom = function() {
	var self = this;
	return function() {};
/*
	return function() {
		if(!self.mousebusy) {
			self.mousebusy = true;
			$('body').css("cursor", "wait");
		}
			var xdom = self.x.domain();
			var low = Math.max(0,parseInt(xdom[0],10));
			var high = Math.max(0,parseInt(xdom[1],10));

			var minID = Math.floor(low / self.k);
			var maxID = Math.floor(high / self.k);
			var newIDs = [];
			var newTileMap = {};
			var toFetch = [];
			for(var tileID = minID; tileID < maxID; tileID++) {
				newIDs.push(tileID);
			}
			newIDs.push(maxID);

			for(var i = 0; i < newIDs.length; i++) {
				var tileID = newIDs[i];
				if(!self.tileMap.hasOwnProperty(tileID)) {
					toFetch.push(tileID);
				} else {
					newTileMap[tileID] = self.tileMap[tileID];
				}
			}
			self.tileMap = newTileMap; //get rid of the stuff we don't need
			self.currentTiles = newIDs; // record the new list of tiles
			console.log(["to fetch",toFetch]);
			console.log(["current tiles",self.currentTiles,self.tileMap]);
			if(toFetch.length > 0) {
				self.getTiles(toFetch,function() {self.redraw()();}); // get the missing tiles from the new list
			} else {
				if(self.mousebusy) {
					self.mousebusy = false;
					$('body').css("cursor", "default");
				}
			}
	};
*/
}


/* re-renders the x-axis and y-axis tick lines and labels*/
ForeCache.Renderer.VisObj.prototype.redraw = function() {
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

/*
		if(self.fixYDomain) {
			self.fixYDomain = false;
			var points = self.points;
			var ydomain = BigDawgVis.domain(points,self.options.yname);
			var buffer = .15*(ydomain[1]-ydomain[0]);
			if((ydomain.length == 0)||(buffer == 0)) buffer = 1;
			self.y.domain([ydomain[0]-buffer,ydomain[1]+buffer])
		}
*/

		// Regenerate y-ticks…
		self.vis.selectAll("g.y.forecache-axis").remove();
		self.vis.append("g")
			.attr("class", "y forecache-axis")
			.call(self.yAxis);

		//Leilani
		self.plot.call(d3.behavior.zoom().scaleExtent([1,1]).x(self.x).y(self.y).on("zoom", self.redraw())
			.on("zoomend",self.afterZoom()));
		self.canvasUpdate();		
	}	
}


