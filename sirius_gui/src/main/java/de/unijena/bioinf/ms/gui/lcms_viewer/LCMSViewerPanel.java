package de.unijena.bioinf.ms.gui.lcms_viewer;

import de.unijena.bioinf.ChemistryBase.ms.lcms.LCMSPeakInformation;
import de.unijena.bioinf.ms.gui.molecular_formular.FormulaList;
import de.unijena.bioinf.ms.gui.table.ActiveElementChangedListener;
import de.unijena.bioinf.projectspace.FormulaResultBean;
import de.unijena.bioinf.projectspace.InstanceBean;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class LCMSViewerPanel extends JPanel implements ActiveElementChangedListener<FormulaResultBean, InstanceBean> {

    private InstanceBean currentInstance;

    private LCMSWebview lcmsWebview;
    private LCMSToolbar toolbar;
    private int activeIndex;

    public LCMSViewerPanel(FormulaList siriusResultElements) {
        // set content
        this.toolbar = new LCMSToolbar(this);
        setLayout(new BorderLayout());
        add(toolbar, BorderLayout.NORTH);
        this.lcmsWebview = new LCMSWebview();
        this.add(lcmsWebview, BorderLayout.CENTER);

        // add listeners
        siriusResultElements.addActiveResultChangedListener(this);
    }

    public String getDescription() {
        return "Chromatographic Peak of the ion in LC-MS.";

    }

    @Override
    public void resultsChanged(InstanceBean experiment, FormulaResultBean sre, List<FormulaResultBean> resultElements, ListSelectionModel selections) {
        // we are only interested in changes of the experiment
        if (currentInstance!=experiment) {
            currentInstance = experiment;
            activeIndex = 0;
            updateContent();
        }
    }

    private void updateContent() {
        final LCMSPeakInformation peakInformation = currentInstance.loadCompoundContainer(LCMSPeakInformation.class).getAnnotation(LCMSPeakInformation.class, LCMSPeakInformation::empty);
        lcmsWebview.setInstance(peakInformation);
        toolbar.reloadContent(peakInformation);
    }

    public void setActiveIndex(int id) {
        if (id != activeIndex) {
            activeIndex = id;
            lcmsWebview.setSampleIndex(activeIndex);
        }
    }
}
