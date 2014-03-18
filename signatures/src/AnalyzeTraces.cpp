#include <stdint.h> // int64_t, etc.
#include <cstdlib> // atoi,atof,strtoull
#include <cstring> // strlen
#include <iostream> // std::cout
#include <vector> // std::vector
#include <pqxx/pqxx> // to connect to postgres
#include <boost/filesystem.hpp> // boost::filesystem::*
#include "ComputeSignatures.h"

/*
std::string cache_root_dir("/home/leilani/_scalar_cache_dir2");
std::string sig_root_dir("/home/leilani/_scalar_sig_dir2");
std::string csv_root_dir("/home/leilani/_scalar_csv_dir2");
std::string dbname("scalar"), user("leilani_testuser");
std::string query("select * from ndsi_agg_7_18_2013"), hashed_query("2a0cf5267692de290efac7e3b6d5a593"),threshold("90000");
*/
std::string cache_root_dir("/home/leibatt/projects/user_study/scalar_backend/_scalar_cache_dir2");
std::string sig_root_dir("/home/leibatt/projects/user_study/scalar_backend/_scalar_sig_dir2");
std::string csv_root_dir("/home/leibatt/projects/user_study/scalar_backend/_scalar_csv_dir2");
std::string dbname("test"), user("testuser");
std::string query("select * from cali100"),hashed_query("85794fe89a8b0c23ce726cca7655c8bc"),threshold("90000");

std::string attr1("attrs.avg_ndsi"),attr2("attrs.max_land_sea_mask"),warmup_threshold("10000"),warmup_hashed_query("39df90e13a84cad54463717b24ef833a");

std::string password("password"), host("127.0.0.1"), port("5432");

std::string tnarr[] = {"warmup","task1","task2","task3"};
std::vector<std::string> tasknames(tnarr,tnarr+4);

const std::string get_hashed_traces =
	"SELECT a.tile_id,b.tile_hash,a.zoom_level \
	FROM user_traces as a, tile_hash as b \
	WHERE a.tile_id = b.tile_id and \
		a.user_id=$1 and a.taskname=$2 and a.query='"+query+"'";

// query for retrieving client-requested tiles for a specific user and task
const std::string get_user_traces = 
	"SELECT tile_id,zoom_level \
	FROM user_traces \
	WHERE user_id=$1 and taskname=$2 and query='"+query+"'";

// query for seeing if user completed a specific task
const std::string check_task =
	"SELECT count(*) \
	FROM user_tile_selections \
	WHERE user_id=$1 and taskname=$2 and query='"+query+"'";

// query for retrieving all users
const std::string get_users =
	"SELECT id FROM users";

bool checkTask(pqxx::connection &conn, int user_id, std::string taskname) {
	try {
		pqxx::nontransaction N(conn);
		pqxx::result R(N.prepared("check_task")(user_id)(taskname).exec());
		if(R.begin() == R.end()) {
			return false;
		}
		pqxx::result::const_iterator itr = R.begin();
		return itr[0].as<int>() > 0;
	} catch (const std::exception &e) {
		std::cerr << e.what() << std::endl;
	}
	return false;
}

std::vector<std::vector<std::string> > getHashedTraces(pqxx::connection &conn, int user_id, std::string taskname) {
	std::vector<std::vector<std::string> > myresult;
	try {
		pqxx::nontransaction N(conn);
		pqxx::result R(N.prepared("get_hashed_traces")(user_id)(taskname).exec());
		for(pqxx::result::const_iterator itr = R.begin(); itr != R.end(); ++itr) {
			std::vector<std::string> item;
			item.push_back(itr[0].as<std::string>()); // tile_id
			item.push_back(itr[1].as<std::string>()); // tile_hash
			item.push_back(itr[2].as<std::string>()); // zoom
			myresult.push_back(item);
		}
	} catch (const std::exception &e) {
		std::cerr << e.what() << std::endl;
	}
	return myresult;
}

