package implementation.eeg;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import abstraction.enums.DBConnector;
import abstraction.structures.View;
import abstraction.structures.ViewMap;
import abstraction.util.DBInterface;

import configurations.Config;
import configurations.VMConfig;



/**
 * @author leibatt
 * This class can be used to generate forecache views for the given mimic waveform id
 */
public class EegViewFactory {
	public static String nameTemplate = "FC_EEG_?1_?2"; // filenumber-region
	public static String queryTemplate = "scan(EEG_?1_?2)";
	public static String[] summaries = new String[]{
			"avg(amplitude) as avg_amplitude"};
	public static DBConnector connectionType = DBConnector.SCIDB;
	
	public String viewsFolder;
	public ViewMap eegViews;
	
	public EegViewFactory() {
		this.viewsFolder = DBInterface.eeg_views_folder;;
		File eegDir = new File(this.viewsFolder);
		if(!eegDir.exists()) {
			try {
				eegDir.mkdir();
			} catch (SecurityException e) {
				System.err.println("could not create directory "+eegDir);
			}
		}
		this.eegViews = new ViewMap(viewsFolder);
	}
	
	public View getView(String filenumber, String region) throws Exception {
		String viewName = nameTemplate.replace("?1", filenumber).replace("?2",region);
		String query = queryTemplate.replace("?1", filenumber).replace("?2",region);
		List<String> summaryFunctions = Arrays.asList(summaries);
		View v = this.eegViews.getView(viewName, query, summaryFunctions, connectionType);
		this.eegViews.saveView(v); // save it on disk so we don't have to make this view again
		return v;
	}
	
	public static void main(String[] args) {
		Config config = new VMConfig(); // use vm-specific configurations
		config.setConfig();
		
		EegViewFactory factory = new EegViewFactory();
		try {
			View v = factory.getView("007","LL");
			System.out.println(v.getName());
			System.out.println(v.getQuery());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
