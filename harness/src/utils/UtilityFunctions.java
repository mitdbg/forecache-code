package utils;

import java.util.ArrayList;
import java.util.List;

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
}
