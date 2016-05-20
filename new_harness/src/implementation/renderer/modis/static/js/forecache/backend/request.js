var ForeCache = ForeCache || {};
ForeCache.Tracker = ForeCache.Tracker || {};
ForeCache.Backend = ForeCache.Backend || {};
ForeCache.Backend.Request = {};

ForeCache.Backend.Request.URL = "http://modis.csail.mit.edu:10001/forecache/modis/fetch/";

ForeCache.Backend.Request.simpleReset = function(callback) {
  var dat = {};
  ForeCache.Backend.Request.reset(dat,callback);
};

ForeCache.Backend.Request.reset = function(dat,callback) {
  dat.reset=true;
  ForeCache.Backend.Request.sendGetRequest(dat,callback);
};

ForeCache.Backend.Request.sendLogs = function(dat,callback) {
  dat.logs=true;
  ForeCache.Backend.Request.sendPostRequest(dat,callback);
};

ForeCache.Backend.Request.getView = function(callback) {
  var dat = {};
  dat.getview=true;
  ForeCache.Backend.Request.sendJsonGetRequest(dat,callback);
};

ForeCache.Backend.Request.updateTileWidths = function(oldts,tileWidths,callback) {
  var dat = {};
  dat.setts = true;
  //dat.ts = {"aggregationWindows":oldts.aggregationWindows,"tileWidths":tileWidths};
  console.log("updating tile widths in tile Structure");
  dat.ts = {};
  var properties = oldts.jsonFields;
  for(var i = 0; i < properties.length; i++) {
    console.log(["property",properties[i]]);
    dat.ts[properties[i]] = oldts[properties[i]];
  }
  dat.ts.tileWidths = oldts.tileWidths;
  var confirmTileStructure = function(jsondata) {
    callback(jsondata);
  };
  ForeCache.Backend.Request.sendJsonGetRequest(dat,confirmTileStructure);
};

ForeCache.Backend.Request.setView = function(newview,callback) {
  var dat = {};
  dat.setview = true;
  dat.view=newview;
  var confirmView = function(jsondata) {
    callback(jsondata === "true");
  };
  ForeCache.Backend.Request.sendJsonGetRequest(dat,confirmView);
};

ForeCache.Backend.Request.setTileStructure = function(newts,callback) {
  var dat = {};
  dat.setts = true;
  //dat.ts = {"aggregationWindows":newts.aggregationWindows,"tileWidths":newts.tileWidths};
  dat.ts = {};
  console.log("setting tile Structure");
  var properties = newts.jsonFields;
  console.log([properties,newts]);
  for(var i = 0; i < properties.length; i++) {
    console.log(["property",properties[i]]);
    dat.ts[properties[i]] = newts[properties[i]];
  }
  var confirmTileStructure = function(jsondata) {
    callback(jsondata === "true");
  };
  ForeCache.Backend.Request.sendJsonGetRequest(dat,confirmTileStructure);
};

ForeCache.Backend.Request.getTileStructure = function(callback) {
  var dat = {};
  dat.getts=true;
  var createTileStructure = function(jsondata) {
/*
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
*/
    //var ts = new ForeCache.Backend.Structures.TileStructure(jsondata.aggregationWindows,jsondata.tileWidths);
    var ts = new ForeCache.Backend.Structures.MultiDimTileStructure(
      jsondata.aggregationWindows,
      jsondata.dimensionGroups,
      jsondata.tileWidths);
    callback(ts);
  };
  ForeCache.Backend.Request.sendJsonGetRequest(dat,createTileStructure);
};


