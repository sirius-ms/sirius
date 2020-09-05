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
import de.unijena.bioinf.projectspace.FormulaResultBean;
import de.unijena.bioinf.projectspace.InstanceBean;
import de.unijena.bioinf.projectspace.sirius.FormulaResult;

import javax.swing.*;
import java.util.*;
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


    public StructureList(final FormulaList source) {
        this(source, DataSelectionStrategy.ALL_SELECTED);
    }

    public StructureList(final FormulaList source, DataSelectionStrategy strategy) {
        super(FingerprintCandidateBean.class, strategy);

        csiScoreStats = new DoubleListStats();
        logPStats = new DoubleListStats();
        tanimotoStats = new DoubleListStats();
        source.addActiveResultChangedListener(this);
        resultsChanged(null, null, source.getElementList(), source.getResultListSelectionModel());
    }

    private JJob<Boolean> backgroundLoader = null;
    private final Lock backgroundLoaderLock = new ReentrantLock();


    @Override
    public void resultsChanged(InstanceBean experiment, FormulaResultBean sre, List<FormulaResultBean> resultElements, ListSelectionModel selectionModel) {
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
                        old.cancel(true);
                        old.getResult(); //await cancellation so that nothing strange can happen.
                    }

                    checkForInterruption();
                    Jobs.runEDTAndWait(() -> {
                        elementList.clear();
                        csiScoreStats.reset();
                        logPStats.reset();
                        tanimotoStats.reset();
                        data = new HashSet<>();
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
                            for (int i = selectionModel.getMinSelectionIndex(); i <= selectionModel.getMaxSelectionIndex(); i++) {
                                if (selectionModel.isSelectedIndex(i)) {
                                    formulasToShow.add(resultElements.get(i));
                                }
                            }
                            break;
                    }
                    checkForInterruption();

                    final List<FingerprintCandidateBean> emChache = new ArrayList<>();
                    for (FormulaResultBean e : formulasToShow) {
                        checkForInterruption();
                        if (e != null) {
                            final FormulaResult res = e.getResult(FingerprintResult.class, FBCandidates.class, FBCandidateFingerprints.class);
                            checkForInterruption();
                            res.getAnnotation(FBCandidateFingerprints.class).ifPresent(fbfps ->
                                    res.getAnnotation(FBCandidates.class).ifPresent(fbc -> {
                                        data.add(e);
                                        for (int j = 0; j < fbc.getResults().size(); j++) {
                                            FingerprintCandidateBean c = new FingerprintCandidateBean(j + 1,
                                                    res.getAnnotationOrThrow(FingerprintResult.class).fingerprint,
                                                    fbc.getResults().get(j),
                                                    fbfps.getFingerprints().get(j),
                                                    e.getPrecursorIonType());
                                            emChache.add(c);
                                            csiScoreStats.addValue(c.getScore());
                                            Optional.ofNullable(c.getXLogPOrNull()).ifPresent(logPStats::addValue);
                                            Double tm = c.getTanimotoScore();
                                            tanimotoStats.addValue(tm == null ? Double.NaN : tm);
                                        }
                                    })
                            );
                        }
                    }
                    checkForInterruption();

                    if (!emChache.isEmpty()) {
                        loadMols = Jobs.MANAGER.submitJob(new LoadMoleculeJob(emChache));
                        Jobs.runEDTAndWait(() -> {
                            elementList.clear(); //todo ugly workaround to prevent double entries because I cannot find out how they come in.
                            if (elementList.addAll(emChache))
                                notifyListeners(data, null, elementList, getResultListSelectionModel());
                        });
                    }
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

    protected Function<FingerprintCandidateBean, Boolean> getBestFunc() {
        return c -> c.getScore() >= csiScoreStats.getMax();
    }
}
