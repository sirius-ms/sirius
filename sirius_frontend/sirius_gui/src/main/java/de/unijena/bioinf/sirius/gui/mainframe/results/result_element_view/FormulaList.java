package de.unijena.bioinf.sirius.gui.mainframe.results.result_element_view;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 30.01.17.
 */

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.ObservableElementList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.swing.DefaultEventSelectionModel;
import de.unijena.bioinf.sirius.gui.mainframe.experiments.ExperimentList;
import de.unijena.bioinf.sirius.gui.mainframe.experiments.ExperimentListChangeListener;
import de.unijena.bioinf.sirius.gui.mainframe.results.ActiveResultChangedListener;
import de.unijena.bioinf.sirius.gui.mainframe.results.ActiveResults;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import de.unijena.bioinf.sirius.gui.structure.SiriusResultElement;
import de.unijena.bioinf.sirius.gui.utils.list_stats.DoubleListStats;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class FormulaList implements ActiveResults {
    private final List<ActiveResultChangedListener> listeners = new LinkedList<>();

    ObservableElementList<SiriusResultElement> resultList;
    DefaultEventSelectionModel<SiriusResultElement> selectionModel;

    private ExperimentContainer ec = null;
    public final FormulaScoreListStats scoreStats = new FormulaScoreListStats();
    public final DoubleListStats isotopeScoreStats = new DoubleListStats();
    public final DoubleListStats treeScoreStats = new DoubleListStats();

    public FormulaList(final ExperimentList compundList) {
        super();
        resultList = new ObservableElementList<>(new BasicEventList<SiriusResultElement>(), GlazedLists.beanConnector(SiriusResultElement.class));
        selectionModel = new DefaultEventSelectionModel<>(resultList);
        selectionModel.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        DefaultEventSelectionModel<ExperimentContainer> m = compundList.getCompoundListSelectionModel();
        if (!m.isSelectionEmpty()) {
            setData(m.getSelected().get(0));
        } else {
            setData(null);
        }

        //this is the selection refresh, element chages are detected by eventlist
        compundList.addChangeListener(new ExperimentListChangeListener() {
            @Override
            public void listChanged(ListEvent<ExperimentContainer> event, DefaultEventSelectionModel<ExperimentContainer> selection) {
                if (!selection.isSelectionEmpty()) {
                    while (event.next()) {
                        if (selection.isSelectedIndex(event.getIndex())) {
                            setData(event.getSourceList().get(event.getIndex()));
                            return;
                        }
                    }
                }
            }

            @Override
            public void listSelectionChanged(DefaultEventSelectionModel<ExperimentContainer> selection) {
                if (!selection.isSelectionEmpty())
                    setData(selection.getSelected().get(0));
                else
                    setData(null);
            }
        });

        selectionModel.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!selectionModel.getValueIsAdjusting()) {
                    if (selectionModel.isSelectionEmpty() || resultList == null || resultList.isEmpty())
                        notifyListeners(ec, null, resultList, selectionModel);
                    else
                        notifyListeners(ec, resultList.get(selectionModel.getMinSelectionIndex()), resultList, selectionModel);
                }
            }
        });

        resultList.addListEventListener(new ListEventListener<SiriusResultElement>() {
            @Override
            public void listChanged(ListEvent<SiriusResultElement> listChanges) {
                if (!selectionModel.getValueIsAdjusting()) {
                    if (!selectionModel.isSelectionEmpty() && resultList != null && !resultList.isEmpty()) {
                        while (listChanges.next()) {
                            if (selectionModel.getMinSelectionIndex() == listChanges.getIndex()) {
                                notifyListeners(ec, resultList.get(selectionModel.getMinSelectionIndex()), resultList, selectionModel);
                                return;
                            }
                        }
                    }
                }
            }
        });
    }

    private void setData(final ExperimentContainer ec) {
        this.ec = ec;
        resultList.getReadWriteLock().writeLock().lock();
        if (this.ec != null && this.ec.getResults() != null && !this.ec.getResults().isEmpty()) {
            if (!this.ec.getResults().equals(resultList)) {
                selectionModel.clearSelection();
                resultList.clear();
//                resultList.addAll(this.ec.getResults());
                intiResultList();
            }
        } else {
            selectionModel.clearSelection();
            resultList.clear();
            scoreStats.update(new double[0]);
            isotopeScoreStats.update(new double[0]);
            treeScoreStats.update(new double[0]);
        }

        //set selection
        SiriusResultElement sre = null;
        if (!resultList.isEmpty()) {
            selectionModel.setSelectionInterval(this.ec.getBestHitIndex(), this.ec.getBestHitIndex());
            sre = resultList.get(selectionModel.getMinSelectionIndex());
        }

        resultList.getReadWriteLock().writeLock().unlock();
        selectionModel.setValueIsAdjusting(false);
        notifyListeners(this.ec, sre, resultList, selectionModel);
    }

    private void intiResultList() {
        List<SiriusResultElement> r = ec.getResults();
        double[] scores = new double[r.size()];
        double[] iScores = new double[r.size()];
        double[] tScores = new double[r.size()];
        int i = 0;
        for (SiriusResultElement element : r) {
            resultList.add(element);
            scores[i] = element.getScore();
            iScores[i] = element.getResult().getIsotopeScore();
            tScores[i++] = element.getResult().getTreeScore();
        }

        this.scoreStats.update(scores);
        this.isotopeScoreStats.update(iScores);
        this.treeScoreStats.update(tScores);
    }

    public ObservableElementList<SiriusResultElement> getResultList() {
        return resultList;
    }

    public ListSelectionModel getResultListSelectionModel() {
        return selectionModel;
    }

    public List<SiriusResultElement> getSelecteValues() {
        List<SiriusResultElement> selected = new ArrayList<>();
        for (int i = selectionModel.getMinSelectionIndex(); i <= selectionModel.getMaxSelectionIndex(); i++) {
            if (selectionModel.isSelectedIndex(i)) {
                selected.add(resultList.get(i));
            }
        }
        return selected;
    }

    public void addActiveResultChangedListener(ActiveResultChangedListener listener) {
        listeners.add(listener);
    }

    public void removeActiveResultChangedListener(ActiveResultChangedListener listener) {
        listeners.remove(listener);
    }

    protected void notifyListeners(ExperimentContainer ec, SiriusResultElement siriusResultElement, List<SiriusResultElement> sre, ListSelectionModel selections) {
        for (ActiveResultChangedListener listener : listeners) {
            listener.resultsChanged(ec, siriusResultElement, sre, selections);
        }
    }
}