ForeCache.Backend.Request.getTileBinary = function(tileid,requestid,callback) {
var dat = {};
  dat.binary = true;
  //dat.zoom = tileid.zoom;
  dat.zoom = tileid.zoom.join(",");
  dat.tile_id = tileid.dimindices.join("_");
  dat.requestid = requestid;
  var createTile = function(arrayBuffer) {
    var fetchEnd = Date.now();
    ForeCache.globalTracker.appendToLog(ForeCache.Tracker.perTileLogName,
      {'action':'fetchTileBinary','tileId':tileid.name,'start':fetchStart,'end':fetchEnd});
    console.log(["time to fetch",fetchEnd-fetchStart]);
    //console.log(["arrayBuffer length",arrayBuffer.byteLength]);
    var decodeStart = Date.now();
    var tdecoder = new ForeCache.Backend.TileDecoder(arrayBuffer);
    //var tile = tdecoder.unpackTile();
    var tile = tdecoder.unpackMdTile();
    var decodeEnd = Date.now();
    ForeCache.globalTracker.appendToLog(ForeCache.Tracker.perTileLogName,
      {'action':'decodeTile','tileId':tileid.name,'start':decodeStart,'end':decodeEnd});
    console.log(["time to decode",decodeEnd-decodeStart]);

    callback(tile); // return the tile object
  };
  var fetchStart = Date.now();
  ForeCache.Backend.Request.sendBinaryGetRequest(dat,createTile);
};

// uses JSON string format to retrieve tile from server
// returns a ForeCache.Backend.Request.Tile object
ForeCache.Backend.Request.getTileJson = function(tileid,requestid,callback) {
  var dat = {};
  dat.json = true;
  dat.zoom = tileid.zoom;
  dat.tile_id = tileid.dimindices.join("_");
  dat.requestid = requestid;
  var createTile = function(jsondata) {
    var fetchEnd = Date.now();
    ForeCache.globalTracker.appendToLog(ForeCache.Tracker.perTileLogName,
      {'action':'fetchTileJson','tileId':tileid.name,'start':fetchStart,'end':fetchEnd});

    var columns = jsondata.data;
    var attributes = jsondata.attributes;
    var dataTypes = jsondata.dataTypes;
    var tile = new ForeCache.Backend.Structures.Tile(columns,attributes,dataTypes,tileid);
    callback(tile); // return the tile object
  };
  var fetchStart = Date.now();
  //ForeCache.Backend.Request.sendJsonGetRequest(dat,createTile);
  ForeCache.Backend.Request.sendJsonGetRequestNoJquery(dat,createTile);
};

// convenience method, chooses JSON or binary format for you
ForeCache.Backend.Request.getTileWithRequestid = function(tileid,requestid,callback) {
  var fetchStart = Date.now();
/*(
  var cb = function(tile) {
    var fetchEnd = Date.now();
    ForeCache.globalTracker.appendToLog(ForeCache.Tracker.perTileLogName,
      {'action':'fetchTile','tileId':tile.id.name,'start':fetchStart,'end':fetchEnd});
    callback(tile);
  };
*/
  //ForeCache.Backend.Request.getTileJson(tileid,requestid,cb);
  ForeCache.Backend.Request.getTileBinary(tileid,requestid,callback);
};

// convenience method, chooses JSON or binary format for you
ForeCache.Backend.Request.getTile = function(tileid,callback) {
  var requestid = ForeCache.Backend.Request.uuid();
  //ForeCache.Backend.Request.getTileJson(tileid,requestid,callback);
  ForeCache.Backend.Request.getTileBinary(tileid,requestid,callback);
};

