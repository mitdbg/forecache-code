package utils;

import java.util.ArrayList;
import java.util.List;

import backend.util.Direction;
import backend.util.DirectionClass;
import backend.util.Model;

public class UtilityFunctions {
	public static Model getModelFromString(String modelName) {
		if(modelName.equals("markov")) {
			return Model.MARKOV;
		} else if (modelName.equals("random")) {
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
		System.out.println();
	}
	
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
	
	public static String getDirectionWord(UserRequest p, UserRequest n) {
		List<Integer> n_id = parseTileIdInteger(n.tile_id);
		List<Integer> p_id = parseTileIdInteger(p.tile_id);
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
	
	public static DirectionClass getDirectionClass(List<Integer> p_id, List<Integer> n_id, int pzoom, int nzoom) {
		return getDirectionClass(getDirection(p_id,n_id,pzoom,nzoom));
	}
	
	public static String getDirectionWord(List<Integer> p_id, List<Integer> n_id, int pzoom, int nzoom) {
		int zoomdiff = nzoom - pzoom;
		int xdiff = n_id.get(0) - p_id.get(0);
		int ydiff = n_id.get(1) - p_id.get(1);
		Direction d = null;
		//System.out.println("zoomdiff: "+zoomdiff+", xdiff: "+xdiff+", ydiff: "+ydiff);
		if(zoomdiff < -1) { // user reset back to top level tile
			d = null;
		} else if(zoomdiff < 0) { // zoom out
			d = Direction.OUT;
		} else if(zoomdiff > 0) { // zoom in
			xdiff = n_id.get(0) - 2 * p_id.get(0);
			ydiff = n_id.get(1) - 2 * p_id.get(1);
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
