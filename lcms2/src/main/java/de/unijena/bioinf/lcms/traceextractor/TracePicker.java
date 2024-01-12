package de.unijena.bioinf.lcms.traceextractor;

import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.lcms.ScanPointMapping;
import de.unijena.bioinf.lcms.trace.ContiguousTrace;
import de.unijena.bioinf.lcms.trace.LCMSStorage;
import de.unijena.bioinf.lcms.trace.ProcessedSample;
import de.unijena.bioinf.lcms.trace.segmentation.TraceSegment;
import de.unijena.bioinf.lcms.trace.segmentation.TraceSegmentationStrategy;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import lombok.Getter;

import java.util.Optional;

public class TracePicker {

    private final static int MIN_SCANPOINT_THRESHOLD = 3;

    protected LCMSStorage storage;

    protected TraceCachingStrategy.Cache cache;

    protected TraceSegmentationStrategy segmentationStrategy;

    @Getter
    protected Deviation allowedMassDeviation;
    private Deviation smallerDev;

    protected ScanPointMapping mapping;

    public TracePicker(ProcessedSample sample, TraceCachingStrategy cachingStrategy, TraceSegmentationStrategy segmentationStrategy) {
        this.storage = sample.getStorage();
        setAllowedMassDeviation(new Deviation(10));
        this.mapping = sample.getMapping();
        this.cache = cachingStrategy.getCacheFor(sample);
        this.segmentationStrategy = segmentationStrategy;
    }

    public void setAllowedMassDeviation(Deviation dev) {
        this.allowedMassDeviation = dev;
        this.smallerDev = dev.divide(3d);
    }

    private int extendLeft(IntArrayList spectrumIds, DoubleArrayList mzs, FloatArrayList intensities, int lastPeakId) {
        int spectrumId = spectrumIds.getInt(spectrumIds.size()-1);
        SimpleSpectrum spectrum = storage.getSpectrumStorage().getSpectrum(spectrumId);
        for (int k=spectrumId-1; k >= 0; --k) {
            SimpleSpectrum other = storage.getSpectrumStorage().getSpectrum(k);
            int j = getConnected(spectrum, other, lastPeakId);
            if (j>=0) {
                spectrum=other;
                lastPeakId=j;
                mzs.add(spectrum.getMzAt(j));
                intensities.add((float)spectrum.getIntensityAt(j));
                spectrumIds.add(k);
            } else break;
        }
        return lastPeakId;
    }
    private int extendLeftUntil(IntArrayList spectrumIds, DoubleArrayList mzs, FloatArrayList intensities, double seedMz, int lastPeakId, int minimumScanId) {
        int spectrumId = spectrumIds.getInt(spectrumIds.size()-1); ;
        SimpleSpectrum spectrum = storage.getSpectrumStorage().getSpectrum(spectrumId);
        for (int k=spectrumId-1; k >= minimumScanId; --k) {
            SimpleSpectrum other = storage.getSpectrumStorage().getSpectrum(k);
            int j = (lastPeakId<0) ? findSeed(other, seedMz) : getConnected(spectrum, other, lastPeakId);
            if (j>=0) {
                lastPeakId=j;
                mzs.add(spectrum.getMzAt(j));
                intensities.add((float)spectrum.getIntensityAt(j));
                spectrumIds.add(k);
            } else {
                mzs.add(Double.NaN);
                intensities.add(0);
                spectrumIds.add(-1);
                lastPeakId=-1;
            };
            spectrum=other;
        }
        // remove leading zeroes
        while (spectrumIds.getInt(spectrumIds.size()-1)<0) {
            spectrumIds.popInt();
            mzs.popDouble();
            intensities.popFloat();
        }
        return lastPeakId;
    }
    private int extendRight(IntArrayList ids, DoubleArrayList mzs, FloatArrayList intensities, int lastPeakId) {
        int spectrumId = ids.getInt(ids.size()-1);
        SimpleSpectrum spectrum = storage.getSpectrumStorage().getSpectrum(spectrumId);
        for (int k=spectrumId+1; k < mapping.length(); ++k) {
            SimpleSpectrum other = storage.getSpectrumStorage().getSpectrum(k);
            int j = getConnected(spectrum, other, lastPeakId);
            if (j>=0) {
                spectrum=other;
                lastPeakId=j;
                mzs.add(spectrum.getMzAt(j));
                intensities.add((float)spectrum.getIntensityAt(j));
                ids.add(k);
            } else break;
        }
        return lastPeakId;
    }
    private int extendRightUntil(IntArrayList ids, DoubleArrayList mzs, FloatArrayList intensities, double seedMz, int lastPeakId, int maximumScanId) {
        int spectrumId = ids.getInt(ids.size()-1);
        SimpleSpectrum spectrum = storage.getSpectrumStorage().getSpectrum(spectrumId);
        for (int k=spectrumId+1; k <= maximumScanId; ++k) {
            SimpleSpectrum other = storage.getSpectrumStorage().getSpectrum(k);
            int j = (lastPeakId<0) ? findSeed(other, seedMz) : getConnected(spectrum, other, lastPeakId);
            if (j>=0) {
                lastPeakId=j;
                mzs.add(spectrum.getMzAt(j));
                intensities.add((float)spectrum.getIntensityAt(j));
                ids.add(k);
            } else {
                mzs.add(Double.NaN);
                intensities.add(0);
                ids.add(-1);
                lastPeakId=-1;
            };
            spectrum=other;
        }
        // remove leading zeroes
        while (ids.getInt(ids.size()-1)<0) {
            ids.popInt();
            mzs.popDouble();
            intensities.popFloat();
        }
        return lastPeakId;
    }

