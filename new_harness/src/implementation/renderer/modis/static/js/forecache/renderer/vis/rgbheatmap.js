// assumes that the ForeCache.Backend object is available
var ForeCache = ForeCache || {};
ForeCache.Renderer = ForeCache.Renderer || {};
ForeCache.Renderer.Vis.Heatmap = {}

/************* Classes *************/
/* ForeCache.Renderer.Vis.RGBHeatmapObj is a subclass, inheriting from ForeCache.Renderer.Vis.VisObj. */

//constructor code
/* chart parameter is a jquery object. */
ForeCache.Renderer.Vis.RGBHeatmapObj = function(chart, options) {
  ForeCache.Renderer.Vis.VisObj.call(this,chart,options);
/*
{i} red_max,green_max,blue_max
{0} 0.889761,0.862038,0.897079

{i} red_min,green_min,blue_min
{0} 0.020844,0.0294212,0.0520942
*/

  //landsat: 0.07058823529412,0.835294
  this.redBrightnessScale = d3.scale.linear()
    //.domain([0,0.889761]) // original
    .domain([0.02,0.54]) // manually white-balanced
    //.domain([0.,0.835294]) // landsat, 13 points below original
    //.domain([0.070588,0.835294])
    .range([0, 255]);
  //landsat: 0.07843137254902,0.7921568627451
  this.greenBrightnessScale = d3.scale.linear()
    //.domain([0,0.862038]) // original
    .domain([0.029,0.54]) // manually white-balanced
    //.domain([0.,0.792157]) // landsat
    //.domain([0.,0.80784]) // chosen to be 13 points below original
    //.domain([0.078431,0.792157])
    .range([0, 255]);
  //landsat: 0.09411764705882,0.78823529411765
  this.blueBrightnessScale = d3.scale.linear()
    //.domain([0,0.897079]) // original
    .domain([0.052,0.62]) // manually white-balanced
    //.domain([0.,0.788235]) // landsat
    //.domain([0.,0.843137]) // chosen to be 13 points below original
    //.domain([0.094118,0.788235])
    .range([0, 255]);
  this.bezierMap = bezierMap;
  this.redBezierMap = getRedBezierMap();
  this.greenBezierMap = getGreenBezierMap();
  this.blueBezierMap = getBlueBezierMap();
  console.log(["red bezier map",this.redBezierMap]);
  };
ForeCache.Renderer.Vis.RGBHeatmapObj.prototype = Object.create(ForeCache.Renderer.Vis.VisObj.prototype);
ForeCache.Renderer.Vis.RGBHeatmapObj.prototype.constructor = ForeCache.Renderer.Vis.RGBHeatmapObj;

