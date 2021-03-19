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

import com.google.common.collect.Range;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.model.lcms.*;
import gnu.trove.list.array.TDoubleArrayList;

import java.util.Optional;

public class ChromatogramBuilder {

    protected final ProcessedSample sample;
    protected final Deviation dev;
    protected final ChromatogramCache cache;


    public ChromatogramBuilder(ProcessedSample sample) {
        this.sample = sample;
        this.dev = new Deviation(20);
        this.cache = new ChromatogramCache();
    }

    public Optional<ChromatographicPeak> detectExact(Scan startingPoint, double mz) {
        final SimpleSpectrum spectrum = sample.storage.getScan(startingPoint);
        int i = Spectrums.binarySearch(spectrum, mz, dev);
        if (i>=0) {
            return buildTrace(spectrum, new ScanPoint(startingPoint, spectrum.getMzAt(i), spectrum.getIntensityAt(i)));
        } else {
            return Optional.empty(); // no chromatographic peak detected
        }
    }

    public Optional<ChromatographicPeak> detect(Range<Integer> scanRange, double mz) {
        // pick most intensive peak in scan range
        ScanPoint best = null;
        SimpleSpectrum bestSpec = null;
        for (Scan s : sample.run.getScans(scanRange.lowerEndpoint(), scanRange.upperEndpoint()).values()) {
            if (!s.isMsMs() && scanRange.contains(s.getIndex())) {
                final SimpleSpectrum spectrum = sample.storage.getScan(s);
                int i = Spectrums.mostIntensivePeakWithin(spectrum, mz, dev);
                if (i>=0) {
                    if (best==null || spectrum.getIntensityAt(i) > best.getIntensity()) {
                        best = new ScanPoint(s, spectrum.getMzAt(i), spectrum.getIntensityAt(i));
                        bestSpec = spectrum;
                    }
                }
            }
        }
        if (best==null) return Optional.empty();
        return buildTrace(bestSpec, best);
    }

    public Optional<ChromatographicPeak> detect(Scan startingPoint, double mz) {
        final SimpleSpectrum spectrum = sample.storage.getScan(startingPoint);
        int i = Spectrums.mostIntensivePeakWithin(spectrum, mz, dev);
        if (i>=0) {
            return buildTrace(spectrum, new ScanPoint(startingPoint, spectrum.getMzAt(i), spectrum.getIntensityAt(i)));
        } else {
            return Optional.empty(); // no chromatographic peak detected
        }
    }

    private Optional<ChromatographicPeak> buildTrace(SimpleSpectrum spectrum, ScanPoint scanPoint) {
        Optional<ChromatographicPeak> peak = cache.retrieve(scanPoint);
        if (peak.isPresent()) {
            return peak;
        }
        final MutableChromatographicPeak rightTrace = new MutableChromatographicPeak();
        final MutableChromatographicPeak leftTrace = new MutableChromatographicPeak();
        rightTrace.extendRight(scanPoint);
        leftTrace.extendRight(scanPoint);
        // extend to the right
        for (Scan scan  : sample.run.getScansAfter(scanPoint.getScanNumber()).values()) {
            if (!scan.isMsMs()) {
                if (tryToExtend(rightTrace, scan)) {
                    // go on!
                } else {
                    // cannot extend further...
                    break;
                }
            }
        }
        // extend to the left
        for (Scan scan  : sample.run.getScansBefore(scanPoint.getScanNumber()).values()) {
            if (!scan.isMsMs()) {
                if (tryToExtend(leftTrace, scan)) {
                    // go on!
                } else {
                    // cannot extend further...
                    break;
                }
            }
        }
        MutableChromatographicPeak concat = MutableChromatographicPeak.concat(leftTrace, rightTrace);
        if (concat.numberOfScans()<=2)
            return Optional.empty();

        // make statistics about deviations within

        Extrema extrema = detectExtrema(concat);

        for (int k=0, n=extrema.numberOfExtrema(); k < n; ++k) {
            if (!extrema.isMinimum(k)) {
                final double intensity = extrema.extrema.get(k);
                final int apexIndex = extrema.getIndexAt(k);
                final int leftIndex, rightIndex;
                if (k > 0) {
                    leftIndex = extrema.getIndexAt(k-1);
                } else leftIndex = 0;

                if (k+1 < n) {
                    rightIndex = extrema.getIndexAt(k+1);
                } else rightIndex = concat.numberOfScans()-1;
                concat.addSegment(leftIndex, apexIndex, rightIndex);
            }
        }

        if (concat.segments.size()==0) {
            return Optional.empty(); // just noise
        }

        concat.trimEdges();
        // if scanPoint is removed during trimming, this is not a real chromatogram
        if (concat.getScanPointForScanId(scanPoint.getScanNumber())==null) return Optional.empty();

        if (cache!=null) cache.add(concat);
        return Optional.of(concat);
    }


