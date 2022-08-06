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

package de.unijena.bioinf.ms.gui.fingerid;

import de.unijena.bioinf.fingerid.FingerprintResult;
import de.unijena.bioinf.fingerid.blast.FBCandidateFingerprints;
import de.unijena.bioinf.fingerid.blast.FBCandidates;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.jjobs.TinyBackgroundJJob;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.molecular_formular.FormulaList;
import de.unijena.bioinf.ms.gui.table.ActionList;
import de.unijena.bioinf.ms.gui.table.ActiveElementChangedListener;
import de.unijena.bioinf.ms.gui.table.list_stats.DoubleListStats;
import de.unijena.bioinf.projectspace.FormulaResult;
import de.unijena.bioinf.projectspace.FormulaResultBean;
import de.unijena.bioinf.projectspace.InstanceBean;
import de.unijena.bioinf.projectspace.fingerid.FBCandidateFingerprintsTopK;
import de.unijena.bioinf.projectspace.fingerid.FBCandidatesTopK;

import javax.swing.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

/**
 * Created by fleisch on 15.05.17.
 */
public class StructureList extends ActionList<FingerprintCandidateBean, Set<FormulaResultBean>> implements ActiveElementChangedListener<FormulaResultBean, InstanceBean> {
    public final DoubleListStats csiScoreStats;
    public final DoubleListStats logPStats;
    public final DoubleListStats tanimotoStats;

    private final AtomicBoolean loadAll = new AtomicBoolean(false);

    public StructureList(final FormulaList source) {
        this(source, DataSelectionStrategy.ALL_SELECTED);
    }

    public StructureList(final FormulaList source, DataSelectionStrategy strategy) {
        super(FingerprintCandidateBean.class, strategy);

        csiScoreStats = new DoubleListStats();
        logPStats = new DoubleListStats();
        tanimotoStats = new DoubleListStats();
        topLevelSelectionModel = elementListSelectionModel;

        /////////// LISTENERS //////////////
        source.addActiveResultChangedListener(this);
    }

    private JJob<Boolean> backgroundLoader = null;
    private final Lock backgroundLoaderLock = new ReentrantLock();


    @Override
    public void resultsChanged(InstanceBean experiment, FormulaResultBean sre, List<FormulaResultBean> resultElements, ListSelectionModel selectionModel) {
        resultsChanged(sre, resultElements, selectionModel, loadAll.get());
    }

    public void resultsChanged(FormulaResultBean sre, List<FormulaResultBean> resultElements, ListSelectionModel selectionModel, final boolean loadAllCandidates) {
        //may be io intense so run in background and execute ony ui updates from EDT to not block the UI too much
        try {
            backgroundLoaderLock.lock();
            final JJob<Boolean> old = backgroundLoader;
            backgroundLoader = Jobs.runInBackground(new TinyBackgroundJJob<>() {
                LoadMoleculeJob loadMols;

                @Override
                protected Boolean compute() throws Exception {
                    //cancel running job if not finished to not wais resources for fetching data that is not longer needed.
                    if (old != null && !old.isFinished()) {
                        old.cancel(false);
                        old.getResult(); //await cancellation so that nothing strange can happen.
                    }

                    checkForInterruption();

                    Jobs.runEDTAndWait(() -> {
                            csiScoreStats.reset();
                            logPStats.reset();
                            tanimotoStats.reset();
                            loadAll.set(loadAllCandidates);
                            setData(new HashSet<>());
                            topLevelSelectionModel.clearSelection();
                    });

                    checkForInterruption();
                    final List<FormulaResultBean> formulasToShow = new LinkedList<>();
                    switch (selectionType) {
                        case ALL:
                            formulasToShow.addAll(resultElements);
                            break;
                        case FIRST_SELECTED:
                            formulasToShow.add(sre);
                            break;
                        case ALL_SELECTED:
                            if (selectionModel == null) {
                                formulasToShow.addAll(resultElements);
                            } else {
                                for (int i = selectionModel.getMinSelectionIndex(); i <= selectionModel.getMaxSelectionIndex(); i++) {
                                    if (selectionModel.isSelectedIndex(i)) {
                                        formulasToShow.add(resultElements.get(i));
                                    }
                                }
                            }
                            break;
                    }
                    checkForInterruption();

                    final List<FingerprintCandidateBean> emChache = new ArrayList<>();
                    {
                        Set<FormulaResultBean> tmpData = new HashSet<>();
                        for (FormulaResultBean formRes : formulasToShow) {
                            checkForInterruption();
                            if (formRes != null) {
                                Class<? extends FBCandidates> cClass = loadAll.get() ? FBCandidates.class : FBCandidatesTopK.class;
                                Class<? extends FBCandidateFingerprints> fpClass = loadAll.get() ? FBCandidateFingerprints.class : FBCandidateFingerprintsTopK.class;

                                final Optional<FormulaResult> resOpt = formRes.getResult(FingerprintResult.class, cClass, fpClass);
                                checkForInterruption();

                                resOpt.ifPresent(res ->
                                        res.getAnnotation(FingerprintResult.class).ifPresent(fpRes ->
                                                res.getAnnotation(fpClass).ifPresent(fbfps ->
                                                        res.getAnnotation(cClass).ifPresent(fbc -> {

                                                            tmpData.add(formRes);
                                                            for (int j = 0; j < fbc.getResults().size(); j++) {
                                                                FingerprintCandidateBean c = new FingerprintCandidateBean(j + 1,
                                                                        fpRes.fingerprint,
                                                                        fbc.getResults().get(j),
                                                                        fbfps.getFingerprints().get(j),
                                                                        formRes.getPrecursorIonType(),
                                                                        formRes
                                                                );
                                                                emChache.add(c);
                                                                csiScoreStats.addValue(c.getScore());
                                                                Optional.ofNullable(c.getXLogPOrNull()).ifPresent(logPStats::addValue);
                                                                Double tm = c.getTanimotoScore();
                                                                tanimotoStats.addValue(tm == null ? Double.NaN : tm);
                                                            }
                                                        })
                                                )
                                        ));
                            }
                        }
                        setData(tmpData);
                    }
                    checkForInterruption();

                    if (refillElementsEDT(emChache))
                        loadMols = Jobs.MANAGER().submitJob(new LoadMoleculeJob(emChache));

                    return true;
                }

                @Override
                public void cancel(boolean mayInterruptIfRunning) {
                    if (loadMols != null && !loadMols.isFinished())
                        loadMols.cancel();
                    super.cancel(mayInterruptIfRunning);
                }
            });
        } finally {
            backgroundLoaderLock.unlock();
        }
    }

    protected void reloadData(boolean loadAll) {
        if (loadAll != this.loadAll.get()){
            List<FormulaResultBean> d = new ArrayList<>();
            readDataByConsumer(d::addAll);
            resultsChanged(null, d, null, loadAll);
        }
    }

    protected Function<FingerprintCandidateBean, Boolean> getBestFunc() {
        return c -> c.getScore() >= csiScoreStats.getMax();
    }
}
