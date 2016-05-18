var ForeCache = ForeCache || {};
ForeCache.Backend = ForeCache.Backend || {};
ForeCache.Backend.Structures = {};

/************** Helper Functions ***************/
// traverses a set of tiles and computes domains based on tile position
ForeCache.Backend.Structures.getTileDomain = function(tiles,ts,index) {
  var domain = [];
  for(var i = 0; i < tiles.length; i++) {
    var tile = tiles[i];
    var min = tile.id.dimindices[index]*ts.tileWidths[index];
    var max = (tile.id.dimindices[index]+1)*ts.tileWidths[index]-1;
    var tempdomain = [min,max];
    if(domain.length == 0) {
      domain = tempdomain;
    } else {
      if(domain[0] > tempdomain[0]) {
        domain[0] = tempdomain[0];
      }
      if(domain[1] < tempdomain[1]) {
        domain[1] = tempdomain[1];
      }
    }
  }
  return domain;
};

// traverses a list of tiles to compute the overall domain for the given index
ForeCache.Backend.Structures.getDomain = function(tiles,index) {
  var domain = [];
  for(var i = 0; i < tiles.length; i++) {
    var tile = tiles[i];
    if(tile.getSize() != 0) {
      var tempdomain = tile.getDomain(index);
      if(domain.length == 0) {
        domain = tempdomain;
      } else {
        if(domain[0] > tempdomain[0]) {
          domain[0] = tempdomain[0];
        }
        if(domain[1] < tempdomain[1]) {
          domain[1] = tempdomain[1];
        }
      }
    }
  }
  return domain;
};

/************* Classes *************/

// MultiDimTileKey object
ForeCache.Backend.Structures.MultiDimTileKey = function(dimindices,zoom) {
  this.dimindices = dimindices; // array
  this.zoom = zoom; // array, for dim groups
  this.name = ["zoom",zoom.join("_"),"pos",dimindices.join("_")].join("_");
};

// NewTileKey object
ForeCache.Backend.Structures.NewTileKey = function(dimindices,zoom) {
  this.dimindices = dimindices; // array
  this.zoom = zoom; // int
  this.name = [zoom,dimindices.join("_")].join("_");
};

// TileMap object
ForeCache.Backend.Structures.TileMap = function() {
  this.tiles = {};
};

ForeCache.Backend.Structures.TileMap.prototype.batchInsert = function(tiles) {
  for(var i = 0; i < tiles.length; i++) {
    this.insert(tiles[i]);
  }
};


ForeCache.Backend.Structures.TileMap.prototype.get = function(id) {
  if(this.containsKey(id)) {
    return this.tiles[id.name];
  }
  return null;
};

ForeCache.Backend.Structures.TileMap.prototype.insert = function(tile) {
  this.tiles[tile.id.name] = tile;
  console.log(["inserting tile",tile.id.name,tile]);
};

ForeCache.Backend.Structures.TileMap.prototype.containsKey = function(id) {
  return this.tiles.hasOwnProperty(id.name);
};

ForeCache.Backend.Structures.TileMap.prototype.remove = function(id) {
  if(this.containsKey(id)) {
    delete this.tiles[id.name];
  }
};

ForeCache.Backend.Structures.TileMap.prototype.getTiles = function() {
  var self = this;
  var values = Object.keys(this.tiles).map(function(key){
    return self.tiles[key];
  });
  return values;
};

ForeCache.Backend.Structures.TileMap.prototype.clear = function() {
  this.tiles = {};
};

// Tile object
ForeCache.Backend.Structures.Tile = function(columns,attributes,dataTypes,id) {
  this.columns = columns;
  this.attributes = attributes;
  this.dataTypes = dataTypes;
  this.id = id;
};

ForeCache.Backend.Structures.Tile.prototype.getIndex = function(name) {
  return this.attributes.indexOf(name);
};

ForeCache.Backend.Structures.Tile.prototype.getSize = function() {
  if(this.columns.length == 0) return 0;
  return this.columns[0].length;
};

ForeCache.Backend.Structures.Tile.prototype.getDomain = function(index) {
  var col = this.columns[index];
	var domain = [];
	if(col.length > 0) {
		domain = [Number(col[0]),Number(col[0])];
  	for(var i = 1; i < col.length; i++) {
    	var val = Number(col[i]);
			if(domain[0] > val) domain[0] = val;
			if(domain[1] < val) domain[1] = val;
		}
	}
	return domain;
};

// TileStructure object
// NOTE: It is not safe to access these fields directly. only tileWidths and numdims makes sense
// to access directly from this object.
ForeCache.Backend.Structures.TileStructure = function(aggregationWindows,tileWidths) {
  this.jsonFields = ["aggregationWindows","tileWidths"];
  this.aggregationWindows = aggregationWindows;
  this.tileWidths = tileWidths;
  this.numdims = this.tileWidths.length;
  this.totalLevels = this.aggregationWindows.length;
};

// given the current zoom position, return the new zoom position
// automatically fixes the zoom level if the user would zoom past accepted bounds
ForeCache.Backend.Structures.TileStructure.prototype.getNewZoomPosition =
function(oldZoomPos, dimIndices,diffs) { // ignores dimIndices
  var newZoomPos = oldZoomPos;
  newZoomPos += diffs[0];
  return this.makeValid(newZoomPos);
};

// did the zoom level actually change?
ForeCache.Backend.Structures.TileStructure.prototype.zoomLevelChanged = function(oldZoomPos,newZoomPos) {
  return oldZoomPos != newZoomPos;
};

