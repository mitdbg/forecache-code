package backend.util;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;

import org.imgscalr.Scalr;

import com.google.gson.Gson;

import utils.ColorBrewer;
import utils.DBInterface;
import utils.UtilityFunctions;

import backend.disk.ScidbTileInterface;
import backend.util.NiceTile;

public class DrawHeatmap {
	
	// default palate length is 9 colors
	public static ColorBrewer defaultColors = ColorBrewer.Spectral;
	public static int defaultPalateLength = 9;
	public static int defaultWidth = 300;
	public static int defaultHeight = 150;
	public static String imageDir = "images/";
	public static String imageFolder = imageDir+DBInterface.arrayname+"/";
	
	public static void buildImage(NiceTile tile) {
		buildImage(tile,DBInterface.xdim,DBInterface.ydim,DBInterface.zattr);
	}
	
	public static void buildImage(NiceTile tile, String x, String y, String z) {
		BufferedImage bi = getBufferedImage(tile,x,y,z);
		Graphics2D ig2 = bi.createGraphics();
		drawHeatMap(tile,x,y,z,ig2);
		BufferedImage toSave = Scalr.resize(bi, defaultWidth); // resize
		saveImageAsPng(toSave,buildFilename(tile.id)); // save
	}
	
	public static String buildFilename(TileKey id) {
		return imageFolder+id.buildTileStringForFile()+".png";
	}
	
	public static BufferedImage getBufferedImage(NiceTile tile, String x, String y, String z) {
		NiceTile.MinMax xrange = tile.extrema.get(tile.getIndex(x));
		NiceTile.MinMax yrange = tile.extrema.get(tile.getIndex(y));
		int w = (int) (xrange.max - xrange.min);
		int h = (int) (yrange.max - yrange.min);
		BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		return bi;
	}
	
	public static void drawHeatMap(NiceTile tile, String x, String y, String z, Graphics g) {
		Color[] palate = defaultColors.getColorPalette(defaultPalateLength);
		List<Double> xcol = tile.getColumn(x);
		List<Double> ycol = tile.getColumn(y);
		List<Double> zcol = tile.getColumn(z);
		NiceTile.MinMax xrange = tile.extrema.get(tile.getIndex(x));
		NiceTile.MinMax yrange = tile.extrema.get(tile.getIndex(y));
		NiceTile.MinMax zrange = tile.extrema.get(tile.getIndex(z));
		
		int w = 1;
		int h = 1;
		System.out.println(xrange.max+","+xrange.min+","+yrange.max+","+yrange.min);
		for(int i = 0; i < xcol.size(); i++) {
			if(xcol.get(i) != null && ycol.get(i) != null && zcol.get(i) != null) {
				int xpixel = (int) xcol.get(i).doubleValue();
				int ypixel = (int) ycol.get(i).doubleValue();
				Color palateIndex = getPalateValue(zcol.get(i),zrange.max,zrange.min,palate);
				g.setColor(palateIndex);
				g.fillRect(xpixel, ypixel, w, h);
				//System.out.println(xpixel +","+ypixel+","+w+","+h+","+palateIndex);
			}
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
			File directory = new File(imageDir);
			directory.mkdirs(); // in case it doesn't exist
			ImageIO.write(img, "PNG", new File(name));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("could not save image file to disk");
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		// get a nice tile
		Params p = new Params();
		p.xmin = 0;
		p.ymin = 0;
		p.xmax = 3600;
		p.ymax = 1800;//1697;
		p.width = 9;
		ScidbTileInterface sti = new ScidbTileInterface(DBInterface.defaultparamsfile,DBInterface.defaultdelim);
		String idstr = "[1, 3]";
		int zoom = 4;
		List<Integer> tile_id = UtilityFunctions.parseTileIdInteger(idstr);
		TileKey id = new TileKey(tile_id,zoom);
		NiceTile tile = sti.getNiceTile(id);
		
		buildImage(tile,"x","y","avg_ndsi");
	}
}