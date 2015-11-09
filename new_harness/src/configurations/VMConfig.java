package configurations;

import abstraction.enums.DBConnector;
import abstraction.util.DBInterface;

public class VMConfig extends Config {

	public VMConfig() {
		super();
	}
	
	public VMConfig(DBConnector db) {
		super(db);
	}
	
	public void setConfig() {
		DBInterface.mimic_views_folder = "/home/leibatt/projects/forecache-code/harness/mimic_views";
		DBInterface.modis_views_folder = "/home/leibatt/projects/forecache-code/harness/modis_views";
		
		DBInterface.nice_tile_cache_dir = "/home/leibatt/projects/user_study/scalar_backend/nice_tile_cache";
		//DBInterface.nice_tile_cache_dir = "/home/leibatt/projects/user_study/scalar_backend/vertica_nice_tile_cache";
		DBInterface.defaultparamsfile = "/home/leibatt/projects/user_study/scalar_backend/thesis2_params.tsv";
		DBInterface.cache_root_dir = "/home/leibatt/projects/user_study/scalar_backend/test_cache";
		//DBInterface.cache_root_dir = "/home/leibatt/projects/user_study/scalar_backend/test_cache2";
		DBInterface.sig_root_dir = "/home/leibatt/projects/user_study/scalar_backend/_scalar_sig_dir2";
		DBInterface.csv_root_dir = "/home/leibatt/projects/user_study/scalar_backend/_scalar_csv_dir2";
		DBInterface.dbname  = "test";
		DBInterface.user  = "testuser";
		DBInterface.arrayname = "thesis2";
		DBInterface.dim1 = "dims.xthesis2";
		DBInterface.dim2  = "dims.ythesis2";
		DBInterface.query  = "select * from thesis2";
		DBInterface.hashed_query  = "85794fe89a8b0c23ce726cca7655c8bc";
		DBInterface.threshold  = "90000";
		DBInterface.minuser = 28;
		DBInterface.xdim = "x";
		DBInterface.ydim = "y";
		DBInterface.zattr = "avg_ndsi";
		DBInterface.groundTruth = "/home/leibatt/projects/scalar-prefetch/gt_updated.csv";

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
