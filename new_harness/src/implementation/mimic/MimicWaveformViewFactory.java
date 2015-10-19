package implementation.mimic;

import java.util.ArrayList;
import java.util.List;

import abstraction.structures.View;
import abstraction.structures.ViewMap;
import abstraction.util.DBInterface;

import configurations.Config;
import configurations.DBConnector;
import configurations.VMConfig;



/**
 * @author leibatt
 * This class can be used to generate forecache views for the given mimic waveform id
 */
public class MimicWaveformViewFactory {
	public static String nameTemplate = "FC_WF?";
	public static String queryTemplate = "ARRAY(apply(filter("+
			"slice(waveform_signal_table,RecordName,?)"+
			",not(is_nan(signal)))"+
			",msec2,msec))";
	public static String[] summaries = new String[]{
			"avg(signal) as avg_signal",
			"min(msec) as msec"};
	public static DBConnector connectionType = DBConnector.BIGDAWG;
	
	public String viewsFolder;
	public ViewMap mimicViews;
	
	public MimicWaveformViewFactory() {
		this.viewsFolder = DBInterface.mimic_views_folder;
		this.mimicViews = new ViewMap(viewsFolder);
	}
	
	public View getMimicWaveformView(String recordName) throws Exception {
		String viewName = nameTemplate.replace("?", recordName);
		String query = queryTemplate.replace("?", recordName);
		List<String> summaryFunctions = new ArrayList<String>();
		System.out.println(summaries.length);
		for(int i = 0; i < summaries.length;i++) {
			summaryFunctions.add(summaries[i]);
		}
		View v = this.mimicViews.getView(viewName, query, summaryFunctions, connectionType);
		this.mimicViews.saveView(v); // save it on disk so we don't have to make this view again
		return v;
	}
	
	public static void main(String[] args) {
		Config config = new VMConfig(); // use vm-specific configurations
		config.setConfig();
		
		MimicWaveformViewFactory factory = new MimicWaveformViewFactory();
		try {
			View v = factory.getMimicWaveformView("325553800011");
			System.out.println(v.getName());
			System.out.println(v.getQuery());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
