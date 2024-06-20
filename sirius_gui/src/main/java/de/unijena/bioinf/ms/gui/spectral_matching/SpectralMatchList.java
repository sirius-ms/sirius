/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
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

package de.unijena.bioinf.ms.gui.spectral_matching;

import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.swing.DefaultEventSelectionModel;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.jjobs.TinyBackgroundJJob;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.fingerid.FingerprintCandidateBean;
import de.unijena.bioinf.ms.gui.mainframe.instance_panel.CompoundList;
import de.unijena.bioinf.ms.gui.mainframe.instance_panel.ExperimentListChangeListener;
import de.unijena.bioinf.ms.gui.table.ActionList;
import de.unijena.bioinf.ms.gui.table.list_stats.DoubleListStats;
import de.unijena.bioinf.projectspace.InstanceBean;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class SpectralMatchList extends ActionList<SpectralMatchBean, InstanceBean> {

    public final DoubleListStats similarityStats;
    public final DoubleListStats sharedPeaksStats;

    private JJob<Boolean> backgroundLoader = null;
    private final Lock backgroundLoaderLock = new ReentrantLock();

    private InstanceBean instanceBean;
    private FingerprintCandidateBean fingerprintCandidateBean;
    private boolean loadAll = false;

    @Getter
    private int size = 0;
    @Getter
    private int totalSize = 0;

    private List<BiConsumer<Integer, Integer>> sizeChangedListeners = new ArrayList<>();

    public SpectralMatchList(CompoundList compoundList) {
        super(SpectralMatchBean.class);
        this.similarityStats = new DoubleListStats();
        this.sharedPeaksStats = new DoubleListStats();
        compoundList.addChangeListener(new ExperimentListChangeListener() {
            @Override
            public void listChanged(ListEvent<InstanceBean> event, DefaultEventSelectionModel<InstanceBean> selection, int fullSize) {
            }

            @Override
            public void listSelectionChanged(DefaultEventSelectionModel<InstanceBean> selection, int fullSize) {
                if (!selection.isSelectionEmpty()) {
                    instanceBean = selection.getSelected().get(0);
                    fingerprintCandidateBean = null;
                } else {
                    instanceBean = null;
                    fingerprintCandidateBean = null;
                }
                reloadData();
            }
        });

        //set initial state because listeners are called on change and not on creation
        DefaultEventSelectionModel<InstanceBean> m = compoundList.getCompoundListSelectionModel();
        if (!m.isSelectionEmpty()) {
            this.instanceBean = m.getSelected().iterator().next();
            this.fingerprintCandidateBean = null;
        } else {
            this.instanceBean = null;
            this.fingerprintCandidateBean = null;
        }
        reloadData();
    }

    public SpectralMatchList(final InstanceBean instanceBean, final FingerprintCandidateBean candidateBean) {
        super(SpectralMatchBean.class);
        this.similarityStats = new DoubleListStats();
        this.sharedPeaksStats = new DoubleListStats();

        this.instanceBean = instanceBean;
        this.fingerprintCandidateBean = candidateBean;
        reloadData();
    }

    public List<SpectralMatchBean> getMatchBeanGroup(long refSpecUUID) {
        if (fingerprintCandidateBean != null) {
            return loadAll ? fingerprintCandidateBean.getSpectralMatchGroup(refSpecUUID) : fingerprintCandidateBean.getSpectralMatchGroupFromTop(refSpecUUID);
        } else if (instanceBean != null) {
            return loadAll ? instanceBean.getSpectralMatchGroup(refSpecUUID) : instanceBean.getSpectralMatchGroupFromTop(refSpecUUID);
        } else {
            return List.of();
        }
    }

    public void addSizeChangedListener(BiConsumer<Integer, Integer> listener) {
        sizeChangedListeners.add(listener);
    }

    public void setLoadAll() {
        loadAll = true;
    }

    public void reloadData() {
        //cancel running job if not finished to not waist resources for fetching data that is not longer needed.
        try {
            backgroundLoaderLock.lock();
            final JJob<Boolean> old = backgroundLoader;
            backgroundLoader = Jobs.runInBackground(new TinyBackgroundJJob<>() {
                @Override
                protected Boolean compute() throws Exception {

                    if (old != null && !old.isFinished()) {
                        old.cancel(false);
                        old.getResult(); //await cancellation so that nothing strange can happen.
                    }
                    checkForInterruption();

                    List<SpectralMatchBean> beans;
                    int total;
                    if (fingerprintCandidateBean != null) {
                        beans = loadAll ? fingerprintCandidateBean.getAllSpectralMatches() : fingerprintCandidateBean.getTopSpectralMatches();
                        total = fingerprintCandidateBean.getNumberOfSpectralMatches();
                    } else if (instanceBean != null) {
                        beans = loadAll ? instanceBean.getAllSpectralMatches() : instanceBean.getTopSpectralMatches();
                        total = instanceBean.getNumberOfSpectralMatches();
                    } else {
                        beans = List.of();
                        total = 0;
                    }

                    Jobs.runEDTAndWait(() -> {
                        similarityStats.reset();
                        sharedPeaksStats.reset();
                        beans.forEach(bean -> {
                            similarityStats.addValue(bean.getMatch().getSimilarity());
                            sharedPeaksStats.addValue(bean.getMatch().getSharedPeaks() != null ? bean.getMatch().getSharedPeaks() : 0);
                        });
                        if (total != totalSize || beans.size() != size) {
                            size = beans.size();
                            totalSize = total;
                            for (BiConsumer<Integer, Integer> listener : sizeChangedListeners) {
                                listener.accept(size, totalSize);
                            }
                        }
                    });

                    checkForInterruption();

                    refillElementsEDT(instanceBean, beans);
                    return true;
                }
            });
        } finally {
            backgroundLoaderLock.unlock();
        }
    }

    protected Function<SpectralMatchBean, Boolean> getBestFunc() {
        return c -> c.getMatch().getSimilarity() >= similarityStats.getMax();
    }

}
