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

package de.unijena.bioinf.sirius.elementdetection.prediction;

import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormulaMap;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.Normalization;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.*;
import java.util.zip.GZIPInputStream;

public class FeatureVector {

    public double[] getFeatureVector(int npeaks) {
        switch (npeaks) {
            case 5:
                return getFeatureVectorNoRbfFivePeaks();
            case 4:
                return getFeatureVectorNoRbfFourPeaks();
            case 3:
                return getFeatureVectorNoRbfThreePeaks();
            default: return null;
        }
    }

    private final SimpleSpectrum ms;

    public SimpleSpectrum getMs() {
        return ms;
    }

    public FeatureVector(SimpleSpectrum spectrum, int npeaks) {
        ms = Spectrums.getNormalizedSpectrum(Spectrums.subspectrum(spectrum, 0, Math.min(spectrum.size(),npeaks)), Normalization.Sum(1d));
    }

    public double[] getFeatureVector() {
        if (ms.size()>=5) return getFeatureVectorNoRbfFivePeaks();
        else if (ms.size()>=4) return getFeatureVectorNoRbfFourPeaks();
        else return getFeatureVectorNoRbfThreePeaks();
    }

    public double[] getFeatureVectorNoRbfFivePeaks() {
        final Integer[] indizes = intensityOrderedIndizes(ms);

        final double p0 = ms.getIntensityAt(0), p1 = ms.getIntensityAt(1), p2 = ms.getIntensityAt(2), p3 = ms.getIntensityAt(3), p4 = ms.getIntensityAt(4);

        double[] vector = new double[]{
                // 5 intensity
                p0,
                p1,
                p2,
                p3,
                p4,

                // 3 min/max/median
                min(ms, 0, 1, 2, 3, 4),
                max(ms, 0, 1, 2, 3, 4),
                median(ms),

                // 6 even/odd
                p0 + p2 + p4,
                p1 + p3,
                min(ms, 0, 2, 4),
                min(ms, 1, 3),
                max(ms, 0, 2, 4),
                max(ms, 1, 3),

                // 6 index of most intensive peak
                indizes[0].doubleValue(),
                indizes[0] == 0 ? 1d : 0d,
                indizes[0] == 1 ? 1d : 0d,
                indizes[0] == 2 ? 1d : 0d,
                indizes[0] == 3 ? 1d : 0d,
                indizes[0] == 4 ? 1d : 0d,

                // 6 index of second most intensive peak
                indizes[1].doubleValue(),
                indizes[1] == 0 ? 1d : 0d,
                indizes[1] == 1 ? 1d : 0d,
                indizes[1] == 2 ? 1d : 0d,
                indizes[1] == 3 ? 1d : 0d,
                indizes[1] == 4 ? 1d : 0d,

                // 6 index of third most intensive peak
                indizes[2].doubleValue(),
                indizes[2] == 0 ? 1d : 0d,
                indizes[2] == 1 ? 1d : 0d,
                indizes[2] == 2 ? 1d : 0d,
                indizes[2] == 3 ? 1d : 0d,
                indizes[2] == 4 ? 1d : 0d,

                // 6 differences
                p0 - p1,
                p0 - p2,
                p0 - p3,
                p1 - p2,
                p1 - p3,
                p2 - p3,

                // 6 ratios
                p0 / p1,
                p0 / p2,
                p0 / p3,
                p1 / p2,
                p1 / p3,
                p2 / p3,
                p3 / p4,

                // 6 rations
                (p0 / p1) - (p1 / p2),
                (p1 / p2) - (p2 / p3),
                (p0 / p1) / (p1 / p2),
                (p1 / p2) / (p2 / p3),
                (p2 / p3) - (p3 / p4),
                (p2 / p3) / (p3 / p4),

                // 7 sums
                p0 + p1,
                p0 + p1 + p2,
                p0 + p1 + p2 + p3,
                p1 + p2 + p3,
                p1 + p2,
                p2 + p3,
                p3 + p4,

                // 11 mass features
                ms.getMzAt(0),
                ms.getMzAt(1) - ms.getMzAt(0),
                ms.getMzAt(2) - ms.getMzAt(0),
                ms.getMzAt(3) - ms.getMzAt(0),
                ms.getMzAt(4) - ms.getMzAt(0),
                ms.getMzAt(2) - ms.getMzAt(1),
                ms.getMzAt(3) - ms.getMzAt(1),
                ms.getMzAt(4) - ms.getMzAt(1),
                ms.getMzAt(3) - ms.getMzAt(2),
                ms.getMzAt(4) - ms.getMzAt(2),
                ms.getMzAt(4) - ms.getMzAt(3)

        };
        return vector;
    }


