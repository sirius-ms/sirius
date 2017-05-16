package de.unijena.bioinf.sirius.gui.mainframe;

import de.unijena.bioinf.sirius.gui.fingerid.CandidateList;
import de.unijena.bioinf.sirius.gui.fingerid.CompoundCandidateView;
import de.unijena.bioinf.sirius.gui.mainframe.molecular_formular.FormulaList;
import de.unijena.bioinf.sirius.gui.mainframe.molecular_formular.FormulaListHeaderPanel;

import javax.swing.*;

public class ResultPanel extends JTabbedPane {


    private FormulaOverviewPanel rvp;
    private TreeVisualizationPanel tvp;
    private SpectraVisualizationPanel svp;
    private CompoundCandidateView ccv;
    private CandidateOverviewPanel cov;

    public ResultPanel(final FormulaList suriusResultElements) {
        super();
        this.setToolTipText("Results");

        rvp = new FormulaOverviewPanel(suriusResultElements);
        tvp = new TreeVisualizationPanel();
        svp = new SpectraVisualizationPanel();
        ccv = new CompoundCandidateView();
//        CandidateList list = new CandidateList(suriusResultElements);
//        cov =  new CandidateOverviewPanel(new CandidateList(suriusResultElements));

        addTab("Overview", rvp);
        addTab("Spectra view", new FormulaListHeaderPanel(suriusResultElements, svp));
        addTab("Tree view", new FormulaListHeaderPanel(suriusResultElements, tvp));
        addTab("CSI:FingerId", new FormulaListHeaderPanel(suriusResultElements, ccv));
//        addTab("Candidate Overview", new FormulaListHeaderPanel(suriusResultElements, cov));
    }

    public void dispose() {
        ccv.dispose();
    }
}
