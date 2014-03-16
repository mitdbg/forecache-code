#ifndef _NORMAL_SIGNATURE_
#define _NORMAL_SIGNATURE_
#include "Tile.h"
#include <vector>

class NormalSignature {
public:
	double mean;
	double stddev;
	NormalSignature(std::vector<double> &input);
	std::string getSignature();
private:
	// computes mean and standard deviation for a single attribute
	void computeSignature(std::vector<double> &input);
};

#endif
