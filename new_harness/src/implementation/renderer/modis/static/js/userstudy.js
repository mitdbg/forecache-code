UserStudy = {};

// taskname: string representing a unique identifier for the current task
// tileManager: TileManager object that the views are tied to
// snapshotSilo: jquery object to hold snapshots
// backendUrl: url for interacting with the data collection server
UserStudy.UserStudyObj = function(taskname,userid,tileManager,snapshotSilo,backendUrl) {
  this.taskname = taskname;
  this.tileManager = tileManager;
  this.snapshotSilo = snapshotSilo;
  this.snapshotMap = {};
  this.userid = userid;
  this.backendUrl = backendUrl;
  this.htmlVariables(); // creates necessary html variables
  this.recordStart(); // user has started this task
};

// takes the logs recorded by ForeCache and sends them all to the backend
UserStudy.UserStudyObj.prototype.saveForeCachePerfLogs = function() {
  ForeCache.globalTracker.printLogToConsole(ForeCache.Tracker.perTileLogName);
  var logNames = ForeCache.globalTracker.getLogNames();
  var data = {};
  data.userid = this.userid; // need to know which user was logged!
  data.taskname = this.taskname; // need to know which task was logged!
  data.logs = {};
  for(var i = 0; i < logNames.length; i++) {
    data.logs[logNames[i]] = ForeCache.globalTracker.logs[logNames[i]].records;
  };
  console.log(data);
  this.sendPostRequest(data,"savePerfLogs/");
};

UserStudy.UserStudyObj.prototype.recordState = function(state) {
  var timestampMillis = Date.now();
  this.sendGetRequest({"state":state,
    "userid":this.userid,
    "taskname":this.taskname,
    "timestampMillis":timestampMillis},"recordTaskTimestamp/");
};

UserStudy.UserStudyObj.prototype.recordFinish = function() {
  this.recordState("finish");
};

UserStudy.UserStudyObj.prototype.recordStart = function() {
  this.recordState("start");
};

UserStudy.UserStudyObj.prototype.takeSnapshot = function(i) {
  var visObj = this.tileManager.visObjects[i];
  var canvasImg = visObj.canvas.node().toDataURL('image/jpeg'); // get image data
  //console.log(["canvasImg",canvasImg]);
  var snapshot = new UserStudy.SnapshotObj(canvasImg,this.tileManager,this.snapshotMap);
  this.snapshotSilo.append(snapshot.snapshotDomObject);
  this.snapshotMap[snapshot.id] = snapshot;
};

// suffix: suffix to add to the backendUrl to send the appropriate request
// data: object containing the json data to send in the request
// [optional] callback: callback to call with the response
// uses jquery
UserStudy.UserStudyObj.prototype.sendGetRequest = function(data,suffix,callback) {
  var url = this.backendUrl + "/" + suffix;
  if(arguments.length == 3) {
    $.getJSON(url,data,callback);
  } else {
    $.getJSON(url,data);
  }
};

// suffix: suffix to add to the backendUrl to send the appropriate request
// data: object containing the json data to send in the request
// [optional] callback: callback to call with the response
// uses jquery
UserStudy.UserStudyObj.prototype.sendPostRequest = function(data,suffix,callback) {
  var url = this.backendUrl + "/" + suffix;
  var func = function(){};
  if(arguments.length == 3) { // was a callback specified?
    func = callback;
  }
  // jquery post call doesn't work properly! using ajax function
  $.ajax({
    type : "POST",
    url : url,
    data: JSON.stringify(data),
    contentType: 'application/json;charset=UTF-8',
    success: func
  });
};

UserStudy.UserStudyObj.prototype.saveSnapshots = function() {
  var snapshotIds = Object.keys(this.snapshotMap);
  var data = {};
  data.snapshots = [];
  for(var i = 0; i < snapshotIds.length; i++) {
    var snapshot = this.snapshotMap[snapshotIds[i]].toJson();
    snapshot.userid = this.userid;
    snapshot.taskname = this.taskname;
    data.snapshots.push(snapshot);
  }
  data.snapshots = JSON.stringify(data.snapshots);
  this.sendGetRequest(data,"saveSnapshots/");
};

// buttonJqueryObj:  a jquery object representing the button to enable
// voId: the index of the view to take a snapshot of using this button (with respect to the
// visObjects list from the TileManager
UserStudy.UserStudyObj.prototype.enableSnapshotButton = function(buttonJqueryObj,voId) {
  var self = this;
  buttonJqueryObj.off('click').on('click',function() {
    self.takeSnapshot(voId);
  });
};

// buttonJqueryObj:  a jquery object representing the button to enable
UserStudy.UserStudyObj.prototype.enableFinishButton = function(buttonJqueryObj) {
  var self = this;
  buttonJqueryObj.off('click').on('click',function() {
    self.recordFinish(); // user has finished this task
    self.saveSnapshots();
    self.saveForeCachePerfLogs();
  });
};

/********** Classes **********/

//TODO: include all dimension ranges, and the tiles visualized in the viewport
UserStudy.SnapshotObj = function(canvasImg,tileManager,snapshotMap) {
  var self = this;
  this.timestampMillis = Date.now();
  this.id = UserStudy.createUuid();
  this.canvasImg = canvasImg;
  this.zoomPos = tileManager.currentZoom; // zoom level position
  this.tileKeys = tileManager.currentTiles; // list of tile keys
  this.snapshotMap = snapshotMap;

  this.dimRanges = {}; // dataset ranges for every dimension
  for(var i = 0; i < tileManager.visObjects.length; i++) {
    vObj = tileManager.visObjects[i];
    this.dimRanges[vObj.xindex] = vObj.x.domain();
    if(vObj.dimensionality == 2) {
      this.dimRanges[vObj.yindex] = vObj.y.domain();
    }
  }

  // what snapshots look like in the dom
  this.snapshotDomObject = $(
    '<div class="snapshot">'
      +'<img src=""/>'
      +'<input class="delete" type="button" value="Delete" />'
    +'</div>'
  );
  this.snapshotDomObject.attr("id",this.id);
  this.snapshotDomObject.find("img").first().attr("src",this.canvasImg);
  this.snapshotDomObject.find("input.delete").first().on("click",function() {
    var id = $(this).parent("div.snapshot").attr("id");
    //console.log(["id",id]);
    $(this).parent("div.snapshot").remove(); // remove from dom
    delete self.snapshotMap[id]; // remove snapshot object from map
  });
};

UserStudy.SnapshotObj.prototype.toJson = function() {
  var snapshot = {};
  snapshot.id = this.id;
  snapshot.canvasImg = this.canvasImg;
  snapshot.zoomPos = this.zoomPos; // zoom level position
  snapshot.dimRanges = this.dimRanges; // dataset ranges for every dimension
  snapshot.tileKeys = this.tileKeys; // list of tile keys
  snapshot.timestampMillis = this.timestampMillis; // when the snapshot was taken
  return snapshot;
};

/********** Helper Functions **********/

UserStudy.UserStudyObj.prototype.htmlVariables = function() {

};

// used to generate random id's
UserStudy.createUuid = function() {
    var d = new Date().getTime();
    var uuid = 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
        var r = (d + Math.random()*16)%16 | 0;
        d = Math.floor(d/16);
        return (c=='x' ? r : (r&0x3|0x8)).toString(16);
    });
    return uuid;
};
