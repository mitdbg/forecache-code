var renderagg = null;
var current_zoom = 0;
var flip = true; // whether we need to do inverted moves

var numdims = 2;
var current_id = [];

// get these from backend
var total_tiles = [];
var future_tiles= [];
var future_tiles_exact = [];
var indexmap = [];

var max_zoom = 1;
var zoom_diff = 2;

var menutype;
var once =0;

$(document).ready(function() {
	$('#sql-query-submit').on('click',user_query_handler);
	$('#button-up').on('click',move_up);
	$('#button-down').on('click',move_down);
	$('#button-left').on('click',move_left);
	$('#button-right').on('click',move_right);
	$('.nav-button').button();

	function move_up() {
		if(flip && renderagg.inv[1]) {
			flip = false;
			return move_down();
		} else {
			flip = true;
		}
		var x_label = renderagg.labelsfrombase.x_label;
		var y_label = renderagg.labelsfrombase.y_label;

		var xpos = indexmap[x_label];
		var ypos = indexmap[y_label];

		var new_id = current_id.slice(0);
		var zoom = current_zoom;
		new_id[ypos] = new_id[ypos] - 1;
		if(new_id[ypos] < 0){
			new_id[ypos] = 0;
		}
		
		//var temp_id = build_index(x,y,renderagg.labelsfrombase.x_label,renderagg.labelsfrombase.y_label);
		//console.log(["temp_id",temp_id]);
		$.getJSON($SCRIPT_ROOT+'/fetch-tile',{tile_xid: -1,tile_yid:-1,level:zoom,
					x_label:x_label,y_label:y_label,
					temp_id:new_id},function(jsondata){
			console.log(jsondata);
			redraw_graph(jsondata);
		});
		console.log(["move up: ",current_id,current_zoom,"-->",new_id,zoom]);
		current_id = new_id;
		return false;
	}

	function move_down() {
		if(flip && renderagg.inv[1]) {
			flip = false;
			return move_up();
		} else {
			flip = true;
		}
		var x_label = renderagg.labelsfrombase.x_label;
		var y_label = renderagg.labelsfrombase.y_label;

		var xpos = indexmap[x_label];
		var ypos = indexmap[y_label];

		var new_id = current_id.slice(0);
		var zoom = current_zoom;
		new_id[ypos] = new_id[ypos] + 1;
		if(new_id[ypos] >= total_tiles[ypos]){
			new_id[ypos] = total_tiles[ypos]-1;
		}
		//var temp_id = build_index(x,y,renderagg.labelsfrombase.x_label,renderagg.labelsfrombase.y_label);
		$.getJSON($SCRIPT_ROOT+'/fetch-tile',{tile_xid: -1,tile_yid:-1,level:zoom,
					x_label:x_label,y_label:y_label,
					temp_id:new_id},function(jsondata){
			console.log(jsondata);
			redraw_graph(jsondata);
		});
		console.log(["move down: ",current_id,current_zoom,"-->",new_id,zoom]);
		current_id = new_id;
		return false;
	}

	function move_left() {
		if(flip && renderagg.inv[0]) {
			flip = false;
			return move_right();
		} else {
			flip = true;
		}
		var x_label = renderagg.labelsfrombase.x_label;
		var y_label = renderagg.labelsfrombase.y_label;

		var xpos = indexmap[x_label];
		var ypos = indexmap[y_label];

		var new_id = current_id.slice(0);
		var zoom = current_zoom;
		new_id[xpos] = new_id[xpos] - 1;
		if(new_id[xpos] < 0){
			new_id[xpos]= 0;
		}
		//var temp_id = build_index(x,y,x_label,y_label);
		$.getJSON($SCRIPT_ROOT+'/fetch-tile',{tile_xid: -1,tile_yid:-1,level:zoom,
					x_label:x_label,y_label:y_label,
					temp_id:new_id},function(jsondata){
			console.log(jsondata);
			redraw_graph(jsondata);
		});
		console.log(["move left: ",current_id,current_zoom,"-->",new_id,zoom]);
		current_id = new_id;
		return false;
	}

	function move_right() {
		if(flip && renderagg.inv[0]) {
			flip = false;
			return move_left();
		} else {
			flip = true;
		}
		var x_label = renderagg.labelsfrombase.x_label;
		var y_label = renderagg.labelsfrombase.y_label;

		var xpos = indexmap[x_label];
		var ypos = indexmap[y_label];

		var new_id = current_id.slice(0);
		var zoom = current_zoom;

		new_id[xpos] = new_id[xpos] + 1;
		if(new_id[xpos] >= total_tiles[xpos]){
			new_id[xpos] = total_tiles[xpos]-1;
		}
		//var temp_id = build_index(x,y,renderagg.labelsfrombase.x_label,renderagg.labelsfrombase.y_label);
		$.getJSON($SCRIPT_ROOT+'/fetch-tile',{tile_xid: -1,tile_yid:-1,level:zoom,
					x_label:x_label,y_label:y_label,
					temp_id:new_id},function(jsondata){
			console.log(jsondata);
			redraw_graph(jsondata);
		});
		console.log(["move right: ",current_id,current_zoom,"-->",new_id,zoom]);
		current_id = new_id;
		return false;
	}

	function zoom_out() {
		var x_label = renderagg.labelsfrombase.x_label;
		var y_label = renderagg.labelsfrombase.y_label;

		var xpos = indexmap[x_label];
		var ypos = indexmap[y_label];

		var new_id = current_id.slice(0);
		var zoom = current_zoom;
		zoom = zoom - 1;
		if(zoom < 0){
			zoom = 0;
		}
		new_id[xpos] = Math.floor(new_id[xpos]/zoom_diff);
		new_id[ypos] = Math.floor(new_id[ypos]/zoom_diff);

		if(zoom != current_zoom) { // if we're actually going somewhere else
			//var temp_id = build_index(x,y,renderagg.labelsfrombase.x_label,renderagg.labelsfrombase.y_label);
			$.getJSON($SCRIPT_ROOT+'/fetch-tile',{tile_xid: -1,tile_yid:-1,level:zoom,
					x_label:x_label,y_label:y_label,
					temp_id:new_id},function(jsondata){
				console.log(jsondata);
				redraw_graph(jsondata);
			});
			console.log(["zoom out: ",current_id,current_zoom,"-->",new_id,zoom]);
			current_id = new_id;
			current_zoom = zoom;
		}
		return false;
	}

	function zoom_in2(x_offset,y_offset) {
		var x_label = renderagg.labelsfrombase.x_label;
		var y_label = renderagg.labelsfrombase.y_label;

		var xpos = indexmap[x_label];
		var ypos = indexmap[y_label];
		if(x_offset < 0) {
			x_offset = 0;
		} else if (x_offset >= future_tiles[xpos]) {
			x_offset = future_tiles[xpos] - 1;
		}
		if(y_offset < 0) {
			y_offset = 0;
		} else if (y_offset >= future_tiles[ypos]) {
			y_offset = future_tiles[ypos] - 1;
		}
		if(renderagg.inv[0]) {
			x_offset = future_tiles[xpos] - x_offset - 1;
		}
		if(renderagg.inv[1]) {
			y_offset = future_tiles[ypos] - y_offset - 1;
		}
		var new_id = current_id.slice(0);
		new_id[xpos] = current_id[xpos] * zoom_diff + x_offset;
		new_id[ypos] = current_id[ypos] * zoom_diff + y_offset;
		zoom = current_zoom + 1;
		if(zoom >= max_zoom) {
			zoom = max_zoom - 1;
		}
		console.log(["zoom",zoom,"max_zoom",max_zoom]);
		if(zoom != current_zoom) { // if we're actually going somewhere else
			//var temp_id = build_index(x,y,x_label,y_label);
			//console.log(["temp id: ",temp_id]);
			$.getJSON($SCRIPT_ROOT+'/fetch-tile',{tile_xid: -1,tile_yid:-1,level:zoom,
					x_label:x_label,y_label:y_label,
					temp_id:new_id},function(jsondata){
				console.log(jsondata);
				redraw_graph(jsondata);
			});
			console.log(["zoom in: ",current_id,current_zoom,"-->",new_id,zoom]);
			current_id = new_id;
			current_zoom = zoom;
		}
		return false;
	}

	function user_query_handler() {
		max_zoom = QVis.DEFAULT_MAX_ZOOM;
		once = 0;
		if(renderagg) {
			renderagg.clear();
		}
		current_zoom = 0;
		querytext = $('#sql-query-text').val();
		resolution_lvl = $('#resolution-lvl-menu').val();
		console.log("resolution: "+resolution_lvl);
		console.log(["script root",$SCRIPT_ROOT]);
		$('#error_message').remove();
		$('#resulting-plot-header').removeClass('show');
		$('#aggplot').removeClass('show');
		$('#outer-aggplot').removeClass('show');
		$('#aggplot-form').removeClass('show');
		$('#nav').removeClass('show');
		$('#answer-select').removeClass('show');
		$.getJSON($SCRIPT_ROOT+'/fetch-first-tile',{query: querytext,data_threshold:resolution_lvl,usenumpy:true},function(jsondata){
			console.log(jsondata);
			if(!("error" in jsondata)) {
				draw_graph(jsondata);
				$('#resulting-plot-header').addClass('show');
				$('#nav').addClass('show');
				$('#aggplot').addClass('show');
				$('#outer-aggplot').addClass('show');
				$('#answer-select').addClass('show');
				$('#answer-select-checkbox').on('click',function() {
					console.log("checking answer select checkbox");
					$('#answer-select-checked-yes').toggleClass('highlight');
					$('#answer-select-checked-no').toggleClass('highlight');
				/*
					if ($('#answer-select-checkbox').is(':checked')) {
						$('#answer-select-checked-yes').addClass('highlight');
						$('#answer-select-checked-no').removeClass('highlight');
					} else {
						$('#answer-select-checked-yes').removeClass('highlight');
						$('#answer-select-checked-no').addClass('highlight');
					}
				*/
				});
				$('#aggplot-form').addClass('show');

				// set index back to 0
				current_id = new Array(numdims);
				for (var i = 0; i < numdims; i++) {current_id[i] = 0;}

				//mapping of labels to id numbers
				indexmap = jsondata['indexes'];
			} else {
				console.log(["error!!!!! OMGOMGOMG",jsondata['error'],jsondata['error']['args'][0].indexOf("\n")]);
				var error_args = jsondata['error']['args'][0].replace(/\n/g,"<br>");
				error_args = error_args.replace(/ /g,"&nbsp");
				var error_string = "<div id=\"error_message\"><p>An error occured in running your query:</p>";
				error_string = error_string + "<p>"+error_args+"</p></div>";
				$("#resulting-plot-header").before($(error_string));
				return false;
			}
		});
		return false;
	}

	function redraw_graph(jsondata){
		// preserve existing labels
		var x_label = renderagg.labelsfrombase.x_label;
		var y_label = renderagg.labelsfrombase.y_label;
		var z_label = renderagg.labelsfrombase.z_label;
		var opts = {overlap:-0, r:1.5};
		var data = jsondata['data'];
		var labels={'names' : jsondata['names'],
                   'x' : x_label,
		   'y' : y_label,
		   'z' : z_label,
		   'dimbases':jsondata['dimbases'],
		   'dimwidths':jsondata['dimwidths'],
		   'dimnames':jsondata['dimnames'],
		   'max':jsondata['max'],
		   'min':jsondata['min'],
		   'inv':renderagg.inv};
		var types = jsondata['types'];
		
		console.log(jsondata['dimbases']);
		console.log(jsondata['dimwidths']);

		numdims =jsondata['numdims'];
		max_zoom = jsondata['max_zoom'];
		total_xtiles = jsondata['total_xtiles'];
		total_ytiles = jsondata['total_ytiles'];
		future_xtiles_exact = jsondata['future_xtiles_exact'];
		future_ytiles_exact = jsondata['future_ytiles_exact'];
		future_xtiles = jsondata['future_xtiles'];
		future_ytiles = jsondata['future_ytiles'];

		future_tiles_exact = jsondata['future_tiles_exact'];
		future_tiles = jsondata['future_tiles'];
		total_tiles = jsondata['total_tiles'];
		console.log("max zoom: "+max_zoom);
		console.log("total x/y tiles: ",total_xtiles+","+total_ytiles);
		console.log("future x/y tiles: ",future_xtiles+","+future_ytiles);

		if(once == 1) {
			once += 1;
			(function() {
				var mini_render = renderagg.mini_render;
				if($USE_CANVAS) { mini_render = renderagg.mini_render_canvas;}
				renderagg.mini_render = function(_data, _labels,_types, opts) {
					mini_render.apply(this,[_data, _labels,_types, opts]);
					console.log("got here in mouseclick thing");
					//$('svg rect').off();
					//$('svg rect').unbind();
					$('#mouseclick_rect').off();
					$('#mouseclick_rect').unbind();
					//renderagg.rectcontainer.selectAll('rect')
					renderagg.rectcontainer.selectAll('#mouseclick_rect')
						.on("click",function(d,i){return check_zoom_in(d3.mouse(this));});

					//$('svg rect')
					$('#mouseclick_rect')
						.bind("contextmenu",function(e) { return zoom_out();});
				}


			})();
		}
		renderagg.mini_render(data, labels, types, opts);

/*
		renderagg.mini_render(data, labels,types);
		renderagg.rectcontainer.selectAll('rect')
			.on("click",function(d,i){return check_zoom_in(d3.mouse(this));});

		$('svg rect')
			.bind("contextmenu",function(e) { return zoom_out();});
*/
	}

	function build_index(xid,yid,x_label,y_label) {
		id = [0,0];
		xpos = indexmap[x_label];
		ypos = indexmap[y_label];
		id[xpos] = xid;
		id[ypos] = yid;
		return id;
	}

	function draw_graph(jsondata) {
		menutype = $('#vis-type-menu').val();
		var opts = {overlap:-0, r:1.5};
		var use_dims = false;
		switch(menutype) {
			case 'mapplot':
				renderagg = new QVis.MapPlot('aggplot', opts);
				break;
			case 'scatterplot':
				renderagg = new QVis.ScatterPlot('aggplot', opts);
				break;
			case 'heatmap':
				renderagg = new QVis.HeatMap('aggplot',opts);
				use_dims = true;
				break;
			default:
				console.log('menu type not supported, using heatmap...');
				renderagg = new QVis.HeatMap('aggplot', opts);
				use_dims = true;
		}
		
		var data = jsondata['data'];

		// set x and y labels
		var x_label = jsondata['dimnames'][0];
		var y_label = x_label;
		if(use_dims) {
			x_label = jsondata['dimnames'][0];
			if(jsondata['dimnames'].length > 1) {
				y_label = jsondata['dimnames'][1];
			} else {
				y_label = x_label;
			}
		} else if(jsondata['names'].length > 0) {
			y_label = jsondata['dimnames'][1];
		}

		var labels={'names' : jsondata['names'],
                   'x' : x_label,
		   'y' : y_label,
		   'z' : '',
		   'dimbases':jsondata['dimbases'],
		   'dimwidths':jsondata['dimwidths'],
		   'dimnames':jsondata['dimnames'],
		   'max':jsondata['max'],
		   'min':jsondata['min']};
		var types = jsondata['types'];
		
		console.log(jsondata['dimbases']);
		console.log(jsondata['dimwidths']);

		numdims =jsondata['numdims'];
		zoom_diff = jsondata['zoom_diff'];
		max_zoom = jsondata['max_zoom'];
		total_xtiles = jsondata['total_xtiles'];
		total_ytiles = jsondata['total_ytiles'];
		future_xtiles_exact = jsondata['future_xtiles_exact'];
		future_ytiles_exact = jsondata['future_ytiles_exact'];
		future_xtiles = jsondata['future_xtiles'];
		future_ytiles = jsondata['future_ytiles'];

		future_tiles_exact = jsondata['future_tiles_exact'];
		future_tiles = jsondata['future_tiles'];
		total_tiles = jsondata['total_tiles'];
		console.log("max zoom: "+max_zoom);

		if(once == 0){
			once += 1;
			(function() {
				var render = renderagg.render;
				if($USE_CANVAS) { render = renderagg.render_canvas;}
				renderagg.render = function(_data, _labels,_types, _opts) {
					render.apply(this,[_data, _labels,_types, _opts]);
					console.log("got here in mouseclick thing");
					//$('svg rect').off();
					//$('svg rect').unbind();
					$('#mouseclick_rect').off();
					$('#mouseclick_rect').unbind();
					renderagg.rectcontainer.selectAll('#mouseclick_rect')
						.on("click",function(d,i){return check_zoom_in(d3.mouse(this));});

					//$('svg rect')
					$('#mouseclick_rect')
						.bind("contextmenu",function(e) { return zoom_out();});
				}


			})();
		}

		renderagg.render(data, labels,types, opts);
		console.log(["after render call:",current_id,current_zoom]);
	}

	function check_zoom_in(coords) {
		console.log(["coords:",coords,"future_tiles_exact",future_tiles_exact]);
		// cut up the space according to the size of the tiles
		var width = Math.min(1.0*renderagg.w / future_tiles_exact[indexmap[renderagg.labelsfrombase.x_label]],renderagg.w);
		var height = Math.min(1.0*renderagg.h / future_tiles_exact[indexmap[renderagg.labelsfrombase.y_label]],renderagg.h);

		// adjust for padding in graph
		var xdim = 1.0*coords[0] - renderagg.px;
		var ydim = 1.0*coords[1] - renderagg.py;
		if(renderagg.inv[0]) {
			xdim = 1.0*renderagg.w - xdim;
		}
		if(renderagg.inv[1]){
			ydim= 1.0*renderagg.h-ydim;
		}

		var xindex = Math.floor(xdim / width);
		var yindex = Math.floor(ydim / height);
		console.log(["width",width,"height",height,"xdim",xdim,"ydim",ydim,"xindex",xindex,"yindex",yindex,"future_xtiles_exact",future_tiles_exact[indexmap[renderagg.labelsfrombase.x_label]],"future_ytiles_exact",future_tiles_exact[indexmap[renderagg.labelsfrombase.y_label]]]);
		return zoom_in2(xindex,yindex);
	}
});
