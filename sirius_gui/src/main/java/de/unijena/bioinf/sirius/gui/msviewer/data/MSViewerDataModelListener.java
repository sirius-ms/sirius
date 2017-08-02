package de.unijena.bioinf.sirius.gui.msviewer.data;

public interface MSViewerDataModelListener {
	
	void signalNoiseInformationChanged();
	
	void markedInformationChanged();
	
	void spectrumChanged();

}
