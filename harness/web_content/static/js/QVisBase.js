var QVis = {}; // namespace

QVis.DEFAULT_MAX_ZOOM = 1;

//Graph object constructor
QVis.Graph = function(rootid,opts) {
    opts = opts || {};
    console.log(['Graph opts', opts, opts.r||1.5])
    this.rootid = rootid;
    this.overlap = opts.overlap || -2;

    this.jsvg = null;
    this.jlegend = null;
    this.filterdiv = null;
    this.slidersdiv = null;
    this.xlabeldiv = null;
    this.ylabeldiv = null;
    this.svg = null;

    this.h = opts['h'] || 600;
    this.w = opts['w'] || 800;
    this.colorscheme = opts['color'] || "GnBu";
    this.px = 0;//80;
    this.py = 0;//30;
}

//error reporting function
QVis.Graph.prototype.error = function(msg) {
    div = $("<div/>").attr({'class':"alert alert-error"})
    a = $("<a/>").addClass('close').attr('data-dismiss', 'alert').text('x')
    div.append(a).text(msg);
    $("#messagebox").append(div);
}

//option updating function
QVis.Graph.prototype.update_opts = function (opts) {
    if (!opts) return;
    this.overlap = opts['overlap'] || this.overlap || -2;
    this.r = opts['r'] || this.r || 1.5;
    this.h = opts['h'] || this.h || 600;
    this.w = opts['w'] || this.w || 800;    
    this.colorscheme = opts['color'] || this.colorscheme || "GnBu";    
}

//data wrapper function
QVis.Graph.prototype.get_data_obj = function(d,type){
    if(this.isNumber(type)) {
        return d;
    } else if(type === 'datetime') {
        return new Date(d*1000);
    } else { // default
        return d;
    }
}

QVis.Graph.prototype.isNumber = function(type) {
    return type === 'double' || type === 'float'
        || type.substring(0,4) === 'uint'
        || type.substring(0,3) === 'int';
}

QVis.Graph.prototype.isDate = function(type) {
    return type === 'datetime' || type === 'datetimez';
}

QVis.Graph.prototype.isString = function(type) {
    return type.substring(0,6) === 'string';
}

//dupe remover (for primatives and strings only) function
QVis.Graph.prototype.remove_dupes = function(arr) {
    var i,
        len=arr.length,
        out=[],
        obj={};

    for (i=0;i<len;i++) {
        obj[arr[i]]=0;
    }
    for (i in obj) {
        out.push(i);
    }
    return out;
}

//TODO: fix this. it's ugly
QVis.Graph.prototype.newCreateScale = function(_data,_types,label,axislength,axispadding,invert,color) {
    var scale;
    if(this.isNumber(_types[label])) {
        minx = d3.min(_data[label]);
        maxx = d3.max(_data[label]);
        console.log("ranges: "+minx+","+maxx);
        if(color){
            scale = d3.scale.quantize();
        } else {
            scale = d3.scale.linear();
        }
        if(invert) {
            scale.domain([this.get_data_obj(maxx,_types[label]), this.get_data_obj(minx,_types[label])])
                .range([axispadding,axislength-axispadding]);
        } else {
            scale.domain([this.get_data_obj(minx,_types[label]), this.get_data_obj(maxx,_types[label])])
                .range([axispadding,axislength-axispadding]);
        }
    } else if (this.isDate(_types[label])) {
        minx = d3.min(_data[label]);
        maxx = d3.max(_data[label]);

        console.log("ranges: "+minx+","+maxx);
        console.log("true date ranges: "+this.get_data_obj(minx,_types[label])+","+this.get_data_obj(maxx,_types[label]));
        if(color && invert) {
            scale = d3.scale.quantize().domain([maxx,minx])
                    .range([axispadding,axislength-axispadding]);
        } else if (color) {
            scale = d3.scale.quantize().domain([minx,maxx])
                    .range([axispadding,axislength-axispadding]);
        } else if(invert) {
                scale = d3.time.scale().domain([this.get_data_obj(maxx,_types[label]), this.get_data_obj(minx,_types[label])])
                    .range([axispadding,axislength-axispadding]);
        } else {
            scale = d3.time.scale().domain([this.get_data_obj(minx,_types[label]), this.get_data_obj(maxx,_types[label])])
                .range([axispadding,axislength-axispadding]);
        }
    } else if (this.isString(_types[label])) {
        self.strings = _data[label]
        self.strings = this.remove_dupes(self.strings);
        var steps = (axislength-2*axispadding)/(self.strings.length-1);
        var range = [];
        if(invert) {
            for(var i = self.strings.length-1; i >= 0 ;i--){
                range.push(axispadding+steps*i);
            }
        } else {
            for(var i = 0; i < self.strings.length;i++){
                range.push(axispadding+steps*i);
            }
        }
        scale = d3.scale.ordinal().domain(self.strings)
        if(!color) {
            scale.range(range);
        }
    } else {
        console.log("unrecognized type: "+_types[label] + " for "+label);
    }
    return scale;
}