ForeCache.Renderer.Vis.RGBHeatmapObj.prototype.updateOpts = function() {
  // call the base class
  ForeCache.Renderer.Vis.VisObj.prototype.updateOpts.call(this); 

  var newopts = this.options;
  newopts.boxwidth = {};

  xstats = this.get_stats(this.xindex);
  //console.log(["xdomain",newopts.xdomain,"xstats",xstats]);
  //newopts.xdomain = [xstats.min,xstats.max];
  //newopts.xdomain = this.adjustForViewportRatio(newopts.xdomain);
  var xm = xstats.mindist; // width of box
  var xd = xstats.max - xstats.min; // space for boxes in domain
  var xw = newopts.size.width; // space for boxes in range
  if(xm > 0) {
    var numboxes = Math.ceil(xd / xm) + 1;
    var boxwidth = Math.floor(1.0 * xw / numboxes);
    if(boxwidth < 1) { // can't have a fraction of a pixel!
      boxwidth = 1; // make it 1 pixel
    }
    newopts.boxwidth.x = Math.max(Math.round(boxwidth / this.viewportRatio),1);
    newopts.size.width = numboxes*boxwidth; // make the width more realistic
    newopts.width = newopts.padding.left + newopts.padding.right + newopts.size.width;
    //console.log([xstats,xm,xd,xw,numboxes,boxwidth]);
  }

  ystats = this.get_stats(this.yindex);
  //console.log(["ydomain",newopts.ydomain,"ystats",ystats]);
  //newopts.ydomain = [ystats.min,ystats.max];
  //newopts.ydomain = this.adjustForViewportRatio(newopts.ydomain);
  //console.log(ystats);
  var ym = ystats.mindist; // height of box
  var yd = ystats.max - ystats.min; // space for boxes in domain
  var yw = newopts.size.height; // space for boxes in range
  if(ym > 0) {
    var numboxes = Math.ceil(yd / ym) + 1;
    var boxwidth = Math.floor(1.0*yw / numboxes);
    if(boxwidth < 1) { // can't have a fraction of a pixel!
      boxwidth = 1; // make it 1 pixel
    }
    newopts.boxwidth.y = Math.max(Math.round(boxwidth / this.viewportRatio),1);
    newopts.size.height = numboxes*boxwidth; // make the height more realistic
    newopts.height = newopts.padding.top + newopts.padding.bottom + newopts.size.height;
    //console.log([ystats,ym,yd,yw,numboxes,boxwidth]);
  }
  //console.log(["updated width",newopts.size.width,"updated height",newopts.size.height]);
};

