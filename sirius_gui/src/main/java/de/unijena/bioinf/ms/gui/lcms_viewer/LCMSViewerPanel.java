package de.unijena.bioinf.ms.gui.lcms_viewer;

import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.molecular_formular.FormulaList;
import de.unijena.bioinf.ms.gui.table.ActiveElementChangedListener;
import de.unijena.bioinf.ms.gui.utils.ToggableSidePanel;
import de.unijena.bioinf.ms.gui.utils.loading.Loadable;
import de.unijena.bioinf.ms.gui.utils.loading.LoadablePanel;
import de.unijena.bioinf.ms.gui.webView.JCefBrowserPanel;
import de.unijena.bioinf.projectspace.FormulaResultBean;
import de.unijena.bioinf.projectspace.InstanceBean;
import io.sirius.ms.sdk.model.AlignedFeatureQualityExperimental;

import javax.swing.*;
import java.awt.*;
import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class LCMSViewerPanel extends JPanel implements ActiveElementChangedListener<FormulaResultBean, InstanceBean>, Loadable {

    private InstanceBean currentInstance;

    private LCMSCefPanel lcmsWebview;
    private LCMSCompoundSummaryPanel summaryPanel;

    private final LoadablePanel loadable;

    public LCMSViewerPanel(SiriusGui gui, FormulaList siriusResultElements) {
        // set content
        setLayout(new BorderLayout());
        this.lcmsWebview = new LCMSCefPanel(gui);
        this.loadable = new LoadablePanel(lcmsWebview);
        this.add(loadable, BorderLayout.CENTER);

        summaryPanel = new LCMSCompoundSummaryPanel();
        JScrollPane scrollpanel = new JScrollPane(summaryPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollpanel.setPreferredSize(new Dimension(400, 320));
        scrollpanel.setMaximumSize(new Dimension(400, Integer.MAX_VALUE));
        this.add(new ToggableSidePanel("quality report", scrollpanel), BorderLayout.EAST);

        // add listeners
        siriusResultElements.addActiveResultChangedListener(this);
    }

    @Override
    public boolean setLoading(boolean loading, boolean absolute) {
        return loadable.setLoading(loading, absolute);
    }

    public void reset() {
        lcmsWebview.updateSelectedFeature(null);
        summaryPanel.reset();
    }

    public String getDescription() {
        return "<html>"
                +"<b>LC-MS and Data Quality Viewer</b>"
                +"<br>"
                + "Shows the chromatographic peak of the ion in LC-MS (left panel)"
                +"<br>"
                + "Shows data quality information (right panel)"
                +"<br>"
                + "Note: Only available if feature finding was performed by SIRIUS (mzml/mzXML)"
                + "</html>";
    }

    @Override
    public void resultsChanged(InstanceBean elementsParent, FormulaResultBean selectedElement, List<FormulaResultBean> resultElements, ListSelectionModel selections) {
        increaseLoading();
        try {
            // we are only interested in changes of the experiment
            if (currentInstance!= elementsParent) {
                currentInstance = elementsParent;
                updateContent();
            }
        } finally {
            disableLoading();
        }
    }

    private void updateContent() {
        if (currentInstance==null) {
            reset();
            return;
        }

        CompletableFuture<AlignedFeatureQualityExperimental> future = currentInstance.getClient().features().getAlignedFeatureQualityExperimentalWithResponseSpec(currentInstance.getProjectManager().projectId, currentInstance.getFeatureId())
                .bodyToMono(AlignedFeatureQualityExperimental.class).onErrorComplete().toFuture();

        lcmsWebview.updateSelectedFeature(currentInstance.getFeatureId());

        try {
            AlignedFeatureQualityExperimental alignedFeatureQuality = future.get();
            summaryPanel.setReport(alignedFeatureQuality);
        } catch (InterruptedException | ExecutionException e) {
            summaryPanel.setReport(null);
            throw new RuntimeException(e);
        }
    }

    private static class LCMSCefPanel extends JCefBrowserPanel {

        public LCMSCefPanel(SiriusGui siriusGui) {
            super(URI.create(siriusGui.getSiriusClient().getApiClient().getBasePath()).resolve("/lcms")
                    + THEME_REST_PARA + "&pid=" + siriusGui.getProjectManager().getProjectId(), siriusGui);
        }
    }

}
