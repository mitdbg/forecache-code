#include <iostream> // std::cout
#include <numeric> // std::accumulate
#include <algorithm> // std::transform
#include <cmath> // std::sqrt
#include <functional> // std::minus
#include <stdint.h> // int64_t, etc.
#include <sstream> //std::ostringstream
#include "NormalSignature.h"
#include "rapidjson/document.h"

NormalSignature::NormalSignature(std::vector<double> &input) : pos() {
	computeSignature(input);
}

NormalSignature::NormalSignature(const char *json) : pos() {
	parseSigData(json);
}

void NormalSignature::computeSignature(std::vector<double> &input) {
	double sum,count;
	count = 1.0 * input.size();
	sum = std::accumulate(input.begin(),input.end(),0.0);
	mean = sum / count;
	std::vector<double> diff(input.size());
	std::transform(input.begin(), input.end(), diff.begin(), std::bind2nd(std::minus<double>(), mean));
	double sq_sum = std::inner_product(diff.begin(), diff.end(), diff.begin(), 0.0);
	stddev = std::sqrt(sq_sum / count);
	//std::cout << "mean: " << mean << ", stddev: " << stddev << std::endl;
}

std::string NormalSignature::getSignature() {
	std::ostringstream sig;
	sig << "{\"mean\":" << mean << ", \"stddev\":" << stddev << "}";
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

