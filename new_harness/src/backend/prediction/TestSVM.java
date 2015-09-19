package backend.prediction;

import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;
import libsvm.svm_parameter;
import libsvm.svm_problem;
import backend.util.Direction;
import backend.util.DirectionClass;
import backend.util.History;
import backend.util.ModelAccuracy;
import backend.util.TileKey;
import utils.DBInterface;
import utils.ExplorationPhase;
import utils.TraceMetadata;
import utils.UserRequest;
import utils.UtilityFunctions;

public class TestSVM {

	public static class SvmWrapper {
		public SvmWrapper() {}
		public svm_model model;
		public svm_problem prob;
		public svm_parameter params;
		double[] sum;
		double[] diffs;
		Map<String,Integer> LabelsToInts;
		List<String> IntsToLabels;
		
		public double[] getFeatures(List<TileKey> history) {
			if(history.size() == 0) return new double[]{0,0,0,0,0,0};
			TileKey last = history.get(history.size()-1);
			int x = last.id[0];
			int y = last.id[1];
			int zoom = last.zoom;
			int incount = 0;
			int outcount = 0;
			int pancount = 0;
			if(history.size() > 1) {
				int prevz = history.get(0).zoom;
				for(int i = 1; i < history.size(); i++) {
					int currz = history.get(i).zoom;
					int zdiff = currz-prevz;
					if(zdiff > 0) { // zoom in
						incount++;
					} else if (zdiff < 0) { // zoom out
						outcount++;
					} else {
						pancount++;
					}
					prevz=currz;
				}
			}
			return new double[]{incount, outcount, pancount, zoom, x, y};
		}
		
		// get the label name for the predicted class
		public String predictLabel(List<TileKey> history) {
			return IntsToLabels.get(predict(history));
		}
		
		// get the ID of the predicted class
		public int predict(List<TileKey> history) {
			double[] features = getFeatures(history);
			int[] labels = new int[model.nr_class];
			svm.svm_get_labels(model, labels);
			svm_node[] nodes = new svm_node[features.length];
			for(int j = 0; j < features.length; j++) {
				nodes[j] = new svm_node();
				nodes[j].index = j+1;
				// normalize using the same mean/stddev from the training data
				nodes[j].value = (features[j] - sum[j]) / diffs[j];
			}
			
			int v = (int) svm.svm_predict(model, nodes);
			return labels[v];
		}
	}
	
	public static SvmWrapper buildSvmPhaseClassifier() {
		boolean header = true;
		int cmax = 1;
		String splitby = "\t";
		
		int[] hlens = {1};
		int[] users = {146, 150, 123, 145, 140, 132, 141, 151, 144, 148, 121, 130, 124, 135, 134, 137, 139, 138};
		int oldphase = 0;
		int direction = 1;
		int userid = 2;
		int taskname = 3;
		int x = 4;
		int y = 5;
		int zoom = 6;
		int phase = 7;
		int next_phase = 8;
		int added = 9;
		int deleted = 10;

		Map<String,Integer> LabelsToInts = new HashMap<String,Integer>();
		List<String> IntsToLabels = new ArrayList<String>();
		
		List<double[]> X = new ArrayList<double[]>();
		List<Integer> labels = new ArrayList<Integer>();
		
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(DBInterface.groundTruth));
			int user = -1;
			if(header) br.readLine(); //get rid of header
			String line = br.readLine();

			int prevzoom = -1;
			List<DirectionClass> hist = new ArrayList<DirectionClass>();

			int incount = 0;
			int outcount = 0;
			int pancount = 0;

