// assumes that the ForeCache.Backend object is available
var ForeCache = ForeCache || {};
ForeCache.Renderer = ForeCache.Renderer || {};
ForeCache.Renderer.Heatmap = {}

// used to create a unique identifier for visualization objects in the DOM
ForeCache.Renderer.Heatmap.getPrefix = function() { return BigDawgVis.getPrefix() + "forecache-heatmap-";};

// returns a pointer to a new div containing the rendered visualization.
// The new div has a unique identifier taking the form: "bigdawgvis-linechart-<UUID>".
// this function also appends the div to the given node in the dom (a.k.a. "root")
ForeCache.Renderer.Heatmap.getVis = function(root,o,jsondata) {
  var name = ForeCache.Renderer.Heatmap.getPrefix()+BigDawgVis.uuid();
  var visDiv = $("<div id=\""+name+"\"></div>").appendTo(root);
  var options = ForeCache.Renderer.Heatmap.updateOpts(o,jsondata.table);
  var graph = new HeatmapObj(visDiv,options,jsondata.table);
  return visDiv;
};

// adds to the default options set in BigDawgVis.updateOpts
ForeCache.Renderer.Heatmap.updateOpts = function(opts,table) {
  var newopts = BigDawgVis.updateOpts(opts,table);
  newopts.padding = opts.padding;
	newopts.size = {
		"width":	newopts.width - newopts.padding.left - newopts.padding.right,
		"height": newopts.height - newopts.padding.top	- newopts.padding.bottom
	};
  newopts.boxwidth = {};

  xstats = ForeCache.Renderer.Heatmap.get_stats(table,opts,newopts.xname);
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

  ystats = ForeCache.Renderer.Heatmap.get_stats(table,opts,newopts.yname);
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
  return newopts;
}

// pass option "sort" in opts as input, where "sort" = the label to collect stats for
// (e.g., sort = opts.xname). assumes numbers
ForeCache.Renderer.Heatmap.get_stats = function(table,opts,sort) {
  var stats = {};
  if(table.length == 0) {
    return stats;
  }
  var temp = [];
  var seen = {};
  // unique values only
  for(var i = 0; i < table.length; i++) {
    var val = table[i][sort];
    if(!seen.hasOwnProperty(val)) {
        seen[val] = true;
        var el = {};
        el[sort] = val;
        temp.push(el);
    }
  }
  // sort the values
  opts.sort = sort;
  BigDawgVis.Util.sortAscending(temp,opts);
  stats.min = temp[0][sort];
  stats.max = temp[temp.length - 1][sort];
  stats.mindist = -1;
  if(temp.length > 1) {
    var prev = temp[0][sort];
    var mindist = stats.max-stats.min;
    for(var i = 1; i < temp.length; i++) {
      var val = temp[i][sort];
      var dist = val - prev;
      if(dist < mindist) {
        mindist = dist;
      }
      prev = val;
    }
    stats.mindist = mindist;
  }
  return stats;
}

/* chart parameter is a jquery object */
HeatmapObj = function(chart, options,points) {
	var self = this;
  self.points = points;

	//default values
	this.xlabel = options.xlabel
	this.ylabel = options.ylabel
	this.fixYDomain = false;
	this.mousebusy = false;

	this.options = options || {};
	this.chart = (chart.toArray())[0]; // get dom element from jquery
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

	//console.log(["points",points]);

	//var halfWidth = Math.ceil((xdomain[1]-xdomain[0])/2);
	// x-scale
	this.x = d3.scale.linear()
		.domain(this.options.xdomain)
		.range([0, this.size.width]);
  this.xpos = this.options.getXPos;

	// y-scale
	this.y = d3.scale.linear()
		.domain(this.options.ydomain)
		.nice()
		.range([this.size.height,0])
		.nice();
  this.ypos = this.options.getYPos;

  // color scale
  // assumes numeric domain
  this.color = d3.scale.quantize()
    .domain(this.options.colordomain)
    .range(this.options.colorscheme);

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

//
// HeatmapObj methods
//

HeatmapObj.prototype.zoomClick = function() {
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

HeatmapObj.prototype.canvasUpdate = function() {
	var self = this;
	this.ctx.beginPath();
	this.ctx.fillStyle = this.background;
	this.ctx.rect(0,0,this.cx,this.cy);
	this.ctx.fill();
	this.ctx.closePath();

	var points = self.points;
  var xw = this.options.boxwidth.x;
  var yw = this.options.boxwidth.y;
	for(var i=0; i < points.length;i++) {
		var d = points[i];
		var x = this.x(d[this.options.xname])+this.padding.left;
		var y = this.y(d[this.options.yname])+this.padding.top;
		
		this.ctx.beginPath();
 		this.ctx.fillStyle = this.color(d[this.options.zname]);
		this.ctx.fillRect(x,y, xw, yw);
		this.ctx.closePath();
	}
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

HeatmapObj.prototype.afterZoom = function() {
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
HeatmapObj.prototype.redraw = function() {
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

