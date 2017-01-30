package de.unijena.bioinf.sirius.gui.mainframe.results;

import de.unijena.bioinf.chemdb.BioFilter;
import de.unijena.bioinf.sirius.gui.dialogs.NoConnectionDialog;
import de.unijena.bioinf.sirius.gui.fingerid.CompoundCandidateView;
import de.unijena.bioinf.sirius.gui.fingerid.FingerIdDialog;
import de.unijena.bioinf.sirius.gui.fingerid.FingerIdTask;
import de.unijena.bioinf.sirius.gui.fingerid.WebAPI;
import de.unijena.bioinf.sirius.gui.mainframe.ActiveResultChangedListener;
import de.unijena.bioinf.sirius.gui.mainframe.results.results_table.SiriusResultTablePanel;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import de.unijena.bioinf.sirius.gui.structure.SiriusResultElement;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;

import static de.unijena.bioinf.sirius.gui.mainframe.MainFrame.MF;

public class ResultPanel extends JPanel implements ListSelectionListener {

    private ResultTreeListModel listModel;
    private JList<SiriusResultElement> resultsJList;
    private SiriusResultTablePanel rvp;
    private TreeVisualizationPanel tvp;
    private SpectraVisualizationPanel svp;
    private CompoundCandidateView ccv;
    private ResultTreeListTextCellRenderer cellRenderer;
    private JTabbedPane centerPane;
    private ExperimentContainer ec;

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

    public ResultPanel() {
        this(null);
    }

    public ResultPanel(ExperimentContainer ec) {
        super();
        this.listeners = new ArrayList<>();
        this.setLayout(new BorderLayout());
        this.setToolTipText("Results");
        this.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(1, 5, 0, 0), "Molecular formulas"));
        this.ec = ec;

        if (this.ec != null) this.listModel = new ResultTreeListModel(ec.getResults());
        else this.listModel = new ResultTreeListModel();
        this.resultsJList = new ResultsTreeList(this.listModel);
        this.listModel.setJList(this.resultsJList);
//		if(this.ec!=null){
//			listRenderer = new ResultTreeListThumbnailCellRenderers(ec.getResults());
//		}else{
//			listRenderer = new ResultTreeListThumbnailCellRenderers(new ArrayList<SiriusResultElement>());
//		}
//		resultsJList.setCellRenderer(listRenderer);
        cellRenderer = new ResultTreeListTextCellRenderer();
        resultsJList.setCellRenderer(cellRenderer);
        resultsJList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        resultsJList.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        resultsJList.setVisibleRowCount(1);
//		resultsJList.getPreferredSize()
        resultsJList.setMinimumSize(new Dimension(0, 45));
        resultsJList.setPreferredSize(new Dimension(0, 45));
        resultsJList.addListSelectionListener(this);


        resultsJList.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getSource().equals(resultsJList)) { // todo do i need that?
                    if (e.getClickCount() == 2) {
                        // Double-click detected
                        int index = resultsJList.locationToIndex(e.getPoint());
                        resultsJList.setSelectedIndex(index);
                        centerPane.setSelectedIndex(2);
                        if (ccv.computationEnabled()) {
                            computeFingerID(false);
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


        JScrollPane listJSP = new JScrollPane(resultsJList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        JPanel temp = new JPanel(new BorderLayout());
        temp.add(listJSP, BorderLayout.NORTH);
        this.add(temp, BorderLayout.NORTH);

        centerPane = new JTabbedPane();
        rvp = new SiriusResultTablePanel(MF.getCompoundView());
        centerPane.add(rvp,"Sirius");


        centerPane.setBorder(BorderFactory.createEmptyBorder());
        tvp = new TreeVisualizationPanel(MF);
        centerPane.addTab("Tree view", tvp);

        svp = new SpectraVisualizationPanel(ec);
        centerPane.addTab("Spectra view", svp);

        ccv = new CompoundCandidateView(MF);
        centerPane.addTab("CSI:FingerId", ccv);
        ccv.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (ccv.computationEnabled()) {
                    computeFingerID(true);
                }
            }
        });

        this.add(centerPane, BorderLayout.CENTER);

        // register listeners
        addActiveResultChangedListener(svp);
        addActiveResultChangedListener(tvp);

//		this.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),"Results"));
    }

    public void changeData(final ExperimentContainer ec) {
        int i = Math.max(resultsJList.getSelectedIndex(), 0);
        cellRenderer.ec = ec;
        SiriusResultElement sre = null;
        resultsJList.removeListSelectionListener(this);
        if (ec != null && ec.getResults() != null && !ec.getResults().isEmpty()) {
            this.listModel.setData(ec.getResults());
            if (this.listModel.getSize() > 0) {
                if (this.ec != ec) {
                    this.ec = ec;
                    this.resultsJList.setSelectedIndex(0);
                } else {
                    resultsJList.setSelectedIndex(i);
                }
                sre = ec.getResults().get(this.resultsJList.getSelectedIndex());
            }
        } else {
            this.listModel.setData(new ArrayList<SiriusResultElement>());
        }
        resultsJList.addListSelectionListener(this);
        final SiriusResultElement element = sre;
        ccv.changeData(ec, sre);

        ////////////////
        // TODO: put all the stuff about into listeners
        ////////////////
        for (ActiveResultChangedListener listener : listeners) {
            listener.resultsChanged(ec, sre);
        }


    }

    public void select(SiriusResultElement sre, boolean fireEvent) {
        if (fireEvent) resultsJList.setSelectedValue(sre, true);
        if (sre == null) {
            tvp.showTree(null);
            ccv.changeData(ec, sre);
        } else {
            tvp.showTree(sre);
            ccv.changeData(ec, sre);
        }
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
        SiriusResultElement resultElement = resultsJList.getSelectedValue();

        //calculate csi
        final FingerIdDialog dialog = new FingerIdDialog(MF, MF.getCsiFingerId(), resultElement.getFingerIdData(), searchAllEnabled);

        final int returnState = dialog.run();
        if (returnState != FingerIdDialog.CANCELED) {
            if (returnState == FingerIdDialog.COMPUTE_ALL) {
                MF.getCsiFingerId().compute(ec, dialog.isBio());
            } else {
                List<SiriusResultElement> selected = resultsJList.getSelectedValuesList();
                java.util.List<FingerIdTask> tasks = new ArrayList<>(selected.size());
                for (SiriusResultElement element : selected) {
                    if (element.getCharge()>0 || element.getResult().getResolvedTree().numberOfEdges() > 0)
                        tasks.add(new FingerIdTask(dialog.isBio(), ec, element));
                }
                MF.getCsiFingerId().computeAll(tasks);
            }
            ccv.changeData(ec, resultElement);
        }
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        SiriusResultElement sre = this.resultsJList.getSelectedValue();
        select(sre, false);
    }

}