    private int findSeed(SimpleSpectrum spectrum, double mz) {
        int peakId = Spectrums.mostIntensivePeakWithin(spectrum, mz, smallerDev);
        if (peakId < 0) peakId = Spectrums.mostIntensivePeakWithin(spectrum, mz, allowedMassDeviation);
        if (peakId >= 0) {
            return peakId;
        } else return -1;
    }

    private int findSeed(SimpleSpectrum spectrum, int spectrumId, double mz, IntArrayList spectrumIds, DoubleArrayList mzs, FloatArrayList intensities) {
        int peakId = findSeed(spectrum,mz);
        spectrumIds.add(spectrumId);
        intensities.add((float)spectrum.getIntensityAt(peakId));
        mzs.add(spectrum.getMzAt(peakId));
        return peakId;
    }



    public Optional<ContiguousTrace> detectTrace(int spectrumId, double mz) {
        Optional<ContiguousTrace> traceFromCache = cache.getTraceFromCache(spectrumId, mz);
        if (traceFromCache.isPresent()) return traceFromCache;
        Optional<ContiguousTrace> detected = pickTrace(spectrumId, mz);
        return detected.map(x->{
            x.setSegments(segmentationStrategy.detectSegments(storage.getStatistics(), x).toArray(TraceSegment[]::new));
            return cache.addTraceToCache(x);
        });
    }

    public Optional<ContiguousTrace> detectMostIntensivePeakWithin(int id, double startMz, double endMz) {
        // first pick spectrum
        final SimpleSpectrum spectrum = storage.getSpectrumStorage().getSpectrum(id);
        final int index = Spectrums.mostIntensivePeakWithin(spectrum, startMz, endMz);
        if (index < 0) return Optional.empty();
        // now search for the peak
        return detectTrace(id, spectrum.getMzAt(index));
    }

    public Optional<ContiguousTrace> detectMostIntensivePeak(int id, double mz) {
        final double abs = allowedMassDeviation.absoluteFor(mz);
        return detectMostIntensivePeakWithin(id, mz-abs, mz+abs);
    }


    private Optional<ContiguousTrace> pickTrace(int spectrumId, double mz) {
        SimpleSpectrum spectrum = storage.getSpectrumStorage().getSpectrum(spectrumId);
        final DoubleArrayList masses = new DoubleArrayList();
        final FloatArrayList intensities = new FloatArrayList();
        final IntArrayList spectrumIds = new IntArrayList();
        final int initialId = findSeed(spectrum, spectrumId, mz, spectrumIds, masses, intensities);
        if (initialId<0) return Optional.empty();
        extendLeft(spectrumIds, masses, intensities, initialId);
        rev(spectrumIds,masses,intensities);
        extendRight(spectrumIds, masses, intensities, initialId);
        if (spectrumIds.size() < MIN_SCANPOINT_THRESHOLD) return Optional.empty();
        return Optional.of(new ContiguousTrace(
                mapping, spectrumIds.getInt(0), spectrumIds.getInt(spectrumIds.size() - 1), masses.toDoubleArray(), intensities.toFloatArray()));
    }


    /**
     * we only connect two peaks by a trace if from both sides the other peak is closest in mass
     */
    private boolean connect(SimpleSpectrum left, SimpleSpectrum right, int leftIdx, int rightIdx) {
        Deviation smallerDev = allowedMassDeviation.divide(3d);

        final double leftMz = left.getMzAt(leftIdx), rightMz = right.getMzAt(rightIdx);
        int i = Spectrums.mostIntensivePeakWithin(left, rightMz, smallerDev);
        if (i<0) i = Spectrums.mostIntensivePeakWithin(left, rightMz, allowedMassDeviation);

        int j = Spectrums.mostIntensivePeakWithin(right, leftMz, smallerDev);
        if (j<0) j = Spectrums.mostIntensivePeakWithin(right, leftMz, allowedMassDeviation);

        return i==leftIdx && j==rightIdx;
    }
    private int getConnected(SimpleSpectrum left, SimpleSpectrum right, int leftIdx) {
        Deviation smallerDev = allowedMassDeviation.divide(3d);
        final double leftMz = left.getMzAt(leftIdx);

        int j = Spectrums.mostIntensivePeakWithin(right, leftMz, smallerDev);
        if (j<0) j = Spectrums.mostIntensivePeakWithin(right, leftMz, allowedMassDeviation);
        if (j<0) return -1;
        final double rightMz = right.getMzAt(j);

        int i = Spectrums.mostIntensivePeakWithin(left, rightMz, smallerDev);
        if (i<0) i = Spectrums.mostIntensivePeakWithin(left, rightMz, allowedMassDeviation);
        if (i<0) return -1;
        return i==leftIdx ? j : -1;
    }

    private void rev(IntArrayList ids, DoubleArrayList masses, FloatArrayList intensities) {
        rev(ids);
        rev(intensities);
        rev(masses);
    }

    private void rev(DoubleArrayList masses) {
        final int n = masses.size();
        for (int i=0; i < n/2; ++i) {
            double a = masses.getDouble(i);
            double b = masses.getDouble(n-(i+1));
            masses.set(i,b);
            masses.set(n-(i+1), a);
        }
    }
    private void rev(FloatArrayList masses) {
        final int n = masses.size();
        for (int i=0; i < n/2; ++i) {
            float a = masses.getFloat(i);
            float b = masses.getFloat(n-(i+1));
            masses.set(i,b);
            masses.set(n-(i+1), a);
        }
    }
    private void rev(IntArrayList masses) {
        final int n = masses.size();
        for (int i=0; i < n/2; ++i) {
            int a = masses.getInt(i);
            int b = masses.getInt(n-(i+1));
            masses.set(i,b);
            masses.set(n-(i+1), a);
        }
    }


}
