// assumes that the ForeCache.Backend object is available
var ForeCache = ForeCache || {};
ForeCache.Renderer = ForeCache.Renderer || {};
ForeCache.Renderer.Vis.Heatmap = {}

/************* Classes *************/
/* ForeCache.Renderer.Vis.LineObj is a subclass, inheriting from ForeCache.Renderer.Vis.VisObj. */

//constructor code
/* chart parameter is a jquery object. */
ForeCache.Renderer.Vis.LineObj = function(chart, options) {
  ForeCache.Renderer.Vis.VisObj.call(this,chart, options);
  this.dimensionality = 1; // overwrite default dimensionality variable
  this.useLegend = false;
  this.useUsMap = false;
};
ForeCache.Renderer.Vis.LineObj.prototype = Object.create(ForeCache.Renderer.Vis.VisObj.prototype);
ForeCache.Renderer.Vis.LineObj.prototype.constructor = ForeCache.Renderer.Vis.LineObj;

ForeCache.Renderer.Vis.LineObj.prototype.renderTile = function(tile) {
  var rows = tile.getSize();
  //console.log(["rows",rows,"tile",tile]);
  //TODO: this is a hack, maybe fix later?
  if(rows == 0) return; // don't render empty tiles...

	var start = true;
	var arcval = 2 * Math.PI;
	var prevx,prevy;
  var xt = 1.0 * tile.id.dimindices[this.xindex]*this.tileManager.getDimTileWidth(this.xindex);
  //console.log(["tile",tile,xt]);

  this.ctx.beginPath();
  this.ctx.strokeStyle = this.fillStyle;
  this.ctx.fillStyle = this.fillStyle;
	for(var i=0; i < rows;i++) {
    var xval = Number(tile.columns[this.xindex][i]) + xt;
    var yval = Number(tile.columns[this.yindex][i]);
		var x = this.x(xval)+this.padding.left;
		var y = this.y(yval)+this.padding.top;
		
		if(start) {
			start = false;
		} else {
			this.ctx.moveTo(prevx,prevy);
			this.ctx.lineTo(x,y);
		}
    this.ctx.arc(x,y, 3, 0, arcval, false);

		prevx = x;
		prevy = y;
	}
	this.ctx.stroke();
 	this.ctx.fill();
	this.ctx.closePath();

  var xmin = this.x(tile.id.dimindices[this.xindex] * this.tileManager.getDimTileWidth(this.xindex)) + this.padding.left;
  var xmax = this.x((tile.id.dimindices[this.xindex]+1) * this.tileManager.getDimTileWidth(this.xindex)) + this.padding.left;
  var ymin = this.padding.top;
  var ymax = this.size.height + this.padding.top;
  //console.log(["drawing lines",xmin,xmax,ymin,ymax]);

  this.ctx.save();
	this.ctx.beginPath();
  this.ctx.setLineDash([6,3]);
 	this.ctx.strokeStyle = "black";
	this.ctx.moveTo(xmin,ymin);
	this.ctx.lineTo(xmin,ymax);

	this.ctx.moveTo(xmax,ymin);
	this.ctx.lineTo(xmax,ymax);
	this.ctx.stroke();
	this.ctx.closePath();
  this.ctx.restore();
};

/****************** Helper Functions *********************/
