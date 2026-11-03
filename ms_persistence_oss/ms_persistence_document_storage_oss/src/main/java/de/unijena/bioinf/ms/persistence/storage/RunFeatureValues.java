/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2026 Bright Giant GmbH
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
 *  You should have received a copy of the GNU General Public License along with SIRIUS.
 *  If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.persistence.storage;

import de.unijena.bioinf.ms.persistence.model.core.feature.Feature;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * What the intensities of the aligned features are computed from - the run id, apex intensity and area under
 * curve of each of their run features - read once and held in primitive arrays, about 24 bytes per run feature.
 * <p>
 * Taken while the project's maxima are derived, which reads every run feature anyway, so that the search index
 * built right after them does not read them all a second time: see
 * {@link SiriusProjectDocumentDatabase#deriveMaximumIntensitiesKeepingRunFeatures()}.
 */
public final class RunFeatureValues {

    /** Stands for a run feature without a run - one imported from a peak list or through the API. */
    private static final long NO_RUN = Long.MIN_VALUE;

    /** Where the run features of an aligned feature start, and how many there are of them. */
    private final Long2IntOpenHashMap firstOf, countOf;
    private final long[] runIds;
    /** {@code NaN} for a run feature without an apex intensity. */
    private final double[] apexIntensities;
    private final double[] areasUnderCurve;

    private RunFeatureValues(Long2IntOpenHashMap firstOf, Long2IntOpenHashMap countOf, long[] runIds,
                             double[] apexIntensities, double[] areasUnderCurve) {
        this.firstOf = firstOf;
        this.countOf = countOf;
        this.runIds = runIds;
        this.apexIntensities = apexIntensities;
        this.areasUnderCurve = areasUnderCurve;
    }

    /**
     * The run features of an aligned feature, as far as its intensity is concerned: aligned feature, run, apex
     * intensity and area under curve, nothing else. Empty for an aligned feature that has none, or that these
     * values were not taken for.
     */
    public @NotNull List<Feature> runFeaturesOf(long alignedFeatureId) {
        int count = countOf.get(alignedFeatureId);
        if (count == 0)
            return List.of();
        int first = firstOf.get(alignedFeatureId);
        List<Feature> features = new ArrayList<>(count);
        for (int i = first; i < first + count; i++)
            features.add(Feature.builder()
                    .alignedFeatureId(alignedFeatureId)
                    .runId(runIds[i] == NO_RUN ? null : runIds[i])
                    .apexIntensity(Double.isNaN(apexIntensities[i]) ? null : apexIntensities[i])
                    .areaUnderCurve(areasUnderCurve[i])
                    .build());
        return features;
    }

    /** Collects the values of one worker's share; {@link #of} puts the shares together. */
    static final class Part {
        private final Long2IntOpenHashMap firstOf = new Long2IntOpenHashMap(), countOf = new Long2IntOpenHashMap();
        private final LongArrayList runIds = new LongArrayList();
        private final DoubleArrayList apexIntensities = new DoubleArrayList(), areasUnderCurve = new DoubleArrayList();

        /** Adds one run feature; those of an aligned feature must come one after the other. */
        void add(@NotNull Feature runFeature) {
            long alignedFeatureId = runFeature.getAlignedFeatureId();
            if (countOf.addTo(alignedFeatureId, 1) == 0)
                firstOf.put(alignedFeatureId, runIds.size());
            runIds.add(runFeature.getRunId() == null ? NO_RUN : runFeature.getRunId());
            apexIntensities.add(runFeature.getApexIntensity() == null ? Double.NaN : runFeature.getApexIntensity());
            areasUnderCurve.add(runFeature.getAreaUnderCurve());
        }
    }

    /** The shares of all workers, as one. */
    static RunFeatureValues of(@NotNull List<Part> parts) {
        int size = parts.stream().mapToInt(part -> part.runIds.size()).sum();
        Long2IntOpenHashMap firstOf = new Long2IntOpenHashMap(), countOf = new Long2IntOpenHashMap();
        long[] runIds = new long[size];
        double[] apexIntensities = new double[size], areasUnderCurve = new double[size];
        int offset = 0;
        for (Part part : parts) {
            int length = part.runIds.size();
            part.runIds.getElements(0, runIds, offset, length);
            part.apexIntensities.getElements(0, apexIntensities, offset, length);
            part.areasUnderCurve.getElements(0, areasUnderCurve, offset, length);
            final int shift = offset;
            part.firstOf.long2IntEntrySet().fastForEach(e -> firstOf.put(e.getLongKey(), e.getIntValue() + shift));
            countOf.putAll(part.countOf);
            offset += length;
        }
        return new RunFeatureValues(firstOf, countOf, runIds, apexIntensities, areasUnderCurve);
    }
}
