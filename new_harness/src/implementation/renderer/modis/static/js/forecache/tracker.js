var ForeCache = ForeCache || {};
ForeCache.Tracker = {};
ForeCache.Tracker.perTileLogName = "perTile";
ForeCache.Tracker.perInteractionLogName = "perInteraction";

/************* Classes *************/

// The Tracker object is used to record performance data
// for data retrieval and data rendering
ForeCache.Tracker.TrackerObj = function(chart, options) {
  this.logs = {};
}


// returns the list of names for the logs stored by this object
ForeCache.Tracker.TrackerObj.prototype.getLogNames = function() {
  return Object.keys(this.logs);
};

ForeCache.Tracker.TrackerObj.prototype.containsLog = function(logName) {
  return this.logs.hasOwnProperty(logName);
}

ForeCache.Tracker.TrackerObj.prototype.startLog = function(logName) {
  this.logs[logName] = new ForeCache.LogObj();
};

ForeCache.Tracker.TrackerObj.prototype.clearLog = function(logName) {
  this.logs[logName].clear();
};

ForeCache.Tracker.TrackerObj.prototype.appendToLog = function(logName,recordObj) {
    this.logs[logName].addRecord(recordObj);
    //console.log(recordObj);
}

ForeCache.Tracker.TrackerObj.prototype.printLogToConsole = function(logName) {
  //var output = this.logs[logName].exportRecords(',');
  console.log(['output for log',logName,this.logs[logName]]);
};


ForeCache.Tracker.TrackerObj.prototype.flushLogData = function(logName) {
  var output = this.logs[logName].exportTsv();
  this.logs[logName] = []; // empty the log
  return output;
};

ForeCache.LogObj = function () {
  this.records = [];
}


ForeCache.LogObj.prototype.clear = function() {
  this.records = [];
};

// all objects using the LogObj pass information in the form of dictionaries
// the log object will automatically handle formatting for csv/tsv
// with export function
ForeCache.LogObj.prototype.addRecord = function(recordObj) {
  this.records.push(recordObj);
}

ForeCache.LogObj.prototype.exportCsv = function() {
  return (this.exportRecords(',')).join('\n');
};

ForeCache.LogObj.prototype.exportTsv = function() {
  return (this.exportRecords('\t')).join('\n');
};

ForeCache.LogObj.prototype.exportRecords = function(delim) {
  var output = [];
  if(this.records.length > 0) {
    var keys = Object.keys(this.records[0]);
    for(var i = 0; i < this.records.length; i++) {
      var record = this.records[i];
      var values = [];
      for(var j = 0; j < keys.length; j++) {
        if(record[keys[j]]) {
          values.push(record[keys[j]]);
        } else {
          values.push("");
        }
      }
      output.push(values.join(delim));
    }
  }
  return output;
};

/****************** Helper Functions *********************/

//TODO: remove duplicate uuid function
ForeCache.Tracker.TrackerObj.prototype.uuid = function() {
    var d = new Date().getTime();
    var uuid = 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
        var r = (d + Math.random()*16)%16 | 0;
        d = Math.floor(d/16);
        return (c=='x' ? r : (r&0x3|0x8)).toString(16);
    });
    return uuid;
};



/****************** Instantiations *********************/
ForeCache.globalTracker = new ForeCache.Tracker.TrackerObj();
ForeCache.globalTracker.startLog(ForeCache.Tracker.perTileLogName);
ForeCache.globalTracker.startLog(ForeCache.Tracker.perInteractionLogName);
