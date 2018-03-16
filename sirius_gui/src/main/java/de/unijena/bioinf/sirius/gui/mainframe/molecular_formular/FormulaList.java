package de.unijena.bioinf.sirius.gui.mainframe.molecular_formular;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 30.01.17.
 */

import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.swing.DefaultEventSelectionModel;
import de.unijena.bioinf.sirius.gui.mainframe.experiments.ExperimentList;
import de.unijena.bioinf.sirius.gui.mainframe.experiments.ExperimentListChangeListener;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import de.unijena.bioinf.sirius.gui.structure.SiriusResultElement;
import de.unijena.bioinf.sirius.gui.table.ActionList;
import de.unijena.bioinf.sirius.gui.table.list_stats.DoubleListStats;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class FormulaList extends ActionList<SiriusResultElement, ExperimentContainer> {
    public final FormulaScoreListStats scoreStats = new FormulaScoreListStats();
    public final DoubleListStats isotopeScoreStats = new DoubleListStats();
    public final DoubleListStats treeScoreStats = new DoubleListStats();
    public final DoubleListStats explainedPeaks = new DoubleListStats();
    public final DoubleListStats explainedIntensity = new DoubleListStats();

    public FormulaList(final ExperimentList compoundList) {
        super(SiriusResultElement.class);

        DefaultEventSelectionModel<ExperimentContainer> m = compoundList.getCompoundListSelectionModel();
        if (!m.isSelectionEmpty()) {
            setData(m.getSelected().get(0));
        } else {
            setData(null);
        }

        //this is the selection refresh, element chages are detected by eventlist
        compoundList.addChangeListener(new ExperimentListChangeListener() {
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


    }

    private void setData(final ExperimentContainer ec) {
        this.data = ec;
        if (this.data != null && this.data.getResults() != null && !this.data.getResults().isEmpty()) {
            if (!this.data.getResults().equals(elementList)) {
                selectionModel.clearSelection();
                elementList.clear();
                intiResultList();
            }
        } else {
            selectionModel.clearSelection();
            elementList.clear();
            scoreStats.update(new double[0]);
            isotopeScoreStats.update(new double[0]);
            treeScoreStats.update(new double[0]);
        }

        //set selection
        SiriusResultElement sre = null;
        if (!elementList.isEmpty()) {
            selectionModel.setSelectionInterval(this.data.getBestHitIndex(), this.data.getBestHitIndex());
            sre = elementList.get(selectionModel.getMinSelectionIndex());
        }

        selectionModel.setValueIsAdjusting(false);
        notifyListeners(this.data, sre, elementList, selectionModel);
    }

    private void intiResultList() {
        List<SiriusResultElement> r = data.getResults();
        if (r != null && !r.isEmpty()) {
            double[] scores = new double[r.size()];
            double[] iScores = new double[r.size()];
            double[] tScores = new double[r.size()];
            int i = 0;
            for (SiriusResultElement element : r) {
                elementList.add(element);
                scores[i] = element.getScore();
                iScores[i] = element.getResult().getIsotopeScore();
                tScores[i++] = element.getResult().getTreeScore();
            }

            this.scoreStats.update(scores);
            this.isotopeScoreStats.update(iScores);
            this.treeScoreStats.update(tScores);


            this.explainedIntensity.setMinScoreValue(0);
            this.explainedIntensity.setMaxScoreValue(1);
            this.explainedIntensity.setScoreSum(this.explainedIntensity.getMax());

            this.explainedPeaks.setMinScoreValue(0);
            this.explainedPeaks.setMaxScoreValue(r.get(0).getNumberOfExplainablePeaks());
            this.explainedPeaks.setScoreSum(this.explainedPeaks.getMax());
        }

    }


    public List<SiriusResultElement> getSelectedValues() {
        List<SiriusResultElement> selected = new ArrayList<>();
        for (int i = selectionModel.getMinSelectionIndex(); i <= selectionModel.getMaxSelectionIndex(); i++) {
            if (selectionModel.isSelectedIndex(i)) {
                selected.add(elementList.get(i));
            }
        }
        return selected;
    }
}
