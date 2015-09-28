var renderagg = null;
var query = null;
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
	$('ul.nav-list > li.disabled > a').click(function () { return false; }); // don't let IE users click on disabled nav links
	$('[rel=tooltip]').tooltip(); // enable tooltips
	$('#sql-query-submit').on('click',user_query_handler);
	$('#button-up').on('click',move_up);
	$('#button-down').on('click',move_down);
	$('#button-left').on('click',move_left);
	$('#button-right').on('click',move_right);
	//$('#done-button a').on('click',goto_next);
	$('#reset-query-button a').on('click',user_query_handler);
	$('#answer-select-checkbox').on('click',function() {
		console.log("checking answer select checkbox");
		$('#answer-select-checked-yes').toggleClass('highlight');
		$('#answer-select-checked-no').toggleClass('highlight');
		if ($('#answer-select-checkbox').is(':checked')) {
			var canvasImg = $('#canvas')[0].toDataURL('image/jpeg'); // get image data
			$.post($SCRIPT_ROOT+'/scalar/tile-selected/',{img:canvasImg,
				x_label:renderagg.vis_metadata['x_label'],
				y_label:renderagg.vis_metadata['y_label'],
				z_label:renderagg.vis_metadata['z_label'],
				x_inv:renderagg.vis_metadata['x_inv'],
				y_inv:renderagg.vis_metadata['y_inv'],
				z_inv:renderagg.vis_metadata['z_inv'],
				color:renderagg.vis_metadata['color'],
				width:renderagg.vis_metadata['width'],
				height:renderagg.vis_metadata['height']});
		} else {
			$.get($SCRIPT_ROOT+'/scalar/tile-unselected/',{});
		}
	});

        function handle_selection(selected) {
		if(selected) { // check if user selected data tile as answer
			$('#answer-select-checkbox').attr('checked',true);
			$('#answer-select-checked-yes').addClass('highlight');
			$('#answer-select-checked-no').removeClass('highlight');
		} else {
			$('#answer-select-checkbox').attr('checked',false);
			$('#answer-select-checked-yes').removeClass('highlight');
			$('#answer-select-checked-no').addClass('highlight');
		}
	}

	function check_same_id(new_id) {
		for(var i = 0; i < current_id.length; i++) { // see if id changed
			if (current_id[i] !== new_id[i]) {
				return false;
			}
		}
		return true; // id is the same
	}

	function disable_directions(xpos,ypos) {
		var disabled = [$('#button-up').hasClass('disabled'),
				$('#button-down').hasClass('disabled'),
				$('#button-left').hasClass('disabled'),
				$('#button-right').hasClass('disabled')];
		console.log(["disabled",disabled]);
		console.log(["xpos",xpos,"ypos",ypos]);
		// disable up
		if((renderagg.inv[1] && (current_id[ypos] < (total_tiles[ypos]-1))) || 
			(!renderagg.inv[1] && (current_id[ypos] > 0))) {
			$('#button-up').removeClass('disabled');
		} else if (!disabled[0]) {
			$('#button-up').addClass('disabled');
		}
		// disable down
		if((renderagg.inv[1] && (current_id[ypos] > 0)) || 
			(!renderagg.inv[1] && (current_id[ypos] < (total_tiles[ypos]-1)))) {
			$('#button-down').removeClass('disabled');
		} else if (!disabled[1]) {
			$('#button-down').addClass('disabled');
		}
		// disable left
		if((renderagg.inv[0] && (current_id[xpos] < (total_tiles[xpos]-1))) || 
			(!renderagg.inv[0] && (current_id[xpos] > 0))) {
			$('#button-left').removeClass('disabled');
		} else if (!disabled[2]) {
			$('#button-left').addClass('disabled');
		}
		// disable right
		if((renderagg.inv[0] && (current_id[xpos] > 0)) || 
			(!renderagg.inv[0] && (current_id[xpos] < (total_tiles[xpos]-1)))) {
			$('#button-right').removeClass('disabled');
		} else if (!disabled[3]) {
			$('#button-right').addClass('disabled');
		}
	}

	function disable_zooms() {
		$('#zoom-disable-div').empty();
		$('#zoom-disable-div').removeClass('show');
		if(current_zoom === 0) { // disable zoom out
			$('#zoom-disable-div').addClass('show');
			$('#zoom-disable-div').append('<span>can\'t zoom out (at highest zoom level)</span>');
		} else if (current_zoom === (max_zoom - 1)) { // disable zoom in
			$('#zoom-disable-div').addClass('show');
			$('#zoom-disable-div').append('<span>can\'t zoom in (at lowest zoom level)</span>');
		}
	}

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
		
		if(!check_same_id(new_id)) { // if we're actually going somewhere else
			console.log(["move up: ",current_id,current_zoom,"-->",new_id,zoom]);
			current_id = new_id;
			get_redraw_data(zoom,x_label,y_label,xpos,ypos,new_id);
		}
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

		if(!check_same_id(new_id)) { // if we're actually going somewhere else
			console.log(["move down: ",current_id,current_zoom,"-->",new_id,zoom]);
			current_id = new_id;
			get_redraw_data(zoom,x_label,y_label,xpos,ypos,new_id);
		}
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

		if(!check_same_id(new_id)) { // id changed
			console.log(["move left: ",current_id,current_zoom,"-->",new_id,zoom]);
			current_id = new_id;
			get_redraw_data(zoom,x_label,y_label,xpos,ypos,new_id);
		}
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

		if(!check_same_id(new_id)) { // if we're actually going somewhere else
			console.log(["move right: ",current_id,current_zoom,"-->",new_id,zoom]);
			current_id = new_id;
			get_redraw_data(zoom,x_label,y_label,xpos,ypos,new_id);
		}
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

		if(current_zoom !== zoom) { // if we're actually going somewhere else
			console.log(["zoom out: ",current_id,current_zoom,"-->",new_id,zoom]);
			current_id = new_id;
			current_zoom = zoom;
			get_redraw_data(zoom,x_label,y_label,xpos,ypos,new_id);
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
		console.log(["current_zoom",current_zoom,"zoom",zoom,"max_zoom",max_zoom]);
		if(current_zoom !== zoom) { // if we're actually going somewhere else
			console.log(["zoom in: ",current_id,current_zoom,"-->",new_id,zoom]);
			current_id = new_id;
			current_zoom = zoom;
			get_redraw_data(zoom,x_label,y_label,xpos,ypos,new_id);
		}
		return false;
	}

	function get_redraw_data(zoom,x_label,y_label,xpos,ypos,new_id) {
		$("body").css("cursor", "progress");
		$('<div id="canvas-overlay"></div>')
			.attr('width', this.w)
			.attr('height', this.h)
			.appendTo('#aggplot');
		$.getJSON($SCRIPT_ROOT+'/scalar/fetch-tile',{tile_xid: -1,tile_yid:-1,level:zoom,
			x_label:x_label,y_label:y_label,
			temp_id:new_id,data_set: $DATA_SET},
			function(jsondata){
				console.log(jsondata);
				redraw_graph(jsondata);
				handle_selection(jsondata['selected']);
				$('#canvas-overlay').remove();
				$("body").css("cursor", "auto");

				disable_directions(xpos,ypos); // need to do this after total_tiles is updated
				disable_zooms();
		});
	}

	function user_query_handler() {
		max_zoom = QVis.DEFAULT_MAX_ZOOM;
		once = 0;
		if(renderagg) {
			renderagg.clear();
		}
		current_zoom = 0;
		resolution_lvl = $('#resolution-lvl-menu').val();
		console.log("resolution: "+resolution_lvl);
		console.log(["script root",$SCRIPT_ROOT]);
		$('#error_message').remove();
		$('#resulting-plot-header').removeClass('show');
		$('#aggplot').removeClass('show');
		$('#aggplot-form').removeClass('show');
		$('#nav').removeClass('show');
		$('#answer-select').removeClass('show');
		$('#legend').removeClass('show');
		//$('#loading_image').addClass('show');
		$("body").css("cursor", "progress");
		$('#vis-loading-modal').modal('show');
		$.getJSON($SCRIPT_ROOT+'/scalar/fetch-first-tile',{data_set: $DATA_SET, task:$TASK,data_threshold:resolution_lvl},function(jsondata){
			console.log(jsondata);
			//$('#loading_image').removeClass('show');
			if(!("error" in jsondata)) {
				draw_graph(jsondata);
				handle_selection(jsondata['selected']);
				$('#resulting-plot-header').addClass('show');
				$('#nav').addClass('show');
				$('#aggplot').addClass('show');
				$('#answer-select').addClass('show');
				$('#aggplot-form').addClass('show');
				$('#legend').addClass('show');

				//disable directional buttons (should all be disabled)
				var x_label = renderagg.labelsfrombase.x_label;
				var y_label = renderagg.labelsfrombase.y_label;
				disable_directions(indexmap[x_label],indexmap[y_label]); // need to do this after total_tiles is updated
				disable_zooms();

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
				$('#vis-loading-modal').modal('hide');
				return false;
			}
			
			$('#vis-loading-modal').modal('hide');
			$("body").css("cursor", "auto");
		});
		return false;
	}

	function redraw_graph(jsondata){
		// TODO: hardcode xdim and ydim
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
				var mini_render = renderagg.mini_render_canvas;
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
		//TODO: hardcode xdim and ydim
		var opts = {overlap:-0, r:1.5};
		var use_dims = true;
		renderagg = new QVis.HeatMap('aggplot', opts);
		
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
				var render = renderagg.render_canvas;
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

	user_query_handler();
});
