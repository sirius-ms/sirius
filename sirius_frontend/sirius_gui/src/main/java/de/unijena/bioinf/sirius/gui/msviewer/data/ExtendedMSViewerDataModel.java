package de.unijena.bioinf.sirius.gui.msviewer.data;

import java.util.List;


public interface ExtendedMSViewerDataModel extends MSViewerDataModel{
	
	public void removeMarkings();
	
	public void removeMarkings( List<MSViewerDataModelListener> ignoredListener);
	
//	public void setNoise(int index, boolean noise);
//	
//	public void setNoise(int[] indices, boolean[] noise);
	
	public void setMarked(int index, boolean marked, List<MSViewerDataModelListener> ignoredListener);
	
	public void setMarked(int[] indices, boolean[] marked, List<MSViewerDataModelListener> ignoredListener);
	
	public void addMSViewerDataModelListener(MSViewerDataModelListener listener);
	
	public void removeMSViewerDataModelListener(MSViewerDataModelListener listener);
	
	public void setIsotopeMode(boolean isotopeMode);

}
