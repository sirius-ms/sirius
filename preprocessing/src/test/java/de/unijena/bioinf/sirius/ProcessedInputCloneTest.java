/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
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
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.sirius;

import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Experiment;
import de.unijena.bioinf.sirius.annotations.DecompositionList;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link ProcessedInput#clone()} is the isolation mechanism that lets several {@link ProcessedInput}s derived from
 * the same preprocessing result be scored concurrently (e.g. the parallel tree computations of the CLI self-test).
 * Isolation only holds if the peak annotation registry is copied as well: the registry hands out the array index
 * under which a {@link de.unijena.bioinf.sirius.PeakAnnotation} stores its value inside each {@link ProcessedPeak},
 * so a shared registry means one clone can overwrite the index another clone already wrote its values under.
 */
public class ProcessedInputCloneTest {

    /** Marker annotation, used to force a second registry entry so indices can drift apart. */
    public static class OtherPeakAnnotation {
    }

    private static ProcessedInput singlePeakInput(double parentMz) {
        final ProcessedPeak parentPeak = new ProcessedPeak();
        parentPeak.setMass(parentMz);
        parentPeak.setRelativeIntensity(1d);
        parentPeak.setIndex(0);
        final List<ProcessedPeak> mergedPeaks = new ArrayList<>();
        mergedPeaks.add(parentPeak);
        final MutableMs2Experiment experiment = new MutableMs2Experiment();
        experiment.setIonMass(parentMz);
        return new ProcessedInput(experiment, experiment, mergedPeaks, parentPeak);
    }

    @Test
    public void cloneDoesNotShareThePeakAnnotationRegistry() {
        final ProcessedInput original = singlePeakInput(358.201538085938d);
        final ProcessedInput copy = original.clone();

        copy.getOrCreatePeakAnnotation(DecompositionList.class);

        assertFalse(original.getPeakAnnotations().containsKey(DecompositionList.class),
                "registering a peak annotation on a clone must not leak into the original's registry");
    }

    @Test
    public void cloneKeepsAnnotationsRegisteredBeforeCloning() {
        final ProcessedInput original = singlePeakInput(358.201538085938d);
        final DecompositionList decompositions = new DecompositionList(Collections.emptyList());
        original.getOrCreatePeakAnnotation(DecompositionList.class).set(original.getParentPeak(), decompositions);

        final ProcessedInput copy = original.clone();

        // the clone inherits both the registry entry and - through ProcessedPeak.clone() - the stored value,
        // so the index handed out before cloning must still resolve.
        assertSame(decompositions, copy.getPeakAnnotationOrThrow(DecompositionList.class).get(copy.getParentPeak()),
                "a peak annotation set before cloning must still be readable from the clone");
    }

    /**
     * Regression test for the intermittent NPE in {@code IsotopePatternInMs1Plugin.beforePeakScoring}: several
     * clones of one {@link ProcessedInput} are scored in parallel and each of them lazily registers its peak
     * annotations. With a shared registry the concurrent {@code containsKey}/{@code put} pairs hand out
     * diverging indices, and a clone that wrote its {@link DecompositionList} under the old index reads back
     * {@code null} afterwards.
     */
    @Test
    public void concurrentClonesEachKeepTheirOwnDecompositionList() throws Exception {
        final int threads = 32;
        final int rounds = 200;
        final AtomicInteger lostAnnotations = new AtomicInteger(0);
        final List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());

        for (int round = 0; round < rounds && lostAnnotations.get() == 0 && errors.isEmpty(); ++round) {
            final ProcessedInput shared = singlePeakInput(358.201538085938d);
            final CyclicBarrier startTogether = new CyclicBarrier(threads);
            final List<Thread> workers = new ArrayList<>(threads);

            for (int t = 0; t < threads; ++t) {
                final int workerId = t;
                final Thread worker = new Thread(() -> {
                    try {
                        final ProcessedInput mine = shared.clone();
                        startTogether.await();
                        // odd workers register a second annotation first, so the registry sizes - and therefore
                        // the handed out indices - differ between workers on an unsynchronized shared registry.
                        if ((workerId & 1) == 1)
                            mine.getOrCreatePeakAnnotation(OtherPeakAnnotation.class);
                        final DecompositionList mySet = new DecompositionList(Collections.emptyList());
                        mine.getOrCreatePeakAnnotation(DecompositionList.class).set(mine.getParentPeak(), mySet);
                        // this is what FragmentationPatternAnalysis/the plugins do: re-resolve the annotation
                        // and read the value back from the very same peak.
                        final DecompositionList readBack =
                                mine.getPeakAnnotationOrThrow(DecompositionList.class).get(mine.getParentPeak());
                        if (readBack != mySet)
                            lostAnnotations.incrementAndGet();
                    } catch (Throwable e) {
                        errors.add(e);
                    }
                }, "clone-worker-" + workerId);
                workers.add(worker);
                worker.start();
            }
            for (Thread worker : workers) worker.join(60_000L);
        }

        assertTrue(errors.isEmpty(), () -> "concurrent scoring of clones failed with " + errors.get(0));
        assertEquals(0, lostAnnotations.get(),
                "a clone must read back the DecompositionList it wrote to its own parent peak");
    }
}
