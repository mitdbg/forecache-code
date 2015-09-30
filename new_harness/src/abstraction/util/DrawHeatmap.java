package abstraction.util;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;

import org.imgscalr.Scalr;

import abstraction.tile.ColumnBasedNiceTile;
import abstraction.util.ColorBrewer;
import abstraction.util.DBInterface;
//import abstraction.util.UtilityFunctions;
//import backend.disk.OldScidbTileInterface;

public class DrawHeatmap {
	
	// default palate length is 9 colors
	public static ColorBrewer defaultColors = ColorBrewer.Spectral;
	public static int defaultPalateLength = 9;
	public static int defaultWidth = 300;
	public static int defaultHeight = 150;
	public static String imageDir = "images/";
	public static String imageFolder = imageDir+DBInterface.arrayname+"/";
	
	public static void buildImage(ColumnBasedNiceTile tile) {
		buildImage(tile,DBInterface.xdim,DBInterface.ydim,DBInterface.zattr);
	}

	// snow cover data: x=0, y=1, z=2
	public static void buildImage(ColumnBasedNiceTile tile, String x, String y, String z) {
		if(tile.getSize() == 0) { // no data!
			return;
		}
		int xi = tile.getIndex(x);
		int yi = tile.getIndex(y);
		int zi = tile.getIndex(z);
		BufferedImage bi = getBufferedImage(tile,xi,yi);
		Graphics2D ig2 = bi.createGraphics();
		drawHeatMap(tile,xi,yi,zi,ig2);
		BufferedImage toSave = Scalr.resize(bi, defaultWidth); // resize
		saveImageAsPng(toSave,buildFilename(tile.id)); // save
	}
	
	public static String buildFilename(NewTileKey id) {
		return imageFolder+id.buildTileStringForFile()+".png";
	}
	
	public static BufferedImage getBufferedImage(ColumnBasedNiceTile tile, int x, int y) {
		//System.out.println(x+","+y);
		//System.out.println(tile.extrema.length);
		double[] xrange = getRange(tile,x);
		double[] yrange = getRange(tile,y);
		//System.out.println(xrange[1]+","+xrange[0]+","+yrange[1]+","+yrange[0]);
		int w = (int) (xrange[1] - xrange[0]);
		int h = (int) (yrange[1] - yrange[0]);
		BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		return bi;
	}
	
	public static void drawHeatMap(ColumnBasedNiceTile tile, int x, int y, int z, Graphics g) {
		Color[] palate = defaultColors.getColorPalette(defaultPalateLength);
		double[] xrange = getRange(tile,x);
		double[] yrange = getRange(tile,y);
		double[] zrange = getRange(tile,z);
		
		int w = 1;
		int h = 1;
		//System.out.println(xrange[1]+","+xrange[0]+","+yrange[1]+","+yrange[0]);
		for(int i = 0; i < tile.getSize(); i++) {
			int xpixel = (Integer) tile.get(x,i);
			int ypixel = (Integer) tile.get(y,i);
			double zval = (Double) tile.get(z,i);
			Color palateIndex = getPalateValue(zval,zrange[1],zrange[0],palate);
			g.setColor(palateIndex);
			g.fillRect(xpixel, ypixel, w, h);
			//System.out.println(xpixel +","+ypixel+","+w+","+h+","+palateIndex);
		}
	}
	
	public static int getPixelValue(Double val, Double max, Double min, Double width) {
		return (int) Math.floor((val - min) * width);
	}
	
	public static Color getPalateValue(Double val, Double max, Double min, Color[] palate) {
		double width = max - min;
		if(width < 0) width *= -1.0;
		width /= palate.length;
		int palateIndex = (int) Math.floor((val - min) / width);
		if(palateIndex == palate.length) palateIndex--;
		return palate[palateIndex];
	}
	
	public static void saveImageAsPng(BufferedImage img, String name) {
		try {
			File directory = new File(imageFolder);
			directory.mkdirs(); // in case it doesn't exist
			ImageIO.write(img, "PNG", new File(name));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("could not save image file to disk");
			e.printStackTrace();
		}
	}
	
	// transforms column domain into a double range
	protected static double[] getRange(ColumnBasedNiceTile tile,int index) {
		List<?> domain = tile.getDomain(index);
		double[] range = new double[2];
		range[0] = (Double) domain.get(0);
		range[1] = (Double) domain.get(1);
		return range;
	}
	
	public static void main(String[] args) {
		// get a nice tile
		/*
		Params p = new Params();
		p.xmin = 0;
		p.ymin = 0;
		p.xmax = 3600;
		p.ymax = 1800;//1697;
		p.width = 9;
		OldScidbTileInterface sti = new OldScidbTileInterface(DBInterface.defaultparamsfile,DBInterface.defaultdelim);
		String idstr = "[1, 3]";
		int zoom = 4;
		int[] tile_id = UtilityFunctions.parseTileIdInteger(idstr);
		NewTileKey id = new NewTileKey(tile_id,zoom);
		ColumnBasedNiceTile tile = sti.getNiceTile(id);
		
		buildImage(tile,"x","y","avg_ndsi");
		*/
	}
}