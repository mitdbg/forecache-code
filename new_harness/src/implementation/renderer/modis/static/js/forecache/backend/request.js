var ForeCache = ForeCache || {};
ForeCache.Backend = ForeCache.Backend || {};
ForeCache.Backend.Request = {};

ForeCache.Backend.Request.URL = "http://modis.csail.mit.edu:10001/forecache/modis/fetch/";

ForeCache.Backend.Request.simpleReset = function(callback) {
  var dat = {};
  ForeCache.Backend.Request.reset(dat,callback);
};

ForeCache.Backend.Request.reset = function(dat,callback) {
  dat.reset=true;
  ForeCache.Backend.Request.sendRequest(dat,callback);
};

ForeCache.Backend.Request.getView = function(callback) {
  var dat = {};
  dat.getview=true;
  ForeCache.Backend.Request.sendJsonRequest(dat,callback);
};

ForeCache.Backend.Request.getTileStructure = function(callback) {
  var dat = {};
  dat.getts=true;
  var createTileStructure = function(jsondata) {
    var aggregationWindows = [];
    var tileWidths = [];
    var totalLevels = jsondata.aggregationWindows.length;
    var numdims = jsondata.tileWidths.length;
    for(var z = 0; z < totalLevels; z++) {
      var windows = [];
      for(var d = 0; d < numdims; d++) {
        windows.push(parseInt(jsondata.aggregationWindows[z][d]));
      }
      aggregationWindows.push(windows);
    }
    for(var d = 0; d < numdims; d++) {
        tileWidths.push(parseInt(jsondata.tileWidths[d]));
    }
    var ts = new ForeCache.Backend.Structures.TileStructure(aggregationWindows,tileWidths);
    callback(ts);
  };
  ForeCache.Backend.Request.sendJsonRequest(dat,createTileStructure);
};


ForeCache.Backend.Request.getTileBinary = function(tileid,callback) {
var dat = {};
  dat.binary = true;
  dat.zoom = tileid.zoom;
  dat.tile_id = tileid.dimindices.join("_");
  var createTile = function(arrayBuffer) {
    console.log(["arrayBuffer length",arrayBuffer.byteLength]);
    var tdecoder = new ForeCache.Backend.TileDecoder(arrayBuffer);
    var tile = tdecoder.unpackTile();
    callback(tile); // return the tile object
  };
  ForeCache.Backend.Request.sendBinaryRequest(dat,createTile);
};

// uses JSON string format to retrieve tile from server
// returns a ForeCache.Backend.Request.Tile object
ForeCache.Backend.Request.getTileJson = function(tileid,callback) {
  var dat = {};
  dat.json = true;
  dat.zoom = tileid.zoom;
  dat.tile_id = tileid.dimindices.join("_");
  var createTile = function(jsondata) {
    var columns = jsondata.data;
    var attributes = jsondata.attributes;
    var dataTypes = jsondata.dataTypes;
    var tile = new ForeCache.Backend.Structures.Tile(columns,attributes,dataTypes,tileid);
    callback(tile); // return the tile object
  };
  //ForeCache.Backend.Request.sendJsonRequest(dat,createTile);
  ForeCache.Backend.Request.sendJsonRequestNoJquery(dat,createTile);
};

// convenience method, chooses JSON or binary format for you
ForeCache.Backend.Request.getTile = function(tileid,callback) {
  ForeCache.Backend.Request.getTileJson(tileid,callback);
  //ForeCache.Backend.Request.getTileBinary(tileid,callback);
};

// retrieves all tiles, then calls the finalcallback function with
// the list of retrieved tiles
ForeCache.Backend.Request.getTiles = function(tileids,finalcallback) {
  var i = 0;
  var tiles = [];
  var processNextTile = function(tile) {
    tiles.push(tile);
    i++;
    if(i < tileids.length) {
      var tid = tileids[i]; // get the next id to retrieve
      ForeCache.Backend.Request.getTile(tid,processNextTile); // repeat loop
    } else { // all tiles have been processed
      finalcallback(tiles);
    }
  };
  var startingid = tileids[0];
  ForeCache.Backend.Request.getTile(startingid,processNextTile);
};

