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
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.jjobs.TinyBackgroundJJob;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.mainframe.instance_panel.CompoundList;
import de.unijena.bioinf.ms.gui.mainframe.instance_panel.ExperimentListChangeListener;
import de.unijena.bioinf.ms.gui.table.ActionList;
import de.unijena.bioinf.ms.gui.table.list_stats.DoubleListStats;
import de.unijena.bioinf.projectspace.InstanceBean;

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


    public StructureList(final CompoundList compoundList) {
        super(FingerprintCandidateBean.class);

        csiScoreStats = new DoubleListStats();
        logPStats = new DoubleListStats();
        tanimotoStats = new DoubleListStats();
        topLevelSelectionModel = elementListSelectionModel;

        DefaultEventSelectionModel<InstanceBean> m = compoundList.getCompoundListSelectionModel();
        if (!m.isSelectionEmpty()) {
            changeData(m.getSelected().get(0));
        } else {
            changeData(null);
        }

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
                            setData(null);
                            topLevelSelectionModel.clearSelection();
                    });

                    checkForInterruption();
                    setData(ec);
                    if (hasData()) {
                        final List<FingerprintCandidateBean> emChache = ec.getStructureCandidates(loadAllCandidates ? Integer.MAX_VALUE : 100); //todo allow user specifiable or pagination
                        checkForInterruption();
                        if (refillElementsEDT(emChache))
                            loadMols = Jobs.MANAGER().submitJob(new LoadMoleculeJob(emChache));
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
            readDataByConsumer(d ->  changeData(d, loadAll));
    }

    protected Function<FingerprintCandidateBean, Boolean> getBestFunc() {
        return c -> c.getScore() >= csiScoreStats.getMax();
    }
}
