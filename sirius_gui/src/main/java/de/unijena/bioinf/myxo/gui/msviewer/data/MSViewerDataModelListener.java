package de.unijena.bioinf.myxo.gui.msviewer.data;

public interface MSViewerDataModelListener {
	
	@SuppressWarnings("unused")
	public void signalNoiseInformationChanged();
	
	public void markedInformationChanged();
	
	public void spectrumChanged();

}