//TODO: fix this. it's ugly
QVis.Graph.prototype.createScale = function(_data,_types,label,axislength,axispadding,invert,color) {
    var scale;
    if(this.isNumber(_types[label])) {
        minx = d3.min(_data.map(function(d){return d[label];}));
        maxx = d3.max(_data.map(function(d){return d[label];}));
        console.log("ranges: "+minx+","+maxx);
        if(color){
            scale = d3.scale.quantize();
        } else {
            scale = d3.scale.linear();
        }
        if(invert) {
            scale.domain([this.get_data_obj(maxx,_types[label]), this.get_data_obj(minx,_types[label])])
                .range([axispadding,axislength-axispadding]);
        } else {
            scale.domain([this.get_data_obj(minx,_types[label]), this.get_data_obj(maxx,_types[label])])
                .range([axispadding,axislength-axispadding]);
        }
    } else if (this.isDate(_types[label])) {
        minx = d3.min(_data.map(function(d){return d[label];}));
        maxx = d3.max(_data.map(function(d){return d[label];}));
        console.log("ranges: "+minx+","+maxx);
        console.log("true date ranges: "+this.get_data_obj(minx,_types[label])+","+this.get_data_obj(maxx,_types[label]));
        if(color && invert) {
            scale = d3.scale.quantize().domain([maxx,minx])
                    .range([axispadding,axislength-axispadding]);
        } else if (color) {
            scale = d3.scale.quantize().domain([minx,maxx])
                    .range([axispadding,axislength-axispadding]);
        } else if(invert) {
                scale = d3.time.scale().domain([this.get_data_obj(maxx,_types[label]), this.get_data_obj(minx,_types[label])])
                    .range([axispadding,axislength-axispadding]);
        } else {
            scale = d3.time.scale().domain([this.get_data_obj(minx,_types[label]), this.get_data_obj(maxx,_types[label])])
                .range([axispadding,axislength-axispadding]);
        }
    } else if (this.isString(_types[label])) {
        self.strings = []
        _data.map(function(d){self.strings.push(d[label]);});
        self.strings = this.remove_dupes(self.strings);
        var steps = (axislength-2*axispadding)/(self.strings.length-1);
        var range = [];
        if(invert) {
            for(var i = self.strings.length-1; i >= 0 ;i--){
                range.push(axispadding+steps*i);
            }
        } else {
            for(var i = 0; i < self.strings.length;i++){
                range.push(axispadding+steps*i);
            }
        }
        scale = d3.scale.ordinal().domain(self.strings)
        if(!color) {
            scale.range(range);
        }
    } else {
        console.log("unrecognized type: "+_types[label] + " for "+label);
    }
    return scale;
}
// clear the relevant
QVis.Graph.prototype.clear = function() {
    //$('#resulting-plot-header').removeClass('show');
    this.map = $("#"+this.rootid + " #map");
    this.jsvg = $("#"+this.rootid + " svg");
    this.jlegend = $("#"+this.rootid+" .legend");
    this.filterdiv = $("#filter-select-div");
    this.slidersdiv = $("#sliders-div");
    this.xlabeldiv = $("#"+this.rootid+"-form .xlabel");    
    this.ylabeldiv = $("#"+this.rootid+"-form .ylabel");    
    this.zlabeldiv = $("#"+this.rootid+"-form .zlabel");    
    this.legend = $("#legend-content");
    this.filterdiv.find("select").remove();
    this.xlabeldiv.find("select").remove();//.empty();
    this.ylabeldiv.find("select").remove();//.empty();
    this.zlabeldiv.find("select").remove();//.empty();
    this.jsvg.empty(); this.jlegend.empty();
    this.map.empty();
    this.legend.empty();
    this.map.append('<div></div>');
    this.brush = null;
    this.max = null;
    this.min = null;
    this.inv = null;
}