    public double[] getFeatureVectorNoRbfFourPeaks() {
        final Integer[] indizes = intensityOrderedIndizes(ms);

        final double p0 = ms.getIntensityAt(0), p1 = ms.getIntensityAt(1), p2 = ms.getIntensityAt(2), p3 = ms.getIntensityAt(3);

        double[] vector = new double[]{
                // 5 intensity
                p0,
                p1,
                p2,
                p3,

                // 3 min/max/median
                min(ms, 0, 1, 2, 3),
                max(ms, 0, 1, 2, 3),
                median(ms),

                // 6 even/odd
                p0 + p2,
                p1 + p3,
                min(ms, 0, 2),
                min(ms, 1, 3),
                max(ms, 0, 2),
                max(ms, 1, 3),

                // 6 index of most intensive peak
                indizes[0].doubleValue(),
                indizes[0] == 0 ? 1d : 0d,
                indizes[0] == 1 ? 1d : 0d,
                indizes[0] == 2 ? 1d : 0d,
                indizes[0] == 3 ? 1d : 0d,

                // 6 index of second most intensive peak
                indizes[1].doubleValue(),
                indizes[1] == 0 ? 1d : 0d,
                indizes[1] == 1 ? 1d : 0d,
                indizes[1] == 2 ? 1d : 0d,
                indizes[1] == 3 ? 1d : 0d,

                // 6 index of third most intensive peak
                indizes[2].doubleValue(),
                indizes[2] == 0 ? 1d : 0d,
                indizes[2] == 1 ? 1d : 0d,
                indizes[2] == 2 ? 1d : 0d,
                indizes[2] == 3 ? 1d : 0d,

                // 6 differences
                p0 - p1,
                p0 - p2,
                p0 - p3,
                p1 - p2,
                p1 - p3,
                p2 - p3,

                // 6 ratios
                p0 / p1,
                p0 / p2,
                p0 / p3,
                p1 / p2,
                p1 / p3,
                p2 / p3,

                // 6 rations
                (p0 / p1) - (p1 / p2),
                (p1 / p2) - (p2 / p3),
                (p0 / p1) / (p1 / p2),
                (p1 / p2) / (p2 / p3),

                // 7 sums
                p0 + p1,
                p0 + p1 + p2,
                p1 + p2 + p3,
                p1 + p2,
                p2 + p3,

                // 11 mass features
                ms.getMzAt(0),
                ms.getMzAt(1) - ms.getMzAt(0),
                ms.getMzAt(2) - ms.getMzAt(0),
                ms.getMzAt(3) - ms.getMzAt(0),
                ms.getMzAt(2) - ms.getMzAt(1),
                ms.getMzAt(3) - ms.getMzAt(1),
                ms.getMzAt(3) - ms.getMzAt(2),

        };
        return vector;
    }


    public double[] getFeatureVectorNoRbfThreePeaks() {
        final Integer[] indizes = intensityOrderedIndizes(ms);

        final double p0 = ms.getIntensityAt(0), p1 = ms.getIntensityAt(1), p2 = ms.getIntensityAt(2);

        double[] vector = new double[]{
                // 5 intensity
                p0,
                p1,
                p2,

                // 3 min/max/median
                min(ms, 0, 1, 2),
                max(ms, 0, 1, 2),
                median(ms),

                // 6 even/odd
                p0 + p2,
                p1 + p2,

                // 6 index of most intensive peak
                indizes[0].doubleValue(),
                indizes[0] == 0 ? 1d : 0d,
                indizes[0] == 1 ? 1d : 0d,
                indizes[0] == 2 ? 1d : 0d,

                // 6 index of second most intensive peak
                indizes[1].doubleValue(),
                indizes[1] == 0 ? 1d : 0d,
                indizes[1] == 1 ? 1d : 0d,
                indizes[1] == 2 ? 1d : 0d,

                // 6 index of third most intensive peak
                indizes[2].doubleValue(),
                indizes[2] == 0 ? 1d : 0d,
                indizes[2] == 1 ? 1d : 0d,
                indizes[2] == 2 ? 1d : 0d,

                // 6 differences
                p0 - p1,
                p0 - p2,
                p1 - p2,

                // 6 ratios
                p0 / p1,
                p0 / p2,
                p1 / p2,

                // 6 rations
                (p0 / p1) - (p1 / p2),
                (p0 / p1) / (p1 / p2),

                // 7 sums
                p0 + p1,
                p1 + p2,

                // 11 mass features
                ms.getMzAt(0),
                ms.getMzAt(1) - ms.getMzAt(0),
                ms.getMzAt(2) - ms.getMzAt(0),
                ms.getMzAt(2) - ms.getMzAt(1),

        };
        return vector;
    }

    private static Integer[] intensityOrderedIndizes(final SimpleSpectrum ms) {
        final Integer[] indizes = new Integer[ms.size()];
        for (int k=0; k < indizes.length; ++k) indizes[k] = k;
        Arrays.sort(indizes, new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return Double.compare(ms.getIntensityAt(o2), ms.getIntensityAt(o1));
            }
        });
        return indizes;
    }

    private static double median(SimpleSpectrum ms) {
        final double[] intensities = Spectrums.copyIntensities(ms);
        Arrays.sort(intensities);
        if (intensities.length%2==0) {
            return (intensities[intensities.length/2] + intensities[intensities.length/2 - 1])/2d;
        } else {
            return intensities[intensities.length/2];
        }
    }

    private static double max(SimpleSpectrum ms, int... indizes) {
        double maximum = ms.getIntensityAt(indizes[0]);
        for (int k=1; k < indizes.length; ++k) maximum = Math.max(maximum, ms.getIntensityAt(indizes[k]));
        return maximum;
    }

    private static double min(SimpleSpectrum ms, int... indizes) {
        double minimum = ms.getIntensityAt(indizes[0]);
        for (int k=1; k < indizes.length; ++k) minimum = Math.min(minimum, ms.getIntensityAt(indizes[k]));
        return minimum;
    }

}

