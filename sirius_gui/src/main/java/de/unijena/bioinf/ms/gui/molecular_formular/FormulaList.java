/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.gui.molecular_formular;

import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.swing.DefaultEventSelectionModel;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.jjobs.TinyBackgroundJJob;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.mainframe.instance_panel.CompoundList;
import de.unijena.bioinf.ms.gui.mainframe.instance_panel.ExperimentListChangeListener;
import de.unijena.bioinf.ms.gui.table.ActionList;
import de.unijena.bioinf.ms.gui.table.list_stats.DoubleListStats;
import de.unijena.bioinf.projectspace.FormulaResultBean;
import de.unijena.bioinf.projectspace.InstanceBean;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import lombok.Getter;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

/**
 * @author Markus Fleischauer
 */
public class FormulaList extends ActionList<FormulaResultBean, InstanceBean> {
    // this is to be backward compatible with SIRIUS < 6.1 projects where no normalized scores is available.
    // In that case we use the old, less precise score normalization in the frontend.
    @Deprecated
    private final Object2DoubleMap<String> idToOldNormalizedScore = new Object2DoubleOpenHashMap<>();

    public final DoubleListStats zodiacScoreStats = new DoubleListStats();
    public final DoubleListStats siriusScoreStats = new DoubleListStats();
    public final DoubleListStats isotopeScoreStats = new DoubleListStats();
    public final DoubleListStats treeScoreStats = new DoubleListStats();
    public final DoubleListStats explainedPeaks = new DoubleListStats();
    public final DoubleListStats explainedIntensity = new DoubleListStats();

    public FormulaList(final CompoundList compoundList) {
        super(FormulaResultBean.class);

        DefaultEventSelectionModel<InstanceBean> m = compoundList.getCompoundListSelectionModel();
        if (!m.isSelectionEmpty()) {
            changeData(m.getSelected().get(0));
        } else {
            changeData(null);
        }

        //this is the selection refresh, element changes are detected by eventlist
        compoundList.addChangeListener(new ExperimentListChangeListener() {
            @Override
            public void listChanged(ListEvent<InstanceBean> event, DefaultEventSelectionModel<InstanceBean> selection, int fullSize) {
                if (!selection.isSelectionEmpty()) {
                    while (event.next()) {
                        if (selection.isSelectedIndex(event.getIndex())) {
                            changeData(event.getSourceList().get(event.getIndex()));
                            return;
                        }
                    }
                }
            }

            @Override
            public void listSelectionChanged(DefaultEventSelectionModel<InstanceBean> selection, List<InstanceBean> selected, List<InstanceBean> deselected, int fullSize) {
                if (!selected.isEmpty())
                    changeData(selected.getFirst());
                else
                    changeData(null);
            }
        });
    }

    private JJob<Boolean> backgroundLoader = null;
    private final Lock backgroundLoaderLock = new ReentrantLock();

    private void changeData(final InstanceBean ec) {
        //cancel running job if not finished to not waist resources for fetching data that is not longer needed.
        try {
            backgroundLoaderLock.lock();
            setData(ec);
            final JJob<Boolean> old = backgroundLoader;
            backgroundLoader = Jobs.runInBackground(new TinyBackgroundJJob<>() {
                @Override
                protected Boolean compute() throws Exception {

                    if (old != null && !old.isFinished()) {
                        old.cancel(true);
                        old.getResult(); //await cancellation so that nothing strange can happen.
                    }
                    checkForInterruption();
                    //fetch data
                    final List<FormulaResultBean> candidates = ec != null ? readDataByFunction(InstanceBean::getFormulaCandidates) : null;
                    checkForInterruption();
                    //loading data
                    intiResultList(candidates);
                    checkForInterruption();

                    //refreshing selection
                    if (!elementList.isEmpty()) {
                        final AtomicInteger index = new AtomicInteger(0);
                        final Function<FormulaResultBean, Boolean> f = getBestHitFunction();
                        for (FormulaResultBean resultBean : elementList) {
                            if (f.apply(resultBean))
                                break;
                            index.incrementAndGet();
                        }
                        //last change to interrupt before propagating change to edt
                        checkForInterruption();

                        //update selection
                        Jobs.runEDTAndWait(() -> {
                            if (index.get() < elementList.size())
                                elementListSelectionModel.setSelectionInterval(index.get(), index.get());
                            else
                                elementListSelectionModel.clearSelection();
                        });
                    } else {
                        //last change to interrupt before propagating change to edt
                        checkForInterruption();
                        Jobs.runEDTAndWait(() -> elementListSelectionModel.clearSelection());
                    }
                    return true;
                }
            });
        } finally {
            backgroundLoaderLock.unlock();
        }
    }