// perform basic rendering tasks common to all graphs
QVis.Graph.prototype.render = function(_data, _labels,_types, opts) {
    //clear everything to get them ready for drawing
    this.clear();

    // user wants us to draw something, so we know we have data now.
    //assumption: data is always presented with labels
    if (!_labels || typeof(_labels) == 'undefined') {
        QVis.error("Did not get any data to render!")
        return;
    }

    this.update_opts(opts); // if new options are passed, update the options

    // you should know why this is necessary
    var self = this;

    if(!self.hasOwnProperty('filter_labels')) {
        self.filter_labels = [];
    }

    this.max = _labels['max'];
    this.min = _labels['min'];
    this.inv = 'inv' in _labels ? _labels['inv'] : [false,true,true];
    console.log("max:");
    console.log(this.max);
    console.log("min:");
    console.log(this.min);

    //if x and y axes should be restricted to dimensions only

    var labelnames = [];
    var found_xlabel = false;
    var found_ylabel = false;
    var attrnames = [];
    var dimnames = _labels.dimnames;

    for(var i = 0; i < _labels.names.length; i++) {
        if(_labels.names[i]['isattr']){ // get all attribute names
            attrnames.push(_labels.names[i]['name']);
        }
        if(!this.dimsonly || !_labels.names[i]['isattr']) {
            if(_labels.names[i]['name'] === _labels.x){
                found_xlabel = true;
            }
            if(_labels.names[i]['name'] === _labels.y){
                found_ylabel = true;
            }
        }
    }

    labelnames.push.apply(labelnames,dimnames);
    if(!this.dimsonly){
        labelnames.push.apply(labelnames,attrnames);
    }


    if(this.dimsonly && labelnames.length > 0) { // fixup xlabel and ylabel
        if(!found_xlabel) {
            _labels.x = labelnames[0]['name'];
        }
        if(!found_ylabel) {
            _labels.y = labelnames[0]['name'];
        }
    }

    var x_label = _labels.x,
        y_label = _labels.y,
        z_label = _labels.z;

    if(this.selectz) {
        if(_labels.z === '') {
            for(var i = 0; i < _labels.names.length; i++) {
                if(this.isNumber(_types[_labels.names[i]['name']])) {
                    z_label = _labels.names[i]['name'];            
                    break;
                }
            }
        } else {
            z_label = _labels.z;
        }
    }
    console.log('z_label: '+z_label);
    
    // render x-axis select options
    if(this.selectx) {
        //this.xlabeldiv.prepend($("<label for=\"xaxis-select\">invert</label>"));
        this.xlabeldiv.find("#xlabel-select").after($("<select name=\"xaxis-select\" id=\"xaxis-select\" class=\"span2\"></select>"));
        var xaxisselect = this.xlabeldiv.find("select");
        var xaxisattrselect = xaxisselect.append($('<optgroup id="xaxis-attrs" label="attrs"></optgroup>')).find("#xaxis-attrs");
        var xaxisdimselect = xaxisselect.append($('<optgroup id="xaxis-dims" label="dims"></optgroup>')).find("#xaxis-dims");
        if(!this.dimsonly) {
            var xaxislabel = d3.selectAll(xaxisattrselect.get()).selectAll("option")
                    .data(attrnames)
                .enter().append("option")
                    .attr("value", function(d) { return d;})
                    .text(function(d) { return d;});
        }
        var xaxislabel = d3.selectAll(xaxisdimselect.get()).selectAll("option")
                .data(dimnames)
            .enter().append("option")
                .attr("value", function(d) { return d;})
                .text(function(d) { return d;});
        console.log(['xaxisselect',xaxisselect]);
        xaxisselect.val(x_label);
    }

    // render y-axis select options
    if(this.selecty) {
        //var yaxisselect = this.ylabeldiv.prepend($("<select></select>")).find("select");
        this.ylabeldiv.find("#ylabel-select").after($("<select name=\"yaxis-select\" id=\"yaxis-select\" class=\"span2\"></select>"));
        var yaxisselect = this.ylabeldiv.find("select");
        var yaxisattrselect = yaxisselect.append($('<optgroup id="yaxis-attrs" label="attrs"></optgroup>')).find("#yaxis-attrs");
        var yaxisdimselect = yaxisselect.append($('<optgroup id="yaxis-dims" label="dims"></optgroup>')).find("#yaxis-dims");
        if(!this.dimsonly) {
            var yaxislabel = d3.selectAll(yaxisattrselect.get()).selectAll("option")
                    .data(attrnames)
                .enter().append("option")
                    .attr("value", function(d) { return d;})
                    .text(function(d) { return d;});
        }
        var yaxislabel = d3.selectAll(yaxisdimselect.get()).selectAll("option")
                .data(dimnames)
            .enter().append("option")
                .attr("value", function(d) { return d;})
                .text(function(d) { return d;});
        yaxisselect.val(y_label);
    }

    // render z-axis select options
    if(this.selectz) {
        //var zaxisselect = this.zlabeldiv.prepend($("<select></select>")).find("select");
        this.zlabeldiv.find("#zlabel-select").after($("<select name=\"zaxis-select\" id=\"zaxis-select\" class=\"span2\"></select>"));
        var zaxisselect = this.zlabeldiv.find("select");
        var zaxisattrselect = zaxisselect.append($('<optgroup id="zaxis-attrs" label="attrs"></optgroup>')).find("#zaxis-attrs");
        var zaxisdimselect = zaxisselect.append($('<optgroup id="zaxis-dims" label="dims"></optgroup>')).find("#zaxis-dims");
        var zaxislabel = d3.selectAll(zaxisattrselect.get()).selectAll("option")
                .data(attrnames.filter(function(d){return self.isNumber(_types[d]);}))
            .enter().append("option")
                .attr("value", function(d) { return d;})
                .text(function(d) { return d;});
        var zaxislabel = d3.selectAll(zaxisdimselect.get()).selectAll("option")
                .data(dimnames.filter(function(d){return self.isNumber(_types[d]);}))
            .enter().append("option")
                .attr("value", function(d) { return d;})
                .text(function(d) { return d;});
        zaxisselect.val(z_label);
    }

        // setup select menu for filters
        this.filterdiv.find("#filter-select-label").after($("<select name=\"filter-select\" id=\"filter-select\" class=\"span2\"></select>"));
        var filterselect = this.filterdiv.find("select");
        var filterattrselect = filterselect.append($('<optgroup id="filter-attrs" label="attrs"></optgroup>')).find("#filter-attrs");
        var filterdimselect = filterselect.append($('<optgroup id="filter-dims" label="dims"></optgroup>')).find("#filter-dims");
        var filterlabel = d3.selectAll(filterattrselect.get()).selectAll("option")
                .data(attrnames.filter(function(d){return self.isNumber(_types[d]);}))
            .enter().append("option")
                .attr("value", function(d) { return d;})
                .text(function(d) { return d;});
/* don't include dimensions for now
        var filterlabel = d3.selectAll(filterdimselect.get()).selectAll("option")
                .data(dimnames.filter(function(d){return self.isNumber(_types[d]);}))
            .enter().append("option")
                .attr("value", function(d) { return d;})
                .text(function(d) { return d;});
*/
    console.log(['filterselect',filterselect]);
    filterselect.val(z_label);

    // setup the apply filters button
    $('#apply-filter-submit').unbind('click');
    $('#apply-filter-submit').click(function() {
        self.use_filters = true;
        self.get_filter_ranges();
        $.get($SCRIPT_ROOT+'/scalar/filters-applied/',{
            'filter_labels[]':self.filter_labels,
            'filter_lowers[]':self.filter_lowers,
            'filter_uppers[]':self.filter_uppers
        });

        self.render(_data, _labels,_types, opts);
        return false;
    });

    // setup the clear filters button
    $('#clear-filter-submit').unbind('click');
    $('#clear-filter-submit').click(function() {
        $.get($SCRIPT_ROOT+'/scalar/filters-cleared/',{});
        self.use_filters = false;
        self.render(_data, _labels,_types, opts);
        return false;
    });

    //set to the current width and height
    $('#update-width').val(self.w);
    $('#update-height').val(self.h);

    // Manually set the new labels and call render_scatterplot again
    (function() {
        var selectedzval = z_label; // get the previous values
        var selectedxval = x_label;
        var selectedyval = y_label;
        var inv = 'inv' in _labels ? _labels['inv'] : [false,true,true];
        console.log(['inv' in _labels,'x' in _labels]);

        self.vis_metadata = {x_label:x_label, y_label:y_label, z_label:z_label, // strings
                    x_inv:inv[0], y_inv:inv[1], z_inv: inv[2], // booleans
                    color:self.colorscheme, // string
                    width: self.w, height: self.h}; // ints

        $("#vis-update-submit").off('click');
        $("#vis-update-submit").click(function() {
            var zval = '';//$("#"+self.rootid+" .zlabel select").val();
            var yval = '';//$("#"+self.rootid+" .zlabel select").val();
            var xval = '';//$("#"+self.rootid+" .zlabel select").val();

            var width = self.w;
            var height = self.h;
            var color = self.colorscheme;

            var inv_new = [false,true,true];
            console.log(["old radio vals: ",inv]);
            console.log(["old color scheme:",color]);
            if(self.selectz) {
                zval = $("#"+self.rootid+"-form .zlabel select").val(); // should be the same as before
                inv_new[2]= $("#"+self.rootid+"-form input:radio[name='zlabel-radio']:checked").val() !== "";
            }
            if(self.selecty) {
                yval = $("#"+self.rootid+"-form .ylabel select").val(); // should be the same as before
                inv_new[1]= $("#"+self.rootid+"-form input:radio[name='ylabel-radio']:checked").val() !== "";
            }
            if(self.selectx) {
                xval = $("#"+self.rootid+"-form .xlabel select").val(); // should be the same as before
                inv_new[0]= $("#"+self.rootid+"-form input:radio[name='xlabel-radio']:checked").val() !== "";
            }

            var neww = $('#update-width').val();
            neww = isNaN(neww) ? width : neww;
            var newh = $('#update-height').val();
            newh = isNaN(newh) ? height : newh;
            var newcolor = $('#color-scheme').val();
            var newcolor = $('#color-scheme').val();

            console.log(["selected options", zval,yval,xval]);
            console.log(["new radio vals: ",inv_new]);
            console.log(["new dims:",neww,newh]);
            console.log(["new color scheme:",newcolor]);
            // if the values haven't changed
            if ((!self.selectz || ((zval === selectedzval) && (inv[2] === inv_new[2]))) 
                && (!self.selecty || ((yval === selectedyval) && (inv[1] === inv_new[1]))) 
                && (!self.selectx || ((xval === selectedxval)  && (inv[0] === inv_new[0])))
                && (width === neww)  // ignore if not a valid number
                && (height === newh) // ignore if not a valid number
                && (color === newcolor)) {
                $('#update-width').val(width);
                $('#update-height').val(height);
                return false;
            }
            selectedxval = xval;
            selectedyval = yval;
            selectedzval = zval;
            width = neww;
            height = newh;
            color = newcolor;
            opts['w'] = width;
            opts['h'] = height;
            opts['color'] = color;
            for(var i = 0; i <= 2; i++) {
                inv[i]= inv_new[i];
            }
            //inv = inv_new;
            // collect metadata for comparison purposes
            self.vis_metadata = {x_label:xval, y_label:yval, z_label:zval, // strings
                        x_inv:inv[0], y_inv:inv[1], z_inv: inv[2], // booleans
                        color:color, // string
                        width: width, height: height}; // ints

            console.log(["changed radio vals: ",inv]);
            var newlabels = {"z" : zval,"y": yval, "x":xval, "names" : _labels.names,'dimnames':_labels.dimnames,'dimwidths':_labels.dimwidths,'dimbases':_labels.dimbases,
                "max":_labels.max,"min":_labels.min, "inv":inv    };
            
            self.render(_data, newlabels,_types, opts);

            // let the backend know the user updated the vis
            $.get($SCRIPT_ROOT+'/scalar/tile-updated/',{
                x_label:self.vis_metadata['x_label'],
                y_label:self.vis_metadata['y_label'],
                z_label:self.vis_metadata['z_label'],
                x_inv:self.vis_metadata['x_inv'],
                y_inv:self.vis_metadata['y_inv'],
                z_inv:self.vis_metadata['z_inv'],
                color:self.vis_metadata['color'],
                width:self.vis_metadata['width'],
                height:self.vis_metadata['height']
            });
            self.update_filters(newlabels);
            return false;
        });
    })();


    return {'x_label':x_label,'y_label':y_label,'z_label':z_label};
}

