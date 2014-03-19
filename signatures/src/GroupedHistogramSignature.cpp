#include <iostream> // std::cout
#include <numeric> // std::accumulate
#include <algorithm> // std::transform
#include <cmath> // std::sqrt, std::pow
#include <functional> // std::minus
#include <stdint.h> // int64_t, etc.
#include <sstream> //std::ostringstream
#include "GroupedHistogramSignature.h"
#include "rapidjson/document.h"

GroupedHistogramSignature::GroupedHistogramSignature(std::vector<std::vector<double> > &input, double mn, double mx, double bins, double origcount) : pos(), histogram() {
	computeSignature(input,mn,mx,bins,origcount);
}

GroupedHistogramSignature::GroupedHistogramSignature(const char *json) : pos(), histogram() {
	parseSigData(json);
}

void GroupedHistogramSignature::computeSignature(std::vector<std::vector<double> > &input, double mn, double mx, double bins, double origcount) {
	assert(input.size() > 0);
	size_t groups = input.size();
	histogram.clear();
	std::vector<double> temp;
	double binwidth = (mx-mn) / bins;

	// for each group
	for(size_t i = 0; i < groups; i++) {
		// build a temporary histogram for this group
		temp.clear();
		temp.resize(bins);
		for(size_t j = 0; j < input[i].size(); j++) {
			temp[(input[i][j]-mn)/binwidth]++;
		}
		for(size_t j = 0; j < temp.size(); j++) {
			if(temp[j] == 0) { // don't let any bins be 0
				temp[j] = MY_DEFAULT_MIN;
			} else { // normalize
				temp[j] /= origcount;
			}
		}
		// add the group to the final histogram
		histogram.insert(histogram.end(),temp.begin(),temp.end());
	}
	//std::cout << "histogram size: " << histogram.size() << std::endl;
}

// return bins as a string
std::string GroupedHistogramSignature::getSignature() {
	std::ostringstream sig;
	sig << "{\"histogram\":[";
	if(histogram.size() > 0) { // add first bin to stream
		sig << histogram[0];
	}
	for(size_t i = 1; i < histogram.size(); i++) {
		sig << "," << histogram[i];
	}
	sig << "]}";
	return sig.str();
}

void GroupedHistogramSignature::parseSigData(const char *json) {
	rapidjson::Document root;
	root.Parse<0>(json);
	const rapidjson::Value &h = root["histogram"];
	for(rapidjson::SizeType i = 0; i < h.Size(); i++) {
		histogram.push_back(h[i].GetDouble());
		//std::cout << histogram[i] << " ";
	}
	//std::cout << std::endl;
}

// modeled after chi squared distance
double GroupedHistogramSignature::computeSimilarity(GroupedHistogramSignature &other) {
	assert(histogram.size() == other.histogram.size());
	double distance = 0;
	for(size_t i = 0; i < histogram.size(); i++) {
		distance += std::pow(other.histogram[i] - histogram[i],2) / histogram[i];
	}
	return distance;
}


