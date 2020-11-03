/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.gui.molecular_formular;

import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.swing.DefaultEventSelectionModel;
import de.unijena.bioinf.GibbsSampling.ZodiacScore;
import de.unijena.bioinf.fingerid.blast.TopCSIScore;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.jjobs.TinyBackgroundJJob;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.mainframe.instance_panel.CompoundList;
import de.unijena.bioinf.ms.gui.mainframe.instance_panel.ExperimentListChangeListener;
import de.unijena.bioinf.ms.gui.table.ActionList;
import de.unijena.bioinf.ms.gui.table.list_stats.DoubleListStats;
import de.unijena.bioinf.projectspace.FormulaResultBean;
import de.unijena.bioinf.projectspace.InstanceBean;
import de.unijena.bioinf.sirius.scores.IsotopeScore;
import de.unijena.bioinf.sirius.scores.SiriusScore;
import de.unijena.bioinf.sirius.scores.TreeScore;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

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
    public final DoubleListStats csiScoreStats = new DoubleListStats();

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

    private JJob<Boolean> backgroundLoader = null;
    private final Lock backgroundLoaderLock = new ReentrantLock();

    private void setData(final InstanceBean ec) {
        //cancel running job if not finished to not waist resources for fetching data that is not longer needed.
        try {
            backgroundLoaderLock.lock();
            this.data = ec; //todo not secure!
            final JJob<Boolean> old = backgroundLoader;
            backgroundLoader = Jobs.runInBackground(new TinyBackgroundJJob<Boolean>() {
                @Override
                protected Boolean compute() throws Exception {

                    if (old != null && !old.isFinished()) {
                        old.cancel(true);
                        old.getResult(); //await cancellation so that nothing strange can happen.
                    }
                    checkForInterruption();
                    if (ec != null && ec.getResults() != null && !ec.getResults().isEmpty()) {
                        checkForInterruption();
                        if (!ec.getResults().equals(elementList)) {
                            checkForInterruption();
                            Jobs.runEDTAndWait(FormulaList.this::intiResultList);
                        }
                    } else {
                        checkForInterruption();
                        Jobs.runEDTAndWait(() -> {
                            if (!elementList.isEmpty()) {
                                elementList.forEach(FormulaResultBean::unregisterProjectSpaceListeners);
                                selectionModel.clearSelection();
                                elementList.clear();
                            } else {
                                // to have notification even if the list is already empty
                                notifyListeners(data, null, elementList, selectionModel);
                            }
                            zodiacScoreStats.update(new double[0]);
                            siriusScoreStats.update(new double[0]);
                            isotopeScoreStats.update(new double[0]);
                            treeScoreStats.update(new double[0]);
                            csiScoreStats.update(new double[0]);
                        });
                    }

                    checkForInterruption();
                    if (!elementList.isEmpty()) {
                        final AtomicInteger index = new AtomicInteger(0);
                        final Function<FormulaResultBean, Boolean> f = getBestFunc();
                        for (FormulaResultBean resultBean : elementList) {
                            if (f.apply(resultBean))
                                break;
                            index.incrementAndGet();
                        }
                        //set selection
                        Jobs.runEDTAndWait(() -> {
                            if (index.get() < elementList.size())
                                selectionModel.setSelectionInterval(index.get(), index.get());
                            else
                                selectionModel.clearSelection();
                        });
                    }
                    return true;
                }
            });
        } finally {
            backgroundLoaderLock.unlock();
        }
    }

    private void intiResultList() {
        elementList.forEach(FormulaResultBean::unregisterProjectSpaceListeners);
        selectionModel.clearSelection();
        elementList.clear();

        final List<FormulaResultBean> r = data.getResults();
        if (r != null && !r.isEmpty()) {
            double[] zscores = new double[r.size()];
            double[] sscores = new double[r.size()];
            double[] iScores = new double[r.size()];
            double[] tScores = new double[r.size()];
            double[] csiScores = new double[r.size()];
            int i = 0;

            for (FormulaResultBean element : r) {
                element.registerProjectSpaceListeners();
                zscores[i] = element.getScoreValueIfNa(ZodiacScore.class, 0d);
                sscores[i] = element.getScoreValue(SiriusScore.class);
                iScores[i] = element.getScoreValue(IsotopeScore.class);
                tScores[i] = element.getScoreValue(TreeScore.class);
                csiScores[i++] = element.getScoreValue(TopCSIScore.class);
            }
            elementList.addAll(r);

            this.zodiacScoreStats.update(zscores);
            this.siriusScoreStats.update(sscores);
            this.isotopeScoreStats.update(iScores);
            this.treeScoreStats.update(tScores);
            this.csiScoreStats.update(csiScores);

            this.explainedIntensity.setMinScoreValue(0).setMaxScoreValue(1)
                    .setScoreSum(this.explainedIntensity.getMax());

            this.explainedPeaks.setMinScoreValue(0).setMaxScoreValue(r.get(0).getNumberOfExplainablePeaks())
                    .setScoreSum(this.explainedPeaks.getMax());
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

    protected Function<FormulaResultBean, Boolean> getBestFunc() {
        return sre -> Double.isFinite(csiScoreStats.getMax()) && !Double.isNaN(csiScoreStats.getMax())
                ? sre.getScoreValue(TopCSIScore.class) >= csiScoreStats.getMax()
                : sre.getScore(ZodiacScore.class)
                .map(it -> it.score() >= zodiacScoreStats.getMax())
                .orElse(sre.getScoreValue(SiriusScore.class) >= siriusScoreStats.getMax());
    }

    protected Function<FormulaResultBean, Double> getRenderScoreFunc() {
        return sre -> sre.getScore(ZodiacScore.class)
                .map(it -> it.score() * 100d)
                .orElse(Math.exp(sre.getScoreValue(SiriusScore.class) - siriusScoreStats.getMax()) / siriusScoreStats.getExpScoreSum() * 100d);
    }
}
