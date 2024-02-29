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

package de.unijena.bioinf.lcms;

import de.unijena.bioinf.ChemistryBase.math.NormalDistribution;
import de.unijena.bioinf.ChemistryBase.math.RealDistribution;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.IsolationWindow;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.model.lcms.ChromatographicPeak;
import de.unijena.bioinf.model.lcms.Precursor;
import de.unijena.bioinf.model.lcms.Scan;
import de.unijena.bioinf.model.lcms.ScanPoint;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class ChimericDetector {

    protected IsolationWindow isolationWindow;
    protected RealDistribution guessedFilter;

    public ChimericDetector(IsolationWindow isolationWindow) {
        this.isolationWindow = isolationWindow;
        final double leftOffset = isolationWindow.getLeftOffset();
        final double rightOffset = isolationWindow.getRightOffset();
        this.guessedFilter = new SplitNormalDistribution(0d,leftOffset*leftOffset, rightOffset*rightOffset);
    }

    public static class Chimeric {
        public final ChromatographicPeak peak;
        public final double estimatedIntensityThatPassesFilter;

        public Chimeric(ChromatographicPeak peak, double estimatedIntensityThatPassesFilter) {
            this.peak = peak;
            this.estimatedIntensityThatPassesFilter = estimatedIntensityThatPassesFilter;
        }
    }

    public List<Chimeric> searchChimerics(ProcessedSample sample, Scan ms1Scan, Precursor precursor, ChromatographicPeak ms1Feature) {
        final Optional<ChromatographicPeak.Segment> segment = ms1Feature.getSegmentForScanId(ms1Scan.getIndex());
        if (segment.isEmpty()) {
            Optional<ChromatographicPeak> detectedFeature = sample.builder.detect(ms1Scan,precursor.getMass(), isolationWindow);
            throw new IllegalArgumentException("MS1 feature does not contain MS1 scan: " + precursor.getMass() + " @ " + precursor.getIntensity() + " at " + ms1Scan.getIndex() + " (" + ms1Scan.getRetentionTime() + " rt)");
        }
        SimpleSpectrum scan = sample.storage.getScan(ms1Scan);
        final double from = precursor.getMass() + isolationWindow.getLeftOffset();
        final double to = precursor.getMass() + isolationWindow.getRightOffset();
        int bgindex=Spectrums.indexOfFirstPeakWithin(scan, from,to);

        ScanPoint scanPoint = ms1Feature.getScanPointForScanId(ms1Scan.getIndex());

        //todo ScanPoint could also just directly store idx of peak
        int precursorPeakIdx = Spectrums.indexOfPeakClosestToMassWithin(scan, scanPoint.getMass(), sample.builder.getAllowedMassDeviation());
        // use most intensive peak instead
        final int mostInteniveIdx = Spectrums.mostIntensivePeakWithin(scan, scanPoint.getMass(), new Deviation(10));
        if (mostInteniveIdx>=0) precursorPeakIdx = mostInteniveIdx;

        if (precursorPeakIdx<0) {
            LoggerFactory.getLogger(ChimericDetector.class).warn("Do not find precursor ion in MS1 scan.");
            return Collections.emptyList();
        }
        final double ms1Mass = scan.getMzAt(precursorPeakIdx);
        final CorrelatedPeakDetector detector = new CorrelatedPeakDetector(Collections.emptySet());
        // now add all other peaks as chimerics
        final ArrayList<Chimeric> chimerics = new ArrayList<>();
        final double signalLevel = scan.getIntensityAt(precursorPeakIdx)*0.25;
        //normalize relative to intensity of ms1Feature signal
        final double norm = guessedFilter.getDensity((ms1Mass - precursor.getMass()));
        for (int k=bgindex; k < scan.size() && scan.getMzAt(k) <= to; ++k) {
            final double mzdiff = scan.getMzAt(k)-ms1Mass;
            final double possInt = scan.getIntensityAt(k)*guessedFilter.getDensity(mzdiff)/norm;
            if (k!=precursorPeakIdx && possInt>signalLevel) {
                // build a mass trace
                Optional<ChromatographicPeak> chim = sample.builder.detectExact(ms1Scan, scan.getMzAt(k));
                if (chim.isPresent()) {
                    if (chim.get().samePeak(ms1Feature)) {
                        LoggerFactory.getLogger(ChimericDetector.class).warn(precursor.toString() + ": there is a chimeric peak that overlaps with the ion. It is unclear, if this chimeric is a real feature or just a badly picked peak. We will ignore it.");
                        continue;
                    }
                    // check if it is an isotope
                    if (CorrelatedPeakDetector.hasMassOfAnIsotope(ms1Mass, scan.getMzAt(k)) && detector.correlate(ms1Feature.mutate(),segment.get(),chim.get().mutate()).filter(f->f.score>=CorrelatedPeakDetector.PROBABILITY_THRESHOLD_ISOTOPES).isPresent()) {
                        // ignore this peak
                        continue;
                    } else {
                        chimerics.add(new Chimeric(chim.get(), possInt));
                    }
                }
            }
        }
        return chimerics;
    }

    protected class SplitNormalDistribution extends RealDistribution {
        private final double mean;
        private final NormalDistribution leftNormal;
        private final NormalDistribution rightNormal;
        //adjust the density to make distributions same height at mean
        private final double scaleRight;

        protected SplitNormalDistribution(double mean, double leftVariance, double rightVariance) {
            this.mean = mean;
            this.leftNormal = new NormalDistribution(mean, leftVariance);
            this.rightNormal = new NormalDistribution(mean, rightVariance);
            this.scaleRight = leftNormal.getDensity(mean) / rightNormal.getDensity(mean);
        }

        @Override
        public double getDensity(double x) {
            if (x <= mean) return leftNormal.getDensity(x);
            else return rightNormal.getDensity(x) * scaleRight;
        }

        @Override
        public double getProbability(double begin, double end) {
            throw new UnsupportedOperationException("not implemented");
        }

        @Override
        public double getCumulativeProbability(double x) {
            throw new UnsupportedOperationException("not implemented");
        }

        @Override
        public double getLogDensity(double x) {
            if (x <= mean) return leftNormal.getLogDensity(x);
            else return rightNormal.getLogDensity(x);
        }


        @Override
        public double getVariance() {
            throw new UnsupportedOperationException("not implemented");
        }

        @Override
        public double getMean() {
            return mean;
        }
    }

}
