QVis.MapPlot = function(rootid,opts) {
	QVis.Graph.call(this,rootid,opts);

	//unique to scatterplots
	this.circlecontainer = null;
	this.r = opts.r || 1.5;
}

//inherit Graph Object
QVis.MapPlot.prototype = new QVis.Graph();

//fix constructor reference
QVis.MapPlot.constructor = QVis.MapPlot;

//to get access to the original functions
QVis.MapPlot.base = QVis.Graph.prototype;

//add a new render function
//data format: {'data':[{},{},...],'names':["","",...],'types':{"":,"":,...}}
QVis.MapPlot.prototype.render2 = function(_data, _labels,_types, opts) {
	$('#map').addClass('show');
	this.selectx = true;
	this.selecty = true;

	var self = this;
	//call the original render function
	self.labelsfrombase = QVis.MapPlot.base.render.call(this,_data,_labels,_types,opts);

	// create x,y axis scales
	var xscale = this.createScale(_data,_types,self.labelsfrombase.x_label,this.w,this.px,this.inv[0],false);
	var yscale = this.createScale(_data,_types,self.labelsfrombase.y_label,this.h,this.py,this.inv[1],false);

	// Create the Google Map…
	var map = new google.maps.Map(d3.select("#map div").node(), {
		zoom: 3,
		center: new google.maps.LatLng(37.76487, -122.41948),
		mapTypeId: google.maps.MapTypeId.TERRAIN
	});
	$('#map div').css('width', this.w+'px').css('height', this.h+'px');

	var mapCircle;
	var i;
	for (var i = 0; i < _data.length; i++) {
		var entry = _data[i];
		//console.log(["lat lons",entry[self.labelsfrombase.x_label],entry[self.labelsfrombase.y_label]]);
		// Construct the circle for each value in citymap. We scale population by 20.
		var dataOptions = {
			strokeColor: '#FF0000',
			strokeOpacity: 0.8,
			strokeWeight: 2,
			fillColor: '#FF0000',
			fillOpacity: 0.35,
			map: map,
			center: new google.maps.LatLng(entry[self.labelsfrombase.x_label],entry[self.labelsfrombase.y_label]),
			radius: 10000
		};
		mapCircle = new google.maps.Circle(dataOptions);
	} 
/*
	var overlay = new google.maps.OverlayView();

	// Add the container when the overlay is added to the map.
	overlay.onAdd = function() {
		var layer = d3.select(this.getPanes().overlayLayer).append("div")
		.attr("class", "stations");

		// Draw each marker as a separate SVG element.
		// We could use a single SVG, but what size would it have?
		overlay.draw = function() {
			var projection = this.getProjection(),
			  padding = 10;

			var marker = layer.selectAll("svg")
			  .data(_data)
			  .each(transform) // update existing markers
			.enter().append("svg:svg")
			  .each(transform)
			  .attr("class", "marker");

			// Add a circle.
			marker.append("svg:circle")
			  .attr("r", self.defaultRadius)
			  .attr("cx", padding)
			  .attr("cy", padding).attr('fill',self.defaultColor(null));

			function transform(data) {
				//LatLng(lat,lon)
				var d = new google.maps.LatLng(data[self.labelsfrombase.x_label], data[self.labelsfrombase.y_label]);
				//console.log(d.lat()+","+d.lng());
				d = projection.fromLatLngToDivPixel(d);
				return d3.select(this)
				    .style("left", (d.x - padding) + "px")
				    .style("top", (d.y - padding) + "px");
			}
		};
	};

	// Bind our overlay to the map…
	overlay.setMap(map);
*/
}

//add a new render function
//data format: {'data':[{},{},...],'names':["","",...],'types':{"":,"":,...}}
QVis.MapPlot.prototype.render = function(_data, _labels,_types, opts) {
	$('#map').addClass('show');
	this.selectx = true;
	this.selecty = true;

	var self = this;
	//call the original render function
	self.labelsfrombase = QVis.MapPlot.base.render.call(this,_data,_labels,_types,opts);

	// create x,y axis scales
	var xscale = this.createScale(_data,_types,self.labelsfrombase.x_label,this.w,this.px,this.inv[0],false);
	var yscale = this.createScale(_data,_types,self.labelsfrombase.y_label,this.h,this.py,this.inv[1],false);

	// Create the Google Map…
	var map = new google.maps.Map(d3.select("#map div").node(), {
		zoom:2,
		center: new google.maps.LatLng(20.841015,-171.738281),
		//zoom: 1,
		//center: new google.maps.LatLng(37.76487, -122.41948),
		//zoom: 13,
		//center: new google.maps.LatLng(42.3584, -71.0603),
		mapTypeId: google.maps.MapTypeId.TERRAIN
	});
	$('#map div').css('width', this.w+'px').css('height', this.h+'px');

	var overlay = new google.maps.OverlayView();

	// Add the container when the overlay is added to the map.
	overlay.onAdd = function() {
		var layer = d3.select(this.getPanes().overlayLayer).append("div")
		.attr("class", "stations");

		// Draw each marker as a separate SVG element.
		// We could use a single SVG, but what size would it have?
		overlay.draw = function() {
			var projection = this.getProjection(),
			  padding = 10;

			var marker = layer.selectAll("svg")
			  .data(_data)
			  .each(transform) // update existing markers
			.enter().append("svg:svg")
			  .each(transform)
			  .attr("class", "marker");

			// Add a circle.
			marker.append("svg:circle")
			  .attr("r", self.defaultRadius)
			  .attr("cx", padding)
			  .attr("cy", padding).attr('fill',self.defaultColor(null));

			function transform(data) {
				//LatLng(lat,lon)
				var d = new google.maps.LatLng(data[self.labelsfrombase.x_label], data[self.labelsfrombase.y_label]);
				//console.log(d.lat()+","+d.lng());
				d = projection.fromLatLngToDivPixel(d);
				return d3.select(this)
				    .style("left", (d.x - padding) + "px")
				    .style("top", (d.y - padding) + "px");
			}
		};
	};

	// Bind our overlay to the map…
	overlay.setMap(map);

}