/*
used only to change the data view. all other objects should stay the same
*/
QVis.Graph.prototype.mini_render = function(_data, _labels,_types, opts) {
    this.jsvg.empty();
    this.map.empty();
    this.map.append('<div></div>');
    this.brush = null;

    this.update_opts(opts); // if new options are passed, update the options

    var self = this;

    //set to the current width and height and color
    $('#update-width').val(self.w);
    $('#update-height').val(self.h);
    $('#color-scheme').val(self.colorscheme);

    // setup the apply filters button
    $('#apply-filter-submit').unbind('click');
    $('#apply-filter-submit').click(function() {
        self.use_filters = true;
        self.get_filter_ranges();
        $.get($SCRIPT_ROOT+'/scalar/filters-applied/',{
                filter_labels:self.filter_labels,
                filter_lowers:self.filter_lowers,
                filter_uppers:self.filter_uppers
        });

        self.render(_data, _labels,_types, opts);
        return false;
    });

    // setup the clear filters button
    $('#clear-filter-submit').unbind('click');
    $('#clear-filter-submit').click(function() {
        $.get($SCRIPT_ROOT+'/scalar/filters-cleared/',{});
        self.use_filters = false;
        self.render(_data, _labels,_types, opts);
        return false;
     });
    //
    // I create and execute this anonymous function so
    // selectedval will be private to and accessible by the .change() callback function
    // Manually set the new labels and call render_scatterplot again
    // 
    // notice that I use "self" instead of "this".
    //
    (function() {
        var selectedzval = self.labelsfrombase.z_label; // get the previous values
        var selectedxval = self.labelsfrombase.x_label;
        var selectedyval = self.labelsfrombase.y_label;
        var width = self.w;
        var height = self.h;
        var color = self.colorscheme;
        var inv = 'inv' in _labels ? _labels['inv'] : [false,true,true];
        console.log(['inv' in _labels,'x' in _labels]);

        self.vis_metadata = {x_label:selectedxval, y_label:selectedyval, z_label:selectedzval, // strings
                    x_inv:inv[0], y_inv:inv[1], z_inv: inv[2], // booleans
                    color:color, // string
                    width: width, height: height}; // ints

        $("#vis-update-submit").off('click');
        $("#vis-update-submit").click(function() {
            console.log("got in update function");
            var zval = '';//$("#"+self.rootid+" .zlabel select").val();
            var yval = '';//$("#"+self.rootid+" .zlabel select").val();
            var xval = '';//$("#"+self.rootid+" .zlabel select").val();

            var inv_new = [false,true,true];
            console.log(["old radio vals: ",inv]);
            if(self.selectz) {
                zval = $("#"+self.rootid+"-form .zlabel select").val(); // should be the same as before
                inv_new[2]= $("#"+self.rootid+"-form input:radio[name='zlabel-radio']:checked").val() !== "";
            }
            if(self.selecty) {
                yval = $("#"+self.rootid+"-form .ylabel select").val(); // should be the same as before
                inv_new[1]= $("#"+self.rootid+"-form input:radio[name='ylabel-radio']:checked").val() !== "";
            }
            if(self.selectx) {
                xval = $("#"+self.rootid+"-form .xlabel select").val(); // should be the same as before
                inv_new[0]= $("#"+self.rootid+"-form input:radio[name='xlabel-radio']:checked").val() !== "";
            }


            var neww = $('#update-width').val();
            neww = isNaN(neww) ? width : neww;
            var newh = $('#update-height').val();
            newh = isNaN(newh) ? height : newh;
            var newcolor = $('#color-scheme').val();

            console.log(["selected options", zval,yval,xval]);
            console.log(["new radio vals: ",inv_new]);
            console.log(["new dims:",neww,newh]);
            console.log(["new color scheme:",newcolor]);
            // if the values haven't changed
            if ((!self.selectz || ((zval === selectedzval) && (inv[2] === inv_new[2]))) 
                && (!self.selecty || ((yval === selectedyval) && (inv[1] === inv_new[1]))) 
                && (!self.selectx || ((xval === selectedxval)  && (inv[0] === inv_new[0])))
                && (width === neww)  // ignore if not a valid number
                && (height === newh)
                && (color === newcolor)) {
                $('#update-width').val(width);
                $('#update-height').val(height);
                return false;
            }
            selectedxval = xval;
            selectedyval = yval;
            selectedzval = zval;
            width = neww;
            height = newh;
            color = newcolor;
            opts['w'] = width;
            opts['h'] = height;
            opts['color'] = color;
            for(var i = 0; i <= 2; i++) {
                inv[i]= inv_new[i];
            }

            // collect metadata for comparison purposes
            self.vis_metadata = {x_label:xval, y_label:yval, z_label:zval, // strings
                        x_inv:inv[0], y_inv:inv[1], z_inv: inv[2], // booleans
                        color:color, // string
                        width: width, height: height}; // ints
            //inv = inv_new;

            console.log(["changed radio vals: ",inv]);
            var newlabels = {"z" : zval,"y": yval, "x":xval, "names" : _labels.names,'dimnames':_labels.dimnames,'dimwidths':_labels.dimwidths,'dimbases':_labels.dimbases,
                "max":self.max,"min":self.min, "inv":inv    };
            
            self.render(_data, newlabels,_types, opts);

            // let the backend know the user updated the vis
            $.get($SCRIPT_ROOT+'/scalar/tile-updated/',{
                x_label:self.vis_metadata['x_label'],
                y_label:self.vis_metadata['y_label'],
                z_label:self.vis_metadata['z_label'],
                x_inv:self.vis_metadata['x_inv'],
                y_inv:self.vis_metadata['y_inv'],
                z_inv:self.vis_metadata['z_inv'],
                color:self.vis_metadata['color'],
                width:self.vis_metadata['width'],
                height:self.vis_metadata['height']
            });
            self.update_filters(newlabels);

            return false;
        });
    })();
}
/*
    this.filterdiv.find("#filter-select-label").after($("<select name=\"filter-select\" id=\"filter-select\" class=\"span2\"></select>"));
        var filterselect = this.filterdiv.find("select");
        var filterattrselect = filterselect.append($('<optgroup id="filter-attrs" label="attrs"></optgroup>')).find("#filter-attrs");
        var filterdimselect = filterselect.append($('<optgroup id="filter-dims" label="dims"></optgroup>')).find("#filter-dims");
        var filterlabel = d3.selectAll(filterattrselect.get()).selectAll("option")
                .data(attrnames.filter(function(d){return self.isNumber(_types[d]);}))
            .enter().append("option")
                .attr("value", function(d) { return d;})
                .text(function(d) { return d;});

        var filterlabel = d3.selectAll(filterdimselect.get()).selectAll("option")
                .data(dimnames.filter(function(d){return self.isNumber(_types[d]);}))
            .enter().append("option")
                .attr("value", function(d) { return d;})
                .text(function(d) { return d;});

        console.log(['filterselect',filterselect]);
        filterselect.val(z_label);
$("#vis-update-submit").off('click');
        $("#vis-update-submit").click(function() {
            var zval = '';//$("#"+self.rootid+" .zlabel select").val();
            var yval = '';//$("#"+self.rootid+" .zlabel select").val();
            var xval = '';//$("#"+self.rootid+" .zlabel select").val();

            var width = self.w;
*/            
QVis.Graph.prototype.add_filter = function() {
    var self = this;
    var precision = 5;
    var step = 100;
    $("#add-filter-submit").off('click');
    $("#add-filter-submit").on('click',function() {

        //get the attribute
        var attr = self.filterdiv.find("select").val();
        // strip what we don't need from the attribute name
        var trueattr = attr.replace('attrs.','attrs-');
        trueattr = trueattr.replace('dims.','dims-');
        var attrlabel = trueattr.replace('_','-');
        console.log(['attr val',attr]);

        //check if there is already a filter
        var exists = false;
        self.slidersdiv.children("div#filter-"+attrlabel+".myslider").each(function() {
            exists = true;
        });

        if(exists) {
            console.log("filter already exists for attribute");
            return false;
        }

        if(!self.hasOwnProperty('filter_labels')) {
            self.filter_labels = [];
        }
        self.filter_labels.push(attr);
        //get min and max values
        var attrmin =self.min[attr];
        var attrmax = self.max[attr];

        //calculate step
        var offset = Math.pow(10,precision);
        var finalstep = Math.ceil((attrmax - attrmin)*offset/step)/offset;
        console.log(["step",finalstep]);

        // round after computing step width
        attrmin = Math.floor(self.min[attr]*offset)/offset;
        attrmax = Math.ceil(self.max[attr]*offset)/offset;

        //add div to sliders div
        //add input object to div with min/max
        var sliderdiv = self.slidersdiv.append(' \
            <div id="filter-'+attrlabel+'" class="myslider form-inline"> \
                <label>'+attr+': '+attrmin+'</label> \
                <input id="slider1" type="text" value="" \
                    data-slider-min="'+attrmin+'" \
                    data-slider-max="'+attrmax+'" \
                    data-slider-step="'+finalstep+'" \
                    data-slider-value="['+attrmin+','
                        +attrmax+']" \
                    data-slider-orientation="horizontal" \
                    data-slider-selection="after" data-slider-tooltip="show"> \
                <label>'+attrmax+'</label> \
                <a id="delete-filter-'+attrlabel+'" class="btn"><i class="icon-remove"></i></a> \
            </div>\
        ');
        //activate slider
        sliderdiv.find('input[type=text]').slider();

        //add delete button
        $('#delete-filter-'+attrlabel).click(function() {
            console.log("deleting div");
            $(this).unbind('click');
            $('#filter-'+attrlabel).remove();
            var index = $.inArray(attr,self.filter_labels);
            if(index > -1) {
                self.filter_labels.splice(index,1);
            }
            return false;
        });
        return false;
    });
    return false;
}

