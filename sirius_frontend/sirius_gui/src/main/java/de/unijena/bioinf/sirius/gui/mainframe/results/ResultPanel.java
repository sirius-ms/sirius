package de.unijena.bioinf.sirius.gui.mainframe.results;

import de.unijena.bioinf.sirius.gui.fingerid.CompoundCandidateView;
import de.unijena.bioinf.sirius.gui.mainframe.results.result_element_view.FormulaList;
import de.unijena.bioinf.sirius.gui.mainframe.results.result_element_view.FormulaListDetailView;
import de.unijena.bioinf.sirius.gui.mainframe.results.result_element_view.FormulaListHeaderPanel;

import javax.swing.*;

public class ResultPanel extends JTabbedPane {


    private ResultsOverviewPanel rvp;
    private TreeVisualizationPanel tvp;
    private SpectraVisualizationPanel svp;
    private CompoundCandidateView ccv;

    public ResultPanel(final FormulaList suriusResultElements) {
        super();
        this.setToolTipText("Results");

        TreeVisualizationPanel tvpTmp = new TreeVisualizationPanel();
        suriusResultElements.addActiveResultChangedListener(tvpTmp);
        SpectraVisualizationPanel svpTmp = new SpectraVisualizationPanel();
        suriusResultElements.addActiveResultChangedListener(svpTmp);
        rvp = new ResultsOverviewPanel(new FormulaListDetailView(suriusResultElements), svpTmp, 1, tvpTmp, 2);

        tvp = new TreeVisualizationPanel();
        svp = new SpectraVisualizationPanel();
        ccv = new CompoundCandidateView();

        addTab("Overview", rvp);
        addTab("Spectra view", new FormulaListHeaderPanel(suriusResultElements, svp));
        addTab("Tree view", new FormulaListHeaderPanel(suriusResultElements, tvp));
        addTab("CSI:FingerId", new FormulaListHeaderPanel(suriusResultElements, ccv));
    }

    public void dispose() {
        ccv.dispose();
    }
}
