package abstraction.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.TermCriteria;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.FeatureDetector;
import org.opencv.highgui.Highgui;
import org.opencv.core.CvType;


import com.google.gson.Gson;

import edu.wlu.cs.levy.CG.KDTree;
import edu.wlu.cs.levy.CG.KeyDuplicateException;
import edu.wlu.cs.levy.CG.KeySizeException;

import abstraction.prediction.DefinedTileView;
import abstraction.query.NewTileInterface;
import abstraction.tile.Column;
import abstraction.tile.ColumnBasedNiceTile;
import abstraction.util.NewTileKey;

public class Signatures {
	public static double[] globalmin = {0,0,-1,-1,-1,0};
	public static double[] globalmax = {44444,22222,1,1,1,7};
	public static int valcount = 6;
	public static int defaultindex = 2; // avg_ndsi;
	public static int defaultfilterindex = 5; // max_land_sea_mask
	public static final double [] defaultfiltervals = {1,7,0};
	public static double default_min = .00000000001;
	public static int defaultbins = 400;
	public static String defaultMetadataDir = "forecache_metadata/";
	
	public static String siftString ="sift";
	public static String denseSiftString = "densesift";

	/**************** Mean/Stddev ****************/
	public static double[] getNormalSignature(ColumnBasedNiceTile tile) {
		return getNormalSignature(tile,defaultindex);
	}
	
