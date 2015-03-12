package utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import backend.util.Direction;
import backend.util.DirectionClass;
import backend.util.Model;
import backend.util.Params;
import backend.util.ParamsMap;
import backend.util.TileKey;

public class UtilityFunctions {
	public static Model getModelFromString(String modelName) {
		if (modelName.equals("random")) {
			return Model.RANDOM;
		} else if (modelName.equals("hotspot")) {
			return Model.HOTSPOT;
		} else if (modelName.equals("momentum")) {
			return Model.MOMENTUM;
		} else if (modelName.equals("normal")) {
			return Model.NORMAL;
		} else if (modelName.equals("histogram")) {
			return Model.HISTOGRAM;
		} else {
			return Model.FHISTOGRAM;
		}
	}
	
	public static void printObjectArray(Object[] array) {
		if(array.length > 0) {
			System.out.print(array[0]);
		}
		for(int i = 1; i < array.length; i++) {
			System.out.print(","+array[i]);
		}
		//System.out.println();
	}
	
	public static void printStringArray(String[] array) {
		if(array.length > 0) {
			System.out.print(array[0]);
		}
		for(int i = 1; i < array.length; i++) {
			System.out.print(","+array[i]);
		}
		//System.out.println();
	}
	
	public static void printIntArray(int[] array) {
		if(array.length > 0) {
			System.out.print(array[0]);
		}
		for(int i = 1; i < array.length; i++) {
			System.out.print(","+array[i]);
		}
		//System.out.println();
	}
	
	public static String urlify(String str) {
		return str.replace(", ","_");
	}
	
	public static String unurlify(String str) {
		return "[" + str.replace("_",", ") + "]";
	}
	
	public static TileKey getKeyFromRequest(UserRequest request) {
		return new TileKey(parseTileIdInteger(request.tile_id), request.zoom);
	}
	
	public static List<Double> parseTileIdDouble(String tile_id, String delim) {
		List<Double> myresult = new ArrayList<Double>();
		String stripped = tile_id.substring(1, tile_id.length() - 1); // remove brackets
		String [] tokens = stripped.split(delim);
		try {
			for(int i = 0; i < tokens.length; i++) {
				if(tokens[i].length() > 0) {
					myresult.add(Double.parseDouble(tokens[i]));
				}
			}
		} catch (NumberFormatException e) {
			System.out.println("error occured while converting string to double");
		}
		return myresult;
	}
	
	public static int[] parseTileIdInteger(String tile_id, String delim) {
		String stripped = tile_id.substring(1, tile_id.length() - 1); // remove brackets
		String [] tokens = stripped.split(delim);
		int[] myresult = new int[tokens.length];
		try {
			for(int i = 0; i < tokens.length; i++) {
				if(tokens[i].length() > 0) {
					myresult[i] = Integer.parseInt(tokens[i]);
				}
			}
		} catch (NumberFormatException e) {
			System.out.println("error occured while converting string to int");
		}
		return myresult;
	}
	
	public static List<Double> parseTileIdDouble(String tile_id) {
		return parseTileIdDouble(tile_id,", ");
	}
	
	public static int[] parseTileIdInteger(String tile_id) {
		return parseTileIdInteger(tile_id,", ");
	}
	
	public static Direction getDirection(TileKey p, TileKey n) {
		return getDirection(p.id,n.id,p.zoom,n.zoom);
	}
	
	public static Direction getDirection(UserRequest p, UserRequest n) {
		int[] n_id = parseTileIdInteger(n.tile_id);
		int[] p_id = parseTileIdInteger(p.tile_id);
		return getDirection(p_id,n_id,p.zoom,n.zoom);
	}
	
	public static String getDirectionWord(UserRequest p, UserRequest n) {
		int[] n_id = parseTileIdInteger(n.tile_id);
		int[] p_id = parseTileIdInteger(p.tile_id);
		return getDirectionWord(p_id,n_id,p.zoom,n.zoom);
	}
	
	public static DirectionClass getDirectionClass(Direction dir) {
		if(dir == Direction.IN0 || dir == Direction.IN1 || dir == Direction.IN2 || dir == Direction.IN3) {
			return DirectionClass.IN;
		} else if (dir == Direction.OUT) {
			return DirectionClass.OUT;
		} else {
			return DirectionClass.PAN;
		}
	}
	
	public static DirectionClass getDirectionClass(int[] p_id, int[] n_id, int pzoom, int nzoom) {
		return getDirectionClass(getDirection(p_id,n_id,pzoom,nzoom));
	}
	
