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

import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.IsolationWindow;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.model.lcms.*;
import gnu.trove.list.array.TDoubleArrayList;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.Range;

import java.util.Optional;
import java.util.function.Consumer;

public class ChromatogramBuilder {

    protected final ProcessedSample sample;
    protected final Deviation dev;
    protected final ChromatogramCache cache;

    @Setter
    @Getter
    protected boolean trimEdges=true;

    public ChromatogramBuilder(ProcessedSample sample) {
        this.sample = sample;
        this.dev = new Deviation(20);
        this.cache = new ChromatogramCache();
    }

    public Optional<ChromatographicPeak> detectExact(Scan startingPoint, double mz) {
        final SimpleSpectrum spectrum = sample.storage.getScan(startingPoint);
        int i = Spectrums.binarySearch(spectrum, mz, dev);
        return buildTraceOrReturnEmpty(i, spectrum, startingPoint);
    }

    public Optional<ChromatographicPeak> detectFirst(Range<Integer> scanRange, int middle, double mz) {
        // pick most intensive peak in scan range
        int left = middle;
        int right = middle+1;
        while (scanRange.contains(left) || scanRange.contains(right)) {
            if (scanRange.contains(left)) {
                final Optional<Scan> t = sample.run.getScanByNumber(left);
                if (t.isPresent() && !t.get().isMsMs()) {
                    final SimpleSpectrum spectrum = sample.storage.getScan(t.get());
                    int i = Spectrums.mostIntensivePeakWithin(spectrum, mz, dev);
                    if (i>=0) {
                        Optional<ChromatographicPeak> peak = buildTrace(spectrum, new ScanPoint(t.get(), spectrum.getMzAt(i), spectrum.getIntensityAt(i)));
                        if (peak.isPresent()) return peak;
                    }
                }
                --left;
            }
            if (scanRange.contains(right)) {
                final Optional<Scan> t = sample.run.getScanByNumber(right);
                if (t.isPresent() && !t.get().isMsMs()) {
                    final SimpleSpectrum spectrum = sample.storage.getScan(t.get());
                    int i = Spectrums.mostIntensivePeakWithin(spectrum, mz, dev);
                    if (i>=0) {
                        Optional<ChromatographicPeak> peak = buildTrace(spectrum, new ScanPoint(t.get(), spectrum.getMzAt(i), spectrum.getIntensityAt(i)));
                        if (peak.isPresent()) return peak;
                    }
                }
                ++right;
            }

        }
        return Optional.empty();
    }

