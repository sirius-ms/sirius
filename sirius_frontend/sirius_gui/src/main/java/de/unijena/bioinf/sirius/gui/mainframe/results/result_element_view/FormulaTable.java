package de.unijena.bioinf.sirius.gui.mainframe.results.result_element_view;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 30.01.17.
 */

import ca.odell.glazedlists.event.ListEvent;
import de.unijena.bioinf.sirius.gui.actions.SiriusActions;
import de.unijena.bioinf.sirius.gui.mainframe.ExperimentListChangeListener;
import de.unijena.bioinf.sirius.gui.mainframe.ExperimentListPanel;
import de.unijena.bioinf.sirius.gui.mainframe.results.ActiveResultChangedListener;
import de.unijena.bioinf.sirius.gui.mainframe.results.ActiveResults;
import de.unijena.bioinf.sirius.gui.mainframe.results.ResultTreeListModel;
import de.unijena.bioinf.sirius.gui.mainframe.results.ResultTreeListTextCellRenderer;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import de.unijena.bioinf.sirius.gui.structure.SiriusResultElement;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class FormulaTable extends JScrollPane implements ActiveResults {
    protected java.util.List<ActiveResultChangedListener> listeners = new ArrayList<>();
    private ResultTreeListModel resultList;
    private JList<SiriusResultElement> resultListView;

    public FormulaTable(final ExperimentListPanel compundList) {
        super(VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_ALWAYS);
//        setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(1, 5, 0, 0), "Molecular formulas"));

        resultList = new ResultTreeListModel(new ArrayList<SiriusResultElement>());//new ObservableElementList<>(new BasicEventList<SiriusResultElement>(), GlazedLists.beanConnector(SiriusResultElement.class));
        resultListView = new JList<>(resultList);
        resultList.setJList(resultListView);

        ResultTreeListTextCellRenderer cellRenderer = new ResultTreeListTextCellRenderer();
        resultListView.setCellRenderer(cellRenderer);
        resultListView.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        resultListView.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        resultListView.setVisibleRowCount(1);
        resultListView.setMinimumSize(new Dimension(0, 45));
        resultListView.setPreferredSize(new Dimension(0, 45));

        add(resultListView);

        setData(compundList.getCompoundListView().getSelectedValue());

        //this is the selsction refresh, element chages are detected by eventlist
        compundList.addChangeListener(new ExperimentListChangeListener() {
            @Override
            public void listChanged(final ListEvent<ExperimentContainer> event, final JList<ExperimentContainer> source) {
                if (!source.isSelectionEmpty()) {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            while (event.next()) {
                                if (event.getType() == ListEvent.UPDATE && source.getMinSelectionIndex() == event.getIndex()) {
                                    System.out.println("DATA Listener");
                                    setData(source.getSelectedValue());
                                    return;
                                }
                            }
                        }
                    });
                }
            }

            @Override
            public void listSelectionChanged(final JList<ExperimentContainer> source) {
                System.out.println("Slection Listener");
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        setData(source.getSelectedValue());
                    }
                });

            }
        });

        resultListView.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    // Double-click detected
                    int index = resultListView.locationToIndex(e.getPoint());
                    resultListView.setSelectedIndex(index);
                    SiriusActions.COMPUTE_CSI_LOCAL.getInstance().actionPerformed(new ActionEvent(resultListView, 112, SiriusActions.COMPUTE_CSI_LOCAL.name()));
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


    }

    private void setData(ExperimentContainer ec) {
        System.out.println("SetData");
        resultList.setData(new ArrayList<SiriusResultElement>());
        if (ec != null) {
            if (ec.getResults() != null && !ec.getResults().isEmpty()) {
                System.out.println("SetDataREALLY");
                resultList.setData(ec.getResults());
                if (resultListView.getModel().getSize() > 0) {
                    resultListView.setSelectedValue(ec.getBestHit(), true);
                    System.out.println(resultListView.getSelectedIndex());
                }
            }
            notifyListeners(ec, resultListView.getSelectedValue());
        }
    }

    public ResultTreeListModel getResultList() {
        return resultList;
    }

    public JList<SiriusResultElement> getResultListView() {
        return resultListView;
    }

    public void addActiveResultChangedListener(ActiveResultChangedListener listener) {
        listeners.add(listener);
    }

    public void removeActiveResultChangedListener(ActiveResultChangedListener listener) {
        listeners.remove(listener);
    }

    protected void notifyListeners(ExperimentContainer ec, SiriusResultElement sre) {
        for (ActiveResultChangedListener listener : listeners) {
            listener.resultsChanged(ec, sre);
        }
    }
}
