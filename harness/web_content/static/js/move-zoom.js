var legend = $("#legend-content");
var userid = '';
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
    $('[rel=tooltip]').tooltip(); // enable tooltips
    $('#button-up').on('click',move_up);
    $('#button-down').on('click',move_down);
    $('#button-left').on('click',move_left);
    $('#button-right').on('click',move_right);
    $('#reset-query-button a').on('click',user_query_handler);

   function move_up(){}
   function move_down(){}
   function move_left(){}
   function move_right(){}

    function get_redraw_data(zoom,x_label,y_label,xpos,ypos,new_id) {
        $("body").css("cursor", "progress");
        $('<div id="canvas-overlay"></div>')
            .attr('width', this.w)
            .attr('height', this.h)
            .appendTo('#aggplot');
        fetch_tile(zoom,x_label,y_label,new_id);
    }

    function get_redraw_data_callback(jsondata) {
        $('#canvas-overlay').remove();
        $("body").css("cursor", "auto");
        if(!("error" in jsondata)) {
            console.log('redraw data callback called');
            console.log(['jsondata',jsondata,jsondata['selected']]);
            redraw_graph(jsondata);
        } else {
            $('#resulting-plot-header').removeClass('show');
            $('#nav').removeClass('show');
            $('#answer-select').removeClass('show');
            $('#aggplot-form').removeClass('show');
            $('#legend').removeClass('show');

            console.log(["error!!!!! OMGOMGOMG",jsondata['error'],jsondata['error']['args'][0].indexOf("\n")]);
            var error_args = jsondata['error']['args'][0].replace(/\n/g,"<br>");
            error_args = error_args.replace(/ /g,"&nbsp");
            var error_string = "<div id=\"error_message\"><p>An error occured in running your query:</p>";
            error_string = error_string + "<p>"+error_args+"</p></div>";
            $("#resulting-plot-header").before($(error_string));
        }
        return false;
    }

    function fetch_tile(zoom,x_label,y_label,new_id) {
        var self = this;
        var tries = 1;
        var maxtries = 120;

        self.getJSON = function(url,data,callback,error_string) {
            $.ajax({
                dataType: "json",
                url:url,
                data:data,
                success:callback,
                error: function(xhr, textStatus, errorThrown ) {
                    console.log('error handler called');
                    console.log(['errors',xhr,textStatus,errorThrown]);
                    callback(error_string);
                }
            });
        };
        var get_results = function(results) {
            console.log('get_results called');
            console.log(['results',results]);
            if(results === 'fail' || results.length == 0) {
                console.log('task failed');
                get_redraw_data_callback({'error':{'type':'','args':['task fail']}});
            } else if (results === 'wait') {
                console.log('task in progress');
                if(tries > maxtries) {
                    get_redraw_data_callback({'error':{'type':'','args':['timeout']}});
                } else {
                    // call this function again in 10 seconds
                    console.log("got a wait response, trying again in 1 second");
                    tries++;
                    setTimeout(self.f,1000); // try to get data again in 1 second
                }
            } else {
                console.log('task completed, initiating callback');
                console.log(['results',results]);
                get_redraw_data_callback(results);
            }
        };

        var get_jobid = function(jsondata) {
            var jobid = jsondata;
            if(jobid.length > 0) {
                // get results here bc this is when we have the jobid
                self.f = function() {
                    console.log('calling f');
                    self.getJSON(
                        $SCRIPT_ROOT+'/scalar/fetch/',
                        {jobid:jobid,gr:1,user:''},
                        get_results,
                        'fail' // error string should be fail
                    );
                };
                self.f();
            }
        };
        
	var tileid = ""+zoom+"_"+new_id[0]+"_"+new_id[1];
        // get the results
        self.getJSON(
            $SCRIPT_ROOT+'/scalar/fetch/',
            {user:'',ft:1,level:zoom, tile_id:tile_id,
                temp_id:new_id,data_set: $DATA_SET},
            get_jobid,
            '' // error string should be empty string
        );
       
    }

    function fetch_first_tile(data,resolution_lvl) {
        var self = this;
        var tries = 1;
        var maxtries = 120;

        self.getJSON = function(url,data,callback,error_string) {
            $.ajax({
                dataType: "json",
                url:url,
                data:data,
                success:callback,
                error: function(xhr, textStatus, errorThrown ) {
                    console.log('error handler called');
                    console.log(['errors',xhr,textStatus,errorThrown]);
                    callback(error_string);
                }
            });
        };
        var get_results = function(results) {
            console.log('get_results called');
            console.log(['results',results]);
            if(results === 'fail' || results.length == 0) {
                console.log('task failed');
                user_query_handler_callback({'error':{'type':'','args':['task fail']}});
            } else if (results === 'wait') {
                console.log('task in progress');
                if(tries > maxtries) {
                    user_query_handler_callback({'error':{'type':'','args':['timeout']}});
                } else {
                    // call this function again in 10 seconds
                    console.log("got a wait response, trying again in 1 second");
                    tries++;
                    setTimeout(self.f,1000); // try to get data again in 1 second
                }
            } else {
                console.log('task completed, initiating callback');
                console.log(['results',results]);
                user_query_handler_callback(results);
            }
        };

	var get_userid = function(jsondata) {
            userid = jsondata;
            console.log(["userid",userid,"length",userid.length]);
            if(userid.length > 0) {
                // get results here bc this is when we have the jobid
                self.f = function() {
                    console.log('calling f');
                    self.getJSON(
                        $SCRIPT_ROOT+'/scalar/fetch/',
            		{fft:1,user:userid,data_set: $DATA_SET,task:$TASK,data_threshold:resolution_lvl},
                        get_jobid,
                        'fail' // error string should be fail
                    );
                };
                self.f();
            }
        };

        var get_jobid = function(jsondata) {
            var jobid = jsondata;
            console.log(["jobid",jobid,"length",jobid > 0]);
            if(jobid.length > 0) {
                // get results here bc this is when we have the jobid
                self.f = function() {
                    console.log('calling f');
                    self.getJSON(
                        $SCRIPT_ROOT+'/scalar/fetch/',
                        {user:userid,jobid:jobid,gr:1},
                        get_results,
                        'fail' // error string should be fail
                    );
                };
                self.f();
            }
        };

	if(userid.length == 0) {        
        // get the userid first
        self.getJSON(
            $SCRIPT_ROOT+'/scalar/fetch/',
            {guid:1},
            get_userid,
            '' // error string should be empty string
        );
	} else {
        // get the results
        self.getJSON(
            $SCRIPT_ROOT+'/scalar/fetch/',
            {fft:1,user:userid,data_set: $DATA_SET,task:$TASK,data_threshold:resolution_lvl},
            get_jobid,
            '' // error string should be empty string
        );

	}
       
    }

    function user_query_handler_callback(jsondata) {
        //console.log(jsondata);
        //$('#loading_image').removeClass('show');
        if(!("error" in jsondata)) {
            draw_graph(jsondata);
            $('#resulting-plot-header').addClass('show');
            $('#nav').addClass('show');
            $('#aggplot').addClass('show');
            $('#legend').addClass('show');

            // set index back to 0
            current_id = new Array(numdims);
            for (var i = 0; i < numdims; i++) {current_id[i] = 0;}
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

    }

    function user_query_handler() {
        max_zoom = QVis.DEFAULT_MAX_ZOOM;
        once = 0;
        current_zoom = 0;
        //$('#resolution-lvl-menu').val();
        resolution_lvl = $RESOLUTION_LVL;
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
        $('#button-up').addClass('disabled');
        $('#button-down').addClass('disabled');
        $('#button-left').addClass('disabled');
        $('#button-right').addClass('disabled');
        
        fetch_first_tile(
            {data_set: $DATA_SET,task:$TASK,data_threshold:resolution_lvl},
            resolution_lvl
        );
        return false;
    }

    function draw_graph(jsondata) {
	console.log(jsondata);
	var attributes = jsondata["attributes"];
	var data = jsondata["data"];
	console.log(["data len",data.length / attributes.length]);
	var extrema = jsondata["extrema"];
	var tile_id = jsondata["id"];

	var l = data.length / attributes.length;
	var al = attributes.length;
	var xind = 0;
	var yind = 1;
	var colorind = 2;
	var lsmaskind = 5;
	var w = 700;
	var pad = 10;
	var h = 400;


var xscale = d3.scale.linear()
  .range([pad, w-pad])
  .domain([extrema["min"][0],extrema["max"][0]]);

var yscale = d3.scale.linear()
  .range([h-pad, pad])
  .domain([extrema["min"][1],extrema["max"][1]]);

var colorscale = d3.scale.quantize()
	.range(colorbrewer["Spectral"][9])
  .domain([1.0,-1.0]);


        $('#canvas').attr('width', w)
                .attr('height', h)
                .attr('class', 'show');

	var canvas = document.getElementById('canvas');
    var ctx = canvas.getContext('2d');
    //console.log(['use filters',temp.use_filters]);
  	ctx.beginPath();
    ctx.fillStyle = "#FFFFFF";
    ctx.fillRect(0,0,w,h); // set the back to be white first
	ctx.closePath();
    for(var drawindex = 0; drawindex < l; drawindex++) {
  	ctx.beginPath();
	if((data[drawindex*al+lsmaskind] < 4) && 
		(data[drawindex*al+lsmaskind] > 1)) {
        ctx.fillStyle = colorscale(data[drawindex*al+colorind]);
	} else {
        ctx.fillStyle = '#C0C0C0';
	}
        ctx.fillRect(xscale(data[drawindex*al+xind]),
            yscale(data[drawindex*al+yind]),2,2);
	ctx.closePath();
    }
    drawLegend(25,200,colorscale);

    }

    function redraw_graph(jsondata){
	draw_graph(jsondata);
    }

    function drawLegend(l_w,l_h,color) {
    $('#legend-content').empty();
    var xpadding = 50;
    var ypadding = 15;
    var numcolors = 9; // number of colors in color scale
    var temp = this;
    var color_domain = color.domain();
    var scale = d3.scale.linear().domain([color_domain[0],color_domain[1]]).range([ypadding,l_h+ypadding]);
    var ticks = [color_domain[0]];
    var step = 1.0 *(color_domain[1] - color_domain[0]) / numcolors; // divide domain by number of colors in the scale
    for(var i = 1; i < numcolors; i++) { // loop over number of colors in scale - 1 to get ticks, I only use 9 by default
        ticks.push(color_domain[0]+i*step);
    }
    ticks.push(color_domain[1]);
    console.log(["ticks",ticks]);
    var axis = d3.svg.axis().scale(scale).orient("left").tickValues(ticks).tickFormat(d3.format(".2f"));
    var svg = d3.select('#legend-content').append("svg")
        .attr("width",l_w+xpadding)
        .attr("height",l_h+2*ypadding);
    svg.selectAll("rect")
        .data(ticks.slice(0,ticks.length-1))
        .enter().append("rect")
        .attr("x", xpadding)
        .attr("y", function(d) { return scale(d); })
        .attr("width",l_w)
        .attr("height",Math.round(l_h/numcolors))
        .attr("fill",function(d) { return color(d) });
    svg.append("g").attr("class","legend-axis").attr("transform","translate("+xpadding+",0)").call(axis);
}
    user_query_handler();
});
