package de.unijena.bioinf.ms.gui.ms_viewer.data;

public interface MSViewerDataModelListener {
	
	void signalNoiseInformationChanged();
	
	void markedInformationChanged();
	
	void spectrumChanged();

}
