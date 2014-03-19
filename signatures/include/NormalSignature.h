#ifndef _NORMAL_SIGNATURE_
#define _NORMAL_SIGNATURE_
#include "Tile.h"
#include <vector>

class NormalSignature {
public:
	double mean;
	double stddev;
	std::vector<double> pos;
	// construct from vector of attribute values
	NormalSignature(std::vector<double> &input);
	// construct from pre-computed signature read on disk
	NormalSignature(const char *json);
	// returns signature encoded as a json object
	std::string getSignature();
	// returns a similarity score between this signature and another signature
	double computeSimilarity(NormalSignature &other);
	// normalizes input data
	void normalize(std::vector<double> &input);
private:
	// computes mean and standard deviation for a single attribute
	void computeSignature(std::vector<double> &input);
	// retrieves mean and standard deviation from json string
	void parseSigData(const char *json);
};

#endif
