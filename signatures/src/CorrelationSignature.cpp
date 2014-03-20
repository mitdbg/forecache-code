#include <iostream> // std::cout
#include <numeric> // std::accumulate
#include <algorithm> // std::transform
#include <cmath> // std::sqrt, std::pow
#include <functional> // std::minus
#include <stdint.h> // int64_t, etc.
#include <sstream> //std::ostringstream
#include "CorrelationSignature.h"
#include "rapidjson/document.h"

CorrelationSignature::CorrelationSignature(std::vector<double> &dim,std::vector<double> &attr) : pos(), histogram() {
	normalize_output = false;
	computeSignature(dim,attr);
}

CorrelationSignature::CorrelationSignature(const char *json) : pos(), histogram() {
	normalize_output = false;
	parseSigData(json);
}

void CorrelationSignature::computeSignature(std::vector<double> &dim,std::vector<double> &attr) {
	std::map<double,std::vector<double> > groups; //automatically sorts keys
	std::map<double,std::vector<double> >::iterator it, next;
	histogram.clear();
	assert(dim.size() == attr.size());
	// build ordered "columns"
	for(size_t i = 0; i < attr.size(); i++) {
		// for this dim value, add corresponding attr val to the list
		groups[dim[i]].push_back(attr[i]);
	}
	it = groups.begin();
	if(it == groups.end()) { // no groups! no data?
		return;
	}
	next = groups.begin();
	next++; // next should be 1 ahead
	if(next == groups.end()) { // no pairs!
		return;
	}
	// while next is not the end
	while(next != groups.end()) {
		// compute pairwise correlation
		size_t count = it->second.size();
		size_t count2 = next->second.size();
		if(count < count2) { // ignore extra values in longer vector
			count = count2;
		}
		std::vector<double> vec1 = it->second;
		std::vector<double> vec2 = it->second;
		double corr, t1, t2, t1_2, t2_2, t1xt2, e1 = 0, e2 = 0;
		for(size_t i = 0; i < count; i++) {
			e1 += vec1[i];
			e2 += vec2[i];
		}
		e1 /= count;
		e2 /= count;
		for(size_t i = 0; i < count; i++) {
			t1 = vec1[i] - e1;
			t2 = vec2[i] - e2;
			t1_2 += std::pow(t1,2);
			t2_2 += std::pow(t2,2);
			t1xt2 = t1 * t2;
		}
		corr = t1xt2 / (std::sqrt(t1_2 * t2_2) + CORR_MIN);
		if(normalize_output) {
			corr = (corr + 1) / 2;
		}
		// store pearson correlation value in histogram
		histogram.push_back(corr);

		// update iterators
		it++;
		next++;
	}
	for(size_t i = 0; i < histogram.size(); i++) {
		if(histogram[i] < MY_DEFAULT_MIN) { // don't let any bins be 0
			histogram[i] = MY_DEFAULT_MIN;
		}
	}
	//std::cout << "input size " << attr.size() << ", histogram size " << histogram.size() << std::endl;
}

// return bins as a string
std::string CorrelationSignature::getSignature() {
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

void CorrelationSignature::parseSigData(const char *json) {
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
double CorrelationSignature::computeSimilarity(CorrelationSignature &other) {
	assert(histogram.size() == other.histogram.size());
	double distance = 0;
	for(size_t i = 0; i < histogram.size(); i++) {
		distance += std::pow(other.histogram[i] - histogram[i],2) / histogram[i];
	}
	return distance;
}

