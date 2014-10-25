package backend.util;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.TermCriteria;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.FeatureDetector;
import org.opencv.highgui.Highgui;

import edu.wlu.cs.levy.CG.KDTree;
import edu.wlu.cs.levy.CG.KeyDuplicateException;
import edu.wlu.cs.levy.CG.KeySizeException;

import backend.MainThread;
import backend.disk.ScidbTileInterface;

public class Signatures {
	public static double[] globalmin = {0,0,-1,-1,-1,0};
	public static double[] globalmax = {44444,22222,1,1,1,7};
	public static int valcount = 6;
	public static int defaultindex = 2; // avg_ndsi;
	public static int defaultfilterindex = 5; // max_land_sea_mask
	public static final double [] defaultfiltervals = {1,7,0};
	public static double default_min = .00000000001;
	public static int defaultbins = 400;

	/**************** Mean/Stddev ****************/
	public static double[] getNormalSignature(byte[] input) throws Exception {
		return getNormalSignature(input,defaultindex);
	}
	
	public static double[] getNormalSignature(byte[]input, int index) throws Exception {
		long start = System.currentTimeMillis();
		double [] x = getData(input);
		int rows = x.length / valcount; // total rows
		double min = globalmin[index];
		double max = globalmax[index];
		double range = max - min;
		double [] histogram = {0.0,0.0};
		double sum = 0;

		if(x.length > 0) {
			//System.out.println("val: "+x[index]);
		} else {
			return histogram;
		}
		for(int i = 0; i < rows; i++) {
			sum += x[i*valcount+index];
		}
		histogram[0] = sum / rows;
		histogram[1] = 0;
		for(int i = 0; i < rows; i++) {
			double diff = x[i*valcount+index] - histogram[0];
			histogram[1] += Math.pow(diff, 2);
		}
		histogram[1] = Math.sqrt(histogram[1]/rows);
		histogram[0] -= min;
		for(int i = 0; i < histogram.length; i++) {
			if(histogram[i] > 2.0) {
				System.out.println("invalid values!" + histogram[i]);
				throw new Exception();
			}
			histogram[i] /= range; // normalize by range
			if(histogram[i] < default_min) {
				histogram[i] = default_min;
			}
			//System.out.print(histogram[i]+",");
		}
		//System.out.println();
		long end = System.currentTimeMillis();
		//System.out.println("Time to build normal dist: "+(end-start)+"ms");
		return histogram;
	}
	
	
	/**************** Sift ****************/
	public static double[] buildSiftSignature(TileKey id, KDTree<Integer> vocabulary, int vocabsize) {
		Mat tile = getSiftDescriptorsForImage(id);
		return buildSiftSignature(tile,vocabulary,vocabsize);
	}
	
