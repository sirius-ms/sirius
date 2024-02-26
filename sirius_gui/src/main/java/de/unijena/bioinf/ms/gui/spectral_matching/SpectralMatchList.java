/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.gui.spectral_matching;

import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.swing.DefaultEventSelectionModel;
import de.unijena.bioinf.ChemistryBase.utils.Utils;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.jjobs.TinyBackgroundJJob;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.fingerid.FingerprintCandidateBean;
import de.unijena.bioinf.ms.gui.fingerid.StructureList;
import de.unijena.bioinf.ms.gui.mainframe.instance_panel.CompoundList;
import de.unijena.bioinf.ms.gui.mainframe.instance_panel.ExperimentListChangeListener;
import de.unijena.bioinf.ms.gui.table.ActionList;
import de.unijena.bioinf.ms.gui.table.list_stats.DoubleListStats;
import de.unijena.bioinf.ms.nightsky.sdk.model.SpectralLibraryMatch;
import de.unijena.bioinf.projectspace.InstanceBean;

import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

public class SpectralMatchList extends ActionList<SpectralMatchBean, InstanceBean> {

    public final DoubleListStats similarityStats;
    public final DoubleListStats sharedPeaksStats;

    private JJob<Boolean> backgroundLoader = null;
    private final Lock backgroundLoaderLock = new ReentrantLock();

    public SpectralMatchList(CompoundList compoundList) {
        super(SpectralMatchBean.class);
        this.similarityStats = new DoubleListStats();
        this.sharedPeaksStats = new DoubleListStats();
        compoundList.addChangeListener(new ExperimentListChangeListener() {
            @Override
            public void listChanged(ListEvent<InstanceBean> event, DefaultEventSelectionModel<InstanceBean> selection) {
                System.out.println("IGNORE LIST CHANGE");
            }

            @Override
            public void listSelectionChanged(DefaultEventSelectionModel<InstanceBean> selection) {
                Utils.withTime("Change data after Instance SELECTION changed", (watch) -> {
                    if (!selection.isSelectionEmpty())
                        changeData(selection.getSelected().get(0), null);
                    else
                        changeData(null, null);
                });

            }
        });

        //set initial state because listeners are called on change and not on creation
        DefaultEventSelectionModel<InstanceBean> m = compoundList.getCompoundListSelectionModel();
        if (!m.isSelectionEmpty()) {
            changeData(m.getSelected().iterator().next(), null);
        } else {
            changeData(null, null);
        }
    }

    public SpectralMatchList(StructureList structureList) {
        super(SpectralMatchBean.class);
        this.similarityStats = new DoubleListStats();
        this.sharedPeaksStats = new DoubleListStats();

        structureList.addActiveResultChangedListener((inst, fpCandidate, resultElements, selections) ->
                changeData(inst, fpCandidate));

        //set initial state because listeners are called on change and not on creation
        structureList.readDataByConsumer(d -> changeData(d, structureList.getSelectedElement()));
    }

    public void changeData(final InstanceBean ec, final FingerprintCandidateBean candidateBean) {
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

                    Jobs.runEDTAndWait(() -> {
                        similarityStats.reset();
                        sharedPeaksStats.reset();
                    });

                    checkForInterruption();

                    if (ec != null) {
                        List<SpectralLibraryMatch> searchResults = candidateBean == null
                                ? ec.getSpectralSearchResults().getAllResults()
                                : candidateBean.getReferenceMatches();

                        List<SpectralMatchBean> matches = searchResults.stream().map(r -> {
                            similarityStats.addValue(r.getSimilarity());
                            sharedPeaksStats.addValue(r.getSharedPeaks());
                            return new SpectralMatchBean(r, ec);
                        }).toList();

                        refillElementsEDT(ec, matches);
                    } else {
                        refillElementsEDT(null, List.of());
                    }
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
