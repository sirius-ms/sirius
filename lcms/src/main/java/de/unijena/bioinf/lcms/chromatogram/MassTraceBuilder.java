package de.unijena.bioinf.lcms.chromatogram;

import com.google.common.collect.Range;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.lcms.ProcessedSample;
import de.unijena.bioinf.model.lcms.Scan;
import de.unijena.bioinf.model.lcms.ScanPoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MassTraceBuilder {


    public MassTrace detectExact(MassTraceCache cache, ProcessedSample sample, Scan startingPoint, double mz) {
        final SimpleSpectrum spectrum = sample.storage.getScan(startingPoint);
        int i = Spectrums.binarySearch(spectrum, mz, new Deviation(15));
        if (i>=0) {
            return buildTrace(cache, sample, new ScanPoint(startingPoint, spectrum.getMzAt(i), spectrum.getIntensityAt(i)));
        } else {
            return MassTrace.empty(); // no chromatographic peak detected
        }
    }

    public MassTrace detect(MassTraceCache cache, ProcessedSample sample, Scan startingPoint, double mz) {
        final SimpleSpectrum spectrum = sample.storage.getScan(startingPoint);
        int i = Spectrums.mostIntensivePeakWithin(spectrum, mz, new Deviation(15));
        if (i>=0) {
            return buildTrace(cache, sample, new ScanPoint(startingPoint, spectrum.getMzAt(i), spectrum.getIntensityAt(i)));
        } else {
            return MassTrace.empty(); // no chromatographic peak detected
        }
    }

    public MassTrace detect(MassTraceCache cache, ProcessedSample sample, Range<Integer> scanRange, double mz) {
        // pick most intensive peak in scan range
        ScanPoint best = null;
        SimpleSpectrum bestSpec = null;
        for (Scan s : sample.run.getScans(scanRange.lowerEndpoint(), scanRange.upperEndpoint()).values()) {
            if (!s.isMsMs() && scanRange.contains(s.getIndex())) {
                final SimpleSpectrum spectrum = sample.storage.getScan(s);
                int i = Spectrums.mostIntensivePeakWithin(spectrum, mz, new Deviation(15));
                if (i>=0) {
                    if (best==null || spectrum.getIntensityAt(i) > best.getIntensity()) {
                        best = new ScanPoint(s, spectrum.getMzAt(i), spectrum.getIntensityAt(i));
                        bestSpec = spectrum;
                    }
                }
            }
        }
        if (best==null) return MassTrace.empty();
        return buildTrace(cache, sample, best);
    }


    public MassTrace buildTrace(MassTraceCache cache, ProcessedSample sample, ScanPoint scanPoint) {
        final MassTrace cachedValue = cache.retrieve(scanPoint);
        if (!cachedValue.isEmpty()) return cachedValue;
        final ArrayList<ScanPoint> rightTrace = new ArrayList<>(), leftTrace = new ArrayList<>();
        rightTrace.add(scanPoint);
        leftTrace.add(scanPoint);
        // extend to the right
        for (Scan scan  : sample.run.getScansAfter(scanPoint.getScanNumber()).values()) {
            if (!scan.isMsMs()) {
                if (tryToExtend(sample, rightTrace, scan)) {
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
                if (tryToExtend(sample, leftTrace, scan)) {
                    // go on!
                } else {
                    // cannot extend further...
                    break;
                }
            }
        }
        // concat
        Collections.reverse(leftTrace);
        leftTrace.remove(leftTrace.size()-1);
        leftTrace.addAll(rightTrace);
        final MassTrace massTrace = new MassTrace(leftTrace);
        if (!massTrace.isEmpty()) {
            cache.add(massTrace);
        }
        return massTrace;
    }

    private boolean tryToExtend(ProcessedSample sample, List<ScanPoint> trace, Scan scan) {
        final Deviation dev = new Deviation(15);
        final ScanPoint previous = trace.get(trace.size()-1);
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
            if (!dev.inErrorWindow(mz, spec.getMzAt(end)) || spec.getIntensityAt(end) < noiseLevel)
                break;
        }
        if (end-start == 1) {
            trace.add(new ScanPoint(scan, spec.getMzAt(start), spec.getIntensityAt(start)));
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
            trace.add(new ScanPoint(scan, spec.getMzAt(bestIndex), spec.getIntensityAt(bestIndex)));
            return true;
        }
    }

    private double score(double mzDiff, double intDiff, double mzVar, double intVar) {
        return Math.exp(-(mzDiff*mzDiff/(4*mzVar) + intDiff*intDiff/(4*intVar)) );
    }

}
