// assumes that the ForeCache.Backend object is available
var ForeCache = ForeCache || {};
ForeCache.Renderer = ForeCache.Renderer || {};
ForeCache.Renderer.Vis.Heatmap = {}

/************* Classes *************/
/* ForeCache.Renderer.Vis.HeatmapObj is a subclass, inheriting from ForeCache.Renderer.Vis.VisObj. */

//constructor code
/* chart parameter is a jquery object. */
ForeCache.Renderer.Vis.HeatmapObj = function(chart, options) {
  ForeCache.Renderer.Vis.VisObj.call(this,chart,options);
  this.dimensionality = 2; // overwrite default dimensionality variable
  this.useLegend = true;
  this.useUsMap = true;
};
ForeCache.Renderer.Vis.HeatmapObj.prototype = Object.create(ForeCache.Renderer.Vis.VisObj.prototype);
ForeCache.Renderer.Vis.HeatmapObj.prototype.constructor = ForeCache.Renderer.Vis.HeatmapObj;

ForeCache.Renderer.Vis.HeatmapObj.prototype.updateOpts = function() {
  // call the base class
  ForeCache.Renderer.Vis.VisObj.prototype.updateOpts.call(this); 

  var newopts = this.options;
  newopts.boxwidth = {};

  xstats = this.get_stats(this.xindex);
  //console.log(["xdomain",newopts.xdomain,"xstats",xstats]);
  //newopts.xdomain = [xstats.min,xstats.max];
  //newopts.xdomain = this.adjustForViewportRatio(newopts.xdomain);
  var xm = xstats.mindist; // width of box
  //var xd = xstats.max - xstats.min; // space for boxes in domain
  var xd = Math.abs(newopts.xdomain[0]-newopts.xdomain[1]); // space for boxes in domain
  var xw = newopts.size.width; // space for boxes in range
  if(xm > 0) {
    var numboxes = Math.ceil(xd / xm) + 1;
    var boxwidth = Math.floor(1.0 * xw / numboxes);
    if(boxwidth < 1) { // can't have a fraction of a pixel!
      boxwidth = 1; // make it 1 pixel
    }
    //newopts.boxwidth.x = Math.max(Math.round(boxwidth / this.viewportRatio),1);
    newopts.boxwidth.x = Math.max(boxwidth,1);//viewport ratio should be accounted for already
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
  var yd = Math.abs(newopts.ydomain[1]-newopts.ydomain[0]); // space for boxes in domain
  var yw = newopts.size.height; // space for boxes in range
  if(ym > 0) {
    var numboxes = Math.ceil(yd / ym) + 1;
    var boxwidth = Math.floor(1.0*yw / numboxes);
    if(boxwidth < 1) { // can't have a fraction of a pixel!
      boxwidth = 1; // make it 1 pixel
    }
    //newopts.boxwidth.y = Math.max(Math.round(boxwidth / this.viewportRatio),1);
    newopts.boxwidth.y = Math.max(boxwidth,1);//viewport ratio should be accounted for already
    newopts.size.height = numboxes*boxwidth; // make the height more realistic
    newopts.height = newopts.padding.top + newopts.padding.bottom + newopts.size.height;
    //console.log([ystats,ym,yd,yw,numboxes,boxwidth]);
  }
  //console.log(["updated width",newopts.size.width,"updated height",newopts.size.height]);
};

ForeCache.Renderer.Vis.HeatmapObj.prototype.renderTile = function(tile) {
  var rows = tile.getSize();
  //TODO: this is a hack, maybe fix later?
  if(rows == 0) return; // don't render empty tiles...
  var xw = this.options.boxwidth.x;
  var yw = this.options.boxwidth.y;
  var xt = 1.0 * tile.id.dimindices[this.xindex]*this.tileManager.getDimTileWidth(this.xindex);
  var yt = 1.0 * tile.id.dimindices[this.yindex]*this.tileManager.getDimTileWidth(this.yindex);
  //console.log(["dimindices[y]",tile.id.dimindices[this.xindex],"dimindices[y]",tile.id.dimindices[this.yindex]]);
  //console.log(["x tile width",this.tileManager.getDimTileWidth(this.xindex),
  //  "y tile width",this.tileManager.getDimTileWidth(this.yindex)]);
/*
  console.log(["tile",tile,"xt",xt,"yt",yt,
      "zdomain",this.colorScale.domain(),
      "xw",this.options.boxwidth.x,
      "xy",this.options.boxwidth.y]);
*/

  // partition data points by final color
  var rects = {};
  var colors = this.colorScale.range();
  for(var i = 0; i < colors.length ; i++) {
    rects[colors[i]]=[];
  }
	for(var i=0; i < rows;i++) {
    var xval = Number(tile.columns[this.xindex][i]) + xt;
    var yval = Number(tile.columns[this.yindex][i]) + yt;
    var zval = tile.columns[this.zindex][i];
		var x = this.x(xval)+this.padding.left;
		var y = this.y(yval)+this.padding.top;
    if(this.inverted.x) { // shift back in pixel space to account for inversion
      x -= xw;
    }
    if(this.inverted.y) {
      y -= yw;
    }
    var fillStyle = this.colorScale(zval);
    rects[fillStyle].push({"x":x,"y":y});
  }

  // render by color groups
  for(var i = 0; i < colors.length; i++) {
	  this.ctx.beginPath();
    var fillStyle = colors[i];
    this.ctx.fillStyle = fillStyle;
    for(var j = 0; j < rects[fillStyle].length; j++) {
      var p = rects[fillStyle][j];
		  this.ctx.rect(p.x,p.y,xw,yw);
    }
    this.ctx.fill();
    this.ctx.closePath();
  }

  var xmin = this.x(tile.id.dimindices[this.xindex] * this.tileManager.getDimTileWidth(this.xindex)) + this.padding.left;
  var ymin = this.y(tile.id.dimindices[this.yindex] * this.tileManager.getDimTileWidth(this.yindex)) + this.padding.top;
  var xmax = this.x((tile.id.dimindices[this.xindex]+1) * this.tileManager.getDimTileWidth(this.xindex)) + this.padding.left;
  var ymax = this.y((tile.id.dimindices[this.yindex]+1) * this.tileManager.getDimTileWidth(this.yindex)) + this.padding.top;
  //console.log(["tile",tile.id.zoom,tile.id.dimindices,"drawing lines",xmin,xmax,ymin,ymax]);

  this.ctx.save();
	this.ctx.beginPath();
  this.ctx.setLineDash([6,3]);
 	this.ctx.strokeStyle = "black";
  this.ctx.strokeWidth = 2;
	this.ctx.moveTo(xmin,ymin);
	this.ctx.lineTo(xmin,ymax);

	this.ctx.moveTo(xmax,ymin);
	this.ctx.lineTo(xmax,ymax);

  this.ctx.moveTo(xmin,ymin);
	this.ctx.lineTo(xmax,ymin);

  this.ctx.moveTo(xmin,ymax);
	this.ctx.lineTo(xmax,ymax);
	this.ctx.stroke();
	this.ctx.closePath();
  this.ctx.restore();


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


ForeCache.Renderer.Vis.HeatmapObj.prototype.modifyColor = function() {
  this.color = this.colorScale;
};

/****************** Helper Functions *********************/