			while(line!=null){
				String[] b = line.split(splitby);
				if(b[deleted].equals("1")) {
					line = br.readLine();
					continue;
				}

				int curruser = Integer.parseInt(b[userid]);
				String currtask = b[taskname];
				int currzoom = Integer.parseInt(b[zoom]);
				int currx = Integer.parseInt(b[x]);
				int curry = Integer.parseInt(b[y]);
				String currphase = b[phase];
				String currdir = b[direction];

				if(user < 0 || curruser != user) {
					user = curruser;
					hist.clear();
					incount = 0;
					outcount = 0;
					pancount = 0;
					prevzoom = -1;
				}

				if(prevzoom > -1) {
					//System.out.println("here");
					int zoomdiff = currzoom - prevzoom;
					//System.out.println("zoomdiff: "+zoomdiff);
					if(zoomdiff == 0) { // pan
						hist.add(DirectionClass.PAN);
						pancount++;
					} else if (zoomdiff > 0) { // currzoom > prevzoom -> zoom in
						hist.add(DirectionClass.IN);
						incount++;
					} else {
						hist.add(DirectionClass.OUT);
						outcount++;
					}

					if(hist.size() > cmax) {
						//System.out.println("here");
						DirectionClass toRemove = hist.remove(0);
						if(toRemove == DirectionClass.IN) {
							incount--;
						} else if (toRemove == DirectionClass.OUT) {
							outcount--;
						} else {
							pancount--;
						}
					}
				}

				prevzoom = currzoom;

				double[] inp = {incount, outcount, pancount, currzoom, currx, curry};
				String label = currphase;
				Integer yval = LabelsToInts.get(label);
				if(yval == null) {
					yval = LabelsToInts.size();
					LabelsToInts.put(label, yval);
					IntsToLabels.add(label);
				}

				X.add(inp);
				labels.add(yval);

				//System.out.print(curruser+"\t"+currtask+"\t"+currx+"\t"+curry+"\t"+currzoom+"\t"+prev+"\t"+currdir);
				line = br.readLine();
			}
			br.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		SvmWrapper model = buildModel(X,labels);
		model.LabelsToInts = LabelsToInts;
		model.IntsToLabels = IntsToLabels;
		