// for this dimension index, get the new midpoint of the viewport
ForeCache.Backend.Structures.TileStructure.prototype.getNewMid =
function(oldZoomPos,newZoomPos,dimIndex,oldMid) {
  var newWindow = this.getAggregationWindow(newZoomPos,dimIndex);
  var oldWindow = this.getAggregationWindow(oldZoomPos,dimIndex);
  //var newWindow = this.aggregationWindows[newZoomPos][dimIndex];
  //var oldWindow = this.aggregationWindows[oldZoomPos][dimIndex];
  return 1.0 * oldMid * oldWindow / newWindow;
};

// get the aggregation window for the given dimension and zoom level
ForeCache.Backend.Structures.TileStructure.prototype.getAggregationWindow =
function(zoomPos,dimIndex) {
  var w = this.aggregationWindows[zoomPos][dimIndex];
  return w;
};

// make the given zoom position into a valid zoom level
ForeCache.Backend.Structures.TileStructure.prototype.makeValid = function(zoomPos) {
    if(zoomPos < 0) {
      zoomPos = 0;
    }
    if(zoomPos == this.totalLevels) {
      zoomPos--;
    }
  return zoomPos;
};




// MultiDimTileStructure object
ForeCache.Backend.Structures.MultiDimTileStructure = function(aggregationWindows,dimensionGroups,tileWidths) {
  this.jsonFields = ["aggregationWindows","dimensionGroups","tileWidths"];
  // lvl 1 = dimension groups, lvl 2 = zoom levels, lvl 3 = aggregation window per dim in group
  this.aggregationWindows = aggregationWindows;
  // lvl 1 = dimension groups, lvl 2 = dimensions in group
  this.dimensionGroups = dimensionGroups;
  this.tileWidths = tileWidths; // per dimension
  this.numdims = this.tileWidths.length;
  this.totalLevels = this.aggregationWindows.length; // per dimension group
  this.totalLevels = [];
  for(var i = 0; i < this.aggregationWindows.length; i++) { // per dimension group
    this.totalLevels.push(this.aggregationWindows[i].length); // count the zoom levels for this dim group
  }

  // used to quickly look up each dimension's corresponding dimension group
  this.dimGroupMap = {};
  for(var i = 0; i < this.dimensionGroups.length; i++) { // for each dimension group
    var group = this.dimensionGroups[i]
    for(var j = 0; j < group.length; j++) { // for each dimension in the group
      this.dimGroupMap[group[j]] = {'groupId':i,'groupPos':j}; // record group id and position for each dimension
    }
  }
};

// given the current zoom position, return the new zoom position
// automatically fixes the zoom level if the user would zoom past accepted bounds
ForeCache.Backend.Structures.MultiDimTileStructure.prototype.getNewZoomPosition =
function(oldZoomPos, dimIndices,diffs) { // ignores dimIndices
  var newZoomPos = [];
  for(var i = 0; i < oldZoomPos.length; i++) {
    newZoomPos.push(oldZoomPos[i]);
  }
  var used = {}; // only update each dimension group once
  for(var i = 0; i < dimIndices.length; i++) { // for every dim index
    var groupId = this.dimGroupMap[dimIndices[i]].groupId; // get the group Id for this index
    if(!used.hasOwnProperty(groupId)) {
      newZoomPos[groupId] += diffs[i];
      used[groupId] = true; // done updating for this dimension group
    }
  }
  newZoomPos = this.makeValid(newZoomPos);
  console.log(["updating zoom position","old position",oldZoomPos,"new position",newZoomPos]);
  return newZoomPos;
};

// did the zoom level actually change?
ForeCache.Backend.Structures.MultiDimTileStructure.prototype.zoomLevelChanged = function(oldZoomPos,newZoomPos) {
  for(var i = 0; i < oldZoomPos.length; i++) {
    if(oldZoomPos[i] != newZoomPos[i]) {
      return true;
    }
  }
  return false;
};

// for this dimension index, get the new midpoint of the viewport
ForeCache.Backend.Structures.MultiDimTileStructure.prototype.getNewMid =
function(oldZoomPos,newZoomPos,dimIndex,oldMid) {
  // which dimension group does this dimension belong to?
  var gd = this.dimGroupMap[dimIndex];
// lvl 1 = dimension groups, lvl 2 = zoom levels, lvl 3 = aggregation window per dim in group
  // lvl 1 = dimension groups, lvl 2 = dimensions in group

  var newPos = newZoomPos[gd.groupId]; // what is the new zoom level for this dimension group?
  var oldPos = oldZoomPos[gd.groupId]; // what is the old zoom level for this dimension group?
  // get the aggregation windows for this dimension
  var newWindow = this.getAggregationWindow(newPos,dimIndex);
  var oldWindow = this.getAggregationWindow(oldPos,dimIndex);
  //var newWindow = this.aggregationWindows[gd.groupId][newPos][gd.groupPos];
  //var oldWindow = this.aggregationWindows[gd.groupId][oldPos][gd.groupPos];
  return 1.0 * oldMid * oldWindow / newWindow;
};

// get the aggregation window for the given dimension and zoom level
ForeCache.Backend.Structures.MultiDimTileStructure.prototype.getAggregationWindow =
function(zoomPos,dimIndex) {
  var gd = this.dimGroupMap[dimIndex];
  var w = this.aggregationWindows[gd.groupId][zoomPos][gd.groupPos];
  return w;
};

// make the given zoom position into a valid zoom level
ForeCache.Backend.Structures.MultiDimTileStructure.prototype.makeValid = function(zoomPos) {
  for(var i = 0; i < zoomPos.length; i++) {
    if(zoomPos[i] < 0) {
      zoomPos[i] = 0;
    }
    if(zoomPos[i] == this.totalLevels[i]) {
      zoomPos[i]--;
    }
  }
  return zoomPos;
};

