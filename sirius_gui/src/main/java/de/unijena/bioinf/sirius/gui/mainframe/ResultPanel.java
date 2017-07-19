package de.unijena.bioinf.sirius.gui.mainframe;

import de.unijena.bioinf.sirius.gui.canopus.CanopusPanel;
import de.unijena.bioinf.sirius.gui.fingerid.CandidateList;
import de.unijena.bioinf.sirius.gui.fingerid.CandidateListDetailViewPanel;
import de.unijena.bioinf.sirius.gui.fingerid.fingerprints.FingerprintPanel;
import de.unijena.bioinf.sirius.gui.fingerid.fingerprints.FingerprintTable;
import de.unijena.bioinf.sirius.gui.mainframe.molecular_formular.FormulaList;
import de.unijena.bioinf.sirius.gui.mainframe.molecular_formular.FormulaListHeaderPanel;
import de.unijena.bioinf.sirius.gui.table.ActionList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.IOException;

public class ResultPanel extends JTabbedPane {

    protected static final Logger logger = LoggerFactory.getLogger(ResultPanel.class);

    private FormulaOverviewPanel rvp;
    private TreeVisualizationPanel tvp;
    private SpectraVisualizationPanel svp;
    private CandidateListDetailViewPanel ccv;
    private CandidateOverviewPanel cov;
    private FingerprintPanel fpt;

    private CanopusPanel classyfireTreePanel;
    private FormulaList fl;

    public ResultPanel(final FormulaList suriusResultElements) {
        super();
        this.setToolTipText("Results");

        rvp = new FormulaOverviewPanel(suriusResultElements);
        tvp = new TreeVisualizationPanel();
        svp = new SpectraVisualizationPanel();
        cov = new CandidateOverviewPanel(new CandidateList(suriusResultElements, ActionList.DataSelectionStrategy.ALL));
        ccv = new CandidateListDetailViewPanel(MainFrame.MF.getCsiFingerId(), new CandidateList(suriusResultElements));
        try {
            fpt = new FingerprintPanel(new FingerprintTable(suriusResultElements));
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            fpt = null;
        }

        addTab("Sirius Overview", null, rvp, rvp.getDescription());
        addTab("Spectra", null, new FormulaListHeaderPanel(suriusResultElements, svp), svp.getDescription());
        addTab("Trees", null, new FormulaListHeaderPanel(suriusResultElements, tvp), tvp.getDescription());
        addTab("CSI:FingerId Overview", null, cov, cov.getDescription());
        addTab("CSI:FingerId Details", null, new FormulaListHeaderPanel(suriusResultElements, ccv), ccv.getDescription());
        if (fpt != null)
            addTab("Predicted Fingerprint", null, new FormulaListHeaderPanel(suriusResultElements, fpt), fpt.getDescription());

        this.fl = suriusResultElements;
    }

    public void dispose() {
        ccv.dispose();
    }

    public void enableCanopus() {
        classyfireTreePanel = new CanopusPanel();
        addTab("Compound Classification", null, new FormulaListHeaderPanel(fl, classyfireTreePanel));
    }

}
