package de.unijena.bioinf.myxo.gui.msviewer.data;

public interface MSViewerDataModelListener {
	
	@SuppressWarnings("unused")
    void signalNoiseInformationChanged();
	
	void markedInformationChanged();
	
	void spectrumChanged();

}
