package de.unijena.bioinf.ms.gui.molecular_formular;

import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.swing.DefaultEventSelectionModel;
import de.unijena.bioinf.GibbsSampling.ZodiacScore;
import de.unijena.bioinf.ms.gui.mainframe.instance_panel.CompoundList;
import de.unijena.bioinf.ms.gui.mainframe.instance_panel.ExperimentListChangeListener;
import de.unijena.bioinf.projectspace.InstanceBean;
import de.unijena.bioinf.projectspace.FormulaResultBean;
import de.unijena.bioinf.ms.gui.table.ActionList;
import de.unijena.bioinf.ms.gui.table.list_stats.DoubleListStats;
import de.unijena.bioinf.sirius.scores.IsotopeScore;
import de.unijena.bioinf.sirius.scores.SiriusScore;
import de.unijena.bioinf.sirius.scores.TreeScore;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class FormulaList extends ActionList<FormulaResultBean, InstanceBean> {
    public final FormulaScoreListStats zodiacScoreStats = new FormulaScoreListStats();
    public final FormulaScoreListStats siriusScoreStats = new FormulaScoreListStats();
    public final DoubleListStats isotopeScoreStats = new DoubleListStats();
    public final DoubleListStats treeScoreStats = new DoubleListStats();
    public final DoubleListStats explainedPeaks = new DoubleListStats();
    public final DoubleListStats explainedIntensity = new DoubleListStats();

    public FormulaList(final CompoundList compoundList) {
        super(FormulaResultBean.class);

        DefaultEventSelectionModel<InstanceBean> m = compoundList.getCompoundListSelectionModel();
        if (!m.isSelectionEmpty()) {
            setData(m.getSelected().get(0));
        } else {
            setData(null);
        }

        //this is the selection refresh, element changes are detected by eventlist
        compoundList.addChangeListener(new ExperimentListChangeListener() {
            @Override
            public void listChanged(ListEvent<InstanceBean> event, DefaultEventSelectionModel<InstanceBean> selection) {
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
            public void listSelectionChanged(DefaultEventSelectionModel<InstanceBean> selection) {
                if (!selection.isSelectionEmpty())
                    setData(selection.getSelected().get(0));
                else
                    setData(null);
            }
        });
    }

    private void setData(final InstanceBean ec) {
        this.data = ec;
        if (this.data != null && this.data.getResults() != null && !this.data.getResults().isEmpty()) {
            if (!this.data.getResults().equals(elementList))
                intiResultList();

        } else {
            elementList.forEach(FormulaResultBean::unregisterProjectSpaceListeners);
            selectionModel.clearSelection();
            elementList.clear();
            zodiacScoreStats.update(new double[0]);
            siriusScoreStats.update(new double[0]);
            isotopeScoreStats.update(new double[0]);
            treeScoreStats.update(new double[0]);
        }

        //set selection
        FormulaResultBean sre = null;
        if (!elementList.isEmpty()) {
            //we have sorted results so best hit is always at 0
            selectionModel.setSelectionInterval(0, 0);
            sre = elementList.get(selectionModel.getMinSelectionIndex());
        }

        selectionModel.setValueIsAdjusting(false);
        notifyListeners(this.data, sre, elementList, selectionModel);
    }

    private void intiResultList() {
        elementList.forEach(FormulaResultBean::unregisterProjectSpaceListeners);
        selectionModel.clearSelection();
        elementList.clear();

        List<FormulaResultBean> r = data.getResults();
        if (r != null && !r.isEmpty()) {
            double[] zscores = new double[r.size()];
            double[] sscores = new double[r.size()];
            double[] iScores = new double[r.size()];
            double[] tScores = new double[r.size()];
            int i = 0;
            for (FormulaResultBean element : r) {
                element.registerProjectSpaceListeners();
                elementList.add(element);
                zscores[i] = element.getScoreValue(ZodiacScore.class);
                sscores[i] = element.getScoreValue(SiriusScore.class);
                iScores[i] = element.getScoreValue(IsotopeScore.class);
                tScores[i++] = element.getScoreValue(TreeScore.class);
            }

            this.zodiacScoreStats.update(zscores);
            this.siriusScoreStats.update(sscores);
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


    public List<FormulaResultBean> getSelectedValues() {
        List<FormulaResultBean> selected = new ArrayList<>();
        for (int i = selectionModel.getMinSelectionIndex(); i <= selectionModel.getMaxSelectionIndex(); i++) {
            if (selectionModel.isSelectedIndex(i)) {
                selected.add(elementList.get(i));
            }
        }
        return selected;
    }
}
