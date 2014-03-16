#ifndef _COMPUTE_SIGNATURES_
#define _COMPUTE_SIGNATURES_

#include <string> // std::string
#include <vector> // std::vector
#include "Tile.h"

class ComputeSignatures {
public:
	// takes a filepath and retrieves the contents as a string
	static const char* loadFile(std::string filepath);
	//writes string to designated filepath
	static void writeFile(std::string filepath, std::string data);
	// builds the filepath using the given tile information
	static std::string buildPath(std::string cache_root, std::string hashed_query, std::string threshold, std::string zoom, std::string hashed_tile_id);
	// parses tile data stored in JSON format
	static void parseTileData(const char *json);
	static std::string computeNormalSignature(Tile &tile, const char * label);
private:
	static void getAttributeVector(Tile &tile, const char * label, std::vector<double> &input);
};

#endif