    public Optional<ChromatographicPeak> detect(Range<Integer> scanRange, double mz) {
        // pick most intensive peak in scan range
        ScanPoint best = null;
        SimpleSpectrum bestSpec = null;
        for (Scan s : sample.run.getScans(scanRange.getMinimum(), scanRange.getMaximum()).values()) {
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

    public void detectWithFallback(Scan startingPoint, double mz, Consumer<ChromatographicPeak> whenTraceFound, Consumer<ScanPoint> whenPeakFound, Runnable whenNothingFound) {
        final SimpleSpectrum spectrum = sample.storage.getScan(startingPoint);
        int i = Spectrums.mostIntensivePeakWithin(spectrum, mz, dev);
        if (i>=0) {
            Optional<ChromatographicPeak> trace = buildTrace(spectrum, new ScanPoint(startingPoint, spectrum.getMzAt(i), spectrum.getIntensityAt(i)));
            if (trace.isPresent()) {
                whenTraceFound.accept(trace.get());
            } else {
                whenPeakFound.accept(new ScanPoint(startingPoint, spectrum.getMzAt(i), spectrum.getIntensityAt(i)));
            }
        } else {
            whenNothingFound.run();
        }
    }

    public Optional<ScanPoint> detectSingleScanPoint(Scan startingPoint, double mz) {
        final SimpleSpectrum spectrum = sample.storage.getScan(startingPoint);
        int i = Spectrums.mostIntensivePeakWithin(spectrum, mz, dev);
        if (i>=0) {
            return Optional.of(new ScanPoint(startingPoint, spectrum.getMzAt(i), spectrum.getIntensityAt(i)));
        } else {
            return Optional.empty(); // no chromatographic peak detected
        }
    }

    public Optional<ChromatographicPeak> detect(Scan startingPoint, double isolationTargetMz, IsolationWindow window) {
        if (window != null) {
            //search in the middle 33% of the window
            final Deviation leftAbsDev = new Deviation(0, window.getLeftOffset() * 0.33);
            final Deviation rightAbsDev = new Deviation(0, window.getRightOffset() * 0.33);
            //todo for windows that isolate the whole isotope pattern a monoisotopic peak detection would be good (to not select the +1 peak with some uncommon elements)
            return detect(startingPoint, isolationTargetMz, leftAbsDev, rightAbsDev); //todo may assuming a normal distribution be better?
        } else {
            return detect(startingPoint, isolationTargetMz, dev);
        }
    }

    public Optional<ChromatographicPeak> detect(Scan startingPoint, double mz, Deviation leftWindow, Deviation rightWindow) {
        final SimpleSpectrum spectrum = sample.storage.getScan(startingPoint);

        double begin = mz - leftWindow.absoluteFor(mz);
        double end = mz + rightWindow.absoluteFor(mz);
        int i = Spectrums.mostIntensivePeakWithin(spectrum, begin, end);

        return buildTraceOrReturnEmpty(i, spectrum, startingPoint);
    }

    public Optional<ChromatographicPeak> detect(Scan startingPoint, double mz, Deviation window) {
        final SimpleSpectrum spectrum = sample.storage.getScan(startingPoint);
        int i = Spectrums.mostIntensivePeakWithin(spectrum, mz, window);
        return buildTraceOrReturnEmpty(i, spectrum, startingPoint);
    }

    private Optional<ChromatographicPeak> buildTraceOrReturnEmpty(int i, SimpleSpectrum spectrum, Scan startingPoint) {
        if (i >=0) {
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

        final TDoubleArrayList slopes = new TDoubleArrayList(concat.numberOfScans());
        for (int k=1, n = concat.numberOfScans(); k < n; ++k) {
            double i = concat.getIntensityAt(k), j = concat.getIntensityAt(k-1);
            if (i>j) slopes.add(i/j);
            else slopes.add(j/i);
        }
        slopes.sort();
        final double slope66 = slopes.get((int)Math.floor(slopes.size()*0.33));//slopes.get((int)Math.floor(slopes.size()*0.66));

        // make statistics about deviations within

        Extrema extrema = detectExtremaWithSmoothing(concat, sample.ms1NoiseModel.getNoiseLevel(scanPoint.getScanNumber(), scanPoint.getMass())*5);
        {
            double[] intensities = new double[concat.numberOfScans()];
            for (int k=0; k < concat.numberOfScans(); ++k) intensities[k] = concat.getIntensityAt(k);
        }
        //detectExtremaWithSmoothing2(concat);
                            //detectExtrema(concat);
        final int before = extrema.numberOfExtrema();
        //extrema.deleteExtremaOfSinglePeaks(slope66);
        final int after = extrema.numberOfExtrema();
        for (int k=0, n=extrema.numberOfExtrema(); k < n; ++k) {
            if (!extrema.isMinimum(k)) {
                final double intensity = extrema.extrema.get(k);
                final int apexIndex = extrema.getIndexAt(k) ;
                int leftIndex, rightIndex;
                if (k > 0) {
                    leftIndex = extrema.getIndexAt(k-1);
                } else leftIndex = 0;

                if (k+1 < n) {
                    rightIndex = extrema.getIndexAt(k+1);
                } else rightIndex = concat.numberOfScans()-1;
                concat.addSegment(leftIndex, apexIndex, rightIndex, (concat.getIntensityAt(apexIndex) / Math.min(concat.getIntensityAt(leftIndex), concat.getIntensityAt(rightIndex)) < 3));
            }
        }

        if (concat.segments.size()==0) {
            return Optional.empty(); // just noise
        }

        if (trimEdges) concat.trimEdges();
        // if scanPoint is removed during trimming, this is not a real chromatogram
        if (concat.getScanPointForScanId(scanPoint.getScanNumber())==null) return Optional.empty();

        if (cache!=null) cache.add(concat);
        return Optional.of(concat);
    }

    private boolean hasMinimaAtEnd(ChromatographicPeak peak) {
        return peak.getIntensityAt(0) < peak.getSegments().firstEntry().getValue().getApexIntensity() && peak.getIntensityAt(peak.numberOfScans()-1) < peak.getSegments().lastEntry().getValue().getApexIntensity();
    }

    private interface TIntDoubleFunction {
        public double call(int i);
    }

    private Extrema detectExtremaWithSmoothing(MutableChromatographicPeak peak, double noise) {
        final double[] intensities = new double[peak.numberOfScans()];
        for (int k=0; k < peak.numberOfScans(); ++k) intensities[k] = peak.getIntensityAt(k);

        final SavitzkyGolayFilter filter = Extrema.getProposedFilter3(intensities);
        final Maxima maxima = new Maxima(intensities);
        if (filter!=null) {
            final double[] smoothedIntensities = filter.applyExtended(intensities);
            maxima.useSmoothedFilterForGuidance(smoothedIntensities);
        }
        while (maxima.split(noise*4, 0.05)) {}
        return maxima.toExtrema();

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

    private static class RingBuffer {
        private double[] buf;
        private int offset = 0;
        private double meanSum = 0d;

        public RingBuffer(double[] init) {
            this.buf = init;
            this.offset = 0;
            for (double v : init) meanSum += v;
        }

        public double mean() {
            return meanSum / buf.length;
        }

        public double std() {
            double m = mean();
            double s = 0d;
            for (double v : buf) s += (v-m)*(v-m);
            return Math.sqrt(s)/buf.length;
        }

        public void put(double value) {
            meanSum -= buf[offset];
            buf[offset++] = value;
            meanSum += value;
            if (offset>=buf.length) offset=0;
        }
    }

}
