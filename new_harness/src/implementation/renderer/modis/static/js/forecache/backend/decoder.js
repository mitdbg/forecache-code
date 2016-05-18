var ForeCache = ForeCache || {};
ForeCache.Backend = ForeCache.Backend || {};
ForeCache.Backend.TileDecoder = function(buffer) {
  // constants
  this.doubleSize = 8;
  this.encoding = 'utf-8';
  this.columnParsers = {
    "java.lang.Float": this.unpackFloatColumn,
    "java.lang.Integer": this.unpackIntegerColumn,
    "java.lang.Double": this.unpackDoubleColumn,
    "java.lang.Long": this.unpackLongColumn,
    "java.lang.String": this.unpackStringColumn,
    "java.lang.Boolean": this.unpackBooleanColumn,
    "java.lang.Character": this.unpackCharacterColumn
  };

  // initialization
  this.dv = new DataView(buffer);
  this.buffer = buffer;
  this.decoder = new TextDecoder(this.encoding);
};

ForeCache.Backend.TileDecoder.prototype.unpackMdTile = function() {
  var offset = 0;

  var res = this.unpackMdKey(offset);
  var id = res.result;
  offset = res.offset;
  console.log(["decoded md key",id,"new offset",offset]);

  var res = this.unpackAttributes(offset);
  offset = res.offset;
  var attributes = res.result;

  var res = this.unpackDataTypes(offset);
  var dataTypes = res.result;
  offset = res.offset;

  var cols = [];
  for(var i = 0; i < dataTypes.length; i++) {
    res = this.unpackColumn(offset,dataTypes[i]);
    offset = res.offset;
    cols.push(res.result);
  }
  //console.log(["attributes",attributes,"dataTypes",dataTypes,"cols",cols]);
  return new ForeCache.Backend.Structures.Tile(cols,attributes,dataTypes,id);
};

ForeCache.Backend.TileDecoder.prototype.unpackTile = function() {
  var offset = 0;

  var res = this.unpackKey(offset);
  var id = res.result;
  offset = res.offset;
  //console.log(["decoded key",id,"new offset",offset]);

  var res = this.unpackAttributes(offset);
  offset = res.offset;
  var attributes = res.result;

  var res = this.unpackDataTypes(offset);
  var dataTypes = res.result;
  offset = res.offset;

  var cols = [];
  for(var i = 0; i < dataTypes.length; i++) {
    res = this.unpackColumn(offset,dataTypes[i]);
    offset = res.offset;
    cols.push(res.result);
  }
  //console.log(["attributes",attributes,"dataTypes",dataTypes,"cols",cols]);
  return new ForeCache.Backend.Structures.Tile(cols,attributes,dataTypes,id);
};

ForeCache.Backend.TileDecoder.prototype.unpackDataTypes = function(offset) {
  var res = this.unpackStrings(offset);
  var offset = res.offset;
  var types = res.result;
  var dataTypes = types;
  return {"result":dataTypes,"offset":offset};
};

ForeCache.Backend.TileDecoder.prototype.unpackAttributes = function(offset) {
  return this.unpackStrings(offset);
};

ForeCache.Backend.TileDecoder.prototype.unpackStrings = function(offset) {
  var offset2 = offset;
  var totalstrings = this.getDouble(offset2);
  var finalStrings = [];
  offset2 += this.doubleSize;
  //console.log(["total strings",totalstrings, "current offset",offset2]);
  for(var i = 0; i < totalstrings; i++) {
    var strlen = this.getDouble(offset2); // string length in bytes
    offset2 += this.doubleSize;
    //var stringView = new DataView(this.buffer,offset2,strlen);
    var stringBytes = new Uint8Array(this.buffer,offset2,strlen);
    var currentString = this.decoder.decode(stringBytes);
    //console.log(currentString);
    finalStrings.push(currentString);
    offset2 += strlen;
  }
  return {"result":finalStrings,"offset":offset2};
};

