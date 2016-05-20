// assumes that the ForeCache.Backend object is available
// assumes that the ForeCache.Tracker object is available
var ForeCache = ForeCache || {};
ForeCache.Backend = ForeCache.Backend || {};
ForeCache.Tracker = ForeCache.Tracker || {};

// create the tile manager *after* creating all of the charts
// then create the tile manager and pass the charts as input (i.e., a list of vis objects)
// valid options to pass to the tile manager:
//  cacheSize: the width in tiles for each navigable axis of the charts
//  startingPos: list of tile keys representing the starting zoom level and position for all charts
//  viewportRatio: ratio between viewport range and true tile range
//    (e.g., 0.5 means viewport range is half of the true range, 
//    so users have to move twice as far to navigate past the true range)
ForeCache.Backend.TileManager = function(ts,view,visObjects,options) {
  var self = this;
  this.options = options || {};
  this.currentZoom = null;
  this.currentTiles = []; // used to keep track of the current tile keys
  this.tileMap = new ForeCache.Backend.Structures.TileMap();
  this.cacheSize = options.cacheSize || 1; // stored here to be consistent across charts
  this.viewportRatio = options.viewportRatio || 1.0; // stored here to be consistent across charts

  this.visObjects = visObjects || [];
  for(var i = 0; i < this.visObjects.length; i++) {
    this.visObjects[i].tileManager = this; // make sure the visObjects know where to find the tile manager
    this.visObjects[i].viewportRatio = this.viewportRatio;
  }
  this.ts = ts; // store the tile structure
  this.view = view; // store the view
  if(options.hasOwnProperty("startingPos")) {
    this.currentTiles = options.startingPos; // these better be tile keys
  } else { // create startingPos
    var dimindices = [];
    var zoomPos = [];
    for(var i = 0; i < this.ts.numdims; i++) {
      dimindices.push(0);
    }
    for(var i = 0; i < this.ts.dimensionGroups.length; i++) {
      zoomPos.push(0);
    }
    this.currentTiles = [new ForeCache.Backend.Structures.MultiDimTileKey(dimindices,zoomPos)];
  }
  this.currentZoom = this.currentTiles[0].zoom; // set the current zoom level
  ForeCache.Backend.Request.simpleReset(function() { // reset the backend
    ForeCache.Backend.Request.setView(self.view,function() { // set the current view
      ForeCache.Backend.Request.setTileStructure(self.ts,function() { // set the current tiling structure
        self.getStartingTiles(); // get the starting data
      });
    });
  });
};

ForeCache.Backend.TileManager.prototype.getStartingTiles = function() {
  var self = this;
  var fetchStart = Date.now();
  // get the starting data, then finish doing setup
  ForeCache.Backend.Request.getTiles(self.currentTiles,function(tiles){
    var fetchEnd = Date.now();
    ForeCache.globalTracker.appendToLog(ForeCache.Tracker.perInteractionLogName,{'action':'fetch','totalTiles':tiles.length,'start':fetchStart,'end':fetchEnd});
    //console.log(['time to fetch all tiles',fetchEnd-fetchStart]);

    // store the starting tiles
    self.tileMap.batchInsert(tiles);
    //finish doing setup for each vis object
    for(var i = 0; i < self.visObjects.length; i++) {
      self.visObjects[i].finishSetup();
    }
  });
};

// get the data domain for this dimension across all tiles
ForeCache.Backend.TileManager.prototype.getTileDomain = function(dimindex) {
  return ForeCache.Backend.Structures.getTileDomain(this.tileMap.getTiles(),this.ts,dimindex);
};

// get the data domain for this column across all tiles (may not be a dimension)
ForeCache.Backend.TileManager.prototype.getDomain = function(dimindex) {
  return ForeCache.Backend.Structures.getDomain(this.tileMap.getTiles(),dimindex);
};

// what is the dimension index for this particular dimension name?
ForeCache.Backend.TileManager.prototype.getDimIndex = function(dimname) {
  var tile0 = this.tileMap.get(this.currentTiles[0]);
  return tile0.getIndex(dimname);
};

// get a tile from the tile Map. i refers to index within
// currentTiles object, not the tile key!
ForeCache.Backend.TileManager.prototype.getTile = function(i) {
  var tilekey = this.currentTiles[i];
  return this.tileMap.get(tilekey);
};

ForeCache.Backend.TileManager.prototype.totalTiles = function() {
  return this.currentTiles.length;
};

// get aggregation window for this dimension index and the current zoom level
ForeCache.Backend.TileManager.prototype.getAggregationWindow = function(dimindex) {
  return this.ts.getAggregationWindow(this.currentZoom,dimindex);
};

