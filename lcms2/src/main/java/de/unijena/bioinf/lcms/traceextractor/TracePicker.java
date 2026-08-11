package de.unijena.bioinf.lcms.traceextractor;

import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.lcms.ScanPointMapping;
import de.unijena.bioinf.lcms.spectrum.SpectrumStorage;
import de.unijena.bioinf.lcms.statistics.SampleStats;
import de.unijena.bioinf.lcms.trace.ContiguousTrace;
import de.unijena.bioinf.lcms.trace.LCMSStorage;
import de.unijena.bioinf.lcms.trace.ProcessedSample;
import de.unijena.bioinf.lcms.trace.segmentation.TraceSegment;
import de.unijena.bioinf.lcms.trace.segmentation.TraceSegmentationStrategy;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntIterator;
import lombok.Getter;

import java.util.Optional;

public class TracePicker extends AbstractTracePicker {

    protected SpectrumStorage spectrumStorage;
    protected ScanPointMapping mapping;
    protected SampleStats stats;

    public TracePicker(ProcessedSample sample, TraceCachingStrategy cachingStrategy, TraceSegmentationStrategy segmentationStrategy) {
        super(sample, cachingStrategy, segmentationStrategy);
        this.mapping = sample.getMapping();
        this.spectrumStorage = sample.getStorage().getSpectrumStorage();
        this.stats = sample.getStorage().getStatistics();
    }

    @Override
    public SimpleSpectrum getSpectrum(int spectrumId) {
        return spectrumStorage.getSpectrum(spectrumId);
    }

    @Override
    public IntIterator iterLeftFrom(int spectrumId) {
        return new IntIterator() {
            private int index = spectrumId-1;
            @Override
            public int nextInt() {
                return index--;
            }

            @Override
            public boolean hasNext() {
                return index>=0;
            }
        };
    }

    @Override
    public float getNoiseLevelFor(ContiguousTrace trace) {
        return stats.noiseLevel(trace.apex());
    }

    @Override
    public float getExpectedPeakWidth(ContiguousTrace trace) {
        return (stats.getExpectedPeakWidth().orElse(0d)).floatValue();
    }

    @Override
    public ContiguousTrace makeTrace(IntArrayList spectrumIds, DoubleArrayList masses, FloatArrayList intensities) {
        return new ContiguousTrace(mapping, spectrumIds.getInt(0), spectrumIds.getInt(spectrumIds.size()-1), masses.toDoubleArray(), intensities.toFloatArray());
    }

    @Override
    public IntIterator iterRightFrom(int spectrumId) {
        final int n = mapping.length();
        return new IntIterator() {
            private int index = spectrumId+1;
            @Override
            public int nextInt() {
                return index++;
            }

            @Override
            public boolean hasNext() {
                return index<n;
            }
        };
    }


}
