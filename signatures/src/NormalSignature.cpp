#include <iostream> // std::cout
#include <numeric> // std::accumulate
#include <algorithm> // std::transform
#include <cmath> // std::sqrt
#include <functional> // std::minus
#include <stdint.h> // int64_t, etc.
#include <sstream> //std::ostringstream
#include "NormalSignature.h"

NormalSignature::NormalSignature(std::vector<double> &input) {
	computeSignature(input);
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
