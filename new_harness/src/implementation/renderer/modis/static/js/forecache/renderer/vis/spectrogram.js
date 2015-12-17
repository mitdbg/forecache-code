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

ForeCache.Renderer.Vis.SpectrogramObj.prototype.modifyColor = function() {
  if(this.scaleType.hasOwnProperty("color")) {
    if(this.scaleType.color === "log") {
      //used to convert original inputs to log-scale values
      var logscale = d3.scale.log().domain(this.colorScale.domain());
      // modify the original color scale to take log-scale values as inputs
      this.colorScale.domain(logscale.range());
      this.color = function(zval) {
        //return this.colorScale(logscale(zval));
        return this.spc_canvas(logscale(zval));
      };
    }
  } else {
    this.color = function(zval) {
      //return this.colorScale(zval);
      return this.spc_canvas(zval);
    };
  }
};

/****************** Helper Functions *********************/

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
    return [0,1-fractional,1];
  } else if (fnum == 5.0) {
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
  var r = Math.floor(rgb[0]*255);
  var g = Math.floor(rgb[1]*255);
  var b = Math.floor(rgb[2]*255);
  return [r,g,b];
};

// makes a proper string to pass to a canvas context
ForeCache.Renderer.Vis.SpectrogramObj.prototype.webgl_to_canvas_string = function(oldrgb) {
  var rgb = this.webgl_to_canvas(oldrgb);
  return 'rgb(' + rgb.join(',') + ')';
};
