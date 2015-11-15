// assumes that the ForeCache.Backend object is available
var ForeCache = ForeCache || {};
ForeCache.Renderer = ForeCache.Renderer || {};
ForeCache.Renderer.Vis.Heatmap = {}

/************* Classes *************/
/* ForeCache.Renderer.Vis.LineObj is a subclass, inheriting from ForeCache.Renderer.Vis.VisObj. */

//constructor code
/* chart parameter is a jquery object. */
ForeCache.Renderer.Vis.LineObj = function(chart, FCBackend, options) {
  ForeCache.Renderer.Vis.VisObj.call(this,chart,FCBackend,options);
};
ForeCache.Renderer.Vis.LineObj.prototype = Object.create(ForeCache.Renderer.Vis.VisObj.prototype);
ForeCache.Renderer.Vis.LineObj.prototype.constructor = ForeCache.Renderer.Vis.LineObj;

ForeCache.Renderer.Vis.LineObj.prototype.renderTile = function(tile) {
  var rows = tile.getSize();
  console.log(["rows",rows]);
  //TODO: this is a hack, maybe fix later?
  if(rows == 0) return; // don't render empty tiles...

	var start = true;
	var arcval = 2 * Math.PI;
	var prevx,prevy;
  var xt = 1.0 * tile.id.dimindices[0]*this.ts.tileWidths[0];
  //console.log(["tile",tile,xt,yt]);
	for(var i=0; i < rows;i++) {
    var xval = Number(tile.columns[this.xindex][i]) + xt;
    var yval = Number(tile.columns[this.yindex][i]);
		var x = this.x(xval)+this.padding.left;
		var y = this.y(yval)+this.padding.top;
		
		if(start) {
			start = false;
		} else {
			this.ctx.beginPath();
			this.ctx.moveTo(prevx,prevy);
 			this.ctx.strokeStyle = this.fillStyle;
			this.ctx.lineTo(x,y);
			this.ctx.stroke();
			this.ctx.closePath();
		}
		this.ctx.beginPath();
 		this.ctx.fillStyle = this.fillStyle;
		this.ctx.arc(x,y, 3, 0, arcval, false);
 		this.ctx.fill();
		this.ctx.closePath();

		prevx = x;
		prevy = y;
	}
  var xmin = this.x(tile.id.dimindices[0] * this.ts.tileWidths[0]) + this.padding.left;
  var xmax = this.x((tile.id.dimindices[0]+1) * this.ts.tileWidths[0]) + this.padding.left;
  var ymin = this.padding.top;
  var ymax = this.size.height + this.padding.top;
  console.log(["drawing lines",xmin,xmax,ymin,ymax]);

	this.ctx.beginPath();
 	this.ctx.strokeStyle = "black";
	this.ctx.moveTo(xmin,ymin);
	this.ctx.lineTo(xmin,ymax);
	this.ctx.stroke();

	this.ctx.moveTo(xmax,ymin);
	this.ctx.lineTo(xmax,ymax);
	this.ctx.stroke();
	this.ctx.closePath();
};

/****************** Helper Functions *********************/