ForeCache.Backend.TileDecoder.prototype.unpackMdKey = function(offset) {
  var offset2 = offset;
  // for zoom
  var zoomLength = this.getDouble(offset2);
  offset2 += this.doubleSize;
  var zoom = [];
  for(var i = 0; i < zoomLength; i++,offset2+=this.doubleSize) {
    zoom.push(this.getDouble(offset2));
  }
  // for dimindices
  var numvals = this.getDouble(offset2);
  offset2 += this.doubleSize;
  var dimindices = [];
  for(var i = 0; i < numvals; i++,offset2+= this.doubleSize) {
    dimindices.push(this.getDouble(offset2));
  }
  //console.log([zoom,dimindices,offset2]);
  return {"result": new ForeCache.Backend.Structures.MultiDimTileKey(dimindices,zoom), "offset":offset2};
};

ForeCache.Backend.TileDecoder.prototype.unpackKey = function(offset) {
  var offset2 = offset;
  // TODO: change this to make zoom levels an array
  var zoom = this.getDouble(offset2);
  offset2 += this.doubleSize;
  var numvals = this.getDouble(offset2);
  offset2 += this.doubleSize;
  var dimindices = [];
  for(var i = 0; i < numvals; i++,offset2+= this.doubleSize) {
    dimindices.push(this.getDouble(offset2));
  }
  //console.log([zoom,dimindices,offset2]);
  return {"result": new ForeCache.Backend.Structures.NewTileKey(dimindices,zoom), "offset":offset2};
};

/************** Column Parsers ***************/
ForeCache.Backend.TileDecoder.prototype.unpackColumn = function(offset,colType) {
  return this.columnParsers[colType].call(this,offset);
};

ForeCache.Backend.TileDecoder.prototype.unpackLongColumn = function(offset) {
  return this.unpackDoubleColumn(offset);
};

ForeCache.Backend.TileDecoder.prototype.unpackFloatColumn = function(offset) {
  return this.unpackDoubleColumn(offset);
};

ForeCache.Backend.TileDecoder.prototype.unpackIntegerColumn = function(offset) {
  return this.unpackDoubleColumn(offset);
};

ForeCache.Backend.TileDecoder.prototype.unpackStringColumn = function(offset) {
  return this.unpackStrings(offset);
};

ForeCache.Backend.TileDecoder.prototype.unpackDoubleColumn = function(offset) {
  var offset2 = offset;
  var numvals = this.getDouble(offset2);
  offset2 += this.doubleSize;
  var col = [];
  for(var i = 0; i < numvals; i++,offset2+=this.doubleSize) {
    var val = this.getDouble(offset2);
    //console.log(["double",val]);
    col.push(val);
  }
  return {"result":col,"offset":offset2};
};

ForeCache.Backend.TileDecoder.prototype.unpackBooleanColumn = function(offset) {
  var offset2 = offset;
  var numvals = this.getDouble(offset2);
  offset2 += this.doubleSize;
  var col = [];
  for(var i = 0; i < numvals; i++,offset2++) {
    var stringBytes = new Uint8Array(this.buffer,offset2,1);
    var currentString = this.decoder.decode(stringBytes);
    //console.log(["char",currentString,"result",currentString==="1"]);
    col.push(currentString === "1");
  }
  return {"result":col,"offset":offset2};
};

ForeCache.Backend.TileDecoder.prototype.unpackCharacterColumn = function(offset) {
  var offset2 = offset;
  var numvals = this.getDouble(offset2);
  offset2 += this.doubleSize;
  var col = [];
  for(var i = 0; i < numvals; i++,offset2++) {
    var stringBytes = new Uint8Array(this.buffer,offset2,1);
    var currentString = this.decoder.decode(stringBytes);
    //console.log(["char",currentString]);
    col.push(currentString);
  }
  return {"result":col,"offset":offset2};
};

/****************** Helper Functions ********************/
ForeCache.Backend.TileDecoder.prototype.getDouble = function(offset) {
  return this.dv.getFloat64(offset);
};
