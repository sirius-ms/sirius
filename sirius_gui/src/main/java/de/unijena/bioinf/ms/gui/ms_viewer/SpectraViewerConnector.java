package de.unijena.bioinf.ms.gui.ms_viewer;

import de.unijena.bioinf.ms.gui.mainframe.result_panel.VisualizationPanelSynchronizer;

public class SpectraViewerConnector {

    VisualizationPanelSynchronizer sync;
    float selection = -1;

    public void registerSynchronizer(VisualizationPanelSynchronizer sync){
        this.sync = sync;
    }

    public float getCurrentSelection() {
        return selection;
    }

    public void selectionChanged(float peak_mz) {
        selection = peak_mz;
        sync.peakChanged(peak_mz);
    }
}
