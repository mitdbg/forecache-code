package utils;

import java.util.ArrayList;
import java.util.List;

import backend.util.Direction;

public class UtilityFunctions {
	
	public static String urlify(String str) {
		return str.replace(", ","_");
	}
	
	public static String unurlify(String str) {
		return "[" + str.replace("_",", ") + "]";
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
	
	public static List<Integer> parseTileIdInteger(String tile_id, String delim) {
		List<Integer> myresult = new ArrayList<Integer>();
		String stripped = tile_id.substring(1, tile_id.length() - 1); // remove brackets
		String [] tokens = stripped.split(delim);
		try {
			for(int i = 0; i < tokens.length; i++) {
				if(tokens[i].length() > 0) {
					myresult.add(Integer.parseInt(tokens[i]));
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
	
	public static List<Integer> parseTileIdInteger(String tile_id) {
		return parseTileIdInteger(tile_id,", ");
	}
	
	public static Direction getDirection(UserRequest p, UserRequest n) {
		List<Integer> n_id = parseTileIdInteger(n.tile_id);
		List<Integer> p_id = parseTileIdInteger(p.tile_id);
		return getDirection(p_id,n_id,p.zoom,n.zoom);
	}
	
	public static Direction getDirection(List<Integer> p_id, List<Integer> n_id, int pzoom, int nzoom) {
		int zoomdiff = nzoom - pzoom;
		int xdiff = n_id.get(0) - p_id.get(0);
		int ydiff = n_id.get(1) - p_id.get(1);
		//System.out.println("zoomdiff: "+zoomdiff+", xdiff: "+xdiff+", ydiff: "+ydiff);
		if(zoomdiff < -1) { // user reset back to top level tile
			return null;
		} else if(zoomdiff < 0) { // zoom out
			return Direction.OUT;
		} else if(zoomdiff > 0) { // zoom in
			xdiff = n_id.get(0) - 2 * p_id.get(0);
			ydiff = n_id.get(1) - 2 * p_id.get(1);
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
}
