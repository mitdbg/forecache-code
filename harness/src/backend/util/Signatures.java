package backend.util;

import java.nio.ByteBuffer;

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
	
	/**************** Histograms ****************/
	public static double getHistogramDistance(double[] a, double[] b) {
		double distance = 0;
		for(int i = 0; i < a.length; i++) {
			distance += Math.pow(b[i] - a[i], 2);
		}
		return Math.sqrt(distance);
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
}