    /*
    - first split a chromatogram into maxima and minima
    - calculate a "noise level" which is mainly the 33% quantile of the slopes in the chromatogram
    - obviously, if a chromatogram is very noise, it is very zick-zack, and the maxima and minima do not mean anything
    - smoothing removes those extrema again.  It is done iteratively: in each round
      we only remove as many extrema, such that still K many extrema remain
      - also, in each round we increase the requirement for smoothing: if we want to remove
      more extrema we have to see larger slopes
      - all this stuff could also be solved without iteration by first sorting all extrema by their slope and then search for the "bend" (ellbow).
     */
    private Extrema detectExtrema(MutableChromatographicPeak peak) {
        // if a chromatographic peak is very long, a single noise level might be problematic. We split it in
        // smaller subgroups. I would say 25 scans are enough to estimate a noise level. For each consecutive 25 scans we define a separate noise level

        float[] noiseLevels = new float[peak.numberOfScans()];
        final TDoubleArrayList medianSlope = new TDoubleArrayList(peak.numberOfScans());
        if (peak.numberOfScans()>=10){
            int k = 0;
            while (k < peak.numberOfScans()) {
                medianSlope.clearQuick();
                int start = k;
                int end = k + 10;
                if (end + 10 > peak.numberOfScans()) end = peak.numberOfScans();
                int middle = start + (end - start) / 2;
                double noiseLevel = 2*sample.ms1NoiseModel.getNoiseLevel(peak.getScanNumberAt(middle), peak.getMzAt(middle));
                for (int i=start; i < end; ++i) {
                    if (i>0) medianSlope.add(Math.abs(peak.getIntensityAt(i) - peak.getIntensityAt(i - 1)));
                }
                medianSlope.sort();
                noiseLevel = Math.max(noiseLevel, medianSlope.getQuick((int)(medianSlope.size()*0.5)));
                //noiseLevel = Math.max(noiseLevel, intensityQuantile.getQuick((int)(intensityQuantile.size()*0.1))/2d);
                for (int i=start; i < end; ++i) noiseLevels[i] = (float)noiseLevel;
                k=end;
            }
        } else {
            for (int i=0; i < peak.numberOfScans(); ++i) {
                noiseLevels[i] = 2*(float)sample.ms1NoiseModel.getNoiseLevel(peak.getScanNumberAt(i), peak.getMzAt(i));
                if (i>0) medianSlope.add(peak.getIntensityAt(i)-peak.getIntensityAt(i-1));
            }
            medianSlope.sort();
            if (medianSlope.size()>0) {
                for (int i = 0; i < peak.numberOfScans(); ++i) {
                    noiseLevels[i] = Math.max(noiseLevels[i], (float)medianSlope.getQuick((int)(medianSlope.size()*0.5)));
                }
            }
        }

        double mxm = 0d;
        for (int i=0; i < peak.numberOfScans(); ++i) mxm = Math.max(mxm, peak.getIntensityAt(i));

        final Extrema extrema = new Extrema();
        boolean minimum = true;
        for (int k=0; k < peak.numberOfScans()-1; ++k) {
            final double a = ((k==0) ? 0 : peak.getIntensityAt(k-1)) + (minimum ? -Float.MIN_VALUE : Float.MIN_VALUE);
            final double b = peak.getIntensityAt(k);
            final double c = peak.getIntensityAt(k+1) + (minimum ? -Float.MIN_VALUE : Float.MIN_VALUE);
            //final double slope = Math.min(Math.abs(b-a),Math.abs(b-c));
            if ((b-a) < 0 && (b - c) < 0) {
                // minimum
                if (minimum) {
                    if (extrema.lastExtremumIntensity() > b)
                        extrema.replaceLastExtremum(k, b);
                } else if (extrema.lastExtremumIntensity() - b > noiseLevels[k]/* || slope/extrema.lastExtremum() >= 0.25*/) {
                    extrema.addExtremum(k, b);
                    minimum = true;
                }
            } else if ((b-a)>0 && (b-c)>0) {
                // maximum
                if (minimum) {
                    if (b - extrema.lastExtremumIntensity() > noiseLevels[k] /* || slope/b>=0.25*/) {
                        extrema.addExtremum(k, b);
                        minimum = false;
                    }
                } else {
                    if (extrema.lastExtremumIntensity() < b) {
                        extrema.replaceLastExtremum(k, b);
                    }
                }
            }

        }
        return extrema;

    }

    private boolean tryToExtend(MutableChromatographicPeak trace, Scan scan) {
        final ScanPoint previous = trace.getRightEdge();
        final SimpleSpectrum spec = sample.storage.getScan(scan);
        final double mz = previous.getMass();
        final double intensity = previous.getIntensity();
        final double mzStd = Math.pow(dev.absoluteFor(mz)/2d,2);
        final double intVar = 1d;
        final double noiseLevel = sample.ms1NoiseModel.getNoiseLevel(scan.getIndex(),mz);
        final int start = Spectrums.indexOfFirstPeakWithin(spec, mz, dev);
        if (start < 0) return false;
        int end;
        for (end=start; end < spec.size(); ++end) {
            if (!dev.inErrorWindow(mz, spec.getMzAt(end)) /*|| spec.getIntensityAt(end) < noiseLevel*/)
                break;
        }
        if (end-start == 1) {
            trace.extendRight(new ScanPoint(scan, spec.getMzAt(start), spec.getIntensityAt(start)));
            return true;
        } else if (end <= start) {
            return false;
        } else {
            int bestIndex=start; double bestScore = 0d;
            for (int k=start; k < end; ++k) {
                double sc = score(spec.getMzAt(k)-mz, Math.log((noiseLevel+spec.getIntensityAt(k))/(noiseLevel+intensity)), mzStd, intVar);
                if (sc > bestScore) {
                    bestIndex = k;
                    bestScore = sc;
                }
            }
            trace.extendRight(new ScanPoint(scan, spec.getMzAt(bestIndex), spec.getIntensityAt(bestIndex)));
            return true;
        }
    }

    private double score(double mzDiff, double intDiff, double mzVar, double intVar) {
        return Math.exp(-(mzDiff*mzDiff/(4*mzVar) + intDiff*intDiff/(4*intVar)) );
    }

    public Deviation getAllowedMassDeviation() {
        return dev;
    }
}
