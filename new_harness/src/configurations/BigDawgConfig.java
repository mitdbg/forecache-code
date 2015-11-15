package configurations;

import abstraction.enums.DBConnector;
import abstraction.util.DBInterface;

public class BigDawgConfig extends Config {

	public BigDawgConfig() {
		super(DBConnector.BIGDAWG);
	}
	
	/*
	public BigDawgConfig(DBConnector db) {
		super(db);
	}
	*/
	
	public void setConfig() {
		DBInterface.mimic_views_folder = "/home/gridsan/lbattle/forecache-code/new_harness/mimic_views";
		DBInterface.mimic_views_folder = "/home/gridsan/lbattle/forecache-code/new_harness/modis_views";
		
		DBInterface.nice_tile_cache_dir = "/home/gridsan/lbattle/forecache-code/data/nice_tile_cache";
		//DBInterface.nice_tile_cache_dir = "/home/gridsan/lbattle/forecache-code/data/vertica_nice_tile_cache";
		DBInterface.defaultparamsfile = "/home/gridsan/lbattle/forecache-code/data/ndsi_agg_7_18_2013_params.tsv";
		DBInterface.cache_root_dir = "/home/gridsan/lbattle/forecache-code/data/test_cache";
		DBInterface.sig_root_dir = "/home/gridsan/lbattle/forecache-code/data/_scalar_sig_dir2";
		DBInterface.csv_root_dir = "/home/gridsan/lbattle/forecache-code/data/_scalar_csv_dir2";
		DBInterface.dbname  = "scalar";
		DBInterface.user  = "leilani_testuser";
		DBInterface.arrayname = "ndsi_agg_7_18_2013";
		DBInterface.dim1 = "dims.latitude_e4ndsi_06_03_2013ndsi_agg_7_18_2013";
		DBInterface.dim2  = "dims.longitude_e4ndsi_06_03_2013ndsi_agg_7_18_2013";
		DBInterface.query  = "select * from ndsi_agg_7_18_2013";
		DBInterface.hashed_query  = "2a0cf5267692de290efac7e3b6d5a593";
		DBInterface.threshold  = "90000";
		DBInterface.minuser = 121;
		DBInterface.xdim = "longitude_e4ndsi_06_03_2013";
		DBInterface.ydim = "latitude_e4ndsi_06_03_2013";
		DBInterface.zattr = "avg_ndsi";
		DBInterface.groundTruth = "/home/gridsan/lbattle/forecache-code/data/gt_updated.csv";

		DBInterface.defaultdelim = "\t";
		DBInterface.warmup_query  = "select * from cali100";
		DBInterface.warmup_hashed_query  = "39df90e13a84cad54463717b24ef833a";


		DBInterface.password = "password";
		DBInterface.host = "127.0.0.1";
		DBInterface.port = "5432";
		DBInterface.attr1  = "attrs.avg_ndsi";
		DBInterface.attr2  = "attrs.max_land_sea_mask";


		DBInterface.vertica_host = "vertica-vm";
		DBInterface.vertica_port = "5433";
		DBInterface.vertica_dbname = "test";
		DBInterface.vertica_user = "dbadmin";
		DBInterface.vertica_password = "password";

		DBInterface.scidb_host = "localhost";
		DBInterface.scidb_port = "1239";
		DBInterface.scidb_user = "scidb";
		DBInterface.scidb_password = "scidb";
	}
}
