// tileManager: TileManager object that the views are tied to
// snapshotSilo: jquery object to hold snapshots
// backendUrl: url for interacting with the data collection server
UserStudyObj = function(tileManager,snapshotSilo,backendUrl) {
  this.tileManager = tileManager;
  this.snapshotSilo = snapshotSilo;
  this.userid = this.createUuid();
  this.backendUrl = backendUrl;
  this.htmlVariables();
};

UserStudyObj.prototype.takeSnapshot = function(i) {
  var visObj = this.tileManager.visObjects[i];
  var snapshot = this.snapshotObject.clone();
  var canvasImg = visObj.canvas.node().toDataURL('image/jpeg'); // get image data
  console.log(["canvasImg",canvasImg]);
  snapshot.find("img").first().attr("src",canvasImg);
  snapshot.find("input.delete").first().on("click",function() { $(this).parent("div.snapshot").remove();});
  //var snapshot = $("<img class='snapshot' src='"+canvasImg+"'></img>");
  this.snapshotSilo.append(snapshot);
  return canvasImg;
};

// suffix: suffix to add to the backendUrl to send the appropriate request
// data: object containing the json data to send in the request
// [optional] callback: callback to call with the response
UserStudyObj.prototype.sendBackendRequest = function(data,suffix,callback) {
  var url = this.backendUrl + "/" + suffix;
  if(arguments.length == 3) {
    $.getJSON(url,data,callback);
  } else {
    $.getJSON(url,data);
  }
};

UserStudyObj.prototype.sendSnapshots = function() {
  var self = this;
  var snapshots = this.snapshotSilo.find(".snapshot");
  snapshots.each(function(i) {
    var canvasImg = $(this).attr("src"); // get the image source
    //send the snapshot to the backend
    //TODO: include all dimension ranges, and the tiles visualized in the viewport
    self.sendBackendRequest({userid:self.userid,imageData:canvasImg});
  });
};

// butonJqueryObj:  a jquery object representing the button to enable
// voId: the index of the view to take a snapshot of using this button (with respect to the
// visObjects list from the TileManager
UserStudyObj.prototype.enableSnapshotButton = function(buttonJqueryObj,voId) {
  var self = this;
  buttonJqueryObj.off('click').on('click',function() {
    self.takeSnapshot(voId);
  });
};

/********** Helper Functions **********/

UserStudyObj.prototype.htmlVariables = function() {
  this.snapshotObject = $(
    '<div class="snapshot">'
      +'<img src=""/>'
      +'<input class="delete" type="button" value="Delete" />'
    +'</div>'
  );
};

// used to generate random id's
UserStudyObj.prototype.createUuid = function() {
    var d = new Date().getTime();
    var uuid = 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
        var r = (d + Math.random()*16)%16 | 0;
        d = Math.floor(d/16);
        return (c=='x' ? r : (r&0x3|0x8)).toString(16);
    });
    return uuid;
};
