package implementation;

import java.util.ArrayList;
import java.util.List;

import configurations.DBConnector;

import abstraction.View;
import abstraction.ViewMap;


/**
 * @author leibatt
 * This class can be used to generate forecache views for the given mimic waveform id
 */
public class MimicWaveformViewFactory {
	public static String nameTemplate = "FC_WF?";
	public static String queryTemplate = "apply(filter("+
			"slice(waveform_signal_table,RecordName,?)"+
			",not(is_nan(signal)))"+
			",msec2,msec)";
	public static String[] summaries = new String[]{
			"avg(signal) as avg_signal",
			"min(msec) as msec"};
	
	public static String viewsFolder = "/home/leibatt/projects/forecache-code/harness/mimic_views";
	
	public ViewMap mimicViews;
	
	public MimicWaveformViewFactory() {
		this.mimicViews = new ViewMap(viewsFolder);
	}
	
	public View getMimicWaveformView(String recordName) throws Exception {
		String viewName = nameTemplate.replace("?", recordName);
		String query = queryTemplate.replace("?", recordName);
		List<String> summaryFunctions = new ArrayList<String>();
		for(int i = 0; i < summaries.length;) {
			summaryFunctions.add(summaries[i]);
		}
		View v = this.mimicViews.getView(viewName, query, summaryFunctions, DBConnector.SCIDB);
		this.mimicViews.saveView(v); // save it on disk so we don't have to make this view again
		return v;
	}
}