	public static String getDirectionWord(int[] p_id, int[] n_id, int pzoom, int nzoom) {
		int zoomdiff = nzoom - pzoom;
		int xdiff = n_id[0] - p_id[0];
		int ydiff = n_id[1] - p_id[1];
		Direction d = null;
		//System.out.println("zoomdiff: "+zoomdiff+", xdiff: "+xdiff+", ydiff: "+ydiff);
		if(zoomdiff < -1) { // user reset back to top level tile
			d = null;
		} else if(zoomdiff < 0) { // zoom out
			d = Direction.OUT;
		} else if(zoomdiff > 0) { // zoom in
			xdiff = n_id[0] - 2 * p_id[0];
			ydiff = n_id[1] - 2 * p_id[1];
			if((xdiff > 0) && (ydiff > 0)) {
				d = Direction.IN2;
			} else if(xdiff > 0) {
				d = Direction.IN3;
			} else if(ydiff > 0) {
				d = Direction.IN1;
			} else {
				d = Direction.IN0;
			}
		} else if(xdiff > 0) {
			d = Direction.RIGHT;
		} else if(xdiff < 0) {
			d = Direction.LEFT;
		} else if(ydiff > 0) {
			d = Direction.UP;
		} else if(ydiff < 0 ){
			d = Direction.DOWN;
		}
		if(d!= null) {
			return d.getWord();
		}
		return null;
	}
	
	public static Direction getDirection(int[] p_id, int[] n_id, int pzoom, int nzoom) {
		int zoomdiff = nzoom - pzoom;
		int xdiff = n_id[0] - p_id[0];
		int ydiff = n_id[1] - p_id[1];
		//System.out.println("zoomdiff: "+zoomdiff+", xdiff: "+xdiff+", ydiff: "+ydiff);
		if(zoomdiff < -1) { // user reset back to top level tile
			return null;
		} else if(zoomdiff < 0) { // zoom out
			return Direction.OUT;
		} else if(zoomdiff > 0) { // zoom in
			xdiff = n_id[0] - 2 * p_id[0];
			ydiff = n_id[1] - 2 * p_id[1];
			if((xdiff > 0) && (ydiff > 0)) {
				return Direction.IN2;
			} else if(xdiff > 0) {
				return Direction.IN3;
			} else if(ydiff > 0) {
				return Direction.IN1;
			} else {
				return Direction.IN0;
			}
		} else if(xdiff > 0) {
			return Direction.RIGHT;
		} else if(xdiff < 0) {
			return Direction.LEFT;
		} else if(ydiff > 0) {
			return Direction.UP;
		} else if(ydiff < 0 ){
			return Direction.DOWN;
		}
		return null;
	}
	
	public static TileKey requestToKey(UserRequest req) {
		int[] id = UtilityFunctions.parseTileIdInteger(req.tile_id);
		TileKey key = new TileKey(id,req.zoom);
		return key;
	}
	
	public static boolean diffKeys(TileKey a, TileKey b) {
		if(a.zoom != b.zoom) return false;
		for(int i = 0; i < a.id.length; i++) {
			if(a.id[i] != b.id[i]) return false;
		}
		return true;
	}
	
	public static List<Direction> buildDirectionPath(List<TileKey> path) {
		List<Direction> newPath = new ArrayList<Direction>();
		if(path.size() <= 1) return newPath;
		TileKey p = path.get(0);
		for(int i = 1; i < path.size(); i++) {
			TileKey n = path.get(i);
			newPath.add(getDirection(p,n));
			p = n;
		}
		return newPath;
	}
	
	public static List<TileKey> buildPath2(TileKey a, TileKey b) {
		List<TileKey> path1 = new ArrayList<TileKey>();
		List<TileKey> path2 = new ArrayList<TileKey>();
		path1.add(a);
		path2.add(b);
		int zoom1 = a.zoom;
		int zoom2 = b.zoom;
		int[] id1 = new int[a.id.length];
		int[] id2 = new int[b.id.length];
		id1 = Arrays.copyOf(a.id,a.id.length);
		id2 = Arrays.copyOf(b.id,b.id.length);
		TileKey curr1 = new TileKey(id1,a.zoom);
		TileKey curr2 = new TileKey(id2,b.zoom);
		boolean change1 = false;
		boolean change2 = false;
		while(!diffKeys(curr1,curr2)) {
			if(zoom1 == zoom2) {
				zoom1--; // zoom out
				zoom2--;
				change1 = true;
				change2 = true;
			} else if(zoom1 > zoom2) {
				zoom1--;
				change1 = true;
				change2 = false;
			} else {
				zoom2--;
				change1 = false;
				change2 = true;
			}
			if((zoom1 < 0) || !change1) { // can't move anymore
				if(zoom1 < 0) {
					zoom1 = 0;
				}
			} else {
				id1 = Arrays.copyOf(id1,id1.length);
				for(int i = 0; i < id1.length; i++) {
					id1[i] /= 2;
				}
				curr1 = new TileKey(id1,zoom1);
				path1.add(curr1);
			}
			
			if((zoom2 < 0) || !change2) { // can't move anymore
				if(zoom2 < 0) {
					zoom2 = 0;
				}
			} else {
				id2 = Arrays.copyOf(id2,id2.length);
				for(int i = 0; i < id2.length; i++) {
					id2[i] /= 2;
				}
				curr2 = new TileKey(id2,zoom2);
				path2.add(curr2);
			}
			//System.out.println("a: "+curr1.zoom+"["+curr1.id[0]+","+curr1.id[1]+"]");
			//System.out.println("b: "+curr2.zoom+"["+curr2.id[0]+","+curr2.id[1]+"]");
		}
		path2.remove(path2.size() - 1);
		Collections.reverse(path2);
		path1.addAll(path2);
		//System.out.print("path: ");
		//for(int i = 0; i < path1.size(); i++) {
		//	System.out.print(path1.get(i)+" ");
		//}
		//System.out.println();
		return path1;
	}
	
