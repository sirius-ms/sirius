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

package de.unijena.bioinf.ms.persistence.model.properties;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.unijena.bioinf.ms.persistence.model.core.feature.Feature;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The highest MS1 intensities of a project: per LC-MS run, and once over every feature there is, from runs and
 * peak lists alike - the reference a feature without a run is held against. Relative intensities are stated in
 * percent of these.
 * <p>
 * A maximum only ever rises: writing features folds them in, deleting features leaves it as it is. So the
 * reference of a relative intensity is the strongest feature the project has held, not the strongest it holds,
 * and a deletion never has to look at anything again.
 * <p>
 * Kept as a project property by the storage layer, which folds every feature it writes into it - whoever writes
 * it, so a project written by the command line stays as current as one written through the API. A project
 * without the property has not had its maxima derived yet.
 */
@Getter
@NoArgsConstructor
@EqualsAndHashCode
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProjectMaximumIntensities {

    /** The highest apex intensity and the highest area under curve, in raw instrument counts. */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Maximum {
        private double apex;
        private double auc;

        /** Takes {@code feature}'s values in, and says whether that raised either of them. */
        boolean fold(@NotNull Feature feature) {
            boolean raised = false;
            Double featureApex = feature.getApexIntensity();
            if (featureApex != null && featureApex > apex) {
                apex = featureApex;
                raised = true;
            }
            if (feature.getAreaUnderCurve() > auc) {
                auc = feature.getAreaUnderCurve();
                raised = true;
            }
            return raised;
        }

        Maximum copy() {
            return new Maximum(apex, auc);
        }

        void merge(@NotNull Maximum other) {
            apex = Math.max(apex, other.apex);
            auc = Math.max(auc, other.auc);
        }
    }

    private static final Maximum NONE = new Maximum();

    /** Per LC-MS run, keyed by the run id a feature carries. */
    private Map<Long, Maximum> runs = new HashMap<>();

    /** Over every feature, with or without a run. */
    private Maximum global = new Maximum();

    /**
     * Whether any feature without a run has been written - the only features held against the global maximum,
     * so a rise of it means nothing to a project that has none.
     */
    private boolean featuresWithoutRun;

    /** The maximum a feature is held against: that of its run, or the global one when it belongs to no run. */
    public @NotNull Maximum of(@NotNull Feature feature) {
        return feature.getRunId() != null ? run(feature.getRunId()) : global;
    }

    /** The maximum of run {@code runId}, zero for a run nothing was folded in for. */
    public @NotNull Maximum run(long runId) {
        Maximum maximum = runs.get(runId);
        return maximum == null ? NONE : maximum;
    }

    /** Folds {@code features} in, and says whether that raised any maximum. */
    public boolean fold(@NotNull Collection<Feature> features) {
        boolean raised = false;
        for (Feature feature : features) {
            if (feature.getRunId() != null) {
                raised |= runs.computeIfAbsent(feature.getRunId(), id -> new Maximum()).fold(feature);
            } else if (!featuresWithoutRun) {
                featuresWithoutRun = true;
                raised = true;
            }
            raised |= global.fold(feature);
        }
        return raised;
    }

    /**
     * Takes in what {@code other} was folded from, as if its features had been folded in here - so features
     * can be folded in parts, in any order, and the parts merged.
     */
    public void merge(@NotNull ProjectMaximumIntensities other) {
        other.runs.forEach((runId, maximum) -> runs.computeIfAbsent(runId, id -> new Maximum()).merge(maximum));
        global.merge(other.global);
        featuresWithoutRun |= other.featuresWithoutRun;
    }

    /** The runs whose maximum is higher here than in {@code earlier}, in either measure. */
    public Set<Long> runsRaisedSince(@NotNull ProjectMaximumIntensities earlier) {
        Set<Long> raised = new HashSet<>();
        runs.forEach((runId, maximum) -> {
            if (!maximum.equals(earlier.run(runId)))
                raised.add(runId);
        });
        return raised;
    }

    /** Whether the global maximum is higher here than in {@code earlier}, in either measure. */
    public boolean globalRaisedSince(@NotNull ProjectMaximumIntensities earlier) {
        return !global.equals(earlier.global);
    }

    /** A copy to fold into without touching this one. */
    public ProjectMaximumIntensities copy() {
        ProjectMaximumIntensities copy = new ProjectMaximumIntensities();
        runs.forEach((runId, maximum) -> copy.runs.put(runId, maximum.copy()));
        copy.global = global.copy();
        copy.featuresWithoutRun = featuresWithoutRun;
        return copy;
    }
}