    private void intiResultList(List<FormulaResultBean> candidates) throws InterruptedException, InvocationTargetException {
        Jobs.runEDTAndWait(() -> {
            elementList.forEach(FormulaResultBean::unregisterProjectSpaceListeners);
            idToOldNormalizedScore.clear();
            if (candidates != null && !candidates.isEmpty()) { //refill case
                elementListSelectionModel.clearSelection();

                double[] zscores = new double[candidates.size()];
                double[] sscores = new double[candidates.size()];
                double[] iScores = new double[candidates.size()];
                double[] tScores = new double[candidates.size()];
                int i = 0;


                for (FormulaResultBean fc : candidates) {
                    zscores[i] = fc.getZodiacScore().orElse(0d); //why do we want 0 here?
                    sscores[i] = fc.getSiriusScoreNormalized().orElse(0d);
                    iScores[i] = fc.getIsotopeScore().orElse(Double.NEGATIVE_INFINITY);
                    tScores[i] = fc.getTreeScore().orElse(Double.NEGATIVE_INFINITY);
                    i++;
                }

                if (Arrays.stream(sscores).allMatch(score -> Double.compare(score, 0d) == 0))
                    computeOldNormalizedScoresAsFallback(candidates, sscores);

                refillElements(candidates);

                this.zodiacScoreStats.update(zscores);
                this.siriusScoreStats.update(sscores);
                this.isotopeScoreStats.update(iScores);
                this.treeScoreStats.update(tScores);

                this.explainedIntensity.setMinScoreValue(0).setMaxScoreValue(1)
                        .setScoreSum(this.explainedIntensity.getMax());

                this.explainedPeaks.setMinScoreValue(0).setMaxScoreValue(candidates.get(0).getNumOfExplainablePeaks().orElseThrow())
                        .setScoreSum(this.explainedPeaks.getMax());
            } else { //clear case

                if (!elementList.isEmpty()) {
                    elementListSelectionModel.clearSelection();
                    refillElements(null);
                } else {
                    // to have notification even if the list is already empty
                    readDataByConsumer(data -> notifyListeners(data, null, elementList, elementListSelectionModel));
                }
                zodiacScoreStats.update(new double[0]);
                siriusScoreStats.update(new double[0]);
                isotopeScoreStats.update(new double[0]);
                treeScoreStats.update(new double[0]);
            }
        });
    }

    @Deprecated
    private void computeOldNormalizedScoresAsFallback(List<FormulaResultBean> formulaCandidates, double[] sscores) {
        System.out.println("Missing Normalized SiriusScores detected. Computing them in GUI. This is likely caused by old project spaces.");
        double maxSiriusScore = formulaCandidates.stream().flatMap(sb -> sb.getSiriusScore().stream())
                .mapToDouble(Double::doubleValue).max().orElse(Double.POSITIVE_INFINITY);
        double sumSiriusScoreExp = formulaCandidates.stream()
                .flatMap(sb -> sb.getSiriusScore().stream())
                .mapToDouble(Double::doubleValue)
                .map(score -> Math.exp(score - maxSiriusScore)).sum();

        int i = 0;
        for (FormulaResultBean formulaCandidate : formulaCandidates){
            sscores[i] = Math.exp(formulaCandidate.getSiriusScore().get() - maxSiriusScore) / sumSiriusScoreExp;
            idToOldNormalizedScore.put(formulaCandidate.getFormulaId(), sscores[i]);
            i++;
        }
    }

    public List<FormulaResultBean> getSelectedValues() {
        List<FormulaResultBean> selected = new ArrayList<>();
        for (int i = elementListSelectionModel.getMinSelectionIndex(); i <= elementListSelectionModel.getMaxSelectionIndex(); i++) {
            if (elementListSelectionModel.isSelectedIndex(i)) {
                selected.add(elementList.get(i));
            }
        }
        return selected;
    }

    @Deprecated
    @Getter
    private final Function<FormulaResultBean, Double> fallBackNormalizedSiriusScoreFunction =
            sre -> idToOldNormalizedScore.getOrDefault(sre.getFormulaId(), Double.NaN);

    @Getter
    private final Function<FormulaResultBean, FormulaListTextCellRenderer.RenderScore> renderScoreFunction =
            sre -> sre.getZodiacScore()
                    .map(s -> new FormulaListTextCellRenderer.RenderScore(s * 100d, "Zodiac"))
                    .orElseGet(() -> new FormulaListTextCellRenderer.RenderScore(sre.getSiriusScoreNormalized()
                            .map(s -> s * 100d)
                            .orElseGet(() -> fallBackNormalizedSiriusScoreFunction.apply(sre)), "SIRIUS"));

    @Getter
    private final Function<FormulaResultBean, Boolean> bestHitFunction =
            sre -> Optional.ofNullable(sre)
                    .map(FormulaResultBean::getParentInstance)
                    .filter(ib -> ib.getStructureAnnotation().isPresent())
                    .flatMap(InstanceBean::getFormulaAnnotation)
                    .map(it -> Objects.equals(it.getFormulaId(), sre.getFormulaId()))
                    .orElse(false);
}