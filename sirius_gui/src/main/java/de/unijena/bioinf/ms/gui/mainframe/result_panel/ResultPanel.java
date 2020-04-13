package de.unijena.bioinf.ms.gui.mainframe.result_panel;

import de.unijena.bioinf.ms.gui.canopus.CanopusPanel;
import de.unijena.bioinf.ms.gui.canopus.compound_classes.CompoundClassList;
import de.unijena.bioinf.ms.gui.fingerid.StructureList;
import de.unijena.bioinf.ms.gui.fingerid.fingerprints.FingerprintTable;
import de.unijena.bioinf.ms.gui.mainframe.result_panel.tabs.*;
import de.unijena.bioinf.ms.gui.molecular_formular.FormulaList;
import de.unijena.bioinf.ms.gui.molecular_formular.FormulaListHeaderPanel;
import de.unijena.bioinf.ms.gui.table.ActionList;
import de.unijena.bioinf.webapi.WebAPI;
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
    private CompoundClassPanel ccp;

    private CanopusPanel classyfireTreePanel;
    private FormulaList fl;

    public ResultPanel(final FormulaList suriusResultElements, WebAPI webAPI) {
        super();
        this.setToolTipText("Results");

        rvp = new FormulaOverviewPanel(suriusResultElements);
        tvp = new TreeVisualizationPanel();
        svp = new SpectraVisualizationPanel();
        cov = new CandidateOverviewPanel(new StructureList(suriusResultElements, ActionList.DataSelectionStrategy.ALL));
        ccv = new CandidateListDetailViewPanel(new StructureList(suriusResultElements));
        try {
            fpt = new FingerprintPanel(new FingerprintTable(suriusResultElements, webAPI));
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            fpt = null;
        }

        ccp = new CompoundClassPanel(new CompoundClassList(suriusResultElements));


        addTab("Sirius Overview", null, rvp, rvp.getDescription());
        addTab("Spectra", null, new FormulaListHeaderPanel(suriusResultElements, svp), svp.getDescription());
        addTab("Trees", null, new FormulaListHeaderPanel(suriusResultElements, tvp), tvp.getDescription());
        addTab("CSI:FingerID Overview", null, cov, cov.getDescription());
        addTab("CSI:FingerID Details", null, new FormulaListHeaderPanel(suriusResultElements, ccv), ccv.getDescription());
        if (fpt != null)
            addTab("Predicted Fingerprint", null, new FormulaListHeaderPanel(suriusResultElements, fpt), fpt.getDescription());
        addTab("Predicted Classyfire Classes", null, new FormulaListHeaderPanel(suriusResultElements, ccp), ccp.getDescription());

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