	public static List<TileKey> buildPath(TileKey a, TileKey b) {
		TileKey finalA, finalB;
		List<TileKey> path = new ArrayList<TileKey>();
		if(b.zoom <= a.zoom) { // zoom out to b
			finalB = b;
			int newzoom = a.zoom;
			int[] id = Arrays.copyOf(a.id, a.id.length);
			TileKey curr = new TileKey(id,newzoom);
			path.add(curr);
			while(newzoom > b.zoom) {
				newzoom--;
				id = Arrays.copyOf(id, id.length);
				for(int i = 0; i < id.length; i++) {
					id[i] /= 2;
				}
				curr = new TileKey(id,newzoom);
				path.add(curr);
			}
			finalA = new TileKey(id,newzoom);
			
			for(int i = 0; i < finalA.id.length; i++) {
				int diff = id[i] - finalB.id[i];
				while(diff != 0) {
					if(diff < 0) { // a < b
						id = Arrays.copyOf(id, id.length);
						id[i]++;
						path.add(new TileKey(id,newzoom));
					} else if (diff > 0) { // a > b
						id = Arrays.copyOf(id, id.length);
						id[i]--;
						curr = new TileKey(id,newzoom);
						path.add(curr);
					}
					diff = id[i] - finalB.id[i];
				}
			}
		} else { // zoom in to b
			finalA = a;
			int newzoom = b.zoom;
			int[] id = Arrays.copyOf(b.id, b.id.length);
			TileKey curr = new TileKey(id,newzoom);
			path.add(curr);
			while(newzoom > a.zoom) {
				newzoom--;
				id = Arrays.copyOf(id, id.length);
				for(int i = 0; i < id.length; i++) {
					id[i] /= 2;
				}
				path.add(new TileKey(id,newzoom));
			}
			finalB = new TileKey(id,newzoom);
			for(int i = 0; i < finalB.id.length; i++) {
				int diff = id[i] - finalA.id[i];
				while(diff != 0) {
					if(diff < 0) { // a < b
						id = Arrays.copyOf(id, id.length);
						id[i]++;
						curr = new TileKey(id,newzoom);
						path.add(curr);
					} else if (diff > 0) { // a > b
						id = Arrays.copyOf(id, id.length);
						id[i]--;
						curr = new TileKey(id,newzoom);
						path.add(curr);
					}
					diff = id[i] - finalA.id[i];
				}
			}
			Collections.reverse(path);
		}
		//System.out.print("path: ");
		//for(int i = 0; i < path.size(); i++) {
		//	System.out.print(path.get(i)+" ");
		//}
		//System.out.println();
		return path;
	}
	
	public static double manhattanDist(TileKey a, TileKey b) {
		double finalDist = 0;
		TileKey finalA, finalB;
		if(b.zoom <= a.zoom) {
			finalB = b;
			int newzoom = a.zoom;
			int[] id = Arrays.copyOf(a.id, a.id.length);
			while(newzoom > b.zoom) {
				newzoom--;
				finalDist += 1;
				for(int i = 0; i < id.length; i++) {
					id[i] /= 2;
				}
			}
			finalA = new TileKey(id,newzoom);
		} else {
			finalA = a;
			int newzoom = b.zoom;
			int[] id = Arrays.copyOf(b.id, b.id.length);
			while(newzoom > a.zoom) {
				newzoom--;
				finalDist += 1;
				for(int i = 0; i < id.length; i++) {
					id[i] /= 2;
				}
			}
			finalB = new TileKey(id,newzoom);
		}
		for(int i = 0; i < finalA.id.length; i++) {
			double temp = finalA.id[i] - finalB.id[i];
			if(temp < 0) {
				finalDist -= temp;
			} else {
				finalDist += temp;
			}
		}
		return finalDist;
	}
}
