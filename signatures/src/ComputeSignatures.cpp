#include <fstream> // std::ifstream
#include <iostream> // std::cout
#include <map> // std::map
#include <cmath> // std::sqrt, std::pow
#include <boost/filesystem.hpp> // boost::filesystem::*
#include "ComputeSignatures.h"
#include "NormalSignature.h"
#include "HistogramSignature.h"
#include "GroupedHistogramSignature.h"

/*
data types: double, int64, uint64
*/

void ComputeSignatures::writeFile(std::string filepath, std::string data) {
	boost::filesystem::path p(filepath);
	p = p.parent_path();
	if(!(boost::filesystem::exists(p))) {
		boost::filesystem::create_directories(p);
	}
	std::ofstream o(filepath.c_str());
	assert(o.is_open());
	size_t bytes = data.size();
	const char * towrite = data.c_str();
	o.write(towrite,bytes);
}

void ComputeSignatures::writeCsv(std::string filepath, std::vector<double> attr1, std::vector<double> attr2) {
	assert(attr1.size() == attr2.size());
	boost::filesystem::path p(filepath);
	p = p.parent_path();
	if(!(boost::filesystem::exists(p))) {
		boost::filesystem::create_directories(p);
	}
	std::ofstream o(filepath.c_str());
	assert(o.is_open());
	for(size_t i = 0; i < attr1.size(); i++) {
		o << attr1[i] << "," << attr2[i] << std::endl;
	}
}

// char array must be deleted by caller
const char* ComputeSignatures::loadFile(std::string filepath) {
	char *returnval = NULL;
	std::ifstream t(filepath.c_str(), std::ifstream::in);
	assert(t.is_open());
	t.seekg(0, std::ios::end);
	size_t size = t.tellg();
	returnval = new char[size+1];
	t.seekg(0);
	t.read(returnval, size); // does not null terminate the string
	returnval[size] = '\0';
	return returnval;
}

std::string ComputeSignatures::buildPath(std::string cache_root, std::string hashed_query, std::string threshold, std::string zoom, std::string hashed_tile_id) {
	return cache_root +"/" + hashed_query + "/" + threshold + "/" +
		zoom + "/" + hashed_tile_id;
}

/*
structure of data:
root
  orig_sqpr
  indexes
  future_tiles_exact
  ...
  data
    dims
      obj
        dims.dim_name
          max
            val
          min
            val
          pos
            val
          dtype
            val (string)
          data
            [val,val,...]
        dims.dim_name
        ...
    attrs
      obj
        attrs.attr_name
          max
            val
          min
            val
          pos
            val
          dtype
            val (string)
          data
            [val,val,...]
        attrs.attr_name
        ...
*/

void ComputeSignatures::parseTileData(const char *json) {
	Tile tile(json);

	int numdims = tile.root["numdims"].GetInt();
	std::cout << "numdims: " << numdims << std::endl;

	const rapidjson::Value& future_tiles_exact = tile.root["future_tiles_exact"];
	std::cout << "future_tiles_exact: ";
	for(rapidjson::SizeType i = 0; i < future_tiles_exact.Size(); i++) {
		std::cout << future_tiles_exact[i].GetDouble() << " ";
	}
	std::cout << std::endl;

	// get index positions for dimensions
	const rapidjson::Value& getIndexes = tile.root["indexes"];
	std::map<std::string,int> indexes;
	for (rapidjson::Value::ConstMemberIterator itr = getIndexes.MemberBegin(); itr != getIndexes.MemberEnd(); ++itr) {
		std::cout << "member name: " << itr->name.GetString() << std::endl;
		std::cout << "member value: " << itr->value.GetInt() << std::endl;
		indexes[itr->name.GetString()] = itr->value.GetInt();
	}

	for (rapidjson::Value::ConstMemberIterator itr = tile.dimsObj->MemberBegin(); itr != tile.dimsObj->MemberEnd(); ++itr) {
		std::cout << "member name: " << itr->name.GetString() << std::endl;
		std::cout << "member type: " << itr->value.GetType() << std::endl;
	}
}

