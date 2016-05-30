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
  ForeCache.globalTracker.appendToLog(ForeCache.Tracker.perInteractionLogName,
    {'interactionData':{"interactionType":"start","state":{},"viewName":null},
    'toFetch':this.currentTiles,'timestampMillis':fetchStart});
  ForeCache.Backend.Request.getTiles(self.currentTiles,function(tiles){
    var fetchEnd = Date.now();
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

// what is the index for this particular column name?
ForeCache.Backend.TileManager.prototype.getIndex = function(dimname) {
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

ForeCache.Backend.TileManager.prototype.makeBusy = function() {
  for(var i = 0; i < this.visObjects.length; i++) { // for each chart
    this.visObjects[i].makeBusy();
  }
};

ForeCache.Backend.TileManager.prototype.makeNotBusy = function() {
  for(var i = 0; i < this.visObjects.length; i++) { // for each chart
    this.visObjects[i].changed = false;
    this.visObjects[i].makeNotBusy();
  }
};

// called when a zoom happens on some visualization
ForeCache.Backend.TileManager.prototype.zoomClick =
function(viewName,dimIndices,diffs) {
  var dimRangeMap = {};
  var newzoom = this.ts.getNewZoomPosition(this.currentZoom,dimIndices,diffs);
  if(!this.ts.zoomLevelChanged(this.currentZoom,newzoom)) { // no change
    this.makeNotBusy();
    return false;
  }

  // make charts busy until done
  this.makeBusy();

  // zoom level changed, prepare to get new tiles
  var changed = [];
  for(var i = 0; i < this.ts.tileWidths.length; i++) {
    changed.push(false);
  }
  var newMids = {};
  var oldMids = {};
  for(var i = 0; i < this.visObjects.length; i++) { // for each chart
    var vObj = this.visObjects[i];

    // compute the new scale ranges
    var newXRangeResult = this.getDimRangeForZoom(this.currentZoom,newzoom,vObj.xindex,vObj.x,this.cacheSize);
    var newXRange = newXRangeResult.newDomain;
    // {"newDomain":newDomain,"newMid":newMid};
    newXRange = vObj.adjustForViewportRatio(newXRange);
    if(vObj.changed && !changed[vObj.xindex]) { // we found a new change
      changed[vObj.xindex] = true;
      dimRangeMap[vObj.xindex] = newXRange;
      newMids[vObj.xindex] = newXRangeResult.newMid;
      oldMids[vObj.xindex] = newXRangeResult.oldMid;
    } else if (!dimRangeMap.hasOwnProperty(vObj.xindex)) { // no change yet, but add range anyway
      dimRangeMap[vObj.xindex] = newXRange;
    }

    // do the same for y axis
    if(vObj.dimensionality == 2) {
      var newYRangeResult = this.getDimRangeForZoom(this.currentZoom,newzoom,vObj.yindex,vObj.y,this.cacheSize);
      var newYRange = newYRangeResult.newDomain;
      newYRange = vObj.adjustForViewportRatio(newYRange);
      if(vObj.changed && !changed[vObj.yindex]) { // we found a new change
        changed[vObj.yindex] = true;
        dimRangeMap[vObj.yindex] = newYRange;
        newMids[vObj.yindex] = newYRangeResult.newMid;
        oldMids[vObj.yindex] = newYRangeResult.oldMid;
      } else if (!dimRangeMap.hasOwnProperty(vObj.yindex)) { // no change yet, but add range anyway
        dimRangeMap[vObj.yindex] = newYRange;
      }
    }
  }

  // fill in the gaps
  for(var i = 0; i < this.ts.tileWidths.length; i++) {
    if(!dimRangeMap.hasOwnProperty(i)) {
      dimRangeMap[i] = this.getDimRangeForUnusedDim(this.currentZoom,newzoom,i,this.cacheSize);
    }
  }
  var interactionData =
{"interactionType":"zoom","state":{"oldMids":oldMids,"oldZoom":this.currentZoom,"newMids":newMids,"newZoom":newzoom},"viewName":viewName};
  // update current zoom level after performing calculations
  this.currentZoom = newzoom;

  // go and actually get the tiles
  return this.afterZoom(interactionData,dimRangeMap);
};

// interactionData: details about the interaction that triggered this call
// drm [optional]: calculated dimension ranges (passed by zoomClick function, not passed otherwise)
// this gets called any time a pan or zoom occurs
ForeCache.Backend.TileManager.prototype.afterZoom =
function(interactionData,drm) {
  var self = this;
  var dimRangeMap = {};
  // make charts busy until done
  this.makeBusy();
  var changed = [];
  for(var i = 0; i < this.ts.tileWidths.length; i++) {
    changed.push(false);
  }
  if(arguments.length == 2) { // dimRangeMap already built
    dimRangeMap = drm;
    for(var i = 0; i < this.visObjects.length; i++) { // for each chart
      var vObj = this.visObjects[i];
      if(vObj.changed) {
        changed[vObj.xindex] = true;
        if(vObj.dimensionality == 2) {
          changed[vObj.yindex] = true;
        }
      }
    }
  } else { // build the dimRangeMap
    for(var i = 0; i < this.visObjects.length; i++) { // for each chart
      var vObj = this.visObjects[i];
      // save the range info for later
      if(vObj.changed && !changed[vObj.xindex]) { // we found a new change
        changed[vObj.xindex] = true;
        dimRangeMap[vObj.xindex] = vObj.x.domain();
        //console.log(["found change in vis obj",i,"dimindex",vObj.xindex,"domain",vObj.x.domain()]);
      } else if (!dimRangeMap.hasOwnProperty(vObj.xindex)) { // no change yet, but add range anyway
        dimRangeMap[vObj.xindex] = vObj.x.domain();
      }

      // do the same for the y axis
      if(vObj.dimensionality == 2) {
        if(vObj.changed && !changed[vObj.yindex]) { // we found a new change
          changed[vObj.yindex] = true;
          dimRangeMap[vObj.yindex] = vObj.y.domain();
        } else if (!dimRangeMap.hasOwnProperty(vObj.yindex)) { // no change yet, but add range anyway
          dimRangeMap[vObj.yindex] = vObj.y.domain();
        }
      }
    }
  }
  // if something changed, update the other vis objects to match
  var cflag = changed.some(function(c){return c;});
  if(cflag) {
    for(var i = 0; i < this.visObjects.length; i++) { // for each chart
      var vObj = this.visObjects[i];
      vObj.changed = false;
      // update the vis objects to be consistent with each other
      if(dimRangeMap.hasOwnProperty(vObj.xindex)) {
        //console.log(["old range",vObj.x.domain(),"new range",dimRangeMap[vObj.xindex]]);
        vObj.x.domain(dimRangeMap[vObj.xindex]);
      }
      if(vObj.dimensionality == 2 && dimRangeMap.hasOwnProperty(vObj.yindex)) {
        vObj.y.domain(dimRangeMap[vObj.yindex]);
      }
    }
    // fill in the gaps for any missing dimensions
    for(var i = 0; i < this.ts.tileWidths.length; i++) {
      if(!dimRangeMap.hasOwnProperty(i)) {
        dimRangeMap[i] = this.getDimRangeForUnusedDim(this.currentZoom,this.currentZoom,i,this.cacheSize);
      }
    }
    // get tile ranges for every dimension, even ones not used by the charts
    var alltiles = [];
    for(var i = 0; i < this.ts.tileWidths.length; i++) {
      if(dimRangeMap.hasOwnProperty(i)) { // is a chart using it?
        alltiles.push(this.getTileRange(dimRangeMap[i],i));
      } else { // unused dim
        // fetch from the same position we've seen before by default
        alltiles.push([this.currentTiles[0].dimindices[i]]);
        //alltiles.push([0]); // just grab tile 0 along this dimension by default
      }
    }
    // get the new tile keys
    var newIDs = this.getFutureTiles(this.currentZoom,alltiles);
    //console.log(["newIDs",newIDs]);

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
    var fetchStart = Date.now();
    ForeCache.globalTracker.appendToLog(ForeCache.Tracker.perInteractionLogName,{'interactionData':interactionData,'toFetch':toFetch,'timestampMillis':fetchStart});

    if(toFetch.length > 0) {
      ForeCache.Backend.Request.getTiles(toFetch,function(tiles) {
        var fetchEnd = Date.now();
        //console.log(['time to fetch all tiles',fetchEnd-fetchStart]);
        self.tileMap.batchInsert(tiles); // insert the fetched tiles
        // tell vis objects to redraw themselves using the new tiles
        self.renderAllVisObjects();
      });
    } else { // change but no new tiles retrieved
      this.renderAllVisObjects();
    } 
    return true;
  } else { // nothing else to do
    this.makeNotBusy();
    return false;
  }
};

// removes duplicate rows for visObj
ForeCache.Backend.TileManager.prototype.getSpan = function(vObj) {
  var span = {};
  var rows = [];
  var xts = [];
  var yts = [];
  if(vObj.dimensionality == 1) {
    for(var i = 0; i < this.currentTiles.length; i++) {
      var tile = this.tileMap.get(this.currentTiles[i]);
      var xt = 1.0 * tile.id.dimindices[vObj.xindex]*this.getDimTileWidth(vObj.xindex);
      var rowCount = tile.getSize();
      for(var j = 0; j < rowCount; j++) {
        var xval = tile.columns[vObj.xindex][j]+xt;
/*
        if(j > (rowCount - 10)) {
          console.log(["j",j,"xval",xval]);
        }
*/
        if(!span.hasOwnProperty(xval)) {
          var row = []; 
          for(var k = 0; k < tile.columns.length; k++) {
            row.push(tile.columns[k][j]);
          }
          rows.push(row);
          xts.push(xt);
          span[xval] = true;
        }
      }
    }
  } else { // vObj.dimensionality == 2
    for(var i = 0; i < this.currentTiles.length; i++) {
      var tile = this.tileMap.get(this.currentTiles[i]);
      var xt = 1.0 * tile.id.dimindices[vObj.xindex]*this.getDimTileWidth(vObj.xindex);
      var yt = 1.0 * tile.id.dimindices[vObj.yindex]*this.getDimTileWidth(vObj.yindex);
      var rowCount = tile.getSize();
      for(var j = 0; j < rowCount; j++) {
        var xval = tile.columns[vObj.xindex][j]+xt;
        if(!span.hasOwnProperty(xval)) {
          var row = []; 
          for(var k = 0; k < tile.columns.length; k++) {
            row.push(tile.columns[k][j]);
          }
          rows.push(row);
          xts.push(xt);
          yts.push(xt);
          span[xval] = {};
          span[xval][yval] = true; 
        } else {
          var span2 = span[xval];
          var yval = tile.columns[vObj.yindex][j]+yt;
          if(!span2.hasOwnProperty(yval)) {
            var row = []; 
            for(var k = 0; k < tile.columns.length; k++) {
              row.push(tile.columns[k][j]);
            }
            rows.push(row);
            span[xval][yval] = true;
          }
        }
      }
    }
  }
/*
  for(var i = 0; i < rows.length && i < 10; i++) {
    console.log(["rows","i",i,rows[i],"x index",vObj.xindex]);
  }
*/
  //console.log(["non duplicate row count",rows.length]);
  return [rows,xts,yts];
};

ForeCache.Backend.TileManager.prototype.renderAllVisObjects = function() {
  var totalRows = 0;
  for(var i = 0; i < this.currentTiles.length; i++) {
    totalRows += this.tileMap.get(this.currentTiles[i]).getSize();
  }
  for(var i = 0; i < this.visObjects.length; i++) { // for each chart
    //var rows = this.getSpan(this.visObjects[i]);
    //console.log(["total non-duplicate rows",rows.length,"total Rows",totalRows]);
    this.visObjects[i].fixYDomain = true;
    this.visObjects[i].redraw()();
  }
};

ForeCache.Backend.TileManager.prototype.getDimTileWidth = function(dimindex) {
  return this.ts.tileWidths[dimindex];
};

ForeCache.Backend.TileManager.prototype.checkDoneRendering = function(visObj) {
  for(var i = 0; i < this.visObjects.length; i++) {
    if(!this.checkDoneRenderingForVisObj(this.visObjects[i])) {
      return false;
    }
  }
  return true;
};

ForeCache.Backend.TileManager.prototype.checkDoneRenderingForVisObj = function(visObj) {
  for(var j = 0; j < this.currentTiles.length; j++) {
    if(!visObj.doneRendering[this.currentTiles[j].name]) {
      return false;
    }
  }
  return true;
};

ForeCache.Backend.TileManager.prototype.setDoneRenderingFalse = function() {
  for(var i = 0; i < this.visObjects.length; i++) {
    this.visObjects[i].doneRendering = {}; // clear the rendering flags
    for(var j = 0; j < this.currentTiles.length; j++) {
      this.visObjects[i].doneRendering[this.currentTiles[j].name] = false;
    }
  }
};

ForeCache.Backend.TileManager.prototype.getDimRangeForUnusedDim =
function(currzoom,newzoom,dimindex,cacheSize) {
  var tile = null;
  for(var i = 0; i < this.currentTiles.length; i++) { // find first nonempty tile
    tile = this.tileMap.get(this.currentTiles[i]);
    if(tile.getSize() > 0) {
      break;
    }
  }
  var min = tile.id.dimindices[dimindex]*this.ts.tileWidths[dimindex];
  var max = (tile.id.dimindices[dimindex]+1)*this.ts.tileWidths[dimindex]-1;
  var domain = [min,max];

  var diff = domain[1]-domain[0];
  var mid = domain[0] + 1.0*(diff)/2; // midpoint
  //console.log(["xmid",xmid,"xdomain",xdomain]);

  var newMid = this.ts.getNewMid(currzoom,newzoom,dimindex,mid);
  var halfWidth = this.ts.tileWidths[dimindex] * cacheSize / 2.0;
  var newDomain = [newMid-halfWidth,newMid+halfWidth];
  //console.log(["dimindex",dimindex,"old mid",mid,"new mid",newMid]);
    
  return newDomain;
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
  //console.log(["dimindex",dimindex,"old mid",mid,"new mid",newMid]);
    
  return {"newDomain":newDomain,"newMid":newMid,"oldMid":mid};
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
