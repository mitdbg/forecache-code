// assumes that the ForeCache.Backend object is available
var ForeCache = ForeCache || {};
ForeCache.Renderer = ForeCache.Renderer || {};
ForeCache.Renderer.Vis = ForeCache.Renderer.Vis || {};

// used to create a unique identifier for visualization objects in the DOM
ForeCache.Renderer.Vis.getPrefix = function() { return "forecache-renderer-vis-";};

// returns a jquery object (a new div) containing the rendered visualization.
// The new div has a unique identifier taking the form: "forecache-renderer-vis-vistype-<UUID>".
// this function also appends the div to the given jquery node in the dom (a.k.a. "root")
ForeCache.Renderer.Vis.getVis = function(root,options,FCBackend) {
  var visType = options.visType;
  var name = ForeCache.Renderer.Vis.getPrefix()+visType+ForeCache.Backend.uuid();
  var visDiv = $("<div id=\""+name+"\"></div>").appendTo(root);
  var graph = ForeCache.Renderer.Vis.selectVis(visType,visDiv,FCBackend,options);
  return visDiv;
};

// dictionary mapping visualization types to their corresponding visualization objects
ForeCache.Renderer.Vis.__visTypes = {
  "heatmap":ForeCache.Renderer.Vis.HeatmapObj,
  "line":ForeCache.Renderer.Vis.LineObj
};


// returns the list of supported visualization types
ForeCache.Renderer.Vis.getSupportedVisTypes = function() {
  return Object.keys(visTypes);
};

// used to choose which visualization object to create
ForeCache.Renderer.Vis.selectVis = function(visType,visDiv,FCBackend,options) {
  var visTypes = ForeCache.Renderer.Vis.__visTypes;
  var keys = Object.keys(visTypes);
  for(var i = 0; i < keys.length; i++) {
    var typename = keys[i];
    if(visType === typename) {
      return new (visTypes[typename])(visDiv,FCBackend,options);
    }
  }
  console.log("visType not recognized. returning null...");
  return null;
};


