$(document).ready(function() {
  var testdiv = $('#testdiv');
  var width = 600;
  var height = 20;

  $("input[name='testbutton']").click(function(e) {
      e.stopPropagation();
      e.preventDefault();
      getHeatmap();
  });

  function getHeatmap() {
    var uid = $("input[name='uid']").val();
    var tid = $("input[name='tid']").val();
    testdiv.empty();
    var svg = d3.selectAll(testdiv.get()).append('svg')
      .attr('width',width)
      .attr('height',height*6)
      .attr('id','testsvg');

    $.get('/test/'+uid+'/'+tid,function(jsondata) {renderHeatmap(svg,jsondata);});
  }

  // d3 object, json data
  function renderHeatmap(svg, data) {
    // user_id, taskname, run_id, step_id, move
    // o = zoom out, L, R, U, D, 0/1/2/3 = zoom in
    var jsondata = JSON.parse(data);
    var count = jsondata.length;
    console.log(["jsondata",count]);
    var xscale = d3.scale.linear().domain([1,count]).range([0,width]);
    var colorscale = d3.scale.ordinal().domain([0,1,2]).range(colorbrewer["Blues"][3]);
    //console.log([colorscale.domain(),colorscale.range()]);
    var rects = svg.selectAll("rect").data(jsondata).enter().append("rect")
      .attr('width',width/count)
      .attr('height',height)
      .attr('x',function(d,i) {return xscale(i);})
      .attr('y',0)
      .attr('fill',function(d) { return colorscale(colormap(d['move']));});
    for(var i = 0; i < 3; i++) {
      svg.append("rect")
        .attr('width',width/count)
        .attr('height',height)
        .attr('x',0)
        .attr('y',height*(2+i))
        .attr('fill',colorscale(i));
      svg.append("text")
        .attr("x",width/count + 5)
        .attr("y",height*(3+i))
        .text(i);
    }
  }

  function colormap(move) {
    //console.log(['move', move]);
    var ret = -1;
    if(move === 'O') {
      ret = 0;
    } else if(move === 'L' || move === 'R' || move === 'U' || move === 'D') {
      ret = 1;
    } else {
      ret = 2;
    }
    //console.log(['ret', ret]);
    return ret;
  }
});
