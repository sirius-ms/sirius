package de.unijena.bioinf.ms.gui.mainframe.result_panel;

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

    private FormulaList fl;

    public ResultPanel(final FormulaList siriusResultElements, WebAPI webAPI) {
        super();
        this.setToolTipText("Results");

        rvp = new FormulaOverviewPanel(siriusResultElements);
        tvp = new TreeVisualizationPanel();
        svp = new SpectraVisualizationPanel();
        cov = new CandidateOverviewPanel(new StructureList(siriusResultElements, ActionList.DataSelectionStrategy.ALL));
        ccv = new CandidateListDetailViewPanel(new StructureList(siriusResultElements));
        try {
            fpt = new FingerprintPanel(new FingerprintTable(siriusResultElements, webAPI));
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            fpt = null;
        }

        ccp = new CompoundClassPanel(new CompoundClassList(siriusResultElements), siriusResultElements);


        addTab("Sirius Overview", null, rvp, rvp.getDescription());
        addTab("Spectra", null, new FormulaListHeaderPanel(siriusResultElements, svp), svp.getDescription());
        addTab("Trees", null, new FormulaListHeaderPanel(siriusResultElements, tvp), tvp.getDescription());
        addTab("CSI:FingerID Overview", null, cov, cov.getDescription());
        addTab("CSI:FingerID Details", null, new FormulaListHeaderPanel(siriusResultElements, ccv), ccv.getDescription());
        if (fpt != null)
            addTab("Predicted Fingerprint", null, new FormulaListHeaderPanel(siriusResultElements, fpt), fpt.getDescription());
        addTab("Predicted Classyfire Classes", null, new FormulaListHeaderPanel(siriusResultElements, ccp), ccp.getDescription());

        this.fl = siriusResultElements;
    }
}