std::vector<std::pair<std::string,int> > getUserTraces(pqxx::connection &conn, int user_id, std::string taskname) {
	std::vector<std::pair<std::string,int> > myresult;
	try {
		pqxx::nontransaction N(conn);
		pqxx::result R(N.prepared("get_user_traces")(user_id)(taskname).exec());
		for(pqxx::result::const_iterator itr = R.begin();
			itr != R.end(); ++itr) {
			std::pair<std::string,int> temp(itr[0].as<std::string>(),itr[1].as<int>());
			myresult.push_back(temp);
		}
	} catch (const std::exception &e) {
		std::cerr << e.what() << std::endl;
	}
	return myresult;
}

// get all user id's from database
std::vector<int> getUsers(pqxx::connection &conn) {
	std::vector<int> myresult;
	try {
		pqxx::nontransaction N(conn);
		pqxx::result R(N.exec(get_users));
		for(pqxx::result::const_iterator itr = R.begin();
			itr != R.end(); ++itr) {
			myresult.push_back(itr[0].as<int>());
		}
	} catch (const std::exception &e) {
		std::cerr << e.what() << std::endl;
	}
	return myresult;
}

// prepare statements for parameterized queries
void prepareStatements(pqxx::connection &conn) {
	conn.prepare("get_hashed_traces",get_hashed_traces)("integer")("varchar", pqxx::prepare::treat_string);
	conn.prepare("get_user_traces",get_user_traces)("integer")("varchar", pqxx::prepare::treat_string);
	conn.prepare("check_task",check_task)("integer")("varchar", pqxx::prepare::treat_string);
}

void getTracesForUsers(pqxx::connection &conn) {
	std::vector<int> users = getUsers(conn);
	for(int i = 0; i < users.size(); i++) {
		int user_id = users[i];
		for(int j = 0; j < tasknames.size(); j++) {
			std::string taskname = tasknames[j];	
			if(checkTask(conn,user_id,taskname)) {
				std::cout << "user " << user_id << " completed " << taskname << std::endl;
				std::vector<std::vector<std::string> > trace = getHashedTraces(conn,user_id,taskname);
				std::cout << "trace of size " << trace.size() << " generated" << std::endl;
/*
				for(int k = 0; k < trace.size(); k++) {
					std::cout << "tile id: " << trace[k][0] << ", tile hash: " << trace[k][1] << ", zoom: " << trace[k][2] << std::endl;
				}
*/

				// do stuff here to analyze trace
				// need to: retrieve tile from disk, compute signature
				// this should be done prior to analyzing user traces
				// eventually need to: find nearest neighbors of al ltiles
			}
		}
	}
}

void sigExample() {
	std::string filepath = ComputeSignatures::buildPath("/home/leibatt/projects/user_study/scalar_backend/_scalar_cache_dir2", hashed_query, threshold, "0", "fabf634233b5a4518efdbe074188301b");
	std::cout << "filepath: " << filepath << std::endl;
	const char* data = ComputeSignatures::loadFile(filepath);
	//std::cout << "data length " << std::strlen(data) << std::endl;
	//ComputeSignatures::parseTileData(data);
	Tile tile(data);
	//ComputeSignatures::computeNormalSignature(tile,"attrs.avg_ndvi");
	ComputeSignatures::computeNormalSignature(tile,"attrs.avg_ndsi");

}

