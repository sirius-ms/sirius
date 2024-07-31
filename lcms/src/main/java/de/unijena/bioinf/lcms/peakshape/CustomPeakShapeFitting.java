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

package de.unijena.bioinf.lcms.peakshape;

import de.unijena.bioinf.ChemistryBase.math.ExponentialDistribution;
import de.unijena.bioinf.ChemistryBase.math.NormalDistribution;
import de.unijena.bioinf.lcms.ProcessedSample;
import de.unijena.bioinf.model.lcms.ChromatographicPeak;
import de.unijena.bioinf.model.lcms.ScanPoint;
import org.apache.commons.lang3.Range;

public class CustomPeakShapeFitting implements PeakShapeFitting<CustomPeakShape> {
    @Override
    public CustomPeakShape fit(ProcessedSample sample, ChromatographicPeak peak, ChromatographicPeak.Segment segment) {
        // a good peak shape is

        double score = 1d;

        // 1. monotonic increasing and decreasing
        score *= checkMonotonicity(sample,peak,segment);
        // 2. has a clearly defined start and end at an minimum
        score *= checkStartAndEndPoint(sample,peak,segment);
        // 3. consists of many data points after and before the maximum
        score *= checkLength(sample,peak,segment);
        // is not too broad
        score *= checkPeakWidth(sample,peak,segment);

        // if the peak apex is clearly above the signal level, we can relax the constraints a little bit
        ScanPoint apex = peak.getApexPeak();
        score *= Math.max(1, Math.sqrt((apex.getIntensity()/sample.ms1NoiseModel.getSignalLevel(apex.getScanNumber(),apex.getMass()))-1));

        return new CustomPeakShape(score);

    }

    private double checkPeakWidth(ProcessedSample sample, ChromatographicPeak peak, ChromatographicPeak.Segment segment) {
        final double ratio = segment.fwhm(0.2)/peak.getIntensityAt(segment.getApexIndex());
        if (ratio <= sample.getMeanPeakWidthToHeightRatio()) return 1d;
        return new NormalDistribution(sample.getMeanPeakWidthToHeightRatio(), sample.getMeanPeakWidthToHeightStd()*sample.getMeanPeakWidthToHeightStd()).getErrorProbability(ratio);
    }

    private double checkLength(ProcessedSample sample, ChromatographicPeak peak, ChromatographicPeak.Segment segment) {
        final Range<Integer> integerRange = segment.calculateFWHM(0.15);
        int ndatapointsLeft = segment.getApexIndex() - integerRange.getMinimum() + 1;
        int ndatapointsRight = integerRange.getMaximum() - segment.getApexIndex() + 1;
        final NormalDistribution distribution = new NormalDistribution(3, 4);
        return distribution.getCumulativeProbability(ndatapointsLeft) * distribution.getCumulativeProbability(ndatapointsRight);
    }

    private double checkStartAndEndPoint(ProcessedSample sample, ChromatographicPeak peak, ChromatographicPeak.Segment segment) {
        double apex = peak.getIntensityAt(segment.getApexIndex());
        double min = peak.getIntensityAt(segment.getStartIndex());
        double min2 = peak.getIntensityAt(segment.getEndIndex());
        return ExponentialDistribution.fromMean(0.25).getCumulativeProbability(1d - (min+min2)/apex);
    }

    private double checkMonotonicity(ProcessedSample sample, ChromatographicPeak peak, ChromatographicPeak.Segment segment) {
        double monotonicIntensity = 0d, nonMonotonicIntensity = 0d;
        Range<Integer> r = segment.calculateFWHM(0.15);
        for (int k=r.getMinimum()+1; k <= segment.getApexIndex(); ++k) {
            final double i2 = peak.getIntensityAt(k);
            final double i1 = peak.getIntensityAt(k-1);
            if (i2 > i1) {
                monotonicIntensity += (i2-i1);
            } else nonMonotonicIntensity += (i1-i2);
        }
        for (int k=segment.getApexIndex()+1; k <= r.getMaximum(); ++k) {
            final double i2 = peak.getIntensityAt(k);
            final double i1 = peak.getIntensityAt(k-1);
            if (i2 < i1) {
                monotonicIntensity += (i1-i2);
            } else nonMonotonicIntensity += (i2-i1);
        }

        return 1d-ExponentialDistribution.fromMean(0.05).getCumulativeProbability(nonMonotonicIntensity/monotonicIntensity);
    }
}
