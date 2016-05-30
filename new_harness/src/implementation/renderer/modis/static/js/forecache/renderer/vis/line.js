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
  this.name = "line";
  this.useUsMap = false;
  this.removeDuplicates = true;
};
ForeCache.Renderer.Vis.LineObj.prototype = Object.create(ForeCache.Renderer.Vis.VisObj.prototype);
ForeCache.Renderer.Vis.LineObj.prototype.constructor = ForeCache.Renderer.Vis.LineObj;

ForeCache.Renderer.Vis.LineObj.prototype.renderRowsHelper = function(rows,xts,yts) {
  var self = this;
 	var start = true;
	var arcval = 2 * Math.PI;
	var prevx = null;
  var prevy = null;

  this.ctx.save();
  this.ctx.beginPath();
  this.ctx.strokeStyle = this.fillStyle;
  this.ctx.fillStyle = this.fillStyle;
	for(var i = 0; i < rows.length; i++) {
    var xt = 1.0 * xts[i]*this.tileManager.getDimTileWidth(this.xindex);
    var xval = Number(rows[i][this.xindex]) + xt;
    var yval = Number(rows[i][this.yindex]);
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

/*
  var xmin = this.x(tile.id.dimindices[this.xindex] * this.tileManager.getDimTileWidth(this.xindex)) + this.padding.left;
  var xmax = this.x((tile.id.dimindices[this.xindex]+1) * this.tileManager.getDimTileWidth(this.xindex)) + this.padding.left;
  var ymin = this.padding.top;
  var ymax = this.size.height + this.padding.top;
  //console.log(["drawing lines",xmin,xmax,ymin,ymax]);

  this.ctx.beginPath();
  this.ctx.setLineDash([6,3]);
 	this.ctx.strokeStyle = "black";
	this.ctx.moveTo(xmin,ymin);
	this.ctx.lineTo(xmin,ymax);

	this.ctx.moveTo(xmax,ymin);
	this.ctx.lineTo(xmax,ymax);
	this.ctx.stroke();
	this.ctx.closePath();
*/
  this.ctx.restore();
  // do the final stuff
  var totalTiles = this.tileManager.totalTiles();
  for(var i = 0; i < totalTiles; i++) {
    var tile = this.tileManager.getTile(i);
    this.doneRenderingTile(tile);
  }
};

ForeCache.Renderer.Vis.LineObj.prototype.renderRowsHelper2 = function(rows,xts,yts) {
  var self = this;
  var prevState = {};
  if(!this.previousRenderState.rows) { // make new state
 	  prevState.start = true;
	  prevState.arcval = 2 * Math.PI;
	  prevState.prevx = null;
    prevState.prevy = null;
    prevState.i = 0;
    this.previousRenderState.rows = prevState;
  } else { // use previous state
    prevState = this.previousRenderState.rows;
  }
  this.ctx.save();
  this.ctx.beginPath();
  this.ctx.strokeStyle = this.fillStyle;
  this.ctx.fillStyle = this.fillStyle;
	for(var i = 0; (i < 1000) && (prevState.i < rows.length);prevState.i++,i++) {
    var xt = 1.0 * xts[i]*this.tileManager.getDimTileWidth(this.xindex);
    var xval = Number(rows[prevState.i][this.xindex]) + xt;
    var yval = Number(rows[prevState.i][this.yindex]);
		var x = this.x(xval)+this.padding.left;
		var y = this.y(yval)+this.padding.top;
		
		if(prevState.start) {
			prevState.start = false;
		} else {
			this.ctx.moveTo(prevState.prevx,prevState.prevy);
			this.ctx.lineTo(x,y);
		}
    this.ctx.arc(x,y, 3, 0, prevState.arcval, false);

		prevState.prevx = x;
		prevState.prevy = y;
	}
	this.ctx.stroke();
 	this.ctx.fill();
  this.ctx.closePath();

/*
  var xmin = this.x(tile.id.dimindices[this.xindex] * this.tileManager.getDimTileWidth(this.xindex)) + this.padding.left;
  var xmax = this.x((tile.id.dimindices[this.xindex]+1) * this.tileManager.getDimTileWidth(this.xindex)) + this.padding.left;
  var ymin = this.padding.top;
  var ymax = this.size.height + this.padding.top;
  //console.log(["drawing lines",xmin,xmax,ymin,ymax]);

  this.ctx.beginPath();
  this.ctx.setLineDash([6,3]);
 	this.ctx.strokeStyle = "black";
	this.ctx.moveTo(xmin,ymin);
	this.ctx.lineTo(xmin,ymax);

	this.ctx.moveTo(xmax,ymin);
	this.ctx.lineTo(xmax,ymax);
	this.ctx.stroke();
	this.ctx.closePath();
*/
  this.ctx.restore();
  // do the final stuff
  if(prevState.i == rows.length) {
    delete this.previousRenderState.rows;
    var totalTiles = this.tileManager.totalTiles();
    for(var i = 0; i < totalTiles; i++) {
      var tile = this.tileManager.getTile(i);
      this.doneRenderingTile(tile);
    }
    return;
  } else {
	  this.ctx.closePath();
    this.ctx.restore(); // just clean up
  }
  console.log("rendered 1000 points");
  prevState.nextAnimationRequest = window.requestAnimationFrame(function(){self.renderRowsHelper(rows);});
};

ForeCache.Renderer.Vis.LineObj.prototype.renderTileHelper = function(tile) {
  var self = this;
  var prevState = {};
  if(!this.previousRenderState[tile.id.name]) { // make new state
 	  prevState.start = true;
	  prevState.arcval = 2 * Math.PI;
	  prevState.prevx = null;
    prevState.prevy = null;
    prevState.xt = 1.0 * tile.id.dimindices[this.xindex]*this.tileManager.getDimTileWidth(this.xindex);
    prevState.i = 0;
    prevState.rows = tile.getSize();
    this.previousRenderState[tile.id.name] = prevState;
  } else { // use previous state
    prevState = this.previousRenderState[tile.id.name];
  }
  this.ctx.save();
  this.ctx.beginPath();
  this.ctx.strokeStyle = this.fillStyle;
  this.ctx.fillStyle = this.fillStyle;
	for(var i = 0; (i < 1000) && (prevState.i < prevState.rows);prevState.i++,i++) {
    var xval = Number(tile.columns[this.xindex][prevState.i]) + prevState.xt;
    var yval = Number(tile.columns[this.yindex][prevState.i]);
		var x = this.x(xval)+this.padding.left;
		var y = this.y(yval)+this.padding.top;
		
		if(prevState.start) {
			prevState.start = false;
		} else {
			this.ctx.moveTo(prevState.prevx,prevState.prevy);
			this.ctx.lineTo(x,y);
		}
    this.ctx.arc(x,y, 3, 0, prevState.arcval, false);

		prevState.prevx = x;
		prevState.prevy = y;
	}
	this.ctx.stroke();
 	this.ctx.fill();
  this.ctx.closePath();

  var xmin = this.x(tile.id.dimindices[this.xindex] * this.tileManager.getDimTileWidth(this.xindex)) + this.padding.left;
  var xmax = this.x((tile.id.dimindices[this.xindex]+1) * this.tileManager.getDimTileWidth(this.xindex)) + this.padding.left;
  var ymin = this.padding.top;
  var ymax = this.size.height + this.padding.top;
  //console.log(["drawing lines",xmin,xmax,ymin,ymax]);

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
  // do the final stuff
  if(prevState.i == prevState.rows) {
    delete this.previousRenderState[tile.id.name];
    this.doneRenderingTile(tile);
    return;
  } else {
	  this.ctx.closePath();
    this.ctx.restore(); // just clean up
  }
  console.log("rendered 1000 points");
  prevState.nextAnimationRequest = window.requestAnimationFrame(function(){self.renderTileHelper(tile);});
};

ForeCache.Renderer.Vis.LineObj.prototype.renderTile = function(tile) {
  var rows = tile.getSize();
  //console.log(["rows",rows,"tile",tile]);
  //TODO: this is a hack, maybe fix later?
  if(rows == 0) {
    this.doneRenderingTile(tile);
    return; // don't render empty tiles...
  }
  this.renderTileHelper(tile);
/*
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
  this.doneRenderingTile(tile);
*/
};

/****************** Helper Functions *********************/