void ComputeSignatures::getAttributeVector(Tile &tile, const char * label, std::vector<double> &input) {
	const rapidjson::Value &attr = (*tile.attrsObj)[label]["data"];
	for(rapidjson::SizeType i = 0; i < attr.Size(); i++) {
		input.push_back(attr[i].GetDouble());
		//std::cout << input[i] << " ";
	}
	//std::cout << std::endl;
}

std::vector<double> ComputeSignatures::filterVector(std::vector<double> &input, std::vector<double> &filter, double filterval) {
	assert(input.size() == filter.size());
	std::vector<double> output;
	for(size_t i = 0; i < input.size(); i++) {
		if(filter[i] == filterval) {
			output.push_back(input[i]);
		}
	}
	return output;
}

void ComputeSignatures::getMaxMin(Tile &tile, const char * label, std::pair<double,double> &input) {
	double max = (*tile.attrsObj)[label]["max"].GetDouble();
	double min = (*tile.attrsObj)[label]["min"].GetDouble();
	input.first = max;
	input.second = min;
}

double ComputeSignatures::getEuclideanDistance(std::vector<double> &d1, std::vector<double> &d2) {
	assert(d1.size() == d2.size());
	double dist = 0;
	for(size_t i = 0; i < d1.size(); i++) {
		dist += std::pow(d1[i] - d2[i],2);
	}
	return std::sqrt(dist);
}

std::string ComputeSignatures::computeNormalSignature(Tile &tile, const char * label) {
	std::vector<double> input;
	getAttributeVector(tile,label, input);
	//std::cout << "input size: " << input.size() << std::endl;;
	NormalSignature sig(input);
	return sig.getSignature();
}

std::string ComputeSignatures::computeHistogramSignature(Tile &tile, const char * label, int bins) {
	std::vector<double> input;
	getAttributeVector(tile,label, input);
	std::pair<double,double> range;
	getMaxMin(tile,label,range);
	double max = range.first;
	double min = range.second;
	//std::cout << "input size: " << input.size() << std::endl;;
	HistogramSignature sig(input,min,max,1.0 * bins,1.0 * input.size());
	return sig.getSignature();
}

std::string ComputeSignatures::computeFilteredHistogramSignature(Tile &tile, const char * label,const char *label2, double filterval, int bins) {
	std::vector<double> input, filter, filteredInput;
	getAttributeVector(tile,label, input);
	getAttributeVector(tile,label2, filter);
	filteredInput = filterVector(input,filter,filterval);
	std::cout << "input: " << input.size() << ", filtered: " << filteredInput.size() << std::endl;

	double max = 0;
	double min= 0;
	if(filteredInput.size() > 0) {
		std::pair<double,double> range;
		getMaxMin(tile,label,range);
		max = range.first;
		min = range.second;
	}
	//std::cout << "input size: " << input.size() << std::endl;
	HistogramSignature sig(filteredInput,min,max,1.0 * bins,1.0 * input.size());
	return sig.getSignature();
}

std::string ComputeSignatures::computeGroupedHistogramSignature(Tile &tile, const char * label,const char *label2, std::vector<double> filtervals, int bins) {
	std::vector<double> input, filter;
	std::vector<std::vector<double> > filteredInputGroups;
	getAttributeVector(tile,label, input);
	getAttributeVector(tile,label2, filter);
	size_t sizes = 0;
	for(size_t i = 0; i < filtervals.size(); i++) {
		filteredInputGroups.push_back(filterVector(input,filter,filtervals[i]));
		sizes += filteredInputGroups[i].size();
	}
	assert(filteredInputGroups.size() > 0);
	//std::cout << "input: " << input.size() << ", filtered: " << filteredInput.size() << std::endl;

	double max = 0;
	double min= 0;
	if(sizes > 0) { // if there are values after filtering
		std::pair<double,double> range;
		getMaxMin(tile,label,range);
		max = range.first;
		min = range.second;
	}
	//std::cout << "input size: " << input.size() << std::endl;
	GroupedHistogramSignature sig(filteredInputGroups,min,max,1.0 * bins,1.0 * input.size());
	return sig.getSignature();
}
