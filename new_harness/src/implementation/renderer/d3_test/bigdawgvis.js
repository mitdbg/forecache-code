var BigDawgVis = BigDawgVis || {};

BigDawgVis.domain = function(table,field) {
	var domain = [];
	if(table.length > 0) {
		domain = [Number(table[0][field]),Number(table[0][field])];
  	for(var i = 1; i < table.length; i++) {
    	var val = Number(table[i][field]);
			if(domain[0] > val) domain[0] = val;
			if(domain[1] < val) domain[1] = val;
		}
	}
	return domain;
};



// used to create a unique identifier for visualization objects in the DOM
BigDawgVis.getPrefix = function() { return "bigdawgvis-";};

BigDawgVis.uuid = function() {
    var d = new Date().getTime();
    var uuid = 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
        var r = (d + Math.random()*16)%16 | 0;
        d = Math.floor(d/16);
        return (c=='x' ? r : (r&0x3|0x8)).toString(16);
    });
    return uuid;
};

// tells you whether the given string can be parsed as a number
BigDawgVis.isNumber = function(toTest) {
  return String(Number(toTest)) !== "NaN";
}

/* checks if the given field is numeric (sets to linear scale).
  Otherwise, sets to an ordinal scale.
*/
BigDawgVis.getAxisScaleType = function(field,table) {
  var useLinear = true;
  for(var i = 0; (i < table.length) && (i < 20); i++) {
    if(!BigDawgVis.isNumber(table[i][field])) {
      useLinear = false;
      break;
    }
  }
  if (useLinear) {
    return "linear";
  } else {
    return "ordinal";
  }
}

// separate scale inference for color scales (want to use quantile, not linear
// for numbers
BigDawgVis.getColorScaleType = function(field,table) {
  var scale = BigDawgVis.getAxisScaleType(field,table);
  if(scale === "linear") {
    scale = "quantile";
  }
  return scale;
};

/*
This method is used to get the fieldnames from the first row of the json table.
assumes format:
[
{"field1":val1,"field2":val2,...},
{"field1":val1,"field2":val2,...},
{"field1":val1,"field2":val2,...},
...
]
*/
BigDawgVis.getFields = function(table) {
  var fields = [];
  if(table.length > 0) {
    var point = table[0]; // get the first datapoint
    for(var key in point) {
      if(point.hasOwnProperty(key)) {
        //console.log(["found field",key]);
        fields.push(key);
      }
    }
  }
  return fields;
};

/*
This method sets some default values for the visualization,
if the original options did not specify them
*/
BigDawgVis.updateOpts = function(opts,table) {
  var newopts = {};
  newopts["width"] = opts['width'] || 200;
  newopts["height"] = opts['height'] || 200;
  newopts["opacity"] = opts['opacity'] || 0.7;

  // gets the field names from the first row
  var fields = BigDawgVis.getFields(table);
    console.log(fields);

  // add defaults if the axes have not been specified...
  if(fields.length > 0) {
  newopts["xname"] = opts['xname'] || fields[0];
  newopts["xlabel"] = opts['xlabel'] || newopts["xname"];
  newopts["xscale"] = BigDawgVis.getAxisScaleType(newopts.xname,table);

  newopts["yname"] = opts['yname'] || newopts["xname"];
  newopts["ylabel"] = opts['ylabel'] || newopts["xlabel"];
  newopts["yscale"] = BigDawgVis.getAxisScaleType(newopts.yname,table);

  newopts["zname"] = opts['zname'] || newopts["xname"];
  newopts["zlabel"] = opts['zlabel'] || newopts["xlabel"];
  newopts["zscale"] = BigDawgVis.getColorScaleType(newopts.zname,table);
  }
  
  if(fields.length > 1) {
    newopts["yname"] = opts['yname'] || fields[1];
    newopts["ylabel"] = opts['ylabel'] || fields[1];
    newopts["yscale"] = opts["yscale"] || BigDawgVis.getAxisScaleType(newopts.yname,table);
  }
  
  if(fields.length > 2) {
    newopts["zname"] = opts['zname'] || fields[2];
    newopts["zlabel"] = opts['zlabel'] || newopts['zname'];
    newopts["zscale"] = opts["zscale"] || BigDawgVis.getColorScaleType(newopts.zname,table);
  }
	if(newopts.zscale === "ordinal") {
		newopts.colorscheme = colorbrewer.Set1[9];
	} else {
		newopts.colorscheme = colorbrewer.YlOrRd[9];
		newopts.colordomain = BigDawgVis.domain(table,newopts.zname);
	}

  return newopts;
};

BigDawgVis.convertBigDawgForVega= convertBigDawgForVega= function(tuples,schema) {
  var vegatable = [];
  for(var i = 0; i < tuples.length; i++) {
    var tup = tuples[i];
    row = {};
    for(var j = 0; j < schema.length; j++) {
      row[schema[j]] = tup[j];
    }
    vegatable.push(row);
  }
  return {"table":vegatable};
};


//Dictionary linking uuids with data for future reference
BigDawgVis.Elements = new Object();
BigDawgVis.SetElement = function( uuid, data ){
    console.log(uuid);
    BigDawgVis.Elements[uuid] = data;
}
BigDawgVis.GetElement = function( uuid ){

    if (BigDawgVis.Elements.hasOwnProperty(uuid)){
        return BigDawgVis.Elements[uuid];
    }
    else {
        return undefined;
    }
}

