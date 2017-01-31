package de.unijena.bioinf.sirius.gui.mainframe.results;

import de.unijena.bioinf.sirius.gui.fingerid.CompoundCandidateView;
import de.unijena.bioinf.sirius.gui.mainframe.results.result_element_view.FormulaTable;
import de.unijena.bioinf.sirius.gui.mainframe.results.result_element_view.FormulaTableCompactView;
import de.unijena.bioinf.sirius.gui.mainframe.results.result_element_view.FormulaTableDetailView;
import de.unijena.bioinf.sirius.gui.mainframe.results.result_element_view.FormulaTableHeaderPanel;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import de.unijena.bioinf.sirius.gui.structure.SiriusResultElement;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ResultPanel extends JTabbedPane {


    private ResultsOverviewPanel rvp;
    protected TreeVisualizationPanel tvp;
    protected SpectraVisualizationPanel svp;
    private CompoundCandidateView ccv;

    private List<ActiveResultChangedListener> listeners;

    public void addActiveResultChangedListener(ActiveResultChangedListener listener) {
        listeners.add(listener);
    }

    public void removeActiveResultChangedListener(ActiveResultChangedListener listener) {
        listeners.remove(listener);
    }

    public void dispose() {
        ccv.dispose();
    }

//    public ResultPanel() {
//        this(null);
//    }


    public ResultPanel(final FormulaTable suriusResultElements) {
        super();
        this.listeners = new ArrayList<>();
        this.setToolTipText("Results");

        TreeVisualizationPanel tvpTmp = new TreeVisualizationPanel();
        suriusResultElements.addActiveResultChangedListener(tvpTmp);
        SpectraVisualizationPanel svpTmp = new SpectraVisualizationPanel();
        suriusResultElements.addActiveResultChangedListener(svpTmp);
        rvp = new ResultsOverviewPanel(new FormulaTableDetailView(suriusResultElements), svpTmp, 1, tvpTmp, 2);

        tvp = new TreeVisualizationPanel();
        svp = new SpectraVisualizationPanel();
        ccv = new CompoundCandidateView();

        addTab("Overview", rvp);
        addTab("Spectra view", new FormulaTableHeaderPanel(suriusResultElements,svp));
        addTab("Tree view", new FormulaTableHeaderPanel(suriusResultElements,tvp));
        addTab("CSI:FingerId", new FormulaTableHeaderPanel(suriusResultElements,ccv));
    }

    public void changeData(final ExperimentContainer ec) {
//        resultsJList.addListSelectionListener(this); //todo implement alternative
        //ccv.changeData(ec, sre); is replaced by listener

        ////////////////
        // TODO: put all the stuff about into listeners
        ////////////////
        /*for (ActiveResultChangedListener listener : listeners) {
            listener.resultsChanged(ec, sre);
        }*/


    }


    //todo can be removed with nu model
    public void select(SiriusResultElement sre, boolean fireEvent) {

        ////////////////
        // TODO: put all the stuff about into listeners
        ////////////////
       /* for (ActiveResultChangedListener listener : listeners) {
            listener.resultsChanged(ec, sre);
        }*/
    }


    //todo this has to be moved!!!!!!!!!
    public void computeFingerID(boolean searchAllEnabled) {

//        SiriusResultElement resultElement = suriusResultElements.getResultListView().getSelectedValue();


    }


}