// as the code stands, this will never happen!
QVis.Graph.prototype.update_filters = function(_labels) {
    var self = this;
    // remove outdated filters
    var results = [];
    self.slidersdiv.children("div.myslider").each(function() {
        var id = $(this).attr('id');
        var name = $(this).attr('id');
        name = name.replace('filter-','');
        name = name.replace('attrs-','attrs.');
        name = name.replace('dims-','dims.');
        name = name.replace('-','_');
        //console.log(['attr name',name]);
        var outdated = true;
        for(var i = 0; i < _labels.names.length ; i++) {
           label = _labels.names[i]['name'];
           //console.log(['name', label]);
           if(label === name) {
                outdated = false;
                break;
           }
        }
        if(outdated) {
            $('#delete-'+id).unbind(); // remove handler on filter delete button
            $(this).remove(); // remove the outdated filter
            var index = $.inArray(attr,self.filter_labels);
            if(index > -1) {
                self.filter_labels.splice(index,1);
            }
        }
    });
    return false;
}

QVis.Graph.prototype.get_filter_ranges = function() {
    var self = this;

    // get the filter ranges and save them.
    // new filters are appended to the end of the div
    // so this should always be in the right order.
    self.filter_ranges = [];
    self.filter_lowers = [];
    self.filter_uppers = [];
    self.slidersdiv.children("div.myslider").each(function() {
        $(this).find("input[type=text]").each(function() {
            var range = $(this).data('slider').getValue()
            self.filter_lowers.push(range[0]);
            self.filter_uppers.push(range[1]);
            self.filter_ranges.push(range);
        });
    });
    console.log(['slider val',self.filter_ranges,'slider label', self.filter_labels]);
    return false;
}

