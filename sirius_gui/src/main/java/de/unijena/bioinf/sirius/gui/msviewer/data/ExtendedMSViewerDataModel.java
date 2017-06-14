package de.unijena.bioinf.sirius.gui.msviewer.data;

import java.util.List;


public interface ExtendedMSViewerDataModel extends MSViewerDataModel{
	
	void removeMarkings();
	
	void removeMarkings(List<MSViewerDataModelListener> ignoredListener);
	
//	public void setNoise(int index, boolean noise);
//	
//	public void setNoise(int[] indices, boolean[] noise);
	
	void setMarked(int index, boolean marked, List<MSViewerDataModelListener> ignoredListener);
	
	void setMarked(int[] indices, boolean[] marked, List<MSViewerDataModelListener> ignoredListener);
	
	void addMSViewerDataModelListener(MSViewerDataModelListener listener);
	
	void removeMSViewerDataModelListener(MSViewerDataModelListener listener);
	
	void setIsotopeMode(boolean isotopeMode);

}
