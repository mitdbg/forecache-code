#ifndef _HISTOGRAM_SIGNATURE_
#define _HISTOGRAM_SIGNATURE_

#include "Tile.h"
#include <vector>

class HistogramSignature {
public:
	std::vector<double> histogram;
	// construct from vector of attribute values
	HistogramSignature(std::vector<double> &input, double mn, double mx, double bins);
	// construct from pre-computed signature read on disk
	HistogramSignature(const char *json);
	// returns signature encoded as a json object
	std::string getSignature();
	// returns a similarity score between this signature and another signature
	double computeSimilarity(HistogramSignature &other);
private:
	// computes mean and standard deviation for a single attribute
	void computeSignature(std::vector<double> &input, double mn, double mx, double bins);
	// retrieves mean and standard deviation from json string
	void parseSigData(const char *json);
};

#endif
