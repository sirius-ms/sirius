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

package de.unijena.bioinf.sirius.peakprocessor;

import de.unijena.bioinf.sirius.ProcessedInput;
import de.unijena.bioinf.sirius.ProcessedPeak;
import de.unijena.bioinf.sirius.annotations.NoiseThresholdSettings;
import gnu.trove.list.array.TDoubleArrayList;

/**
 * Keeps only the K most intensive peaks and delete all peaks with intensity below given threshold
 */
public class NoiseIntensityThresholdFilter implements MergedSpectrumProcessor {

    @Override
    public void process(ProcessedInput input) {

        final NoiseThresholdSettings settings = input.getAnnotationOrDefault(NoiseThresholdSettings.class);
        final ProcessedPeak parent = input.getParentPeak();
        double base = 0d;
        switch (settings.basePeak) {
            case LARGEST:
                for (ProcessedPeak p : input.getMergedPeaks())
                    base = Math.max(p.getRelativeIntensity(),base);
                break;
            case NOT_PRECURSOR:
                final double pm = (parent.getMass()-0.5d);
                for (ProcessedPeak p : input.getMergedPeaks())
                    if (p.getMass() < pm)
                        base = Math.max(p.getRelativeIntensity(),base);
                break;
            case SECOND_LARGEST:
                double a = Double.NEGATIVE_INFINITY; double b = Double.NEGATIVE_INFINITY;
                for (ProcessedPeak p : input.getMergedPeaks()) {
                    if (p.getRelativeIntensity()>a) {
                        b = a;
                        a = p.getRelativeIntensity();
                    } else if (p.getRelativeIntensity() > b) {
                        b = p.getRelativeIntensity();
                    }
                }
                base = b;
                break;
            default: base = 1d;
        }
        final double scale = base;
        input.getMergedPeaks().removeIf(peak -> peak!=parent && ((peak.getSumIntensity() < settings.absoluteThreshold) || (peak.getRelativeIntensity()/scale) < settings.intensityThreshold));
        if (input.getMergedPeaks().size()>settings.maximalNumberOfPeaks) {
            final TDoubleArrayList intensities = new TDoubleArrayList(input.getMergedPeaks().size());
            for (ProcessedPeak p : input.getMergedPeaks()) intensities.add(p.getRelativeIntensity());
            intensities.sort();
            double threshold = intensities.get(intensities.size()-settings.maximalNumberOfPeaks);
            input.getMergedPeaks().removeIf(peak -> peak!=parent && peak.getRelativeIntensity() <= threshold);
        }

        input.resetIndizes();

    }
}
