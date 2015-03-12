#ifndef _CORRELATION_SIGNATURE_
#define _CORRELATION_SIGNATURE_
#include "Tile.h"
#include <vector>

class CorrelationSignature {
public:
	std::vector<double> histogram;
	std::vector<double> pos;
	bool normalize_output;
	// construct from vector of attribute values
	CorrelationSignature(std::vector<double> &dim, std::vector<double> &attr);
	// construct from pre-computed signature read on disk
	CorrelationSignature(const char *json);
	// returns signature encoded as a json object
	std::string getSignature();
	// returns a similarity score between this signature and another signature
	double computeSimilarity(CorrelationSignature &other);
private:
	static const double MY_DEFAULT_MIN = 1e-10;
	static const double CORR_MIN = 1e-20;
	// computes mean and standard deviation for a single attribute
	void computeSignature(std::vector<double> &dim, std::vector<double> &attr);
	// retrieves mean and standard deviation from json string
	void parseSigData(const char *json);
};

#endif
