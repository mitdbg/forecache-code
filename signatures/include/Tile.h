#ifndef _TILE_
#define  _TILE_

#include <map> //std::map
#include <string> // std::string
#include "rapidjson/document.h"

class Tile {
public:
	rapidjson::Document root;
	rapidjson::Value *dimsObj;
	rapidjson::Value *attrsObj;
	Tile(const char *json) {
		root.Parse<0>(json);
		dimsObj = &(root["data"]["dims"]["obj"]);
		attrsObj = &(root["data"]["attrs"]["obj"]);
	};
};

#endif
