var ForeCache = ForeCache || {};
ForeCache.Tracker = {};

/************* Classes *************/

ForeCache.Tracker.perTileLogName = "perTile";
ForeCache.Tracker.perInteractionLogName = "perInteraction";


// The Tracker object is used to record performance data
// for data retrieval and data rendering
ForeCache.Tracker = function(chart, options) {
  this.logs = {};
}

ForeCache.Tracker.prototype.containsLog = function(logName) {
  return this.logs.hasOwnProperty(logName);
}

ForeCache.Tracker.prototype.startLog = function(logName) {
  this.logs[logName] = new ForeCache.LogObj();
};

ForeCache.Tracker.prototype.appendToLog = function(logName,recordObj) {
    this.logs[logName].addRecord(recordObj);
    //console.log(recordObj);
}

ForeCache.Tracker.prototype.printLogToConsole = function(logName) {
  var output = this.logs[logName].exportRecords(',');
  console.log(['output for log',logName,output]);
};

ForeCache.LogObj = function () {
  this.records = [];
}

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
    var keys = this.records[0].keys();
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
ForeCache.Tracker.prototype.uuid = function() {
    var d = new Date().getTime();
    var uuid = 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
        var r = (d + Math.random()*16)%16 | 0;
        d = Math.floor(d/16);
        return (c=='x' ? r : (r&0x3|0x8)).toString(16);
    });
    return uuid;
};



/****************** Instantiations *********************/
ForeCache.globalTracker = new ForeCache.Tracker();
ForeCache.globalTracker.startLog(ForeCache.Tracker.perTileLogName);
ForeCache.globalTracker.startLog(ForeCache.Tracker.perInteractionLogName);
