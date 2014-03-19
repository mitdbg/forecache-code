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
	// write 2 attributes to disk in csv format
	static void writeCsv(std::string filepath, std::vector<double> attr1, std::vector<double> attr2);
	// builds the filepath using the given tile information
	static std::string buildPath(std::string cache_root, std::string hashed_query, std::string threshold, std::string zoom, std::string hashed_tile_id);
	// parses tile data stored in JSON format
	static void parseTileData(const char *json);
	static std::string computeNormalSignature(Tile &tile, const char * label);
	static std::string computeHistogramSignature(Tile &tile, const char * label, int bins);
	static std::string computeFilteredHistogramSignature(Tile &tile, const char * label,const char *label2, double filterval, int bins);
	static std::string computeGroupedHistogramSignature(Tile &tile, const char *label, const char *label2, std::vector<double> filtervals, int bins);
	static void getAttributeVector(Tile &tile, const char * label, std::vector<double> &input);
	static std::vector<double> filterVector(std::vector<double> &input, std::vector<double> &filter, double filterval);
	static void getMaxMin(Tile &tile, const char * label, std::pair<double,double> &input);
	static double getEuclideanDistance(std::vector<double> &d1, std::vector<double> &d2);
};

#endif