	public static double[] getNormalSignature(ColumnBasedNiceTile tile, int index) {
		Column col = tile.getColumn(index);
		int rows = col.getSize(); // total rows
		double min = globalmin[index];
		double max = globalmax[index];
		double range = max - min;
		double [] histogram = {0.0,0.0};
		double sum = 0;

		if((rows == 0) || (!col.isNumeric())) {
			return histogram;
		}
		
		for(int i = 0; i < rows; i++) {
			sum += (Double) tile.get(index, i);
		}
		histogram[0] = sum / rows;
		histogram[1] = 0;
		for(int i = 0; i < rows; i++) {
			double diff = ((Double) tile.get(index, i)) - histogram[0];
			histogram[1] += Math.pow(diff, 2);
		}
		histogram[1] = Math.sqrt(histogram[1]/rows);
		histogram[0] -= min;
		for(int i = 0; i < histogram.length; i++) {
			//if(histogram[i] > 2.0) {
			//	System.out.println("invalid values!" + histogram[i]);
			//	throw new Exception();
			//}
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
	
	/**************** Dense Sift ****************/
	public static class DenseSiftSignature implements Callable<double[]> {
		DefinedTileView dtv;
		NewTileKey id;
		ConcurrentKDTree<Integer> vocabulary;
		int vocabsize;
		
		public DenseSiftSignature(DefinedTileView dtv, NewTileKey id, ConcurrentKDTree<Integer> vocabulary, int vocabsize) {
			this.dtv = dtv;
			this.id = id;
			this.vocabulary = vocabulary;
			this.vocabsize = vocabsize;
		}
		
		@Override
		public double[] call() {
			ColumnBasedNiceTile tile = new ColumnBasedNiceTile(this.id);
			this.dtv.nti.getTile(dtv.v, dtv.ts, tile);
			return buildDenseSiftSignature(tile,this.vocabulary,this.vocabsize);
		}
	}
	
	public static List<double[]> buildDenseSiftSignaturesInParallel(DefinedTileView dtv,
			List<NewTileKey> ids, ConcurrentKDTree<Integer> vocabulary, int vocabsize) {
		ExecutorService executor = Executors.newFixedThreadPool(3);
		List<DenseSiftSignature> inputs = new ArrayList<DenseSiftSignature>();
		List<double[]> finalResults = new ArrayList<double[]>();
		for(NewTileKey id : ids) {
			inputs.add(new DenseSiftSignature(dtv,id,vocabulary,vocabsize));
		}
		List<Future<double[]>> results;
		try {
			results = executor.invokeAll(inputs);
	        executor.shutdown();
	        
	        for (Future<double[]> result : results) {
	            finalResults.add(result.get());
	        }
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return finalResults;
	}
	
	public static double[] buildDenseSiftSignature(ColumnBasedNiceTile tile, ConcurrentKDTree<Integer> vocabulary, int vocabsize) {
		Mat m = getDenseSiftDescriptorsForImage(tile);
		return buildSiftSignature(m,vocabulary,vocabsize);
	}
	
	public static double[] buildDenseSiftSignature(Mat tile, ConcurrentKDTree<Integer> vocabulary, int vocabsize) {
		return buildSiftSignature(tile,vocabulary,vocabsize);
	}
	
	// for general use
	public static Mat getDenseSiftDescriptorsForImage(ColumnBasedNiceTile tile) {
		Mat descriptors = readMat(denseSiftString,tile.id); // do we have the descriptors already?
		if(descriptors != null) {
			return descriptors;
		}
		MatOfKeyPoint keypoints = new MatOfKeyPoint();
		descriptors = new Mat(1,1,CvType.CV_32FC1);
		
		File t = new File(DrawHeatmap.buildFilename(tile.id));
		if(!(t.exists() && t.isFile())) {
			DrawHeatmap.buildImage(tile);
		}
		if(!t.exists()) return new Mat();
		Mat image = Highgui.imread(t.getPath(),Highgui.CV_LOAD_IMAGE_GRAYSCALE);

		FeatureDetector detector = FeatureDetector.create(FeatureDetector.DENSE); // detect dense keypoints
		DescriptorExtractor extractor = DescriptorExtractor.create(DescriptorExtractor.SIFT); // extract sift features
		detector.detect(image, keypoints);
		extractor.compute(image, keypoints, descriptors);
		
		//System.out.println("file: "+t);
		writeMat(denseSiftString,descriptors,tile.id); // save the descriptors we just computed for this tile
		return descriptors;
	}
	
	/**************** Sift ****************/
	public static class SiftSignature implements Callable<double[]> {
		DefinedTileView dtv;
		NewTileKey id;
		ConcurrentKDTree<Integer> vocabulary;
		int vocabsize;
		
		public SiftSignature(DefinedTileView dtv, NewTileKey id, ConcurrentKDTree<Integer> vocabulary, int vocabsize) {
			this.dtv = dtv;
			this.id = id;
			this.vocabulary = vocabulary;
			this.vocabsize = vocabsize;
		}
		
		@Override
		public double[] call() {
			ColumnBasedNiceTile tile = new ColumnBasedNiceTile(this.id);
			this.dtv.nti.getTile(dtv.v, dtv.ts, tile);
			return buildSiftSignature(tile,this.vocabulary,this.vocabsize);
		}
	}
	
	public static List<double[]> buildSiftSignaturesInParallel(DefinedTileView dtv, List<NewTileKey> ids, ConcurrentKDTree<Integer> vocabulary, int vocabsize) {
		ExecutorService executor = Executors.newFixedThreadPool(3);
		List<SiftSignature> inputs = new ArrayList<SiftSignature>();
		List<double[]> finalResults = new ArrayList<double[]>();
		for(NewTileKey id : ids) {
			inputs.add(new SiftSignature(dtv,id,vocabulary,vocabsize));
		}
		List<Future<double[]>> results;
		try {
			results = executor.invokeAll(inputs);
	        executor.shutdown();
	        
	        for (Future<double[]> result : results) {
	            finalResults.add(result.get());
	        }
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return finalResults;
	}
	
	public static double[] buildSiftSignature(ColumnBasedNiceTile tile, ConcurrentKDTree<Integer> vocabulary, int vocabsize) {
		Mat m = getSiftDescriptorsForImage(tile);
		return buildSiftSignature(m,vocabulary,vocabsize);
	}
	
	public static double[] buildSiftSignature(Mat tile, ConcurrentKDTree<Integer> vocabulary, int vocabsize) {
		long a = System.currentTimeMillis();
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
			//try {
				word = vocabulary.nearest(key);
			//} catch (KeySizeException e) {
				// TODO Auto-generated catch block
			//	e.printStackTrace();
			//}
			
			// increment count for this word
			if(word != null) {
				histogram[word]++;
			}
		}
		
		if(totaldescriptors > 0) {
			// normalize histogram
			for(int i = 0; i < vocabsize; i++) {
				histogram[i] /= totaldescriptors;
			}
		}
		
		long b = System.currentTimeMillis();
		//System.out.println("signature:"+(b-a));
		return histogram;
	}
	
	// for general use
	public static Mat getSiftDescriptorsForImage(ColumnBasedNiceTile tile) {
		Mat descriptors = readMat(siftString,tile.id); // do we have the descriptors already?
		if(descriptors != null) {
			return descriptors;
		}
		
		MatOfKeyPoint keypoints = new MatOfKeyPoint();
		descriptors = new Mat(1,1,CvType.CV_32FC1);
		
		File t = new File(DrawHeatmap.buildFilename(tile.id));
		if(!(t.exists() && t.isFile())) {
			DrawHeatmap.buildImage(tile);
		}
		if(!t.exists()) return new Mat();
		Mat image = Highgui.imread(t.getPath(),Highgui.CV_LOAD_IMAGE_GRAYSCALE);

		FeatureDetector detector = FeatureDetector.create(FeatureDetector.SIFT);
		DescriptorExtractor extractor = DescriptorExtractor.create(DescriptorExtractor.SIFT);
		detector.detect(image, keypoints);
		extractor.compute(image, keypoints, descriptors);
		
		//System.out.println("file: "+t);
		System.out.println(tile.id.buildTileStringForFile());
		writeMat(siftString,descriptors,tile.id); // save the descriptors we just computed for this tile
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
			distance += Math.pow(a[i] - b[i], 2) / Math.max(.00000001,(a[i] + b[i]));
		}
		return distance / 2;
	}
	
	public static double[] getHistogramSignature(ColumnBasedNiceTile tile) {

		return getHistogramSignature(tile,defaultindex,defaultbins);
	}
	
	public static double[] getHistogramSignature(ColumnBasedNiceTile tile, int index, int bins) {
		Column col = tile.getColumn(index);
		int rows = col.getSize(); // total rows
		double min = globalmin[index];
		double max = globalmax[index];
		double [] histogram = new double[bins];
		double binwidth = (max - min) / bins;
		for(int i = 0; i < bins; i++) {
			histogram[i] = 0.0;
		}
		if((rows == 0) || (!col.isNumeric())) {
			return histogram;
		}
		for(int i = 0; i < rows; i++) {
			double val = (Double) tile.get(index,i);
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
		return histogram;
	}
	
	/**************** filtered Histograms ****************/
	
	public static double[] getFilteredHistogramSignature(ColumnBasedNiceTile tile) {
		return getFilteredHistogramSignature(tile,defaultindex,defaultfilterindex,defaultfiltervals[0],defaultbins);
	}
	
	public static double[] getFilteredHistogramSignature(ColumnBasedNiceTile tile, int index, int filterindex, double filtervalue, int bins) {
		Column col = tile.getColumn(index);
		int rows = col.getSize(); // total rows
		double min = globalmin[index];
		double max = globalmax[index];
		double [] histogram = new double[bins];
		double binwidth = (max - min) / bins;
		for(int i = 0; i < rows; i++) {
			double val = (Double) tile.get(index,i);
			double filter = (Double) tile.get(filterindex,i);
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
		return histogram;
	}
	
	/******************general ********************/
	public static double[][] matToArray(Mat toConvert) {
		double[][] array = new double[toConvert.rows()][toConvert.cols()];
		if(array.length == 0 || array[0].length == 0) {
			System.out.println("array is empty");
			return null;
		}
		for(int i = 0; i < array.length; i++) {
			for(int j = 0; j < array[0].length; j++) {
				array[i][j] = toConvert.get(i,j)[0];
			}
		}
		return array;
	}
	
	public static Mat arrayToMat(double[][] toConvert) {
		int rows = toConvert.length;
		if(rows == 0 || toConvert[0].length == 0) {
			System.out.println("array is null");
			return null;
		}
		int cols = toConvert[0].length;
		Mat m = Mat.zeros(rows, cols,CvType.CV_32FC1);
		for(int i = 0; i < rows; i++) { // rows
			for(int j = 0; j < cols; j++) { // cols
				m.put(i, j, toConvert[i][j]);
			}
		}
		return m;
	}
	
	public static void writeMat(String sig, Mat toSave, NewTileKey id) {
		File directory = new File(defaultMetadataDir+sig+"/");
		directory.mkdirs();
		String filename = id.buildTileStringForFile();
		File file = new File(defaultMetadataDir+sig+"/"+filename);
		try {
			Gson gson = new Gson();
			double[][] array = matToArray(toSave);
			if(array == null) return;
			//System.out.println("writing array "+id.buildTileStringForFile()+": "+array.length+","+array[0].length);
			//System.out.println("original mat "+id.buildTileStringForFile()+": "+toSave.rows()+","+toSave.cols());
			//System.out.println();
			String content = gson.toJson(array);
			BufferedWriter output = new BufferedWriter(new FileWriter(file));
	        output.write(content);
	        output.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static Mat readMat(String sig, NewTileKey id) {
		Mat returnval = null;
		String filename = id.buildTileStringForFile();
		File file = new File(defaultMetadataDir+sig+"/"+filename);
		if(!(file.exists() && file.isFile())) return returnval;
		StringBuilder fileContents = new StringBuilder((int)file.length());
	    Scanner scanner = null;
		try {
			scanner = new Scanner(file);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    String lineSeparator = System.getProperty("line.separator");
	    if(scanner != null) {
		    try {
		        while(scanner.hasNextLine()) {        
		            fileContents.append(scanner.nextLine() + lineSeparator);
		        }
		        
		    } finally {
		        scanner.close();
		    }
		    String contents = fileContents.toString();
		    Gson gson = new Gson();
		    double[][] array = gson.fromJson(contents, double[][].class);
		    if(array == null) {
				System.out.println("array is null for tile"+id.buildTileStringForFile());
		    }
		    //System.out.println("contents: "+contents);
		    returnval = arrayToMat(array);
	    }
	    return returnval;
	}
	
	public static class ConcurrentKDTree<T> {
		protected KDTree<T> tree;
		
		public ConcurrentKDTree(int cols) {
			tree = new KDTree<T>(cols);
		}
		
		public synchronized void insert(double[] key, T value) {
			try {
				this.tree.insert(key, value);
			} catch (KeySizeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (KeyDuplicateException e) {
				// TODO Auto-generated catch block
				System.out.println("found duplicate key!");
				e.printStackTrace();
			}
		}
		
		public synchronized T nearest(double[] key) {
			T returnval = null;
			try {
				returnval = tree.nearest(key);
			} catch (KeySizeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return returnval;
		}
		
	}
	
	// use kd-tree to make nearest neighbor fast
	public static ConcurrentKDTree<Integer> buildConcurrentKDTree(Mat keys) {
		int rows = keys.rows();
		int cols = keys.cols();
		ConcurrentKDTree<Integer> tree = new ConcurrentKDTree<Integer>(cols);
		for(int i = 0; i < rows; i++) {
			double[] key = new double[cols];
			for(int j = 0; j < cols; j++) {
				key[j] = keys.get(i, j)[0];
			}
			//try {
				tree.insert(key, i); // which row was this?
			//} catch (KeySizeException e) {
				// TODO Auto-generated catch block
			//	e.printStackTrace();
			//} catch (KeyDuplicateException e) {
				// TODO Auto-generated catch block
			//	System.out.println("found duplicate key!");
			//	e.printStackTrace();
			//}
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
		TermCriteria criteria = new TermCriteria(TermCriteria.COUNT,200,1);
		Mat centers = new Mat();
		int attempts = 1;
		if(observations.rows() < totalClusters) {
			//System.out.println("total observations: "+observations.rows());
			totalClusters = observations.rows();
		}
			Core.kmeans(observations,totalClusters,labels,criteria,attempts, Core.KMEANS_RANDOM_CENTERS, centers);
		return centers;
	}
}
