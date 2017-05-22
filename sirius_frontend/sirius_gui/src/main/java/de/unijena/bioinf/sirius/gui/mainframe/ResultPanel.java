package de.unijena.bioinf.sirius.gui.mainframe;

import de.unijena.bioinf.sirius.gui.fingerid.CandidateList;
import de.unijena.bioinf.sirius.gui.fingerid.CandidateListDetailViewPanel;
import de.unijena.bioinf.sirius.gui.fingerid.fingerprints.FingerprintPanel;
import de.unijena.bioinf.sirius.gui.fingerid.fingerprints.FingerprintTable;
import de.unijena.bioinf.sirius.gui.mainframe.molecular_formular.FormulaList;
import de.unijena.bioinf.sirius.gui.mainframe.molecular_formular.FormulaListHeaderPanel;
import de.unijena.bioinf.sirius.gui.table.ActionList;

import javax.swing.*;

public class ResultPanel extends JTabbedPane {


    private FormulaOverviewPanel rvp;
    private TreeVisualizationPanel tvp;
    private SpectraVisualizationPanel svp;
    private CandidateListDetailViewPanel ccv;
    private CandidateOverviewPanel cov;
    private FingerprintPanel fpt;

    public ResultPanel(final FormulaList suriusResultElements) {
        super();
        this.setToolTipText("Results");

        rvp = new FormulaOverviewPanel(suriusResultElements);
        tvp = new TreeVisualizationPanel();
        svp = new SpectraVisualizationPanel();
        cov = new CandidateOverviewPanel(new CandidateList(suriusResultElements, ActionList.DataSelectionStrategy.ALL));
        ccv = new CandidateListDetailViewPanel(MainFrame.MF.getCsiFingerId(), new CandidateList(suriusResultElements));
        fpt = new FingerprintPanel(new FingerprintTable(suriusResultElements));
//        CandidateList list = new CandidateList(suriusResultElements);


        addTab("Molecular Formulas", rvp);
        addTab("Compound Candidates", cov);
        addTab("Spectra", new FormulaListHeaderPanel(suriusResultElements, svp));
        addTab("Trees", new FormulaListHeaderPanel(suriusResultElements, tvp));
        addTab("CSI:FingerId", new FormulaListHeaderPanel(suriusResultElements, ccv));
        addTab("Fingerprint", new FormulaListHeaderPanel(suriusResultElements, fpt));
    }

    public void dispose() {
        ccv.dispose();
    }
}
