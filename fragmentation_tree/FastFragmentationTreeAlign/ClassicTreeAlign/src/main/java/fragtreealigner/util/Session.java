
package fragtreealigner.util;

import fragtreealigner.domainobjects.db.FragmentationTreeDatabase;
import fragtreealigner.ui.MainFrame;

import java.io.Serializable;

@SuppressWarnings("serial")
public class Session implements Serializable {
	private Parameters parameters;
	private MainFrame mainFrame;
	private FragmentationTreeDatabase fragTreeDB;
	private FragmentationTreeDatabase altFragTreeDB;

	public void setParameters(Parameters parameters) {
		this.parameters = parameters;
	}

	public Parameters getParameters() {
		return parameters;
	}
	
	public void setMainFrame(MainFrame mainFrame) {
		this.mainFrame = mainFrame;
	}

	public MainFrame getMainFrame() {
		return mainFrame;
	}

	public void setFragTreeDB(FragmentationTreeDatabase fragTreeDB) {
		this.fragTreeDB = fragTreeDB;
	}

	public void setAltFragTreeDB(FragmentationTreeDatabase altFragTreeDB) {
		this.altFragTreeDB = altFragTreeDB;
	}
	public FragmentationTreeDatabase getFragTreeDB() {
		return fragTreeDB;
	}

	public FragmentationTreeDatabase getAltFragTreeDB() {
		return altFragTreeDB;
	}	
}