// retrieves all tiles, then calls the finalcallback function with
// the list of retrieved tiles
ForeCache.Backend.Request.getTiles = function(tileids,finalcallback) {
  var i = 0;
  var tiles = [];
  var requestid = ForeCache.Backend.Request.uuid();
  var processNextTile = function(tile) {
    tiles.push(tile);
    i++;
    if(i < tileids.length) {
      var tid = tileids[i]; // get the next id to retrieve
      ForeCache.Backend.Request.getTileWithRequestid(tid,requestid,processNextTile); // repeat loop
    } else { // all tiles have been processed
      finalcallback(tiles);
    }
  };
  var startingid = tileids[0];
  ForeCache.Backend.Request.getTileWithRequestid(startingid,requestid,processNextTile);
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
  var requestid = ForeCache.Backend.Request.uuid();
  var processNextTile = function(tile) { // ForeCache.Backend.Request.Tile object
    tilecallback(tile);
    tiles.push(tile);
    i++;
    if(i < tileids.length) {
      var tid = tileids[i]; // get the next id to retrieve
      ForeCache.Backend.Request.getTileWithRequestid(tid,requestid,processNextTile); // repeat loop
    } else { // all tiles have been processed
      if(dofc) {
        finalcallback(tiles);
      }
    }
  };
  var startingid = tileids[0];
  ForeCache.Backend.Request.getTileWithRequestid(startingid,requestid,processNextTile);
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

ForeCache.Backend.Request.sendGetRequest = function(dat,callback) {
  //$.get(ForeCache.Backend.Request.URL,dat,callback);
  ForeCache.Backend.Request.sendGetRequestHelper(dat,"",callback);
};

ForeCache.Backend.Request.sendPostRequest = function(dat,callback) {
  ForeCache.Backend.Request.sendPostRequestHelper(dat,"",callback);
};

ForeCache.Backend.Request.sendJsonGetRequest = function(dat,callback) {
  //$.getJSON(ForeCache.Backend.Request.URL,dat,callback);
  ForeCache.Backend.Request.sendJsonGetRequestNoJquery(dat,callback);
};

// IE 10 does not support req.responeType = 'json'
// just manually getting string and parsing as json
ForeCache.Backend.Request.sendJsonGetRequestNoJquery = function(dat,callback) {
  var jsonParse = function(jsonstring) {
    var jsondata = JSON.parse(jsonstring);
    callback(jsondata);
    //console.log(["jsondata",jsondata]);
  };
  ForeCache.Backend.Request.sendGetRequestHelper(dat,"",jsonParse);
};

ForeCache.Backend.Request.sendBinaryGetRequest = function(dat,callback) {
  ForeCache.Backend.Request.sendGetRequestHelper(dat,"arraybuffer",callback);
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
    var p = encodeURIComponent(key) + "=";
    if((typeof val === 'string') || ForeCache.Backend.Request.isNumber(val) || (typeof val === 'boolean')) {
      p += encodeURIComponent(val);
    } else { // is some object
      //console.log("is object");
      p += encodeURIComponent(JSON.stringify(val));
    }
    //parsedParams.push(encodeURIComponent(key) + "=" + encodeURIComponent(val));
    //console.log(["param",key,JSON.stringify(val),encodeURIComponent(JSON.stringify(val))]);
    parsedParams.push(p);
  }
  return parsedParams.join("&");
};


ForeCache.Backend.Request.sendGetRequestHelper = function(dat,responseType,callback,errorCallback) {
  ForeCache.Backend.Request.sendRequestHelper(dat,"GET",responseType,callback,errorCallback);
};

ForeCache.Backend.Request.sendPostRequestHelper = function(dat,responseType,callback,errorCallback) {
  ForeCache.Backend.Request.sendRequestHelper(dat,"POST",responseType,callback,errorCallback);
};

ForeCache.Backend.Request.sendRequestHelper = function(dat,requestType,responseType,callback,errorCallback) {
  //var paramsString = $.param(dat);
  var paramsString = ForeCache.Backend.Request.buildParams(dat);
  var newUrl = (requestType==="GET") ? ForeCache.Backend.Request.URL + "?" + paramsString : ForeCache.Backend.Request.URL;
  var oReq = new XMLHttpRequest();
  oReq.open(requestType,newUrl, true); // async = true
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

  if(requestType==="GET") {
    oReq.send();
  } else { // post method
    oReq.send(paramsString);
  }
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

ForeCache.Backend.Request.isNumber  = function(n) {
  return !isNaN(parseFloat(n)) && isFinite(n);
};