void moveToCsv(const boost::filesystem::path &dir_path) {
	if(!boost::filesystem::exists(dir_path)) {
		return;
	}
	boost::filesystem::directory_iterator end_itr; // to check end of iterator
	for(boost::filesystem::directory_iterator itr(dir_path); itr != end_itr; ++itr) {
		if(boost::filesystem::is_directory(itr->status())) {
			moveToCsv(itr->path()); // recurse on new dir
		} else if (boost::filesystem::is_regular_file(itr->status())) { // see below
			boost::filesystem::path filepath = itr->path();
			//std::cout << "filename: " << filepath.string() << std::endl;
			const char* data = ComputeSignatures::loadFile(filepath.string());
			Tile tile(data);
			std::vector<double> attr1data;
			std::vector<double> attr2data;
			ComputeSignatures::getAttributeVector(tile,attr1.c_str(),attr1data);
			ComputeSignatures::getAttributeVector(tile,attr2.c_str(),attr2data);
			boost::filesystem::path zoompath = filepath.parent_path();
			boost::filesystem::path thresholdpath = zoompath.parent_path();
			boost::filesystem::path querypath = thresholdpath.parent_path().filename();
			zoompath = zoompath.filename();
			thresholdpath = thresholdpath.filename();
			std::string csvpath = ComputeSignatures::buildPath(csv_root_dir, querypath.string(), thresholdpath.string(), zoompath.string(), filepath.filename().string());
			std::cout << "csvpath: " << csvpath << std::endl;
			ComputeSignatures::writeCsv(csvpath+".csv",attr1data,attr2data);
			delete data;
		}
	}
}

void computeSignatures(const boost::filesystem::path &dir_path) {
	if(!boost::filesystem::exists(dir_path)) {
		return;
	}
	boost::filesystem::directory_iterator end_itr; // to check end of iterator
	for(boost::filesystem::directory_iterator itr(dir_path); itr != end_itr; ++itr) {
		if(boost::filesystem::is_directory(itr->status())) {
			computeSignatures(itr->path()); // recurse on new dir
		} else if (boost::filesystem::is_regular_file(itr->status())) { // see below
			boost::filesystem::path filepath = itr->path();
			//std::cout << "filename: " << filepath.string() << std::endl;
			const char* data = ComputeSignatures::loadFile(filepath.string());
			//std::cout << "data length " << std::strlen(data) << std::endl;
			//ComputeSignatures::parseTileData(data);
			Tile tile(data);
			//ComputeSignatures::computeNormalSignature(tile,"attrs.avg_ndvi");
			//std::string sig = ComputeSignatures::computeNormalSignature(tile,attr1.c_str());
			//std::string sig = ComputeSignatures::computeHistogramSignature(tile,attr1.c_str(), 400);
			std::string sig = ComputeSignatures::computeFilteredHistogramSignature(tile,attr1.c_str(),attr2.c_str(),1.0, 400);
			//std::cout << "signature: " << sig << std::endl;
			boost::filesystem::path zoompath = filepath.parent_path();
			boost::filesystem::path thresholdpath = zoompath.parent_path();
			boost::filesystem::path querypath = thresholdpath.parent_path().filename();
			zoompath = zoompath.filename();
			thresholdpath = thresholdpath.filename();
			std::string sigpath = ComputeSignatures::buildPath(sig_root_dir, querypath.string(), thresholdpath.string(), zoompath.string(), filepath.filename().string());
			//std::cout << "sigpath: " << sigpath << std::endl;
			//ComputeSignatures::writeFile(sigpath+".normalsig",sig);
			ComputeSignatures::writeFile(sigpath+".histsig",sig);
			delete data;
		}
	}
}

int main (int argc, char **argv) {
/*
	if(argc < 5) {
		std::cout << "usage: ./computeSignatures <array name> <zipf-alpha>  <zipf-numranks> <numcells>" << std::endl;
		return 0;
	}

	std::string arrayName(argv[1]);
	double alpha = std::atof(argv[2]);
	int numRanks = std::atoi(argv[3]);
	uint64_t numCells = (uint64_t) std::strtoull(argv[4],NULL,10);
*/
	pqxx::connection conn("dbname="+dbname+" user="+user+" password="+password
		+" hostaddr="+host+" port="+port);
	if(conn.is_open()) {
		std::cout << "Successfully opened database connection!" << std::endl;
	} else {
		std::cout << "Could not open database connection!" << std::endl;
		exit(0);
	}
	// setup for parameterized queries
	prepareStatements(conn);
	// execute queries to get user traces
	getTracesForUsers(conn);
	//sigExample();
	boost::filesystem::path p, rt(cache_root_dir + "/"+hashed_query);
	computeSignatures(rt);
	//moveToCsv(rt);
	return 0;
}
