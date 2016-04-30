function getBlueBezierMap() {
  return getBezierMapFromFunction(computeBlueCubicBezier);
}

function getGreenBezierMap() {
  return getBezierMapFromFunction(computeGreenCubicBezier);
}

function getRedBezierMap() {
  return getBezierMapFromFunction(computeRedCubicBezier);
}

function getBezierMap() {
  return getBezierMapFromFunction(computeDefaultCubicBezier2);
}

function getBezierMapFromFunction(bezierFunction) { // dynamically make a map that includes all possible x values from 0 to 255
  var bezierMap = {};
  //var bezier = computeDefaultCubicBezier();
  var bezier = bezierFunction();
  for(var i = 0.00; i <= 1.001; i+=0.001) { // fine granularity to make sure we catch everything
    var p = bezier(i);
    var x = Math.floor(p.x);
    var y = Math.floor(p.y);
    if(!bezierMap.hasOwnProperty(x)) {
      bezierMap[x]=y;
    }
  }
/*
  // for validation
  var keys = Object.keys(bezierMap);
  for(var i = 0; i < keys.length; i++) {
    console.log([keys[i],bezierMap[keys[i]]]);
  }
  // should produce 256 keys
  console.log(["total keys",keys.length]);
*/
  return bezierMap;
}

function computeBlueCubicBezier() {
  p0 = {x:0.0,y:0.0};
  p1 = {x:12,y:79.7};
  p2 = {x:173.0,y:229.0};
  //p2 = {x:150.0,y:149.0};
  p3 = {x:255.0,y:255.0};
  return computeCubicBezier(p0,p1,p2,p3);
}

function computeGreenCubicBezier() {
  p0 = {x:0.0,y:0.0};
  p1 = {x:-1.0,y:66.0};
  //p1 = {x:-2.6,y:85.0};
  p2 = {x:150.0,y:149.0};
  p3 = {x:255.0,y:255.0};
  return computeCubicBezier(p0,p1,p2,p3);
}

function computeRedCubicBezier() {
  p0 = {x:0.0,y:0.0};
  p1 = {x:-1.0,y:66.0};
  //p1 = {x:-1.0,y:105.0};
  p2 = {x:150.0,y:149.0};
  p3 = {x:255.0,y:255.0};
  return computeCubicBezier(p0,p1,p2,p3);
}

function computeDefaultCubicBezier2() {
  p0 = {x:0.0,y:0.0};
  p1 = {x:11.0,y:90.0};
  p2 = {x:92.0,y:187.0};
  p3 = {x:255.0,y:255.0};
  return computeCubicBezier(p0,p1,p2,p3);
}

// the original I tried to copy from the tutorial
// built this curve to approximate this tutorial:
// http://earthobservatory.nasa.gov/blogs/elegantfigures/2013/10/22/how-to-make-a-true-color-landsat-8-image/
function computeDefaultCubicBezier() {
  p0 = {x:0.0,y:0.0};
  p1 = {x:23.0,y:169.0};
  p2 = {x:51.0,y:241.0};
  p3 = {x:255.0,y:255.0};
  return computeCubicBezier(p0,p1,p2,p3);
}

function computeCubicBezier(p0,p1,p2,p3) {
  cx = 3*(p1.x - p0.x);
  bx = 3*(p2.x - p1.x) - cx;
  ax = p3.x - p0.x - cx - bx;

  cy = 3*(p1.y - p0.y);
  by = 3*(p2.y - p1.y) - cy;
  ay = p3.y - p0.y - cy - by;
  console.log([cx,bx,ax,cy,by,ay]);
  return function(t) {
    x_t = ax*Math.pow(t,3) + bx*Math.pow(t,2) + cx*t + p0.x;
    y_t = ay*Math.pow(t,3) + by*Math.pow(t,2) + cy*t + p0.y;
    return {"x":x_t,"y":y_t};
  };
}

 var bezierMap = getBezierMap();
console.log(["bezierMap",bezierMap]);

