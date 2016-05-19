// assumes that the ForeCache.Backend object is available
// assumes that the ForeCache.Tracker object is available
var ForeCache = ForeCache || {};
ForeCache.Backend = ForeCache.Backend || {};
ForeCache.Tracker = ForeCache.Tracker || {};

ForeCache.Backend.TileManager = function(options,visObjects) {
  var self = this;
  this.options = options || {};
  this.currentZoom = null;
  this.currentTiles = []; // used to keep track of the current tile keys
  this.tileMap = new ForeCache.Backend.Structures.TileMap();
  // cache size is saved by each individual visualization

  this.visObjects = visObjects || [];
  for(var i = 0; i < this.visObjects.length; i++) {
    this.visObjects[i].tileManager = this; // make sure the visObjects know where to find the tile manager
  }

  ForeCache.Backend.Request.getTileStructure(function(ts) {
    self.ts = ts; // store the tile structure
    console.log(["tile structure",self.ts]);
    var newTileWidths = options.tileWidths || ts.tileWidths;
    ForeCache.Backend.Request.updateTileWidths(ts, newTileWidths, function(changed) {
      if(changed) { // successfully updated tile widths
        self.ts.tileWidths = newTileWidths;
        console.log(["successfully changed tile widths to",newTileWidths]);
      }
      if(options.hasOwnProperty("startingPos")) {
        self.currentTiles = options.startingPos; // these better be tile keys
      } else {
        var dimindices = [];
        for(var i = 0; i < self.ts.numdims; i++) {
          dimindices.push(0);
        }
        //self.currentTiles = [new ForeCache.Backend.Structures.NewTileKey(dimindices,0)];
        self.currentTiles = [new ForeCache.Backend.Structures.MultiDimTileKey(dimindices,[0])];
      }
      console.log(["current tiles",self.currentTiles]);
      self.currentZoom = self.currentTiles[0].zoom;
      self.getStartingTiles(); // get the starting data
    });
};

ForeCache.Backend.TileManager.prototype.getStartingTiles = function() {
  var self = this;
  var fetchStart = Date.now();
  // get the starting data, then finish doing setup
  ForeCache.Backend.Request.getTiles(self.currentTiles,function(tiles){
    var fetchEnd = Date.now();
    ForeCache.globalTracker.appendToLog(ForeCache.Tracker.perInteractionLogName,{'action':'fetch','totalTiles':tiles.length,'start':fetchStart,'end':fetchEnd});
    console.log(['time to fetch all tiles',fetchEnd-fetchStart]);

    // store the starting tiles
    self.tileMap.batchInsert(tiles);
    //finish doing setup for each vis object
    for(var i = 0; i < self.visObjects.length; i++) {
      self.visObjects[i].finishSetup(tiles);
    }
  });
};

// get the data domain for this dimension across all tiles
ForeCache.Backend.TileManager.prototype.getTileDomain = function(dimindex) {
  return ForeCache.Backend.Structures.getTileDomain(this.tileMap.getTiles(),this.ts,dimindex);
};

// called when a zoom happens on some visualization
ForeCache.Backend.TileManager.prototype.zoomClick =
function(dimIndices,zoomDiff) {
  var dimRangeMap = {};
  // TODO: get the new zoom level
  //

  if(!self.ts.zoomLevelChanged(self.currentZoom,newZoom)) { // no change
    // set mouse not busy?
    return;
  }

  // zoom level changed, prepare to get new tiles
  this.currentZoom = newzoom;
  for(var i = 0; i < this.visObjects.length; i++) { // for each chart
    var vObj = this.visObjects[i];
    //TODO: identify dimensionality of the chart, so we know if yindex is necessary
    //

    // fetch the relevant indices
    var xindex = vObj.xindex;
    var xScale = vObj.x;
    var yindex = vObj.yindex;
    var yScale = vObj.y;
    // compute the new scale ranges
    var newXRange = this.getDimRangeForZoom(currzoom,newzoom,xindex,xScale,vObj.cacheSize);
    newXRange = vObj.adjustForViewportRatio(newXRange);
    var newYRange = this.getDimRangeForZoom(currzoom,newzoom,yindex,yScale,vObj.cacheSize);
    newYRange = vObj.adjustForViewportRatio(newYRange);
    // save the range info for later
    if(!dimRangeMap.hasOwnProperty(xindex)) {
      dimRangeMap[xindex] = newXRange;
    }
    if(!dimRangeMap.hasOwnProperty(yindex)) {
      dimRangeMap[yindex] = newYRange;
    }
    // tell the vObj to update itself in anticipation of the new tiles
    vObj.x.domain(newXRange);
    vObj.y.domain(newYRange);
  }
  // go and actually get the tiles
  this.afterZoom(currzoom,newzoom,dimRangeMap);
};

ForeCache.Backend.TileManager.prototype.afterZoom =
function(currzoom,newzoom,dimRangeMap) {
  // get tile ranges for every zoom level, even ones not used by the charts
  var alltiles = [];
  for(var i = 0; i < this.ts.tileWidths.length; i++) {
    if(this.dimRangeMap.hasOwnProperty(i)) { // is a chart using it?
      alltiles.push(this.getTileRange(dimRangeMap[i],i));
    } else { // unused dim
      allTiles.push([0]); // just grab tile 0 along this dimension by default
    }
  }
  // get the new tile keys
  var newIDs = getFutureTiles(newzoom,alltiles);

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
  //console.log(["proposed tiles",newIDs]);
  //console.log(["to fetch",toFetch.length,toFetch]);
  //console.log(["current tiles",self.currentTiles.length,self.currentTiles,self.tileMap]);
  if(toFetch.length > 0) {
    var fetchStart = Date.now();
    ForeCache.Backend.Request.getTiles(toFetch,function(tiles) {
      var fetchEnd = Date.now();
      ForeCache.globalTracker.appendToLog(ForeCache.Tracker.perInteractionLogName,{'action':'fetch','totalTiles':tiles.length,'start':fetchStart,'end':fetchEnd});
      console.log(['time to fetch all tiles',fetchEnd-fetchStart]);
      self.tileMap.batchInsert(tiles); // insert the fetched tiles
      // tell vis objects to redraw themselves using the new tiles
      for(var i = 0; i < self.visObjects.length; i++) { // for each chart
        self.visObjects[i].redraw()();
      }
    });
    return true;
  } else { // nothing else to do
    return false;
  }
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
  var minID = Math.floor(low / self.ts.tileWidths[dimindex]);
  var maxID = Math.floor(high / self.ts.tileWidths[dimindex]);
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
  var di = [];
  var tk = [];
  for(var i = 0; i < this.ts.numdims; i++) {
    di.push(alltiles[i][0]);
    tk.push(0);
  }
  var totalKeys = 1;
  for(var i = 0; i < alltiles.length; i++) { // compute the total number of expected keys
    totalKeys *= alltiles[i].length;
  }
  for(var i = 0; i < totalKeys; i++) { // create the tile key for each expected key
    var newkey = new ForeCache.Backend.Structures.MultiDimTileKey(di,zoomPos);
    futuretiles.push(newkey);
    // create the next key
    tk[0]++;
    var j = 0;
    for(; j < (tk.length - 1); j++) {
      if(tk[j] == alltiles[j].length) {
        tk[j]--;
        tk[j+1]++;
      }
      di[j] = alltiles[j][tk[j]];
    }
    di[j] = alltiles[j][tk[j]];
  }
  //console.log(futuretiles);
  return futuretiles;
};
