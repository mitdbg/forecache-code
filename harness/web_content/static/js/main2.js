var renderagg = null;
var menutype;

$(document).ready(function() {
	$('#sql-query-submit').on('click',user_query_handler);
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
		//$('#loading_image').addClass('show');
		$("body").css("cursor", "progress");
		$.getJSON($SCRIPT_ROOT+'/scalar/json-data',{query: querytext,data_threshold:resolution_lvl},function(jsondata){
			//$('#loading_image').removeClass('show');
			$("body").css("cursor", "auto");
			console.log(jsondata);
			if(!("error" in jsondata)) {
				if(jsondata['reduce_res']) { // query met reduce_res requirements
					var user_reduce_res = confirm('Query result will be large. Reduce resolution?');
					if(user_reduce_res) {
						$( "#dialog" ).dialog("open");
					} else { // run original query
						console.log("running original query");
						noreduce(querytext);
					}
					return false;
				}
				$('#resulting-plot-header').addClass('show');
				draw_graph(jsondata);
				$('#aggplot').addClass('show');
				$('#answer-select').addClass('show');
				$('#aggplot-form').addClass('show');
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

	function reduce(querytext,reduce_type,predicate) {
		options = {query: querytext, reduce_type: reduce_type,resolution:resolution_lvl};
		if(predicate) {
			options.predicate = predicate;
		}
		$.getJSON($SCRIPT_ROOT+'/scalar/json-data-reduce',options,function(jsondata){
			console.log('jsondata: '+jsondata);
			$('#resulting-plot-header').addClass('show');
			draw_graph(jsondata);
			$('#aggplot').addClass('show');
			$('#answer-select').addClass('show');
			$('#aggplot-form').addClass('show');
		});
		return false;
	}

	function noreduce(querytext) {
		$.getJSON($SCRIPT_ROOT+'/scalar/json-data-noreduction',{query: querytext},function(jsondata){
			console.log(jsondata);
			$('#resulting-plot-header').addClass('show');
			draw_graph(jsondata);
			$('#aggplot').addClass('show');
			$('#answer-select').addClass('show');
			$('#aggplot-form').addClass('show');
		});
		return false;
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

		if(menutype === 'mapplot'){
			renderagg.render2(data,labels,types,opts);
		} else {
			renderagg.render(data, labels,types, opts);
		}
	}
});
