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

import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.swing.DefaultEventSelectionModel;
import de.unijena.bioinf.ChemistryBase.utils.IOFunctions;
import de.unijena.bioinf.confidence_score.ConfidenceMode;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.jjobs.TinyBackgroundJJob;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.mainframe.instance_panel.CompoundList;
import de.unijena.bioinf.ms.gui.mainframe.instance_panel.ExperimentListChangeListener;
import de.unijena.bioinf.ms.gui.table.ActionList;
import de.unijena.bioinf.ms.gui.table.list_stats.DoubleListStats;
import de.unijena.bioinf.projectspace.InstanceBean;

import javax.swing.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

public class StructureList extends ActionList<FingerprintCandidateBean, InstanceBean> {
    public final DoubleListStats csiScoreStats;
    public final DoubleListStats logPStats;
    public final DoubleListStats tanimotoStats;

    private final AtomicBoolean loadAll = new AtomicBoolean(false);

    private final CompoundList compoundList;

    private final IOFunctions.BiIOFunction<InstanceBean, Integer, List<FingerprintCandidateBean>> dataExtractor; //todo allow user specifiable or pagination
    public StructureList(final CompoundList compoundList, IOFunctions.BiIOFunction<InstanceBean, Integer, List<FingerprintCandidateBean>> dataExtractor) {
        super(FingerprintCandidateBean.class);
        this.dataExtractor = dataExtractor;
        this.compoundList = compoundList;
        elementListSelectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        csiScoreStats = new DoubleListStats();
        logPStats = new DoubleListStats();
        tanimotoStats = new DoubleListStats();

        /////////// LISTENERS //////////////
        //this is the selection refresh, element changes are detected by eventlist
        compoundList.addChangeListener(new ExperimentListChangeListener() {
            @Override
            public void listChanged(ListEvent<InstanceBean> event, DefaultEventSelectionModel<InstanceBean> selection) {
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
            public void listSelectionChanged(DefaultEventSelectionModel<InstanceBean> selection) {
                if (!selection.isSelectionEmpty())
                    changeData(selection.getSelected().get(0));
                else
                    changeData(null);
            }
        });

        //set initial state because listeners are called on change and not on creation
        DefaultEventSelectionModel<InstanceBean> m = compoundList.getCompoundListSelectionModel();
        if (!m.isSelectionEmpty()) {
            changeData(m.getSelected().get(0));
        } else {
            changeData(null);
        }
    }

    private JJob<Boolean> backgroundLoader = null;
    private final Lock backgroundLoaderLock = new ReentrantLock();

    private void changeData(final InstanceBean ec) {
        changeData(ec, loadAll.get());
    }

    public void changeData(final InstanceBean ec, final boolean loadAllCandidates) {
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
                    });

                    checkForInterruption();

                    if (ec != null) {
                        final List<FingerprintCandidateBean> fpcChache = dataExtractor.apply(ec, loadAllCandidates ? Integer.MAX_VALUE : 100);
                        //prepare stats for filters and views before setting data
                        fpcChache.forEach(fpc ->{
                            csiScoreStats.addValue(fpc.getCandidate().getCsiScore());
                            fpc.getXLogPOpt().ifPresent(logPStats::addValue);
                            tanimotoStats.addValue(fpc.getTanimotoScore());
                        });
                        checkForInterruption();
                        if (refillElementsEDT(ec, fpcChache)) {
                            checkForInterruption();
                            if (!fpcChache.isEmpty())
                                loadMols = Jobs.MANAGER().submitJob(new LoadMoleculeJob(fpcChache));
                        }
                    } else {
                        refillElementsEDT(null, List.of());
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

    protected void reloadData(boolean loadAll) {
        if (loadAll != this.loadAll.get())
            readDataByConsumer(d -> changeData(d, loadAll));
    }

    protected Function<FingerprintCandidateBean, Boolean> getBestFunc() {
        return c -> {
            if (compoundList.getConfidenceDisplayMode() == ConfidenceMode.APPROXIMATE) {
                return c.getCandidate().getStructDistToTopHit()!=null && c.getCandidate().getStructDistToTopHit()<=2;
            } else {
                return c.getScore() >= csiScoreStats.getMax();
            }
        };
    }
}
