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

// NewTileKey object
ForeCache.Backend.Structures.NewTileKey = function(dimindices,zoom) {
  this.dimindices = dimindices;
  this.zoom = zoom;
  this.name = [zoom,dimindices.join("_")].join("_");
};

ForeCache.Backend.Structures.TileMap = function() {
  this.tiles = {};
};

// TileMap object
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
ForeCache.Backend.Structures.TileStructure = function(aggregationWindows,tileWidths) {
  this.aggregationWindows = aggregationWindows;
  this.tileWidths = tileWidths;
  this.numdims = this.tileWidths.length;
  this.totalLevels = this.aggregationWindows.length;
};