		return model;
	}
	
	public static SvmWrapper buildModel(List<double[]> X, List<Integer> labels) {
		SvmWrapper model = new SvmWrapper();
		
		// build the SVM classifier
		model.prob = new svm_problem();
		model.prob.y = new double[labels.size()];
		model.prob.x = new svm_node[labels.size()][];
		model.prob.l = labels.size();
		model.sum = new double[X.get(0).length];
		model.diffs = new double[model.sum.length];
		for(int i = 0; i < labels.size(); i++) {
			model.prob.y[i] = labels.get(i);
			double[] inp = X.get(i);
			model.prob.x[i] = new svm_node[inp.length];
			for(int j = 0; j < inp.length; j++) {
				model.sum[j] += inp[j];
				model.prob.x[i][j] = new svm_node();
				model.prob.x[i][j].index = j+1;
				model.prob.x[i][j].value = inp[j];
			}
		}
		// compute the average
		for(int j = 0; j < model.sum.length; j++) {
			model.sum[j] /= labels.size();
		}
		// compute standard deviation
		for(int i = 0; i < labels.size(); i++) {
			for(int j = 0; j < model.diffs.length; j++) {
				model.diffs[j] += Math.pow(model.prob.x[i][j].value-model.sum[j], 2);
			}
		}
		for(int j = 0; j < model.sum.length; j++) {
			model.diffs[j] = Math.sqrt(model.diffs[j] / labels.size());
		}

		// normalize data
		for(int i = 0; i < labels.size(); i++) {
			for(int j = 0; j < model.diffs.length; j++) {
				model.prob.x[i][j].value = (model.prob.x[i][j].value - model.sum[j]) / model.diffs[j];
			}
		}

		model.params = new svm_parameter();
		model.params.C = 1.0;
		model.params.cache_size = 200;
		model.params.coef0 = 0.0;
		model.params.degree = 3;
		model.params.gamma = 0.07;
		model.params.shrinking = 1;
		model.params.probability = 0;
		model.params.kernel_type = svm_parameter.RBF;
		model.params.svm_type = svm_parameter.C_SVC;
		model.params.eps = 0.001;

		// train the model
		model.model = svm.svm_train(model.prob, model.params);
		
		return model;
	}
	
	public static void runSvm3(String input, boolean header, int cmax) {
		int[] hlens = {1};
		int[] users = {146, 150, 123, 145, 140, 132, 141, 151, 144, 148, 121, 130, 124, 135, 134, 137, 139, 138};
		int oldphase = 0;
		int direction = 1;
		int userid = 2;
		int taskname = 3;
		int x = 4;
		int y = 5;
		int zoom = 6;
		int phase = 7;
		int next_phase = 8;
		int added = 9;
		int deleted = 10;

		Map<String,Integer> LabelsToInts = new HashMap<String,Integer>();
		List<String> IntsToLabels = new ArrayList<String>();
		List<String> output = new ArrayList<String>();
		
		for(int hl = 0; hl < hlens.length; hl++) {
			int hlen = hlens[hl];
			double overall_accuracy = 0;
			double total = 0;
			for(int u = 0; u < users.length; u++) {
				int uid = users[u];
				List<double[]> X = new ArrayList<double[]>();
				List<Integer> labels = new ArrayList<Integer>();
				List<double[]> TestX = new ArrayList<double[]>();
				List<Integer> TestLabels = new ArrayList<Integer>();

				String splitBy = "\t";
				BufferedReader br;
				try {
					br = new BufferedReader(new FileReader(input));
					String user = null;
					if(header) br.readLine(); //get rid of header
					String line = br.readLine();
					
					int prevzoom = -1;
					String prev = null;
					
					int[] in = new int[cmax]; // 0 = prev, 1 = prev-prev
					int[] out = new int[cmax];
					int[] pan = new int[cmax];
					
					while(line!=null){
						String[] b = line.split(splitBy);
						if(b[deleted].equals("1")) {
							line = br.readLine();
							continue;
						}
						
						String ustring = b[userid];
						String currtask = b[taskname];
						int currzoom = Integer.parseInt(b[zoom]);
						int currx = Integer.parseInt(b[x]);
						int curry = Integer.parseInt(b[y]);
						String currphase = b[phase];
						String currdir = b[direction];
						
						if(user == null || !ustring.equals(user)) {
							user = ustring;
							prevzoom = -1;
							for(int i = 0; i < in.length; i++) {
								in[i] = out[i] = pan[i] = 0;
							}
						}
						for(int i = cmax-1; i > 0; i--) {
							in[i] = in[i-1];
							out[i] = out[i-1];
							pan[i] = pan[i-1];
						}
						
						if(prevzoom > -1) {
							int zoomdiff = currzoom - prevzoom;
							in[0] = out[0] = pan[0] = 0;
							//System.out.println("zoomdiff: "+zoomdiff);
							if(zoomdiff == 0) { // pan
								pan[0] = 1;
							} else if (zoomdiff > 0) { // currzoom > prevzoom -> zoom in
								in[0] = 1;
							} else {
								out[0] = 1;
							}
						}
						
						prevzoom = currzoom;
						
						double[] inp = new double[in.length+out.length+pan.length+3];
						for(int i = 0; i < in.length; i++) {
							System.out.print("\t"+in[i]);
							inp[i] = in[i];
						}
						
						for(int i = 0; i < out.length; i++) {
							System.out.print("\t"+out[i]);
							inp[in.length+i] = out[i];
						}
						for(int i = 0; i < pan.length; i++) {
							System.out.print("\t"+pan[i]);
							inp[in.length+out.length+i] = pan[i];
						}
						System.out.println();
						inp[inp.length-3] = currzoom;
						inp[inp.length-2] = currx;
						inp[inp.length-1] = curry;
						
						String label = currphase;
						Integer yval = LabelsToInts.get(label);
						if(yval == null) {
							yval = LabelsToInts.size();
							LabelsToInts.put(label, yval);
							IntsToLabels.add(label);
						}
						if(Integer.parseInt(ustring) == uid) { // test data
							TestX.add(inp);
							TestLabels.add(yval);
						} else { // training data
							X.add(inp);
							labels.add(yval);
						}

						//System.out.print(curruser+"\t"+currtask+"\t"+currx+"\t"+curry+"\t"+currzoom+"\t"+prev+"\t"+currdir);
						line = br.readLine();
					}
					br.close();
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				// build the SVM classifier
				SvmWrapper wrapped = buildModel(X,labels);
				
				//Test the model
				double accuracy = 0;
				for(int i = 0; i < TestX.size(); i++) {
					double[] inp = TestX.get(i);

					svm_node[] nodes = new svm_node[inp.length];
					for(int j = 0; j < inp.length; j++) {
						nodes[j] = new svm_node();
						nodes[j].index = j+1;
						// normalize using the same mean/stddev from the training data
						nodes[j].value = (inp[j] - wrapped.sum[j]) / wrapped.diffs[j];
					}
					int[] class_labels = new int[wrapped.model.nr_class];
					svm.svm_get_labels(wrapped.model, class_labels);
					int v = (int) svm.svm_predict(wrapped.model, nodes);
					//String pred = IntsToLabels.get(class_labels[v]);
					//System.out.println("labels: "+class_labels[0]+","+class_labels[1]+","+class_labels[2]);
					if(class_labels[v] == TestLabels.get(i)) {
						//System.out.println("hit at row "+i);
						accuracy++;
						overall_accuracy++;
					}
					total++;
				}
				accuracy /= TestX.size();
				output.add("accuracy for user "+uid+": "+accuracy);
			}
			overall_accuracy /= total;
			for(String line : output) {
				System.out.println(line);
			}
			System.out.println("overall accuracy for hlen "+hlen+": "+overall_accuracy);
		}
	}
	
	public static void runSvm2(String input, boolean header, int cmax) {
		int[] hlens = {1};
		int[] users = {146, 150, 123, 145, 140, 132, 141, 151, 144, 148, 121, 130, 124, 135, 134, 137, 139, 138};
		int oldphase = 0;
		int direction = 1;
		int userid = 2;
		int taskname = 3;
		int x = 4;
		int y = 5;
		int zoom = 6;
		int phase = 7;
		int next_phase = 8;
		int added = 9;
		int deleted = 10;

		Map<String,Integer> LabelsToInts = new HashMap<String,Integer>();
		List<String> IntsToLabels = new ArrayList<String>();
		List<String> output = new ArrayList<String>();
		
		for(int hl = 0; hl < hlens.length; hl++) {
			int hlen = hlens[hl];
			double overall_accuracy = 0;
			double total = 0;
			for(int u = 0; u < users.length; u++) {
				int uid = users[u];
				List<double[]> X = new ArrayList<double[]>();
				List<Integer> labels = new ArrayList<Integer>();
				List<double[]> TestX = new ArrayList<double[]>();
				List<Integer> TestLabels = new ArrayList<Integer>();

				String splitBy = "\t";
				BufferedReader br;
				try {
					br = new BufferedReader(new FileReader(input));
					int user = -1;
					if(header) br.readLine(); //get rid of header
					String line = br.readLine();

					int prevzoom = -1;
					List<DirectionClass> hist = new ArrayList<DirectionClass>();

					int incount = 0;
					int outcount = 0;
					int pancount = 0;

					while(line!=null){
						String[] b = line.split(splitBy);
						if(b[deleted].equals("1")) {
							line = br.readLine();
							continue;
						}

						int curruser = Integer.parseInt(b[userid]);
						String currtask = b[taskname];
						int currzoom = Integer.parseInt(b[zoom]);
						int currx = Integer.parseInt(b[x]);
						int curry = Integer.parseInt(b[y]);
						String currphase = b[phase];
						String currdir = b[direction];

						if(user < 0 || curruser != user) {
							user = curruser;
							hist.clear();
							incount = 0;
							outcount = 0;
							pancount = 0;
							prevzoom = -1;
						}

						if(prevzoom > -1) {
							//System.out.println("here");
							int zoomdiff = currzoom - prevzoom;
							//System.out.println("zoomdiff: "+zoomdiff);
							if(zoomdiff == 0) { // pan
								hist.add(DirectionClass.PAN);
								pancount++;
							} else if (zoomdiff > 0) { // currzoom > prevzoom -> zoom in
								hist.add(DirectionClass.IN);
								incount++;
							} else {
								hist.add(DirectionClass.OUT);
								outcount++;
							}

							if(hist.size() > cmax) {
								//System.out.println("here");
								DirectionClass toRemove = hist.remove(0);
								if(toRemove == DirectionClass.IN) {
									incount--;
								} else if (toRemove == DirectionClass.OUT) {
									outcount--;
								} else {
									pancount--;
								}
							}
						}

						prevzoom = currzoom;


						double[] inp = {incount, outcount, pancount, currzoom, currx, curry};
						String label = currphase;
						Integer yval = LabelsToInts.get(label);
						if(yval == null) {
							yval = LabelsToInts.size();
							LabelsToInts.put(label, yval);
							IntsToLabels.add(label);
						}
						if(curruser == uid) { // test data
							TestX.add(inp);
							TestLabels.add(yval);
						} else { // training data
							X.add(inp);
							labels.add(yval);
						}

						//System.out.print(curruser+"\t"+currtask+"\t"+currx+"\t"+curry+"\t"+currzoom+"\t"+prev+"\t"+currdir);
						line = br.readLine();
					}
					br.close();
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				// build the SVM classifier
				SvmWrapper wrapped = buildModel(X,labels);
				
				//Test the model
				double accuracy = 0;
				for(int i = 0; i < TestX.size(); i++) {
					double[] inp = TestX.get(i);

					svm_node[] nodes = new svm_node[inp.length];
					for(int j = 0; j < inp.length; j++) {
						nodes[j] = new svm_node();
						nodes[j].index = j+1;
						// normalize using the same mean/stddev from the training data
						nodes[j].value = (inp[j] - wrapped.sum[j]) / wrapped.diffs[j];
					}
					int[] class_labels = new int[wrapped.model.nr_class];
					svm.svm_get_labels(wrapped.model, class_labels);
					int v = (int) svm.svm_predict(wrapped.model, nodes);
					//String pred = IntsToLabels.get(class_labels[v]);
					//System.out.println("labels: "+class_labels[0]+","+class_labels[1]+","+class_labels[2]);
					if(class_labels[v] == TestLabels.get(i)) {
						//System.out.println("hit at row "+i);
						accuracy++;
						overall_accuracy++;
					}
					total++;
				}
				accuracy /= TestX.size();
				output.add("accuracy for user "+uid+": "+accuracy);
			}
			overall_accuracy /= total;
			for(String line : output) {
				System.out.println(line);
			}
			System.out.println("overall accuracy for hlen "+hlen+": "+overall_accuracy);
		}
	}

	public static void runSvm(boolean header) {
		int[] hlens = {1,2};
		int[] users = {146, 150, 123, 145, 140, 132, 141, 151, 144, 148, 121, 130, 124, 135, 134, 137, 139, 138};
		// user  taskname  x y zoom  prevdirection direction incount outcount  pancount  phase
		int userid = 0;
		int taskname = 1;
		int x = 2;
		int y = 3;
		int zoom = 4;
		int prevdirection = 5;
		int direction = 6;
		int incount = 7;
		int outcount = 8;
		int pancount = 9;
		int phase = 10;

		String splitBy = "\t";
		BufferedReader br;

		Map<String,Integer> LabelsToInts = new HashMap<String,Integer>();
		List<String> IntsToLabels = new ArrayList<String>();

		for(int hl = 0; hl < hlens.length; hl++) {
			int hlen = hlens[hl];
			double overall_accuracy = 0;
			double total = 0;
			for(int u = 0; u < users.length; u++) {
				int uid = users[u];
				List<double[]> X = new ArrayList<double[]>();
				List<Integer> labels = new ArrayList<Integer>();
				List<double[]> TestX = new ArrayList<double[]>();
				List<Integer> TestLabels = new ArrayList<Integer>();

				// collect the train and test datasets
				try {
					br = new BufferedReader(new FileReader("/Volumes/E/mit/vis/code/scalar-prefetch/code/harness/out_"+hlen+".tsv"));
					if(header) br.readLine(); //get rid of header
					String line = br.readLine();
					while(line!=null){
						String[] row = line.split(splitBy);
						double[] inp = {Float.parseFloat(row[incount]),
								Float.parseFloat(row[outcount]),
								Float.parseFloat(row[pancount]),
								Float.parseFloat(row[zoom]),
								Float.parseFloat(row[x]),
								Float.parseFloat(row[y])};
						String label = row[phase];
						Integer yval = LabelsToInts.get(label);
						if(yval == null) {
							yval = LabelsToInts.size();
							LabelsToInts.put(label, yval);
							IntsToLabels.add(label);
						}
						int uval = Integer.parseInt(row[userid]);
						//if(uval == uid) { // test data
						TestX.add(inp);
						TestLabels.add(yval);
						//} else { // training data
						X.add(inp);
						labels.add(yval);
						//}
						line = br.readLine();
					}
					br.close();
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				// build the SVM classifier
				svm_problem prob = new svm_problem();
				prob.y = new double[labels.size()];
				prob.x = new svm_node[labels.size()][];
				prob.l = labels.size();
				double[] sum = new double[X.get(0).length];
				double[] diffs = new double[sum.length];
				for(int i = 0; i < labels.size(); i++) {
					prob.y[i] = labels.get(i);
					double[] inp = X.get(i);
					prob.x[i] = new svm_node[inp.length];
					for(int j = 0; j < inp.length; j++) {
						sum[j] += inp[j];
						prob.x[i][j] = new svm_node();
						prob.x[i][j].index = j+1;
						prob.x[i][j].value = inp[j];
					}
				}
				// compute the average
				for(int j = 0; j < sum.length; j++) {
					sum[j] /= labels.size();
				}
				// compute standard deviation
				for(int i = 0; i < labels.size(); i++) {
					for(int j = 0; j < diffs.length; j++) {
						diffs[j] += Math.pow(prob.x[i][j].value-sum[j], 2);
					}
				}
				for(int j = 0; j < sum.length; j++) {
					diffs[j] = Math.sqrt(diffs[j] / labels.size());
				}

				// normalize data
				for(int i = 0; i < labels.size(); i++) {
					for(int j = 0; j < diffs.length; j++) {
						prob.x[i][j].value = (prob.x[i][j].value - sum[j]) / diffs[j];
					}
				}

				svm_parameter params = new svm_parameter();
				params.C = 1.0;
				params.cache_size = 200;
				params.coef0 = 0.0;
				params.degree = 3;
				params.gamma = 0.07;
				params.shrinking = 1;
				params.probability = 0;
				params.kernel_type = svm_parameter.RBF;
				params.svm_type = svm_parameter.C_SVC;
				params.eps = 0.001;
				//params.nu = 0.5;
				//params.p = 0.1;
				//params.nr_weight = 0;
				//params.weight_label = new int[0];
				//params.weight = new double[0];

				// train the model
				svm_model model = svm.svm_train(prob, params);

				//Test the model
				double accuracy = 0;
				for(int i = 0; i < TestX.size(); i++) {
					double[] inp = TestX.get(i);

					svm_node[] nodes = new svm_node[inp.length];
					for(int j = 0; j < inp.length; j++) {
						nodes[j] = new svm_node();
						nodes[j].index = j+1;
						// normalize using the same mean/stddev from the training data
						nodes[j].value = (inp[j] - sum[j]) / diffs[j];
					}
					int[] class_labels = new int[model.nr_class];
					svm.svm_get_labels(model, class_labels);
					int v = (int) svm.svm_predict(model, nodes);
					//String pred = IntsToLabels.get(class_labels[v]);
					//System.out.println("labels: "+class_labels[0]+","+class_labels[1]+","+class_labels[2]);
					if(class_labels[v] == TestLabels.get(i)) {
						//System.out.println("hit at row "+i);
						accuracy++;
						overall_accuracy++;
					}
					total++;
				}
				accuracy /= TestX.size();
				System.out.println("accuracy for user "+uid+": "+accuracy);
			}
			overall_accuracy /= total;
			System.out.println("overall accuracy for hlen "+hlen+": "+overall_accuracy);
		}
	}

	public static void main(String[] args) throws Exception {
		//runSvm(true);
		//runSvm2("/Volumes/E/mit/vis/code/scalar-prefetch/gt_updated.csv",true,1);
		//runSvm2("/home/leibatt/projects/scalar-prefetch/gt_updated.csv",true,1);
		//runSvm2("/home/leibatt/projects/scalar-prefetch/gt_updated.csv",true,2);
		runSvm3("/home/leibatt/projects/scalar-prefetch/gt_updated.csv",true,5);
		//buildSvmPhaseClassifier();
	}
}
