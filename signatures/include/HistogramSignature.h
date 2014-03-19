#ifndef _HISTOGRAM_SIGNATURE_
#define _HISTOGRAM_SIGNATURE_

#include <vector>
#include "Tile.h"

class HistogramSignature {
public:
	std::vector<double> histogram;
	std::vector<double> pos;
	// construct from vector of attribute values
	HistogramSignature(std::vector<double> &input, double mn, double mx, double bins, double origcount);
	// construct from pre-computed signature read on disk
	HistogramSignature(const char *json);
	// returns signature encoded as a json object
	std::string getSignature();
	// returns a similarity score between this signature and another signature
	double computeSimilarity(HistogramSignature &other);
private:
	static const double MY_DEFAULT_MIN = .0001;
	// computes mean and standard deviation for a single attribute
	void computeSignature(std::vector<double> &input, double mn, double mx, double bins, double origcount);
	// retrieves mean and standard deviation from json string
	void parseSigData(const char *json);
};

#endif
