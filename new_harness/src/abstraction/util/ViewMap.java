package abstraction.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;




import configurations.DBConnector;

/**
 * @author leibatt
 * Used to manage views in ForeCache. User creates a view,
 * representing the data they want to explore.
 */
public class ViewMap {
	protected Map<String,View> viewMap;
	protected String viewFolder = null; // location to save views
	
	
	public ViewMap(String viewFolder) {
		this.viewMap = new HashMap<String,View>();
		this.viewFolder = viewFolder;
	}
	
	// get existing view, otherwise create it
	public View getView(String viewName,String query,List<String> summaries, DBConnector connectionType) throws Exception {
		if(this.viewMap.containsKey(viewName)) return this.viewMap.get(viewName);
		View v = getSavedView(viewName);
		if(v != null) return v;
		return createView(viewName,query,summaries,connectionType);
	}
	
	// create new view (do not use if the view may exist already!)
	public View createView(String viewName,String query,List<String> summaries, DBConnector connectionType) throws Exception {
		if(this.viewMap.containsKey(viewName)) throw new Exception("view name '"+viewName+"' already exists!");
		View v = new View(viewName,query, summaries,connectionType);
		this.viewMap.put(viewName, v);
		return v;
	}
	
	public void removeView(String viewName) {
		this.viewMap.remove(viewName);
	}
	
	public void removeView(View v) {
		this.viewMap.remove(v.getName());
	}
	
	public void clearViews() {
		this.viewMap.clear();
	}
	
	// creates a filename to associate with this view name
	public String getViewFilename(String viewName) {
		return viewName + ".ser";
	}
	
	// retrieves saved view from disk
	public View getSavedView(String viewName) {
		View v = null;
		ObjectInputStream in;
		String filename = getViewFilename(viewName);
		File dir = new File(viewFolder);
		dir.mkdirs();
		File file = new File(dir,filename);
		try {
			in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(file)));
	        v = (View) in.readObject();
	        if(v != null) this.viewMap.put(viewName, v);
	        in.close();
		} catch (FileNotFoundException e) {
			System.out.println("could not find file: "+filename);
			//e.printStackTrace();
		} catch (IOException e) {
			System.out.println("could not read file from disk: "+filename);
			//e.printStackTrace();
		} catch (ClassNotFoundException e) {
			System.out.println("could not find class definition for class: "+View.class);
			e.printStackTrace();
		}
		return v;
	}
	
	// save this view to disk for later use
	public boolean saveView(View v) {
		File dir = new File(viewFolder);
		dir.mkdirs();
		String filename = getViewFilename(v.getName());
		File file = new File(dir,filename);
		ObjectOutputStream out;
		try {
			out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
			out.writeObject(v);
			out.close();
			return true;
		} catch (FileNotFoundException e) {
			System.out.println("could not write view '"+v+"' to disk.");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("could not write '"+v+"' to disk.");
			e.printStackTrace();
		}
		return false;
	}
}