/*
used to create the brush so user can select objects in the visualization
the color parameter should be a function returning the correct fill value
*/
QVis.Graph.prototype.add_brush = function(xscale,yscale,xlabel,ylabel,color,container) {
    var draw_obj = this.draw_obj;
    var svg = this.svg;
    var default_color = "#CCCCCC";

    // I am pretty sure I don't need these p parameters,
    // they were for the frames in the d3 example I used.

    // Clear the previously-active brush, if any.
    function brushstart(p) {
        if (brush.data !== p) {
            container.call(brush.clear());
            brush.x(xscale).y(yscale).data = p;
        }
    }

    // Highlight the selected objects.
    function brush(p) {
        var e = brush.extent();
        container.selectAll(draw_obj).attr("fill", function(d) {
            return e[0][0] <= d[xlabel] && d[xlabel] <= e[1][0] //x
                && e[0][1] <= d[ylabel] && d[ylabel] <= e[1][1] //y
                ? color(d) : default_color;
        });
    }

    // If the brush is empty, select all circles.
    function brushend() {
        if (brush.empty()) {
            container.selectAll(draw_obj).attr("fill", function(d){return color(d);});
        }
    }

    // so I have access to the brush extent for zooms
    this.brush = d3.svg.brush()
        .on("brushstart", brushstart)
        .on("brush", brush)
        .on("brushend", brushend);

    var brush = this.brush;

    container.call(brush.x(xscale).y(yscale));
}

QVis.Graph.prototype.add_axes = function(xscale,yscale,x_label,y_label,stringticks,_types){
    var self = this;
    var xaxis = d3.svg.axis().scale(xscale).orient('bottom').tickPadding(10);// orient just describes what side of the axis to put the text on
    var yaxis = d3.svg.axis().scale(yscale).orient('left').tickPadding(10);
    var df = d3.time.format("%Y-%m-%d");
    if(_types[x_label] === "datetime") {
        xaxis.ticks(d3.time.days,1);
    } else if(_types[x_label] === 'string') {
        
    } else {
        xaxis.ticks(10);
    }
    if(_types[y_label] === "datetime") {
        yaxis.ticks(d3.time.days,1);
    } else if(_types[y_label] === 'string') {
        
    } else {
        yaxis.ticks(6);
    }
    xaxis.tickSize(-this.h+2*this.py); // makes the tick lines
    yaxis.tickSize(-this.w+2*this.px); // makes the tick lines

    if(_types[x_label] === "datetime") {
        xaxis.tickFormat(df);
    }
    if(_types[y_label] === "datetime") {
        yaxis.tickFormat(df);
    }

    this.svg.append("g")
          .attr("class", "xaxis")
          .attr("transform", "translate(0," + (this.h-this.py) + ")")
          .call(xaxis);

    this.svg.append("g")
          .attr("class", "yaxis")
          .attr("transform", "translate("+this.px+" 0)")
          .call(yaxis);
}

