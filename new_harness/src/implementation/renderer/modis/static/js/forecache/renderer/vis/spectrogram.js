// assumes that the ForeCache.Backend object is available
var ForeCache = ForeCache || {};
ForeCache.Renderer = ForeCache.Renderer || {};
ForeCache.Renderer.Vis.Spectrogram = {}

/************* Classes *************/
/* ForeCache.Renderer.Vis.SpectrogramObj is a subclass, inheriting from
 * ForeCache.Renderer.Vis.HeatmapObj. */

//constructor code
/* chart parameter is a jquery object. */
ForeCache.Renderer.Vis.SpectrogramObj = function(chart, options) {
  ForeCache.Renderer.Vis.HeatmapObj.call(this,chart,options);
};
ForeCache.Renderer.Vis.SpectrogramObj.prototype = Object.create(ForeCache.Renderer.Vis.HeatmapObj.prototype);
ForeCache.Renderer.Vis.SpectrogramObj.prototype.constructor = ForeCache.Renderer.Vis.SpectrogramObj;


ForeCache.Renderer.Vis.HeatmapObj.prototype.renderTile = function(tile) {
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
    var zval = Number(tile.columns[this.zindex][i]);
		var x = this.x(xval)+this.padding.left;
		var y = this.y(yval)+this.padding.top;
    if(this.inverted.x) { // shift back in pixel space to account for inversion
      x -= xw;
    }
    if(this.inverted.y) {
      y -= yw;
    }
		
		this.ctx.beginPath();
 		//this.ctx.fillStyle = this.color(tile,i);
    //this.ctx.fillStyle = this.spc_canvas(this.get_amplitude(tile,i,true));
    this.ctx.fillStyle = this.spc_canvas(this.log_scale_normalized(this.get_amplitude(tile,i,true),this.vAmpRange));
    if(i > (50*513 - 10)) {
      if(i < (50*513+20)) {
    var num = this.log_scale_normalized(this.get_amplitude(tile,i,true),this.vAmpRange) * 8.0;
  var fractional = num % 1.0;
  var fnum = Math.floor(num);
  console.log(["get amp",this.get_amplitude(tile,i,true),"vamp",this.vAmpRange,"amplitude",this.log_scale_normalized(this.get_amplitude(tile,i,true),this.vAmpRange),"fnum",fnum,"fnum==0.0?",fnum==0.0,"fractional",fractional,"color",this.ctx.fillStyle]);
  console.log(["color",this.spc(this.get_amplitude(tile,i,true)),this.webgl_to_canvas(this.spc(this.get_amplitude(tile,i,true))),this.webgl_to_canvas_string(this.spc(this.get_amplitude(tile,i,true)))]);
  console.log(this.spc(this.log_scale_normalized(this.get_amplitude(tile,i,true),this.vAmpRange)));
      }
    }
		this.ctx.fillRect(x,y, xw, yw);
		this.ctx.closePath();
	}
  var xmin = this.x(tile.id.dimindices[0] * this.ts.tileWidths[0]) + this.padding.left;
  var ymin = this.y(tile.id.dimindices[1] * this.ts.tileWidths[1]) + this.padding.top;
  var xmax = this.x((tile.id.dimindices[0]+1) * this.ts.tileWidths[0]) + this.padding.left;
  ymax = this.y((tile.id.dimindices[1]+1) * this.ts.tileWidths[1]) + this.padding.top;
  console.log(["tile",tile.id.zoom,tile.id.dimindices,"drawing lines",xmin,xmax,ymin,ymax]);

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
};

ForeCache.Renderer.Vis.SpectrogramObj.prototype.modifyColor = function() {
  if(this.scaleType.hasOwnProperty("color")) {
    if(this.scaleType.color === "log") {
      var count = 0;
      this.vAmpRange = ForeCache.Backend.Structures.getDomain(this.tileMap.getTiles(),this.zindex);
      if(this.vAmpRange[0] > this.vAmpRange[1]) {
        var low = this.vAmpRange[1];
        var high = this.vAmpRange[0];
        this.vAmpRange = [low,high];
      }
      this.vAmpRange[0] = this.log_scale(this.vAmpRange[0]);
      this.vAmpRange[1] = this.log_scale(this.vAmpRange[1]);
      this.color = function(tile,i) {
        var col = this.spc_canvas(this.log_scale_normalized(this.get_amplitude(tile,i,true),this.vAmpRange));
        //console.log(["color",col]);
        return col;
      };
    }
  } else {
    this.color = function(tile,i) {
      return this.spc_canvas(this.get_amplitude(tile,i,false));
    };
  }
};

// assume we only want to navigate in 1 direction, even though there are two dimensions
ForeCache.Renderer.Vis.SpectrogramObj.prototype.getZoomDims = function() {
  return 1;
};

/****************** Helper Functions *********************/


