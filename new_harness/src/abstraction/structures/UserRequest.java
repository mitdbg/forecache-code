package abstraction.structures;

/*
 * Local class for storing information about individual tile requests
 * 
 */
public class UserRequest {
	public String tile_id;
	public String tile_hash;
	public int zoom;

	public UserRequest(String tile_id,int zoom) {
		this.tile_id = tile_id;
		this.zoom = zoom;
		this.tile_hash = "";
	}
	
	public UserRequest(String tile_id, int zoom, String tile_hash) {
		this(tile_id,zoom);
		this.tile_hash = tile_hash;
		
	}
	
	@Override
	public String toString() {
		return "UserRequest("+this.zoom+","+this.tile_id+")";
	}
}
