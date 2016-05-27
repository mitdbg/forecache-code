UserStudy = {};

// tileManager: TileManager object that the views are tied to
// snapshotSilo: jquery object to hold snapshots
// backendUrl: url for interacting with the data collection server
UserStudy.UserStudyObj = function(tileManager,snapshotSilo,backendUrl) {
  this.tileManager = tileManager;
  this.snapshotSilo = snapshotSilo;
  this.snapshotMap = {};
  this.userid = UserStudy.createUuid();
  this.backendUrl = backendUrl;
  this.htmlVariables(); // creates necessary html variables
};

UserStudy.UserStudyObj.prototype.takeSnapshot = function(i) {
  var visObj = this.tileManager.visObjects[i];
  var canvasImg = visObj.canvas.node().toDataURL('image/jpeg'); // get image data
  console.log(["canvasImg",canvasImg]);
  var snapshot = new UserStudy.SnapshotObj(canvasImg,this.snapshotMap);
  this.snapshotSilo.append(snapshot.snapshotDomObject);
  this.snapshotMap[snapshot.id] = snapshot;
};

// suffix: suffix to add to the backendUrl to send the appropriate request
// data: object containing the json data to send in the request
// [optional] callback: callback to call with the response
// uses jquery
UserStudy.UserStudyObj.prototype.sendBackendRequest = function(data,suffix,callback) {
  var url = this.backendUrl + "/" + suffix;
  if(arguments.length == 3) {
    $.getJSON(url,data,callback);
  } else {
    $.getJSON(url,data);
  }
};

UserStudy.UserStudyObj.prototype.sendSnapshots = function() {
  var self = this;
  var snapshotIds = Object.keys(this.snapshotMap);
  for(var i = 0; i < snapshotIds.length; i++) {
    var snapshotObj = this.snapshotMap[snapshotIds[i]];
    //TODO: include all dimension ranges, and the tiles visualized in the viewport
    this.sendBackendRequest({userid:this.userid,imageData:snapshotObj.canvasImg});
  }
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

/********** Classes **********/
UserStudy.SnapshotObj = function(canvasImg,snapshotMap) {
  var self = this;
  this.id = UserStudy.createUuid();
  this.canvasImg = canvasImg;
  this.zoomPos = null; // zoom level position
  this.dimRanges = null; // dataset ranges for every dimension
  this.tiles = null; // list of tile keys
  this.snapshotMap = snapshotMap;

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