// calls tilecallback function with each individual tile
// then calls the finalcallback function with the final list of tiles
// when retrieval is finished. final callback function is optional
ForeCache.Backend.Request.getAndProcessTiles = function(tileids,tilecallback,finalcallback) {
  if(tileids.length == 0) {
    return;
  }
  var dofc = arguments.length == 3;
  var i = 0;
  var tiles = [];
  var processNextTile = function(tile) { // ForeCache.Backend.Request.Tile object
    tilecallback(tile);
    tiles.push(tile);
    i++;
    if(i < tileids.length) {
      var tid = tileids[i]; // get the next id to retrieve
      ForeCache.Backend.Request.getTile(tid,processNextTile); // repeat loop
    } else { // all tiles have been processed
      if(dofc) {
        finalcallback(tiles);
      }
    }
  };
  var startingid = tileids[0];
  ForeCache.Backend.Request.getTile(startingid,processNextTile);
};

/************** Helper Functions ***************/
ForeCache.Backend.Request.uuid = function() {
    var d = new Date().getTime();
    var uuid = 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
        var r = (d + Math.random()*16)%16 | 0;
        d = Math.floor(d/16);
        return (c=='x' ? r : (r&0x3|0x8)).toString(16);
    });
    return uuid;
};

ForeCache.Backend.Request.sendRequest = function(dat,callback) {
  //$.get(ForeCache.Backend.Request.URL,dat,callback);
  ForeCache.Backend.Request.sendRequestHelper(dat,"",callback);
};

ForeCache.Backend.Request.sendJsonRequest = function(dat,callback) {
  //$.getJSON(ForeCache.Backend.Request.URL,dat,callback);
  ForeCache.Backend.Request.sendJsonRequestNoJquery(dat,callback);
};

// IE 10 does not support req.responeType = 'json'
// just manually getting string and parsing as json
ForeCache.Backend.Request.sendJsonRequestNoJquery = function(dat,callback) {
  var jsonParse = function(jsonstring) {
    var jsondata = JSON.parse(jsonstring);
    callback(jsondata);
    console.log(["jsondata",jsondata]);
  };
  ForeCache.Backend.Request.sendRequestHelper(dat,"",jsonParse);
};

ForeCache.Backend.Request.sendBinaryRequest = function(dat,callback) {
  ForeCache.Backend.Request.sendRequestHelper(dat,"arraybuffer",callback);
};

// function written to replace the $.param function from jQuery.
// I don't need a fancy params building function, so I chose to eliminate
// the jQuery dependency.
// shallow parsing of parameters in the dictionary passed as input (i.e., dat)
// shallow = absolutely no nested parsing, so don't pass anything crazy!
ForeCache.Backend.Request.buildParams = function(dat) {
  var parsedParams = [];
  for(var key in dat) {
    var val = dat[key];
    if((val == null) || (typeof(val) === 'undefined')) {
      val = "";
    }
    parsedParams.push(encodeURIComponent(key) + "=" + encodeURIComponent(val));
  }
  return parsedParams.join("&");
};

ForeCache.Backend.Request.sendRequestHelper = function(dat,responseType,callback,errorCallback) {
  //var paramsString = $.param(dat);
  var paramsString = ForeCache.Backend.Request.buildParams(dat);
  var newUrl = ForeCache.Backend.Request.URL + "?" + paramsString;
  var oReq = new XMLHttpRequest();
  oReq.open("GET",newUrl, true); // async = true
  oReq.responseType = responseType;
  oReq.onload = function (oEvent) {
    if(oReq.readyState == 4) { // state = DONE
      var reqStatus = oReq.status;
      if(reqStatus == 200) { // status = OK
        var data = oReq.response;
        if(responseType === "") { // assume we want the DOM string instead
          data = oReq.responseText;
        }
        callback && callback(data);
      } else {
        errorCallback && errorCallback(reqStatus);
      }
    }
  };

  oReq.send();
};

// converts nested array in row-major format to nested array in column-major format
ForeCache.Backend.Request.rowsToColumns = function(rows) {
  if(rows.length == 0) {
    return [];
  }
  var columns = [];
  var numcols = rows[0].length;
  for(var c = 0; c < numcols; c++) {
    columns.push([]);
  }
  for(var r = 0; r < rows.length; r++) {
    for(var c = 0; c < numcols; c++) {
      columns[c].push(rows[r][c]);
    }
  }
  return columns;
};
