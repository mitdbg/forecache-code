#include <fstream> // std::ifstream
#include <iostream> // std::cout
#include <map> // std::map
#include <boost/filesystem.hpp> // boost::filesystem::*
#include "ComputeSignatures.h"
#include "NormalSignature.h"

/*
data types: double, int64, uint64
*/

void ComputeSignatures::writeFile(std::string filepath, std::string data) {
	boost::filesystem::path p(filepath);
	p = p.parent_path();
	if(!(boost::filesystem::exists(p))) {
		if(boost::filesystem::create_directories(p)) {
			std::ofstream o(filepath.c_str());
			assert(o.is_open());
			size_t bytes = data.size();
			const char * towrite = data.c_str();
			o.write(towrite,bytes);
		}
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

std::string ComputeSignatures::computeNormalSignature(Tile &tile, const char * label) {
	std::vector<double> input;
	getAttributeVector(tile,label, input);
	//std::cout << "input size: " << input.size() << std::endl;;
	NormalSignature sig(input);
	return sig.getSignature();
}