// this function is a modified version of getAmplitude function
// from http://github.com/joshblum/eeg-toolkit in file spec-viewer.html
// assumes that ydomain = [0,512]
ForeCache.Renderer.Vis.SpectrogramObj.prototype.get_amplitude = function(tile,i,uselog) {
  // yt should evaluate to 0 anyway, so removed it
  //var yt = 1.0 * tile.id.dimindices[1]*this.ts.tileWidths[1];
  var index = Number(tile.columns[this.yindex][i]);// + yt;
  var newindex = i;
  if(uselog) {
    var diff = this.options.ydomain[1] - this.options.ydomain[0];
    if(diff == 0) { // don't divide by zero
      diff = 1.0;
    }
    //console.log(["diff",diff,"ydomain",this.options.ydomain]);
    newindex = Math.pow(diff,Math.min(1.0,1.0*index/diff)) / diff;
    //newindex += 1.0/512.0; // now on a 0 to 1 scale
    // map back to a 0 to 512 scale
    var mapping = newindex * 512.0;
    //console.log(["mapping",mapping]);
    var low = Math.floor(mapping); // change into the two nearest integers
    var high = Math.ceil(mapping);
    var lowdiff = mapping - low;
    var highdiff = high - mapping;
    if(lowdiff <= highdiff) { // lower index is closer
      newindex = low;
    } else { // higher index is closer
      newindex = high;
    }
    // make sure it's not outside the ydomain
    newindex = Math.min(this.options.ydomain[1],newindex);
    newindex = Math.max(this.options.ydomain[0],newindex);
  }
  // figure out where this is inside of the tile
  var idiff = newindex - index;
  //console.log(["old index",index,"newindex",newindex,"old i",i,"new i",i+idiff]);
  //console.log(["old amplitude",tile.columns[this.zindex][i],"new amplitude",tile.columns[this.zindex][i+idiff]]);
  return tile.columns[this.zindex][i+idiff];
}

// copy of the logScale function from http://github.com/joshblum/eeg-toolkit
// in file spec-viewer.html
ForeCache.Renderer.Vis.SpectrogramObj.prototype.log_scale_normalized = function(amplitude,vAmpRange) {
  var value = this.log_scale(amplitude);//20.0*this.log10(amplitude);
  value = Math.max(value, vAmpRange[0]);
  value = Math.min(value, vAmpRange[1]);
  value = (value - vAmpRange[0]) / (vAmpRange[1] - vAmpRange[0]);
  return value;
}

ForeCache.Renderer.Vis.SpectrogramObj.prototype.log_scale = function(amplitude,vAmpRange) {
  return 20.0*this.log10(amplitude);
}

// copy of the physicalColor function from http://github.com/joshblum/eeg-toolkit
// in file spec-viewer.html
// generates webgl color values (need to convert to canvas)
ForeCache.Renderer.Vis.SpectrogramObj.prototype.spectrogram_physical_color = function(amplitude) {
  var num = amplitude * 8.0;
  var fractional = num % 1.0;
  var fnum = Math.floor(num);
  //console.log(["amplitude",amplitude,"fnum",fnum,"fractional",fractional]);
  if(fnum == 0.0) {
    return [fractional,0,0];
  } else if (fnum == 1.0) {
    return [1,fractional,0];
  } else if (fnum == 2.0) {
    return [1-fractional,1,0];
  } else if (fnum == 3.0) {
    return [0,1,fractional];
  } else if (fnum == 4.0) {
    return [0,1-fractional,1]; } else if (fnum == 5.0) {
    return [fractional,0,1];
  } else if (fnum == 6.0) {
    return [1,1-fractional,1];
  } else {
    return [1,1,1];
  }
}

// shorthand function name for ease of use
ForeCache.Renderer.Vis.SpectrogramObj.prototype.spc = function(amplitude) {
  return this.spectrogram_physical_color(amplitude);
};

ForeCache.Renderer.Vis.SpectrogramObj.prototype.spc_canvas = function(amplitude) {
  return this.webgl_to_canvas_string(this.spc(amplitude));
};

// converts RGB values for WebGL to canvas equivalent values
// WebGL goes from (0,0,0) to (1,1,1)
// canvas goes from (0,0,0) to (255,255,255)
ForeCache.Renderer.Vis.SpectrogramObj.prototype.webgl_to_canvas = function(rgb) {
  var r = Math.min(255,Math.round(rgb[0]*255));
  var g = Math.min(255,Math.round(rgb[1]*255));
  var b = Math.min(255,Math.round(rgb[2]*255));
  return [r,g,b];
};

// makes a proper string to pass to a canvas context
ForeCache.Renderer.Vis.SpectrogramObj.prototype.webgl_to_canvas_string = function(oldrgb) {
  var rgb = this.webgl_to_canvas(oldrgb);
  return 'rgb(' + rgb.join(',') + ')';
};
