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

import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.core.feature.Feature;
import de.unijena.bioinf.ms.persistence.model.properties.ProjectMaximumIntensities;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Derives the MS1 intensities the API reports for an aligned feature.
 * <p>
 * The aligned feature's own {@code apexIntensity} is NOT one of them: it is the apex of the merged trace, in
 * which every run's intensities are divided by that run's normalization constant and then summed over runs -
 * a dimensionless value near 1 that grows with the number of runs. The run features carry the raw apex
 * intensity and area under curve of their own trace, so the reported values are their maxima, in instrument
 * counts like the quantification table.
 * <p>
 * The relative intensities put each run feature in proportion to the most intense feature of its run (100 =
 * that run's strongest feature) and again take the maximum over the runs, so a relative bound compares a
 * feature with the run it was measured in rather than with the project. A feature without a run - from a peak
 * list or an API import - is held against the strongest feature of the whole project instead. Both maxima only
 * ever rise (see {@link ProjectMaximumIntensities}): after the strongest feature of a run is deleted, the
 * reference is still the one it set, so no remaining feature reaches 100.
 */
public final class FeatureIntensities {

    private FeatureIntensities() {
    }

    /**
     * The intensities of one aligned feature. {@code apex} and {@code auc} are raw instrument counts, the relative
     * values are percent of the run maximum (the project maximum without a run); each is null when unknown.
     */
    public record Intensities(@Nullable Double apex, @Nullable Double auc,
                              @Nullable Double apexRelative, @Nullable Double aucRelative) {
        public static final Intensities UNKNOWN = new Intensities(null, null, null, null);
    }

    /**
     * The intensities of {@code feature}: the maxima over its run features, raw and relative to the maximum
     * each is held against. Run features without a value are skipped, and if none of them has one the intensity is unknown.
     * So it is for an aligned feature with no run features at all: an apex intensity of its own, where there
     * is one, belongs to a merged trace and is not an intensity in counts.
     *
     * @param feature an aligned feature whose run features have been fetched, if it has any
     * @param maxima  the maximum intensities of the project the feature belongs to
     */
    public static Intensities of(@NotNull AlignedFeatures feature, @NotNull ProjectMaximumIntensities maxima) {
        List<Feature> runFeatures = feature.getFeatures().orElse(null);
        if (runFeatures == null || runFeatures.isEmpty())
            // nothing measured to report; an apex intensity of its own, where there is one, is a merged
            // trace's and not an intensity in counts
            return Intensities.UNKNOWN;

        Double apex = null, auc = null, apexRelative = null, aucRelative = null;
        for (Feature runFeature : runFeatures) {
            ProjectMaximumIntensities.Maximum maximum = maxima.of(runFeature);
            Double runApex = runFeature.getApexIntensity();
            if (runApex != null && runApex > 0) {
                apex = max(apex, runApex);
                apexRelative = max(apexRelative, percentOf(runApex, maximum.getApex()));
            }
            double runAuc = runFeature.getAreaUnderCurve();
            if (runAuc > 0) {
                auc = max(auc, runAuc);
                aucRelative = max(aucRelative, percentOf(runAuc, maximum.getAuc()));
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