QVis.Graph.prototype.drawCircles = function(container,_data,_types,xscale,yscale,x_label,y_label,radius,color) {
    var temp = this;
    /*
    var range = 100;
    var steps = _data.length/range+1;
    for(var drawindex = 0; (drawindex < steps) && (drawindex*range < _data.length); drawindex++) {
        console.log("drawing range: "+drawindex*range+"-"+(drawindex*range+range));
        var data;
        if(drawindex*range+range > _data.length) {
            data = _data.slice(drawindex*range,_data.length)
        } else {
            data = _data.slice(drawindex*range,drawindex*range+range)
        }
        //console.log(data);
        var circles = d3.select("#"+this.rootid + " svg g.circlecontainer").selectAll('circle')
            .data(data,function(d,i){return drawindex*range+i;}) //need to specify an id so points don't get replaced
                
        circles.enter().append('circle')
            .attr('cy', function(d) { return yscale(temp.get_data_obj(d[y_label],_types[y_label]));})
            .attr('cx', function(d) { return xscale(temp.get_data_obj(d[x_label],_types[x_label]));})
            .attr('r', function(d) { return radius(temp.get_data_obj(d[x_label],_types[x_label]));})
            .attr('fill', function(d){if(drawindex === 0){return 'blue';}else{returncolor(d);}})
            .attr('label', function(d,i){return drawindex*range+i;});
    }
    */
/*
    // if I end up using this, I need a way to track the points when using the brush
    for(var drawindex = 0; drawindex < _data.length; drawindex++) {
        container.append('circle')
            .attr('cy', yscale(temp.get_data_obj(_data[drawindex][y_label],_types[y_label])))
            .attr('cx', xscale(temp.get_data_obj(_data[drawindex][x_label],_types[x_label])))
            .attr('r', radius(temp.get_data_obj(_data[drawindex][x_label],_types[x_label])))
            .attr('fill', color(_data[drawindex]))
            .attr('label', drawindex);
    }
*/

    container.selectAll('circle')
        .data(_data).enter().append('circle')
            .attr('cy', function(d) { return yscale(temp.get_data_obj(d[y_label],_types[y_label]));})
            .attr('cx', function(d) { return xscale(temp.get_data_obj(d[x_label],_types[x_label]));})
            .attr('r', function(d) { return radius(temp.get_data_obj(d[x_label],_types[x_label]));})
            .attr('fill', function(d){return color(d);})
            .attr('label', function(d,i){return i;});
}

// just spits out a radius of 2
QVis.Graph.prototype.defaultRadius = function(d) {
    return 2;
}

// just spits out blue
QVis.Graph.prototype.defaultColor = function(d) {
    return 'red';
}

QVis.Graph.prototype.newDrawRects =
function(container,_data,_types,xscale,yscale,x_label,
        y_label,z_label,width,height,color) {
    var start = Math.round((new Date().getTime())/1000);
    var temp = this;
    var xdata = _data[x_label];
    var ydata = _data[y_label];
    container.selectAll('rect')
        .data(xdata)
    .enter().append('rect')
        .attr('y', function(d,i) {
                    return yscale(temp.get_data_obj(ydata[i],_types[y_label]))})
        .attr('x', function(d,i) {
                    return xscale(temp.get_data_obj(d,_types[x_label]))})
        .attr('width', function(d,i) {
                    return width(temp.get_data_obj(d,_types[x_label]))})
        .attr('height', function(d,i) {
                    return height(temp.get_data_obj(d,_types[x_label]))})
        .attr('fill', color)
        .attr('label', function(d,i) {
                    return "("+temp.get_data_obj(d[y_label],_types[y_label])+","+temp.get_data_obj(d[x_label],_types[x_label])+") = "+color(d);});

/*
    container.selectAll('rect')
        .on("click",function(d,i){console.log(d3.mouse(this))});
*/
    var end = Math.round((new Date().getTime())/1000);
    console.log(["draw rect duration",end-start]);
}
QVis.Graph.prototype.drawRects = function(container,_data,_types,xscale,yscale,x_label,y_label,width,height,color) {
    var start = Math.round((new Date().getTime())/1000);
    var temp = this;
    /*
    var range = 1000;
    var steps = _data.length/range+1;
    for(var drawindex = 0; (drawindex < steps) && (drawindex*range < _data.length); drawindex++) {
        console.log("drawing range: "+drawindex*range+"-"+(drawindex*range+range));
        var data;
        if(drawindex*range+range > _data.length) {
            data = _data.slice(drawindex*range,_data.length)
        } else {
            data = _data.slice(drawindex*range,drawindex*range+range)
        }
        container.selectAll('rect')
                .data(data)
            .enter().append('rect')
                .attr('y', function(d) { return yscale(temp.get_data_obj(d[y_label],_types[y_label]))})
                .attr('x', function(d) { return xscale(temp.get_data_obj(d[x_label],_types[x_label]))})
                .attr('width', function(d) { return width(temp.get_data_obj(d[x_label],_types[x_label]))})
                .attr('height', function(d) { return height(temp.get_data_obj(d[x_label],_types[x_label]))})
                .attr('fill', function(d) { return color(temp.get_data_obj(d[x_label],_types[x_label]))})
                .attr('label', x_label);
    }
    */
/*
    for(var drawindex = 0; drawindex < _data.length; drawindex++) {
        //console.log(xscale(temp.get_data_obj(_data[drawindex][y_label],_types[x_label])));
        //console.log(yscale(temp.get_data_obj(_data[drawindex][y_label],_types[y_label])));
        //console.log(width(temp.get_data_obj(_data[drawindex][x_label],_types[x_label])));
        //console.log(height(temp.get_data_obj(_data[drawindex][x_label],_types[x_label]));
        //console.log(color(_data[drawindex]));
        container.append('rect')
            .attr('y', yscale(temp.get_data_obj(_data[drawindex][y_label],_types[y_label])))
            .attr('x', xscale(temp.get_data_obj(_data[drawindex][x_label],_types[x_label])))
            .attr('width', width(temp.get_data_obj(_data[drawindex][x_label],_types[x_label])))
            .attr('height', height(temp.get_data_obj(_data[drawindex][x_label],_types[x_label])))
            .attr('fill', color(_data[drawindex]))
            .attr('label', drawindex);
    }

*/
    container.selectAll('rect')
        .data(_data)
    .enter().append('rect')
        .attr('y', function(d) { return yscale(temp.get_data_obj(d[y_label],_types[y_label]))})
        .attr('x', function(d) { return xscale(temp.get_data_obj(d[x_label],_types[x_label]))})
        .attr('width', function(d) { return width(temp.get_data_obj(d[x_label],_types[x_label]))})
        .attr('height', function(d) { return height(temp.get_data_obj(d[x_label],_types[x_label]))})
        .attr('fill', function(d) { return color(d);})
        .attr('label', function(d,i) {return "("+temp.get_data_obj(d[y_label],_types[y_label])+","+temp.get_data_obj(d[x_label],_types[x_label])+") = "+color(d);});

/*
    container.selectAll('rect')
        .on("click",function(d,i){console.log(d3.mouse(this))});
*/
    var end = Math.round((new Date().getTime())/1000);
    console.log(["draw rect duration",end-start]);
}