ForeCache.Renderer.Vis.RGBHeatmapObj.prototype.renderTile = function(tile) {
  console.log(["tile",tile,xt,yt,this.options.boxwidth.x,this.options.boxwidth.y]);
  var rows = tile.getSize();
  //TODO: this is a hack, maybe fix later?
  if(rows == 0) return; // don't render empty tiles...
  var xw = this.options.boxwidth.x;
  var yw = this.options.boxwidth.y;
  var xt = 1.0 * tile.id.dimindices[0]*this.ts.tileWidths[0];
  var yt = 1.0 * tile.id.dimindices[1]*this.ts.tileWidths[1];
  //console.log(["tile",tile,xt,yt]);
	for(var i=0; i < rows;i++) {
    var xval = Number(tile.columns[this.xindex][i]) + xt;
    var yval = Number(tile.columns[this.yindex][i]) + yt;
    //var zval = tile.columns[this.zindex][i];
		var x = this.x(xval)+this.padding.left;
		var y = this.y(yval)+this.padding.top;
    if(this.inverted.x) { // shift back in pixel space to account for inversion
      x -= xw;
    }
    if(this.inverted.y) {
      y -= yw;
    }
		
		this.ctx.beginPath();
   
/*
 		this.ctx.fillStyle = "rgb("
      +Number(Math.max(0,Math.min(255,Math.floor(255*tile.columns[Number(tile.getIndex("red"))][i]))))+","
      +Number(Math.max(0,Math.min(255,Math.floor(255*tile.columns[Number(tile.getIndex("green"))][i]))))+","
      +Number(Math.max(0,Math.min(255,Math.floor(255*tile.columns[Number(tile.getIndex("blue"))][i]))))+")";
*/

/*
 		this.ctx.fillStyle = "rgb("
      +this.bezierMap[Math.max(0,Math.min(255,Math.floor(255*tile.columns[Number(tile.getIndex("red"))][i])))]+","
      +this.bezierMap[Math.max(0,Math.min(255,Math.floor(255*tile.columns[Number(tile.getIndex("green"))][i])))]+","
      +this.bezierMap[Math.max(0,Math.min(255,Math.floor(255*tile.columns[Number(tile.getIndex("blue"))][i])))]+")";
*/


/*
    this.ctx.fillStyle = "rgb("+
      Math.min(255,Math.max(0,Math.floor(this.redBrightnessScale(tile.columns[Number(tile.getIndex("red"))][i]))))+","+
      Math.min(255,Math.max(0,Math.floor(this.greenBrightnessScale(tile.columns[Number(tile.getIndex("green"))][i]))))+","+
      Math.min(255,Math.max(0,Math.floor(this.blueBrightnessScale(tile.columns[Number(tile.getIndex("blue"))][i]))))+")";
*/


    this.ctx.fillStyle = "rgb("+
      this.redBezierMap[Math.min(255,Math.max(0,Math.floor(this.redBrightnessScale(tile.columns[Number(tile.getIndex("red"))][i]))))]+","+
      this.greenBezierMap[Math.min(255,Math.max(0,Math.floor(this.greenBrightnessScale(tile.columns[Number(tile.getIndex("green"))][i]))))]+","+
      //Math.min(255,Math.max(0,Math.floor(this.blueBrightnessScale(tile.columns[Number(tile.getIndex("blue"))][i]))))+")";
      this.blueBezierMap[Math.min(255,Math.max(0,Math.floor(this.blueBrightnessScale(tile.columns[Number(tile.getIndex("blue"))][i]))))]+")";

    //console.log(["fill style:",this.ctx.fillStyle,x,y,xw,yw]);
		this.ctx.fillRect(x,y, xw, yw);
		this.ctx.closePath();
	}
  var xmin = this.x(tile.id.dimindices[0] * this.ts.tileWidths[0]) + this.padding.left;
  var ymin = this.y(tile.id.dimindices[1] * this.ts.tileWidths[1]) + this.padding.top;
  var xmax = this.x((tile.id.dimindices[0]+1) * this.ts.tileWidths[0]) + this.padding.left;
  ymax = this.y((tile.id.dimindices[1]+1) * this.ts.tileWidths[1]) + this.padding.top;
  //console.log(["tile",tile.id.zoom,tile.id.dimindices,"drawing lines",xmin,xmax,ymin,ymax]);

	this.ctx.beginPath();
 	this.ctx.strokeStyle = "black";
	this.ctx.moveTo(xmin,ymin);
	this.ctx.lineTo(xmin,ymax);
	this.ctx.stroke();

	this.ctx.moveTo(xmax,ymin);
	this.ctx.lineTo(xmax,ymax);
	this.ctx.stroke();

  this.ctx.moveTo(xmin,ymin);
	this.ctx.lineTo(xmax,ymin);
	this.ctx.stroke();

  this.ctx.moveTo(xmin,ymax);
	this.ctx.lineTo(xmax,ymax);
	this.ctx.stroke();
	this.ctx.closePath();


/*
  var xb = xw - 1;
  var yb = yw - 1;

  for(var i = (xmin-xb); i <= (xmax+xb); i++) {
  	this.ctx.beginPath();
 		this.ctx.fillStyle = "black";
		this.ctx.fillRect(i,ymin-yb, 1, 1);
		this.ctx.fillRect(i,ymax+yb, xw, yw);
		this.ctx.closePath();
  }
  for(var j = (ymin-yb); j <= (ymax+yb); j++) {
  	this.ctx.beginPath();
 		this.ctx.fillStyle = "black";
		this.ctx.fillRect(xmin-xb,j, xw, yw);
		this.ctx.fillRect(xmax+xb,j, xw, yw);
		this.ctx.closePath();
  }
*/
};


ForeCache.Renderer.Vis.RGBHeatmapObj.prototype.modifyColor = function() {
  this.color = this.colorScale;
};

/****************** Helper Functions *********************/

// computs max, min, and min dist between any two points for the given column
// computes stats across all current tiles
ForeCache.Renderer.Vis.RGBHeatmapObj.prototype.get_stats = function(index) {
  var stats = {};
  for(var i = 0; i < this.currentTiles.length; i++) {
    var id = this.currentTiles[i];
    var col = this.tileMap.get(id).columns[index];
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
ForeCache.Renderer.Vis.RGBHeatmapObj.prototype.get_stats_helper = function(col) {
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