/*
// so it doesn't need to be computed
var bezierMap = {
  "0": 0,
  "1": 7,
  "2": 14,
  "3": 21,
  "4": 27,
  "5": 34,
  "6": 40,
  "7": 46,
  "8": 52,
  "9": 58,
  "10": 63,
  "11": 68,
  "12": 73,
  "13": 77,
  "14": 82,
  "15": 86,
  "16": 91,
  "17": 95,
  "18": 99,
  "19": 102,
  "20": 106,
  "21": 109,
  "22": 113,
  "23": 116,
  "24": 119,
  "25": 122,
  "26": 125,
  "27": 127,
  "28": 130,
  "29": 133,
  "30": 135,
  "31": 138,
  "32": 140,
  "33": 142,
  "34": 145,
  "35": 147,
  "36": 149,
  "37": 151,
  "38": 153,
  "39": 155,
  "40": 157,
  "41": 159,
  "42": 160,
  "43": 162,
  "44": 164,
  "45": 165,
  "46": 167,
  "47": 168,
  "48": 170,
  "49": 171,
  "50": 173,
  "51": 174,
  "52": 176,
  "53": 177,
  "54": 178,
  "55": 180,
  "56": 181,
  "57": 182,
  "58": 183,
  "59": 185,
  "60": 186,
  "61": 187,
  "62": 188,
  "63": 189,
  "64": 190,
  "65": 191,
  "66": 192,
  "67": 193,
  "68": 194,
  "69": 195,
  "70": 196,
  "71": 197,
  "72": 198,
  "73": 199,
  "74": 200,
  "75": 201,
  "76": 201,
  "77": 202,
  "78": 203,
  "79": 204,
  "80": 205,
  "81": 205,
  "82": 206,
  "83": 207,
  "84": 208,
  "85": 208,
  "86": 209,
  "87": 210,
  "88": 211,
  "89": 211,
  "90": 212,
  "91": 212,
  "92": 213,
  "93": 214,
  "94": 214,
  "95": 215,
  "96": 216,
  "97": 216,
  "98": 217,
  "99": 217,
  "100": 218,
  "101": 219,
  "102": 219,
  "103": 220,
  "104": 220,
  "105": 221,
  "106": 221,
  "107": 222,
  "108": 222,
  "109": 223,
  "110": 223,
  "111": 224,
  "112": 224,
  "113": 225,
  "114": 225,
  "115": 226,
  "116": 226,
  "117": 226,
  "118": 227,
  "119": 227,
  "120": 228,
  "121": 228,
  "122": 229,
  "123": 229,
  "124": 229,
  "125": 230,
  "126": 230,
  "127": 231,
  "128": 231,
  "129": 231,
  "130": 232,
  "131": 232,
  "132": 233,
  "133": 233,
  "134": 233,
  "135": 233,
  "136": 234,
  "137": 234,
  "138": 235,
  "139": 235,
  "140": 235,
  "141": 236,
  "142": 236,
  "143": 236,
  "144": 236,
  "145": 237,
  "146": 237,
  "147": 237,
  "148": 238,
  "149": 238,
  "150": 238,
  "151": 238,
  "152": 239,
  "153": 239,
  "154": 239,
  "155": 240,
  "156": 240,
  "157": 240,
  "158": 240,
  "159": 241,
  "160": 241,
  "161": 241,
  "162": 241,
  "163": 242,
  "164": 242,
  "165": 242,
  "166": 242,
  "167": 243,
  "168": 243,
  "169": 243,
  "170": 243,
  "171": 243,
  "172": 244,
  "173": 244,
  "174": 244,
  "175": 244,
  "176": 245,
  "177": 245,
  "178": 245,
  "179": 245,
  "180": 245,
  "181": 245,
  "182": 246,
  "183": 246,
  "184": 246,
  "185": 246,
  "186": 246,
  "187": 247,
  "188": 247,
  "189": 247,
  "190": 247,
  "191": 247,
  "192": 247,
  "193": 248,
  "194": 248,
  "195": 248,
  "196": 248,
  "197": 248,
  "198": 248,
  "199": 249,
  "200": 249,
  "201": 249,
  "202": 249,
  "203": 249,
  "204": 249,
  "205": 249,
  "206": 250,
  "207": 250,
  "208": 250,
  "209": 250,
  "210": 250,
  "211": 250,
  "212": 250,
  "213": 250,
  "214": 251,
  "215": 251,
  "216": 251,
  "217": 251,
  "218": 251,
  "219": 251,
  "220": 251,
  "221": 251,
  "222": 252,
  "223": 252,
  "224": 252,
  "225": 252,
  "226": 252,
  "227": 252,
  "228": 252,
  "229": 252,
  "230": 252,
  "231": 253,
  "232": 253,
  "233": 253,
  "234": 253,
  "235": 253,
  "236": 253,
  "237": 253,
  "238": 253,
  "239": 253,
  "240": 253,
  "241": 253,
  "242": 254,
  "243": 254,
  "244": 254,
  "245": 254,
  "246": 254,
  "247": 254,
  "248": 254,
  "249": 254,
  "250": 254,
  "251": 254,
  "252": 254,
  "253": 254,
  "254": 254,
  "255": 255
};

*/