// called when a zoom happens on some visualization
ForeCache.Backend.TileManager.prototype.zoomClick =
function(dimIndices,diffs) {
  var dimRangeMap = {};
  var newzoom = this.ts.getNewZoomPosition(this.currentZoom,dimIndices,diffs);
  if(!this.ts.zoomLevelChanged(this.currentZoom,newzoom)) { // no change
    for(var i = 0; i < this.visObjects.length; i++) { // for each chart
      this.visObjects[i].changed = false;
      this.visObjexts[i].makeNotBusy();
    }
    return false;
  }

  // make charts busy until done
  for(var i = 0; i < this.visObjects.length; i++) { // for each chart
    this.visObjects[i].makeBusy();
  }

  // zoom level changed, prepare to get new tiles
  for(var i = 0; i < this.visObjects.length; i++) { // for each chart
    var vObj = this.visObjects[i];

    // compute the new scale ranges
    var newXRange = this.getDimRangeForZoom(this.currentZoom,newzoom,vObj.xindex,vObj.x,this.cacheSize);
    newXRange = vObj.adjustForViewportRatio(newXRange);
    // save the range info for later
    if(vObj.changed && !dimRangeMap.hasOwnProperty(vObj.xindex)) {
      dimRangeMap[vObj.xindex] = newXRange;
    }

    // tell the vObj to update itself in anticipation of the new tiles
    vObj.x.domain(newXRange);
    vObj.fixYDomain = true;

    // do the same for y axis
    if(vObj.dimensionality == 2) {
      var yindex = vObj.yindex;
      var yScale = vObj.y;
      var newYRange = this.getDimRangeForZoom(this.currentZoom,newzoom,yindex,yScale,this.cacheSize);
      newYRange = vObj.adjustForViewportRatio(newYRange);
      if(vObj.changed && !dimRangeMap.hasOwnProperty(yindex)) {
        dimRangeMap[yindex] = newYRange;
      }
      vObj.y.domain(newYRange);
    }
  }
  // update current zoom level after performing calculations
  this.currentZoom = newzoom;

  // go and actually get the tiles
  return this.afterZoom(dimRangeMap);
};

// drm parameter is optional (passed by zoomClick function, not passed otherwise)
// this gets called any time a pan or zoom occurs
ForeCache.Backend.TileManager.prototype.afterZoom =
function(drm) {
  var self = this;
  var dimRangeMap = {};
  for(var i = 0; i < this.visObjects.length; i++) { // for each chart
    this.visObjects[i].makeBusy();
  }

  if(arguments.length == 1) { // dimRangeMap already built
    dimRangeMap = drm;
  } else { // build the dimRangeMap
    for(var i = 0; i < this.visObjects.length; i++) { // for each chart
      var vObj = this.visObjects[i];
      // save the range info for later
      if(vObj.changed && !dimRangeMap.hasOwnProperty(vObj.xindex)) {
        //console.log(["found change in vis obj",i,"dimindex",vObj.xindex,"domain",vObj.x.domain()]);
        dimRangeMap[vObj.xindex] = vObj.x.domain();
      }
      if(vObj.dimensionality == 2) {
        if(vObj.changed && !dimRangeMap.hasOwnProperty(vObj.yindex)) {
          dimRangeMap[vObj.yindex] = vObj.y.domain();
        }
      }
    }
  }
  // if anything changed, update the other vis objects to match
  var changed = false;
  for(var i = 0; i < this.visObjects.length; i++) { // for each chart
    var vObj = this.visObjects[i];
    vObj.changed = false;
    // update the vis objects to be consistent with each other
    if(dimRangeMap.hasOwnProperty(vObj.xindex)) {
      changed = true;
      //console.log(["old range",vObj.x.domain(),"new range",dimRangeMap[vObj.xindex]]);
      vObj.x.domain(dimRangeMap[vObj.xindex]);
    }
    if(vObj.dimensionality == 2 && dimRangeMap.hasOwnProperty(vObj.yindex)) {
      changed = true;
      // update the vis objects to be consistent with each other
      vObj.y.domain(dimRangeMap[vObj.yindex]);
    }
  }
  if(changed) { // if there was a change in one or more dimensions
    // get tile ranges for every dimension, even ones not used by the charts
    var alltiles = [];
    for(var i = 0; i < this.ts.tileWidths.length; i++) {
      if(dimRangeMap.hasOwnProperty(i)) { // is a chart using it?
        alltiles.push(this.getTileRange(dimRangeMap[i],i));
      } else { // unused dim
        alltiles.push([0]); // just grab tile 0 along this dimension by default
      }
    }
    // get the new tile keys
    var newIDs = this.getFutureTiles(this.currentZoom,alltiles);

    // get the new tiles to fetch
    var newTileMap = new ForeCache.Backend.Structures.TileMap();
    var toFetch = [];
    for(var i = 0; i < newIDs.length; i++) {
      var tileID = newIDs[i];
      if(!this.tileMap.containsKey(tileID)) {
        toFetch.push(tileID);
      } else {
        newTileMap.insert(this.tileMap.get(tileID));
      }
    }
    this.tileMap = newTileMap; //get rid of the stuff we don't need
    this.currentTiles = newIDs; // record the new list of tiles
    //console.log(["to fetch",toFetch.length,toFetch]);
    //console.log(["current tiles",this.currentTiles.length,this.currentTiles,this.tileMap]);
    if(toFetch.length > 0) {
      var fetchStart = Date.now();
      ForeCache.Backend.Request.getTiles(toFetch,function(tiles) {
        var fetchEnd = Date.now();
        ForeCache.globalTracker.appendToLog(ForeCache.Tracker.perInteractionLogName,{'action':'fetch','totalTiles':tiles.length,'start':fetchStart,'end':fetchEnd});
        //console.log(['time to fetch all tiles',fetchEnd-fetchStart]);
        self.tileMap.batchInsert(tiles); // insert the fetched tiles
        // tell vis objects to redraw themselves using the new tiles
        for(var i = 0; i < self.visObjects.length; i++) { // for each chart
          self.visObjects[i].redraw()();
        }
      });
    } else {
      for(var i = 0; i < this.visObjects.length; i++) { // for each chart
        this.visObjects[i].redraw()();
      }
    } 
    return true;
  } else { // nothing else to do
    for(var i = 0; i < this.visObjects.length; i++) { // for each chart
      this.visObjects[i].makeNotBusy();
    }
    return false;
  }
};

