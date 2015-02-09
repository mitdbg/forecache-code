package frontend;

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
import utils.DBInterface;
import utils.ExplorationPhase;
import utils.TraceMetadata;
import utils.UserRequest;
import utils.UtilityFunctions;

public class TestSVM {
	
	public static void runSvm(boolean header) {
		int[] hlens = {1};
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
		runSvm(true);
	}
}
