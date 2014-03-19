#ifndef _GROUPED_HISTOGRAM_SIGNATURE_
#define _GROUPED_HISTOGRAM_SIGNATURE_

#include <vector>
#include "Tile.h"

class GroupedHistogramSignature {
public:
	std::vector<double> histogram;
	std::vector<double> pos; // position of tile represented by signature
	// construct from vector of attribute values
	GroupedHistogramSignature(std::vector<std::vector<double> > &input, double mn, double mx, double bins, double origcount);
	// construct from pre-computed signature read on disk
	GroupedHistogramSignature(const char *json);
	// returns signature encoded as a json string
	std::string getSignature();
	// returns a similarity score between this signature and another signature
	double computeSimilarity(GroupedHistogramSignature &other);
private:
	static const double MY_DEFAULT_MIN = .0001;
	// computes histogram for a group of attributes or filters
	void computeSignature(std::vector<std::vector<double> > &input, double mn, double mx, double bins, double origcount);
	// retrieves histogram from json string
	void parseSigData(const char *json);
};

#endif
