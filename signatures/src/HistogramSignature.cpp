#include <iostream> // std::cout
#include <numeric> // std::accumulate
#include <algorithm> // std::transform
#include <cmath> // std::sqrt, std::pow
#include <functional> // std::minus
#include <stdint.h> // int64_t, etc.
#include <sstream> //std::ostringstream
#include "HistogramSignature.h"
#include "rapidjson/document.h"

double MY_DEFAULT_MIN = .0001;

HistogramSignature::HistogramSignature(std::vector<double> &input, double mn, double mx, double bins, double origcount) : pos(), histogram() {
	computeSignature(input,mn,mx,bins,origcount);
}

HistogramSignature::HistogramSignature(const char *json) : pos(), histogram() {
	parseSigData(json);
}

void HistogramSignature::computeSignature(std::vector<double> &input, double mn, double mx, double bins, double origcount) {
	histogram.clear();
	histogram.resize(bins);
	double binwidth = (mx-mn) / bins;
	for(size_t i = 0; i < input.size(); i++) {
		histogram[(input[i]-mn)/binwidth]++;
	}
	for(size_t i = 0; i < histogram.size(); i++) {
		if(histogram[i] == 0) { // don't let any bins be 0
			histogram[i] = MY_DEFAULT_MIN;
		} else { // normalize
			histogram[i] /= origcount;
		}
	}
}

// return bins as a string
std::string HistogramSignature::getSignature() {
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

void HistogramSignature::parseSigData(const char *json) {
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
double HistogramSignature::computeSimilarity(HistogramSignature &other) {
	assert(histogram.size() == other.histogram.size());
	double distance = 0;
	for(size_t i = 0; i < histogram.size(); i++) {
		distance += std::pow(other.histogram[i] - histogram[i],2) / histogram[i];
	}
	return distance;
}