	public static double[] buildSiftSignature(Mat tile, KDTree<Integer> vocabulary, int vocabsize) {
		double[] histogram = new double[vocabsize];
		int totaldescriptors = tile.rows();
		for(int i = 0; i < totaldescriptors; i++) { // each row is a descriptor
			// build key
			int width = tile.cols();
			double[] key = new double[width];
			for(int k = 0; k < width; k++) {
				key[k] = tile.get(i,k)[0];
			}

			// find nearest cluster center to this key
			Integer word = null;
			try {
				word = vocabulary.nearest(key);
			} catch (KeySizeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			// increment count for this word
			if(word != null) {
				histogram[word]++;
			}
		}
		
		// normalize histogram
		for(int i = 0; i < vocabsize; i++) {
			histogram[i] /= totaldescriptors;
		}
		
		return histogram;
	}
	
	// for use with main thread
	public static Mat getSiftDescriptorsForImage(TileKey id) {
		return getSiftDescriptorsForImage(id,MainThread.scidbapi);
	}
	
	// for general use
	public static Mat getSiftDescriptorsForImage(TileKey id, ScidbTileInterface scidbapi) {
		NiceTile tile = scidbapi.getNiceTile(id);
		MatOfKeyPoint keypoints = new MatOfKeyPoint();
		Mat descriptors = new Mat();
		
		File t = new File(DrawHeatmap.buildFilename(tile));
		if(!(t.exists() && t.isFile())) {
			DrawHeatmap.buildImage(tile);
		}
		Mat image = Highgui.imread(t.getPath(),Highgui.CV_LOAD_IMAGE_GRAYSCALE);

		FeatureDetector detector = FeatureDetector.create(FeatureDetector.SIFT);
		DescriptorExtractor extractor = DescriptorExtractor.create(DescriptorExtractor.SIFT);
		detector.detect(image, keypoints);
		extractor.compute(image, keypoints, descriptors);
		
		System.out.println("file: "+t);
		return descriptors;
	}
	
	/**************** Histograms ****************/
	public static double getHistogramDistance(double[] a, double[] b) {
		double distance = 0;
		for(int i = 0; i < a.length; i++) {
			distance += Math.pow(b[i] - a[i], 2);
		}
		return Math.sqrt(distance);
	}
	
	public static double chiSquaredDistance(double[] a, double[] b) {
		double distance = 0;
		for(int i = 0; i < a.length; i++) {
			distance += Math.pow(a[i] - b[i], 2) / (a[i] + b[i]);
		}
		return distance / 2;
	}
	
	public static double[] getHistogramSignature(byte[] input) {
		return getHistogramSignature(input,defaultindex,defaultbins);
	}
	
	public static double[] getHistogramSignature(byte[] input, int index, int bins) {
		long start = System.currentTimeMillis();
		double [] x = getData(input);
		int rows = x.length / valcount; // total rows
		double min = globalmin[index];
		double max = globalmax[index];
		double [] histogram = new double[bins];
		double binwidth = (max - min) / bins;
		for(int i = 0; i < bins; i++) {
			histogram[i] = 0.0;
		}
		if(x.length > 0) {
			//System.out.println("val: "+x[index]);
		} else {
			return histogram;
		}
		for(int i = 0; i < rows; i++) {
			double val = x[i*valcount+index];
			histogram[(int)((val - min) / binwidth)]++;
		}
		for(int i = 0; i < histogram.length; i++) {
			histogram[i] /= rows; // normalize by total rows
			if(histogram[i] < default_min) {
				histogram[i] = default_min;
			}
			//System.out.print(histogram[i]+",");
		}
		//System.out.println();
		long end = System.currentTimeMillis();
		//System.out.println("Time to build histogram: "+(end-start)+"ms");
		return histogram;
	}
	
	/**************** filtered Histograms ****************/
	
	public static double[] getFilteredHistogramSignature(byte[] input) {
		return getFilteredHistogramSignature(input,defaultindex,defaultfilterindex,defaultfiltervals[0],defaultbins);
	}
	
	public static double[] getFilteredHistogramSignature(byte[] input, int index, int filterindex, double filtervalue, int bins) {
		long start = System.currentTimeMillis();
		double [] x = getData(input);
		int rows = x.length / valcount; // total rows
		double min = globalmin[index];
		double max = globalmax[index];
		double [] histogram = new double[bins];
		double binwidth = (max - min) / bins;
		for(int i = 0; i < rows; i++) {
			double val = x[i*valcount+index];
			double filter = x[i*valcount + filterindex];
			if(filter == filtervalue) {
				histogram[(int)((val - min) / binwidth)]++;
			}
		}
		for(int i = 0; i < histogram.length; i++) {
			histogram[i] /= rows; // normalize by relevant rows
			if(histogram[i] < default_min) {
				histogram[i] = default_min;
			}
			//System.out.print(histogram[i]+",");
		}
		//System.out.println();
		long end = System.currentTimeMillis();
		//System.out.println("Time to build filtered histogram: "+(end-start)+"ms");
		return histogram;
	}
	
	/******************general ********************/
	// use kd-tree to make nearest neighbor fast
	public static KDTree<Integer> buildKDTree(Mat keys) {
		int rows = keys.rows();
		int cols = keys.cols();
		KDTree<Integer> tree = new KDTree<Integer>(cols);
		for(int i = 0; i < rows; i++) {
			double[] key = new double[cols];
			for(int j = 0; j < cols; j++) {
				key[j] = keys.get(i, j)[0];
			}
			try {
				tree.insert(key, i); // which row was this?
			} catch (KeySizeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (KeyDuplicateException e) {
				// TODO Auto-generated catch block
				System.out.println("found duplicate key!");
				e.printStackTrace();
			}
		}
		return tree;
	}
	
	public static double[] getData(byte[] input) {
		long start = System.currentTimeMillis();
		ByteBuffer buffer = ByteBuffer.wrap(input);
		int count = input.length / 8;
		double[] result = new double[count];
		for(int i = 0; i < count; i++) {
			result[i] = buffer.getDouble(i*8);
		}
		long end = System.currentTimeMillis();
		//System.out.println("Time to convert from bytes to doubles: "+(end-start)+"ms");
		return result;
	}
	
	public static Mat getKmeansCenters(Mat observations, int totalClusters) {
		Mat labels = new Mat();
		TermCriteria criteria = new TermCriteria(TermCriteria.COUNT,100,1);
		Mat centers = new Mat();
		int attempts = 1;
		Core.kmeans(observations,totalClusters,labels,criteria,attempts, Core.KMEANS_RANDOM_CENTERS, centers);
		return centers;
	}
}
