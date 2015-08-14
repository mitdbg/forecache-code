package abstraction;

public class BigDawgTileInterface extends Scidb14_12TileInterface {
	
	/************* For Precomputation ***************/
	/************* Helper Functions ***************/
	
	// wrap SciDB queries in "ARRAY(?)" syntax
	public String addBigDawgWrapper(String query) {
		return "ARRAY("+query+")";
	}
	
	@Override
	public String generateRetrieveTileQuery(View v, TileStructure ts,NewTileKey id) {
		return addBigDawgWrapper(super.generateRetrieveTileQuery(v, ts, id));
	}
	
	@Override
	public String generateZoomLevelQuery(View v, TileStructure ts, int zoom) {
		return addBigDawgWrapper(super.generateZoomLevelQuery(v, ts, zoom));
	}
}
