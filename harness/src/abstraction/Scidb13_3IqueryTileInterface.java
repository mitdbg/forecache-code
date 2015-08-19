package abstraction;

import java.util.List;

public class Scidb13_3IqueryTileInterface extends Scidb14_12IqueryTileInterface {
	//NOTE: SciDB 13.3 does *not* support tsv output format for iquery!!!!
	protected String scidbVersion = "13.3";
	protected String outputFormat = "csv+";

	/************* For Precomputation ***************/
	/************* Helper Functions ***************/
	@Override
	public synchronized String[] buildCmdNoOutput(String query) {
		String[] myresult = new String[3];
		myresult[0] = "bash";
		myresult[1] = "-c";
		myresult[2] = "export SCIDB_VER="+scidbVersion+" ; "+
				"export PATH=/opt/scidb/$SCIDB_VER/bin:/opt/scidb/$SCIDB_VER/share/scidb:$PATH ; "+
				"export LD_LIBRARY_PATH=/opt/scidb/$SCIDB_VER/lib:$LD_LIBRARY_PATH ; "+
				"source ~/.bashrc ; iquery -o "+outputFormat+" -aq \"" + query + "\"";
		return myresult;
	}

	@Override
	public synchronized String[] buildCmd(String query) {
		String[] myresult = new String[3];
		myresult[0] = "bash";
		myresult[1] = "-c";
		myresult[2] = "export SCIDB_VER="+scidbVersion+" ; "+
				"export PATH=/opt/scidb/$SCIDB_VER/bin:/opt/scidb/$SCIDB_VER/share/scidb:$PATH ; "+
				"export LD_LIBRARY_PATH=/opt/scidb/$SCIDB_VER/lib:$LD_LIBRARY_PATH ; "+
				"source ~/.bashrc ; iquery -o "+outputFormat+" -aq \"" + query + "\"";
		return myresult;
	}
	
	public static void main(String[] args) {
		String query = "list('arrays')";
		ColumnBasedNiceTile tile = new ColumnBasedNiceTile();
		Scidb13_3IqueryTileInterface sdbi = new Scidb13_3IqueryTileInterface();
		List<String> dataTypes = sdbi.getQueryDataTypes(query);
		for(String dt : dataTypes) {
			System.out.println(dt);
		}
		System.out.println();
		sdbi.getTile(query, tile);
		for(String a : tile.attributes) {
			System.out.println(a);
		}
		System.out.println();
		System.out.println(tile.getSize());
		System.out.println();
		for(Class<?> c : tile.dataTypes) {
			System.out.println(c);
		}
		System.out.println();
		System.out.println(tile.get(0, 0));
		System.out.println(tile.get(1, 0));
		System.out.println(tile.get(2, 0));
		System.out.println(tile.get(3, 0));
		System.out.println(tile.get(4, 0));
	}
}
