package de.unijena.bioinf.sirius.gui.mainframe.results;

import de.unijena.bioinf.chemdb.BioFilter;
import de.unijena.bioinf.sirius.gui.configs.ConfigStorage;
import de.unijena.bioinf.sirius.gui.dialogs.NoConnectionDialog;
import de.unijena.bioinf.sirius.gui.fingerid.CompoundCandidateView;
import de.unijena.bioinf.sirius.gui.fingerid.FingerIdDialog;
import de.unijena.bioinf.sirius.gui.fingerid.FingerIdTask;
import de.unijena.bioinf.sirius.gui.fingerid.WebAPI;
import de.unijena.bioinf.sirius.gui.mainframe.MainFrame;
import de.unijena.bioinf.sirius.gui.mainframe.results.result_element_view.DefaultResultElementListenerPanel;
import de.unijena.bioinf.sirius.gui.mainframe.results.result_element_view.FormulaTable;
import de.unijena.bioinf.sirius.gui.mainframe.results.results_table.SiriusResultTablePanel;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import de.unijena.bioinf.sirius.gui.structure.SiriusResultElement;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;

public class ResultPanel extends JTabbedPane implements ActiveResults {
import static de.unijena.bioinf.sirius.gui.mainframe.MainFrame.MF;

public class ResultPanel extends JPanel implements ListSelectionListener {


    private SiriusResultTablePanel rvp;
    private DefaultResultElementListenerPanel tvp;
    private DefaultResultElementListenerPanel svp;
    private DefaultResultElementListenerPanel ccv;
    private CompoundCandidateView ccvv; //todo get rid of that
    private MainFrame owner;
    private ExperimentContainer ec;
    private FormulaTable suriusResultElements;

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

    public ResultPanel() {
        this(null);
    }

    public ResultPanel(ExperimentContainer ec) {
        super();
        this.listeners = new ArrayList<>();
        this.setToolTipText("Results");
        this.ec = ec;

        suriusResultElements = new FormulaTable(this.owner.compoundList);
        final JList<SiriusResultElement> resultsJList =  suriusResultElements.getResultsJList();
        resultsJList.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getSource().equals(resultsJList)) { // todo do i need that? every fired to this listener should be from resultJlist
                    if (e.getClickCount() == 2) {
                        // Double-click detected
                        int index = resultsJList.locationToIndex(e.getPoint());
                        resultsJList.setSelectedIndex(index);
                       setSelectedIndex(3);
                        if (ccvv.computationEnabled()) {
                            computeFingerID(false);//todo some nice listener thing??
                        }
                    }
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
            }

            @Override
            public void mouseReleased(MouseEvent e) {
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }
        });


        rvp = new SiriusResultTablePanel(owner.compoundList);
        add(rvp,"Sirius");


//        centerPane.setBorder(BorderFactory.createEmptyBorder());
        tvp = new DefaultResultElementListenerPanel(suriusResultElements,new TreeVisualizationPanel(owner, config));
        addTab("Tree view", tvp);

        svp = new DefaultResultElementListenerPanel(suriusResultElements,new SpectraVisualizationPanel(ec));
        addTab("Spectra view", svp);

        ccvv = new CompoundCandidateView(owner);
        ccv = new DefaultResultElementListenerPanel(suriusResultElements,ccvv);
        addTab("CSI:FingerId", ccv);
        ccvv.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (ccvv.computationEnabled()) {
                    computeFingerID(true);
                }
            }
        });

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
        for (ActiveResultChangedListener listener : listeners) {
            listener.resultsChanged(ec, sre);
        }
    }

    public void computeFingerID(boolean searchAllEnabled) {
        //Test connection
        if (!WebAPI.getRESTDb(BioFilter.ALL).testConnection()) {
            new NoConnectionDialog(MF);
            return;
        }
        SiriusResultElement resultElement = suriusResultElements.getResultsJList().getSelectedValue();

        //calculate csi
        final FingerIdDialog dialog = new FingerIdDialog(MF, MF.getCsiFingerId(), resultElement.getFingerIdData(), searchAllEnabled);

        final int returnState = dialog.run();
        if (returnState != FingerIdDialog.CANCELED) {
            if (returnState == FingerIdDialog.COMPUTE_ALL) {
                MF.getCsiFingerId().compute(ec, dialog.isBio());
            } else {
                List<SiriusResultElement> selected = suriusResultElements.getResultsJList().getSelectedValuesList();
                java.util.List<FingerIdTask> tasks = new ArrayList<>(selected.size());
                for (SiriusResultElement element : selected) {
                    if (element.getCharge()>0 || element.getResult().getResolvedTree().numberOfEdges() > 0)
                        tasks.add(new FingerIdTask(dialog.isBio(), ec, element));
                }
                MF.getCsiFingerId().computeAll(tasks);
            }
        }
    }



}
