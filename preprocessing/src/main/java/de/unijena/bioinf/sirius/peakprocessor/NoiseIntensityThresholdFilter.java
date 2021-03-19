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

import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.sirius.ProcessedInput;
import de.unijena.bioinf.sirius.ProcessedPeak;
import de.unijena.bioinf.sirius.annotations.NoiseThresholdSettings;
import gnu.trove.list.array.TDoubleArrayList;

import java.util.ListIterator;

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
                    base = Math.max(p.maxIntensity(),base);
                break;
            case NOT_PRECURSOR:
                final double pm = (parent.getMass()-20d);
                for (ProcessedPeak p : input.getMergedPeaks())
                    if (p.getMass() < pm)
                        base = Math.max(p.maxIntensity(),base);
                break;
            case SECOND_LARGEST:
                double a = Double.NEGATIVE_INFINITY; double b = Double.NEGATIVE_INFINITY;
                for (ProcessedPeak p : input.getMergedPeaks()) {
                    final double mx = p.maxIntensity();
                    if (mx>a) {
                        b = a;
                        a = mx;
                    } else if (mx > b) {
                        b = mx;
                    }
                }
                base = b;
                break;
            default: base = 1d;
        }
        // Kai: it seems that for compounds with MANY spectra of different CE, removing peaks which occur only in
        // a few of them might be too harsh, in particular if each spectrum itself has high quality as in NIST.
        // So we now remove peaks per spectrum. In preprocessing we do smarter noise removal anyways.

        final double scale = base;
        final double deleteInt = Math.max(settings.absoluteThreshold, base*settings.intensityThreshold);
        ListIterator<ProcessedPeak> iter = input.getMergedPeaks().listIterator();
        while (iter.hasNext()) {
            final ProcessedPeak peak = iter.next();
            boolean allBelow = true;
            for (Peak p : peak.getOriginalPeaks()) {
                if (p.getIntensity()>=deleteInt) {
                    allBelow = false;
                    break;
                }
            }
            if (!peak.isSynthetic() && peak != input.getParentPeak() && allBelow) {
                iter.remove();
            }
        }

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
