package de.unijena.bioinf.sirius.gui.mainframe.results;

import de.unijena.bioinf.sirius.gui.fingerid.CompoundCandidateView;
import de.unijena.bioinf.sirius.gui.mainframe.results.result_element_view.DefaultResultElementListenerPanel;
import de.unijena.bioinf.sirius.gui.mainframe.results.result_element_view.FormulaTable;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import de.unijena.bioinf.sirius.gui.structure.SiriusResultElement;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ResultPanel extends JTabbedPane {


    private ResultsOverviewPanel rvp;
    protected DefaultResultElementListenerPanel tvp;
    protected DefaultResultElementListenerPanel svp;
    private DefaultResultElementListenerPanel ccv;
    private CompoundCandidateView ccvv; //todo get rid of that

    private List<ActiveResultChangedListener> listeners;

    public void addActiveResultChangedListener(ActiveResultChangedListener listener) {
        listeners.add(listener);
    }

    public void removeActiveResultChangedListener(ActiveResultChangedListener listener) {
        listeners.remove(listener);
    }

    public void dispose() {
        ccvv.dispose();
    }

//    public ResultPanel() {
//        this(null);
//    }


    public ResultPanel(final FormulaTable suriusResultElements) {
        super();
        this.listeners = new ArrayList<>();
        this.setToolTipText("Results");

//        centerPane.setBorder(BorderFactory.createEmptyBorder());
        TreeVisualizationPanel tvpTmp = new TreeVisualizationPanel();
//        tvp = new DefaultResultElementListenerPanel(suriusResultElements, new TreeVisualizationPanel(),true);

        SpectraVisualizationPanel svpTmp = new SpectraVisualizationPanel();
//        svp = new DefaultResultElementListenerPanel(suriusResultElements, new SpectraVisualizationPanel(),true);
        ccvv = new CompoundCandidateView();
//        ccv = new DefaultResultElementListenerPanel(suriusResultElements, ccvv,true);

        JPanel jp = new JPanel(new BorderLayout());
        jp.add(suriusResultElements, BorderLayout.NORTH);
        SpectraVisualizationPanel sp = new SpectraVisualizationPanel();
        jp.add(sp, BorderLayout.CENTER);
        suriusResultElements.addActiveResultChangedListener(sp);

        rvp = new ResultsOverviewPanel(this, svpTmp, 1, tvpTmp, 2);
        addTab("Overview", rvp);
        addTab("Spectra view", jp);
//        addTab("Tree view", tvp);
//        addTab("CSI:FingerId", ccv);

        // register listeners //todo moved
//        addActiveResultChangedListener(svp);
//        addActiveResultChangedListener(tvp);

//		this.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),"Results"));
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
