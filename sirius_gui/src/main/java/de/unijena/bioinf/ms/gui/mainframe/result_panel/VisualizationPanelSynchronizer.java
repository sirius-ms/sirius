package de.unijena.bioinf.ms.gui.mainframe.result_panel;

import de.unijena.bioinf.ms.gui.mainframe.result_panel.tabs.SpectraVisualizationPanel;
import de.unijena.bioinf.ms.gui.mainframe.result_panel.tabs.TreeVisualizationPanel;
import de.unijena.bioinf.ms.gui.ms_viewer.SpectraViewerConnector;
import de.unijena.bioinf.ms.gui.tree_viewer.TreeViewerConnector;

public class VisualizationPanelSynchronizer {

    TreeViewerConnector tvc;
    SpectraViewerConnector svc;
    TreeVisualizationPanel tvp;
    SpectraVisualizationPanel svp;

    private VisualizationPanelSynchronizer(TreeVisualizationPanel tvp, SpectraVisualizationPanel svp) {
        this.tvp = tvp;
        this.svp = svp;
        this.tvc = tvp.getConnector();
        this.svc = svp.getConnector();
        tvc.registerSynchronizer(this);
        svc.registerSynchronizer(this);
    }

    public void fragmentChanged(float new_mz) {
        svp.browser.executeJS("SpectrumPlot.setSelection(main.spectrum, " + new_mz + ")");
    }

    public void peakChanged(float new_mz) {
        tvp.browser.executeJS("setSelection(" + new_mz + ")");
    }

    public static VisualizationPanelSynchronizer synchronize(TreeVisualizationPanel tvp, SpectraVisualizationPanel svp){
        return new VisualizationPanelSynchronizer(tvp, svp);
    }

}
