var StatesRenderer = StatesRenderer || {};

/************* Classes *************/

// VisObj is a super class for visualizations. All visualization types should be able to
// inherit from this class, and only fill in a small number of functions.
// THIS CLASS DOES NOT RENDER ANYTHING!!!! Note also
// that the chart parameter is a jquery object
StatesRenderer.renderObj = function(chart, options) {
	var self = this;
  this.coordinatesLoaded = false;
};

/*
StatesRenderer.renderObj.prototype.loadCoordinates = function(callback) {
    if (this.coordinatesLoaded) {
      callback();
    }
    var self = this;
    function setCoordinates(json) {
      self.Coordinates = json;
      self.coordinatesLoaded = true;
      callback();
    };
    var xobj = new XMLHttpRequest();
    xobj.overrideMimeType("application/json");
    xobj.open('GET', '', true); // Replace 'my_data' with the path to your file
    xobj.onreadystatechange = function () {
          if (xobj.readyState == 4 && xobj.status == "200") {
            // Required use of an anonymous callback as .open will NOT return a value but simply
            // returns undefined in asynchronous mode
            setCoordinates(xobj.responseText);
          }
    };
    xobj.send(null);  
}
*/

StatesRenderer.renderObj.prototype.renderUsa = function (latwindow,lngwindow,ctx,xScale,yScale,padding)
{
  var stateNames = StatesRenderer.Regions.USA;
  for(var i = 0; i < stateNames.length; i++) {
    var stateName = stateNames[i];
      this.renderState(stateName,latwindow,lngwindow,ctx,xScale,yScale,padding);
  }
}

StatesRenderer.renderObj.prototype.renderState = function(stateName,latwindow,lngwindow,ctx,xScale,yScale,padding) {
  var self = this;
  //this.loadCoordinates(function() {
    var coords = StatesRenderer.Coordinates[stateName].Coordinates;
    this.renderRegion(coords,latwindow,lngwindow,ctx,xScale,yScale,padding);
  //});
}

// coords = [{newlat,newlng},{newlat,newlng},...]
// latwindow = aggregation window to apply to latitude coordinates
// lngwindow = aggregation window to apply to longitude coordinates
StatesRenderer.renderObj.prototype.renderRegion = function(coords,latwindow,lngwindow,ctx,xScale,yScale,padding) {
  var lng = coords[1].newlng2 * 1.0 / lngwindow;
  var lat = coords[0].newlat2 * 1.0 / latwindow;;
  var x = xScale(lng)+padding.left;
	var y = yScale(lat)+padding.top;
  var origx = x;
  var origy = y;
  ctx.save();
  ctx.beginPath();
  ctx.strokeStyle = "black";
  ctx.lineWidth = 1;
  ctx.moveTo(x,y);
	for(var i=1; i < coords.length;i++) {
    var coord = coords[i];
    lng = coord.newlng2 * 1.0 / lngwindow;
    lat = coord.newlat2 * 1.0 / latwindow;
		x = xScale(lng)+padding.left;
		y = yScale(lat)+padding.top;
		
		ctx.lineTo(x,y);
	}
  ctx.lineTo(origx,origy);
  ctx.stroke();
	ctx.closePath();
  ctx.restore();
};

/****************** Helper Functions *********************/


StatesRenderer.Regions = {
"USA":["Alabama", "Alaska", "Arizona", "Arkansas", "California", "Colorado", "Connecticut",
"Delaware","Florida", "Georgia", "Hawaii", "Idaho", "Illinois", "Indiana", "Iowa", "Kansas",
"Kentucky","Louisiana", "Maine", "Maryland", "Massachusetts", "Michigan", "Minnesota",
"Mississippi","Missouri", "Montana", "Nebraska", "Nevada", "New Hampshire", "New Jersey",
"New Mexico","New York", "North Carolina", "North Dakota", "Ohio", "Oklahoma", "Oregon",
"Pennsylvania","Rhode Island", "South Carolina", "South Dakota", "Tennessee", "Texas",
"Utah", "Vermont","Virginia","Washington", "West Virginia", "Wisconsin", "Wyoming"],

"Canada":["Alberta","British Columbia","Manitoba","New Brunswick","Newfoundland","Northwest Territories","Nova Scotia","Nunavut","Ontario","Prince Edward Island","Quebec","Saskatchewan","Yukon"]
};

