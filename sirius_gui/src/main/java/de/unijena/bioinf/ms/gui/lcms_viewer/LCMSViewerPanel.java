package de.unijena.bioinf.ms.gui.lcms_viewer;

import de.unijena.bioinf.ChemistryBase.ms.lcms.CoelutingTraceSet;
import de.unijena.bioinf.ChemistryBase.ms.lcms.LCMSPeakInformation;
import de.unijena.bioinf.ms.gui.molecular_formular.FormulaList;
import de.unijena.bioinf.ms.gui.table.ActiveElementChangedListener;
import de.unijena.bioinf.ms.gui.utils.ToggableSidePanel;
import de.unijena.bioinf.projectspace.FormulaResultBean;
import de.unijena.bioinf.projectspace.InstanceBean;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Optional;

public class LCMSViewerPanel extends JPanel implements ActiveElementChangedListener<FormulaResultBean, InstanceBean> {

    private InstanceBean currentInstance;
    private LCMSPeakInformation currentInfo;

    private LCMSWebview lcmsWebview;
    private LCMSToolbar toolbar;
    private LCMSCompoundSummaryPanel summaryPanel;
    private int activeIndex;

    public LCMSViewerPanel(FormulaList siriusResultElements) {
        // set content
        this.toolbar = new LCMSToolbar(this);
        setLayout(new BorderLayout());
        add(toolbar, BorderLayout.NORTH);
        this.lcmsWebview = new LCMSWebview();
        this.add(lcmsWebview, BorderLayout.CENTER);

        summaryPanel = new LCMSCompoundSummaryPanel();
        this.add(new ToggableSidePanel("quality report", summaryPanel), BorderLayout.EAST);



        // add listeners
        siriusResultElements.addActiveResultChangedListener(this);
    }

    public void reset() {
        lcmsWebview.reset();
        summaryPanel.reset();
        toolbar.reset();
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
        // we are only interested in changes of the experiment
        if (currentInstance!= elementsParent) {
            currentInstance = elementsParent;
            activeIndex = 0;
            updateContent();
        }
    }

    private void updateContent() {
        if (currentInstance==null) {
            reset();
            return;
        }
        //todo nightsky: fill with new LCMS data
        final LCMSPeakInformation peakInformation = null;//currentInstance.loadCompoundContainer(LCMSPeakInformation.class).getAnnotation(LCMSPeakInformation.class, LCMSPeakInformation::empty);
        currentInfo = peakInformation;
        lcmsWebview.setInstance(peakInformation);
        toolbar.reloadContent(peakInformation);
        updateInfo();
    }

    public void setActiveIndex(int id) {
        if (id != activeIndex) {
            activeIndex = id;
            lcmsWebview.setSampleIndex(activeIndex);
            updateInfo();
            invalidate();
        }
    }

    private void updateInfo() {
        //todo nightsky: fill with new LCMS data
        final Optional<CoelutingTraceSet> trace = activeIndex < currentInfo.length() ? currentInfo.getTracesFor(activeIndex) : Optional.empty();
//        if (trace.isPresent())
//            summaryPanel.set(trace.get(), currentInstance.getExperiment());
//        else summaryPanel.reset();
    }
}