QVis.Graph.prototype.drawZoomGrid = function(ctx,x_interval,y_interval,xinv,yinv) {
    var drawline = function(x1,y1,x2,y2) {
        ctx.moveTo(x1,y1);
        ctx.lineTo(x2,y2);
        ctx.lineWidth = 3;
        ctx.strokeStyle = '#000000';//'#ffffff';
        ctx.stroke();
    }
    var self = this;
    if (xinv) {
        for (var x = self.w - self.px - x_interval; x > self.px; x -= x_interval) { // draw vertical lines
            var dist = self.px + x;
            drawline(dist,self.py,dist,self.h-self.py);
        }

    } else {
        for (var x = x_interval; x < (self.w - 2*self.px); x += x_interval) { // draw vertical lines
            var dist = self.px + x;
            drawline(dist,self.py,dist,self.h-self.py);
        }
    }
    if (yinv) {
        for (var y = self.h - self.py - y_interval; y > self.py; y -= y_interval) { // draw horizontal lines
            var dist = self.py + y;
            drawline(self.px,dist,self.w-self.px,dist);
        }
    } else {
        for (var y = y_interval; y < (self.h - 2*self.py); y += y_interval) { // draw horizontal lines
            var dist = self.py + y;
            drawline(self.px,dist,self.w-self.px,dist);
        }
    }
}

QVis.Graph.prototype.newDrawRectsCanvas = function(ctx,_data,_types,xscale,yscale,x_label,y_label,z_label,width,height,color) {
    //var ctx = canvas.getContext('2d');
    var temp = this;
    var start = Math.round((new Date().getTime())/1000);
    //console.log(['use filters',temp.use_filters]);
    if(temp.use_filters) {
        color = temp.wrap_color_with_filters(color,_data);
    }
    var xdata = _data[x_label];
    var ydata = _data[y_label];
    var zdata = _data[z_label];
    ctx.fillStyle = "#FFFFFF";
    ctx.fillRect(0,0,this.w,this.h); // set the back to be white first
    for(var drawindex = 0; drawindex < xdata.length; drawindex++) {
        ctx.fillStyle = color(zdata[drawindex],drawindex);
        ctx.fillRect(xscale(temp.get_data_obj(xdata[drawindex],_types[x_label])),
            yscale(temp.get_data_obj(ydata[drawindex],_types[y_label])),
            width(temp.get_data_obj(xdata[drawindex],_types[x_label])),
            height(temp.get_data_obj(xdata[drawindex],_types[x_label]))
        );
    }
    var end = Math.round((new Date().getTime())/1000);
    console.log(["draw rect duration",end-start]);
}

QVis.Graph.prototype.drawRectsCanvas = function(ctx,_data,_types,xscale,yscale,x_label,y_label,width,height,color) {
    //var ctx = canvas.getContext('2d');
    var temp = this;
    var start = Math.round((new Date().getTime())/1000);
    //console.log(['use filters',temp.use_filters]);
    if(temp.use_filters) {
        color = temp.wrap_color_with_filters(color);
    }
    ctx.fillStyle = "#FFFFFF";
    ctx.fillRect(0,0,this.w,this.h); // set the back to be white first
    for(var drawindex = 0; drawindex < _data.length; drawindex++) {
        ctx.fillStyle = color(_data[drawindex]);
        ctx.fillRect(xscale(temp.get_data_obj(_data[drawindex][x_label],_types[x_label])),
            yscale(temp.get_data_obj(_data[drawindex][y_label],_types[y_label])),
            width(temp.get_data_obj(_data[drawindex][x_label],_types[x_label])),
            height(temp.get_data_obj(_data[drawindex][x_label],_types[x_label]))
        );
    }
    var end = Math.round((new Date().getTime())/1000);
    console.log(["draw rect duration",end-start]);
}

QVis.Graph.prototype.wrap_color_with_filters = function(color, _data) {
    var self = this;
    return function(d,i) {
        if(self.passes_filters(i,_data)) {
            return color(d);
        } else {
            return self.filterscale(d);
        }
    };
}

/*
returns true if given data point satisfies all filter requirements
only check filters if use_filters flag is set
*/
QVis.Graph.prototype.passes_filters = function(i,_data) {
    var self = this;
    for(var filterid = 0; filterid < self.filter_labels.length; filterid++) {
        var filter_label = self.filter_labels[filterid];
        var filter_range = self.filter_ranges[filterid];
        var p = _data[filter_label][i];
        //var p = d[filter_label];
        //console.log(['label',filter_label,'range',filter_range,'p',p]);
        if((p < filter_range[0]) || (p > filter_range[1])) {
            return false;
        }
    }
    //console.log('pass');
    return true;
}

/*
Draws the legend for the graph. Needs the container to draw the legend in,
desired dimensions of the legend, and the color scale.
*/
QVis.Graph.prototype.drawLegend = function(l_w,l_h,color) {
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
    var svg = d3.selectAll(this.legend.get()).append("svg")
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

QVis.Graph.prototype.drawLines = function(container,_data,_types,xscale,yscale) {
    //sort the data
    //draw then draw the lines

}

