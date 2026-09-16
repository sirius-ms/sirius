/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2024 Bright Giant GmbH
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

package de.unijena.bioinf.ms.middleware.model.features;

import de.unijena.bioinf.ms.persistence.model.core.DataSource;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.core.feature.Feature;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Derives the MS1 intensities the API reports for an aligned feature.
 * <p>
 * The aligned feature's own {@code apexIntensity} is NOT one of them: it is the apex of the merged trace, in
 * which every run's intensities are divided by that run's normalization constant and then summed over runs -
 * a dimensionless value near 1 that grows with the number of runs. The run features carry the raw apex
 * intensity and area under curve of their own trace, so the reported values are their maxima, in instrument
 * counts like the quantification table.
 * <p>
 * The relative intensities put each run feature in proportion to the most intense feature of the same
 * {@linkplain #groupOf group} (100 = that group's strongest feature) and again take the maximum over the
 * groups, so a {@code %} bound compares a feature with the run it was measured in rather than with the project.
 */
public final class FeatureIntensities {

    private FeatureIntensities() {
    }

    /**
     * The intensities of one aligned feature. {@code apex} and {@code auc} are raw instrument counts, the relative
     * values are percent of the group maximum; each is null when unknown.
     */
    public record Intensities(@Nullable Double apex, @Nullable Double auc,
                              @Nullable Double apexRelative, @Nullable Double aucRelative) {
        public static final Intensities UNKNOWN = new Intensities(null, null, null, null);
    }

    /**
     * The group a run feature's intensity is compared with: its LC-MS run, or - for data imported without runs,
     * such as peak lists, where every file is one measurement - the source file of the aligned feature it belongs
     * to. Features with neither share one group.
     */
    public static String groupOf(@NotNull Feature runFeature, @NotNull AlignedFeatures owner) {
        return runFeature.getRunId() != null ? runGroup(runFeature.getRunId()) : sourceGroup(owner);
    }

    public static String runGroup(long runId) {
        return "run:" + runId;
    }

    public static String sourceGroup(@NotNull AlignedFeatures owner) {
        return "source:" + owner.getDataSource().map(DataSource::getSource).orElse("");
    }

    /** The highest apex intensity and area under curve any run feature reaches, per {@linkplain #groupOf group}. */
    public static final class RunMaxima {
        private final Map<String, double[]> maxima;

        private RunMaxima(Map<String, double[]> maxima) {
            this.maxima = maxima;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static RunMaxima empty() {
            return new RunMaxima(Map.of());
        }

        double apexMaximum(@NotNull String group) {
            double[] max = maxima.get(group);
            return max == null ? 0d : max[0];
        }

        double aucMaximum(@NotNull String group) {
            double[] max = maxima.get(group);
            return max == null ? 0d : max[1];
        }

        /**
         * The groups whose maximum differs between {@code this} and {@code other}, in either measure - the groups
         * whose features carry a relative intensity that no longer holds.
         */
        public Set<String> changedGroups(@NotNull RunMaxima other) {
            Set<String> changed = new HashSet<>();
            Set<String> groups = new HashSet<>(maxima.keySet());
            groups.addAll(other.maxima.keySet());
            for (String group : groups) {
                if (apexMaximum(group) != other.apexMaximum(group) || aucMaximum(group) != other.aucMaximum(group))
                    changed.add(group);
            }
            return changed;
        }

        public static final class Builder {
            private final Map<String, double[]> maxima = new HashMap<>();

            /** Takes the run feature's values into the group's maximum. */
            public Builder add(@NotNull String group, @NotNull Feature runFeature) {
                double[] max = maxima.computeIfAbsent(group, g -> new double[2]);
                Double apex = runFeature.getApexIntensity();
                if (apex != null && apex > max[0])
                    max[0] = apex;
                if (runFeature.getAreaUnderCurve() > max[1])
                    max[1] = runFeature.getAreaUnderCurve();
                return this;
            }

            public RunMaxima build() {
                return new RunMaxima(Map.copyOf(maxima));
            }
        }
    }

    /**
     * The intensities of {@code feature}: the maxima over its run features, raw and relative to each group's
     * maximum. Run features without a value are skipped; if run features exist but none has a value, the
     * intensity is unknown (never the normalized merged value). Only a feature with no run features at all
     * (imported over the API) reports its own apex value, which is then the MS1 precursor peak intensity
     * written at import, and no relative value, since there is no group to relate it to.
     *
     * @param feature an aligned feature whose run features have been fetched, if it has any
     * @param maxima  the group maxima of the project the feature belongs to
     */
    public static Intensities of(@NotNull AlignedFeatures feature, @NotNull RunMaxima maxima) {
        List<Feature> runFeatures = feature.getFeatures().orElse(null);
        if (runFeatures == null || runFeatures.isEmpty())
            return new Intensities(feature.getApexIntensity(), null, null, null);

        Double apex = null, auc = null, apexRelative = null, aucRelative = null;
        for (Feature runFeature : runFeatures) {
            String group = groupOf(runFeature, feature);
            Double runApex = runFeature.getApexIntensity();
            if (runApex != null && runApex > 0) {
                apex = max(apex, runApex);
                apexRelative = max(apexRelative, percentOf(runApex, maxima.apexMaximum(group)));
            }
            double runAuc = runFeature.getAreaUnderCurve();
            if (runAuc > 0) {
                auc = max(auc, runAuc);
                aucRelative = max(aucRelative, percentOf(runAuc, maxima.aucMaximum(group)));
            }
        }
        return new Intensities(apex, auc, apexRelative, aucRelative);
    }

    private static @Nullable Double percentOf(double value, double maximum) {
        return maximum > 0 ? value / maximum * 100d : null;
    }

    private static @Nullable Double max(@Nullable Double current, @Nullable Double candidate) {
        if (candidate == null)
            return current;
        return current == null || candidate > current ? candidate : current;
    }
}