ForeCache.Backend.TileManager.prototype.getDimTileWidth = function(dimindex) {
  return this.ts.tileWidths[dimindex];
};

ForeCache.Backend.TileManager.prototype.getDimRangeForZoom =
function(currzoom,newzoom,dimindex,dimScale,cacheSize) {
  var domain = dimScale.domain(); // raw data values for this zoom level
  var diff = domain[1]-domain[0];
  var mid = domain[0] + 1.0*(diff)/2; // midpoint
  //console.log(["xmid",xmid,"xdomain",xdomain]);

  var newMid = this.ts.getNewMid(currzoom,newzoom,dimindex,mid);
  var halfWidth = this.ts.tileWidths[dimindex] * cacheSize / 2.0;
  var newDomain = [newMid-halfWidth,newMid+halfWidth];
  //console.log(["x","old mid",xmid,"new mid",newXmid]);
    
  return newDomain;
};

ForeCache.Backend.TileManager.prototype.getTileRange = function(dimRange,dimindex) {
  var low = Math.max(0,parseInt(dimRange[0],10));
  var high = Math.max(0,parseInt(dimRange[1],10));

  //console.log(["dimindex",dimindex,"low",low,"high",high]);
  var minID = Math.floor(low / this.ts.tileWidths[dimindex]);
  var maxID = Math.floor(high / this.ts.tileWidths[dimindex]);
  var newIDs = [];
  for(var tileID = minID; tileID < maxID; tileID++) {
    newIDs.push(tileID);
  }
  newIDs.push(maxID);
  return newIDs;
};

ForeCache.Backend.TileManager.prototype.getFutureTiles =
function(zoomPos,alltiles) {
  var futuretiles = [];
  var tk = [];
  for(var i = 0; i < this.ts.numdims; i++) {
    tk.push(0);
  }
  var totalKeys = 1;
  for(var i = 0; i < alltiles.length; i++) { // compute the total number of expected keys
    totalKeys *= alltiles[i].length;
  }
  for(var i = 0; i < totalKeys; i++) { // create the tile key for each expected key
    var di = [];
    for(var j = 0; j < tk.length; j++) {
      di.push(alltiles[j][tk[j]]);
    }
    var newkey = new ForeCache.Backend.Structures.MultiDimTileKey(di,zoomPos);
    futuretiles.push(newkey);
    //console.log(["newkey",newkey,"tk",tk]);
    // create the next key
    tk[0]++;
    var j = 0;
    for(; j < (tk.length - 1); j++) {
      if(tk[j] == alltiles[j].length) {
        tk[j] = 0;
        tk[j+1]++;
      }
      //console.log(["i",i,"j",j,"di[j]",di[j],"alltiles[j][tk[j]]",alltiles[j][tk[j]],"tk",tk,"alltiles",alltiles]);
    }
    //console.log(["i",i,"j",j,"di[j]",di[j],"alltiles[j][tk[j]]",alltiles[j][tk[j]],"tk",tk,"alltiles",alltiles]);
  }
  //console.log(["alltiles",alltiles,"future tiles",futuretiles]);
  //console.log(["future tiles",futuretiles]);
  return futuretiles;
};
