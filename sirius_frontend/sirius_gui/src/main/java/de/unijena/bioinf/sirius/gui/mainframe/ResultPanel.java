package de.unijena.bioinf.sirius.gui.mainframe;

import de.unijena.bioinf.sirius.gui.fingerid.CandidateList;
import de.unijena.bioinf.sirius.gui.fingerid.CompoundCandidateView;
import de.unijena.bioinf.sirius.gui.mainframe.molecular_formular.FormulaList;
import de.unijena.bioinf.sirius.gui.mainframe.molecular_formular.FormulaListHeaderPanel;
import de.unijena.bioinf.sirius.gui.table.ActionList;

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
        cov = new CandidateOverviewPanel(new CandidateList(suriusResultElements, ActionList.DataSelectionStrategy.ALL));
        ccv = new CompoundCandidateView(MainFrame.MF.getCsiFingerId(), new CandidateList(suriusResultElements));
//        CandidateList list = new CandidateList(suriusResultElements);


        addTab("Molecular Formulas", rvp);
        addTab("Compound Candidates", cov);
        addTab("Spectra", new FormulaListHeaderPanel(suriusResultElements, svp));
        addTab("Trees", new FormulaListHeaderPanel(suriusResultElements, tvp));
        addTab("CSI:FingerId", new FormulaListHeaderPanel(suriusResultElements, ccv));
    }

    public void dispose() {
        ccv.dispose();
    }
}
