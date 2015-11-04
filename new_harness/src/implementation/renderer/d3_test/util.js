//setup--assumes bigdawgvis.js has already been loaded
BigDawgVis.Util = {};

BigDawgVis.Util.max = function(table,opts) {
  groups = opts.groups || [];
  var togroup = groups[0];
  var toagg = opts.yname;
  var newy = "max_"+toagg;

  var result = {"table":[]};
  var maxs = {};
  for(var i = 0; i < table.length; i++) {
    var key = table[i][togroup];
    var val = table[i][toagg];
    //console.log([key,val]);
    if(key in maxs) {
      if(val > maxs[key]) maxs[key] = val;
    } else {
      maxs[key] = val;
    }
  }

  for (var key in maxs) {
    if(maxs.hasOwnProperty(key)) {
      var tup = {};
      tup[togroup] = key;
      tup[newy] = maxs[key];
      result.table.push(tup);
      console.log(["key",key,"max",tup[newy]]);
    }
  }
  opts.yname = newy;
  opts.ylabel = newy;
  return result;
}

BigDawgVis.Util.min = function(table,opts) {
  groups = opts.groups || [];
  var togroup = groups[0];
  var toagg = opts.yname;
  var newy = "min_"+toagg;

  var result = {"table":[]};
  var mins = {};
  for(var i = 0; i < table.length; i++) {
    var key = table[i][togroup];
    var val = table[i][toagg];
    //console.log([key,val]);
    if(key in mins) {
      if(val < mins[key]) mins[key] = val;
    } else {
      mins[key] = val;
    }
  }

  for (var key in mins) {
    if(mins.hasOwnProperty(key)) {
      var tup = {};
      tup[togroup] = key;
      tup[newy] = mins[key];
      result.table.push(tup);
      console.log(["key",key,"min",tup[newy]]);
    }
  }
  opts.yname = newy;
  opts.ylabel = newy;
  return result;
}

BigDawgVis.Util.mean = function(table,opts) {
  groups = opts.groups || [];
  var togroup = groups[0];
  var toagg = opts.yname;
  var newy = "mean_"+toagg;
  //console.log(["key",togroup,"y",toagg]);

  var result = {"table":[]};
  var sums = {};
  var counts = {};
  for(var i = 0; i < table.length; i++) {
    var key = table[i][togroup];
    var val = table[i][toagg];
    //console.log([key,val]);
    if(key in sums) {
      sums[key] = sums[key] + Number(val);
      counts[key] = counts[key] + 1;
    } else {
      sums[key] = Number(val);
      counts[key] = 1;
    }
    //console.log(["sum:",sums[key],"count",counts[key]]);
  }

  for (var key in sums) {
    if(sums.hasOwnProperty(key)) {
      var tup = {};
      tup[togroup] = key;
			if(counts[key] == 0) tup[newy] = 0;
			else tup[newy] = sums[key] * 1.0 / counts[key];
      result.table.push(tup);
      console.log(["key",key,"mean",tup[newy],"sum",sums[key],"count",counts[key]]);
    }
  }
  opts.yname = newy;
  opts.ylabel = newy;
  return result;
}

BigDawgVis.Util.count = function(table,opts) {
  groups = opts.groups || [];
  var togroup = groups[0];
  var toagg = opts.yname;
  var newy = "count_"+togroup;
  //console.log(["key",togroup,"y",toagg]);

  var result = {"table":[]};
  var counts = {};
  for(var i = 0; i < table.length; i++) {
    var key = table[i][togroup];
    if(key in counts) {
      counts[key] = counts[key] + 1;
    } else {
      counts[key] = 1;
    }
    //console.log(["count",counts[key]]);
  }

  for (var key in counts) {
    if(counts.hasOwnProperty(key)) {
      var tup = {};
      tup[togroup] = key;
      tup[newy] = counts[key];
      result.table.push(tup);
      console.log(["key",key,"count",counts[key]]);
    }
  }
  opts.yname = newy;
  opts.ylabel = newy;
  return result;
}

BigDawgVis.Util.normalize = function(table,opts) {
    norm = opts.norm || opts.yname;
    var domain = BigDawgVis.domain(table, norm);
    var range = opts.outputRange  || [0.0,1.0];
  	var newy = "norm_"+norm;
    var result = table;
    for(var row = 0; row < result.length; row++) {
        var val = result[row][norm];
        if (!isNaN( parseFloat(val)) ) {
            var original = parseFloat(val);
            var normalized = ((original - domain[0]) / (domain[1] - domain[0])) * (range[1] - range[0]) + range[0];
            result[row][newy] = Math.round(normalized);
        }
    }
	opts.yname = newy;
    return {"table":result};
}

BigDawgVis.Util.sortAscending = function(table,opts) {
    sort = opts.sort || opts.xname;
    table.sort(function (a, b) {
        return a[sort] - b[sort]
    });
    return table;
}

BigDawgVis.Util.sortDescending = function(table,opts) {
    sort = opts.sort || opts.xname;
    table.sort(function (a, b) {
        return b[sort] - a[sort]
    });
    return table;
}

BigDawgVis.Util.filter = function(table,opts,value) {
    filter = opts.filter || opts.yname;
    result.filter( function(d){
        return d[filter] == value;
    });
    return result;
}

BigDawgVis.Util.round = function(table,opts) {
    round = opts.yname;
    var newy = "round_"+round;
    var result = table;
    for(var row = 0; row < result.length; row++) {
        var val = result[row][round];
        if (!isNaN( parseFloat(val)) ) {
            result[row][newy] = Math.round(parseFloat(val));
        }
    }
    opts.yname = newy;
    return {"table":result};
}

