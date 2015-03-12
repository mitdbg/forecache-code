#include <iostream> // std::cout
#include <numeric> // std::accumulate
#include <algorithm> // std::transform
#include <cmath> // std::sqrt
#include <functional> // std::minus
#include <stdint.h> // int64_t, etc.
#include <sstream> //std::ostringstream
#include "NormalSignature.h"
#include "rapidjson/document.h"

bool normalize_input = true;

NormalSignature::NormalSignature(std::vector<double> &input, double min, double max) : pos() {
	computeSignature(input, min, max);
}

NormalSignature::NormalSignature(const char *json) : pos() {
	parseSigData(json);
}

void NormalSignature::computeSignature(std::vector<double> &input, double min, double max) {
	double sum = 0;
	double count = 1.0 * input.size();
	if(normalize_input) {
		double range = max - min;
		for(size_t i = 0; i < count; i++) {
			input[i] = (input[i] - min) / range; // normalize in place
			sum += input[i]; // sum normalized version;
		}
	} else {
		sum = std::accumulate(input.begin(),input.end(),0.0);
	}
	mean = sum / count;
	std::vector<double> diff(input.size());
	std::transform(input.begin(), input.end(), diff.begin(), std::bind2nd(std::minus<double>(), mean));
	double sq_sum = std::inner_product(diff.begin(), diff.end(), diff.begin(), 0.0);
	stddev = std::sqrt(sq_sum / count);
	mean += MY_DEFAULT_MIN; // don't let these be 0
	stddev += MY_DEFAULT_MIN;
	//std::cout << "mean: " << mean << ", stddev: " << stddev << std::endl;
}

std::string NormalSignature::getSignature() {
	std::ostringstream sig;
	//sig << "{\"mean\":" << mean << ", \"stddev\":" << stddev << "}";

	// treat as a histogram
	sig << "{\"histogram\":[" << mean << ", " << stddev << "]}";
	return sig.str();
}

void NormalSignature::parseSigData(const char *json) {
	rapidjson::Document root;
	root.Parse<0>(json);

	mean = root["mean"].GetDouble();
	stddev = root["stddev"].GetDouble();
}

double NormalSignature::computeSimilarity(NormalSignature &other) {
	double md = std::pow(mean - other.mean,2);
	double sd = std::pow(stddev - other.stddev,2);
	return (md + sd);
}

void NormalSignature::normalize(std::vector<double> &input) {
}

