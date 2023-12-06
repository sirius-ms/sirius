package de.unijena.bioinf.lcms.trace;

import com.google.common.collect.Range;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.MsInstrumentation;
import de.unijena.bioinf.ChemistryBase.ms.lcms.MsDataSourceReference;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.io.lcms.MzMLParser;
import de.unijena.bioinf.lcms.MemoryFileStorage;
import de.unijena.bioinf.lcms.ScanPointMapping;
import de.unijena.bioinf.lcms.SpectrumStorage;
import de.unijena.bioinf.lcms.spectrum.Ms2SpectrumHeader;
import de.unijena.bioinf.lcms.trace.segmentation.ApexDetection;
import de.unijena.bioinf.lcms.trace.segmentation.LegacySegmenter;
import de.unijena.bioinf.model.lcms.LCMSRun;
import de.unijena.bioinf.model.lcms.Scan;
import it.unimi.dsi.fastutil.floats.FloatArrayList;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ProcessedSample {

    private LCMSStorage traceStorage;
    private MsDataSourceReference sourceReference;

    private MsInstrumentation instrumentation;
    private ScanPointMapping mapping;
    private int numberOfHighQualityTraces;

    public ProcessedSample(MsDataSourceReference reference, MsInstrumentation instrumentation, ScanPointMapping scanPointMapping, LCMSStorage storage)  {
        this.mapping = scanPointMapping;
        this.sourceReference = reference;
        this.instrumentation = instrumentation;
        this.traceStorage = storage;
    }

    public void detectTraces() {
        TracePicker picker = new TracePicker(traceStorage, mapping);
        ApexDetection apexDetection = new LegacySegmenter();
        AssociatedPeakDetector aso = new AssociatedPeakDetector(traceStorage, new HashSet<>(Arrays.asList(
                PrecursorIonType.fromString("[M+H]+"),PrecursorIonType.fromString("[M+Na]+"),
                PrecursorIonType.fromString("[M-H2O+H]+"),PrecursorIonType.fromString("[M+K]+"),
                PrecursorIonType.fromString("[M+NH3+H]+")
        )));
        // first pick all Traces corresponding to MS/MS
        FloatArrayList medianIntensities = new FloatArrayList();
        for (Ms2SpectrumHeader h : traceStorage.ms2SpectraHeader()) {
            Optional<ContiguousTrace> contiguousTrace = picker.detectMostIntensivePeak(h.getParentId(), h.getPrecursorMz());
            if (contiguousTrace.isPresent()) {
                ContiguousTrace t = contiguousTrace.get();
                aso.detectAllAssociatedTraces(picker, apexDetection, t);
                SimpleSpectrum spec = traceStorage.getSpectrum(t.apex());
                medianIntensities.add((float)Spectrums.getMedianIntensity(spec));
            }
        }
        final float intensityThreshold;
        if (medianIntensities.size()>=20) {
            intensityThreshold = 4*(float)medianIntensities.doubleStream().average().orElse(0d);
        } else {
            // pick 20 random spectra
            int stepSize = Math.max(1, mapping.length()/30);
            for (int j=1; j <= 20; ++j) {
                SimpleSpectrum spec = traceStorage.getSpectrum(j*stepSize);
                medianIntensities.add((float)Spectrums.getMedianIntensity(spec));
            }
            intensityThreshold = 4*(float)medianIntensities.doubleStream().average().orElse(0d);
        }
        System.out.println("intensityThreshold = " + intensityThreshold);
        // lets do the extreme case: pick all traces above 1e5
        final long time = System.currentTimeMillis();
        int good=0;
        for (int sid=0; sid < mapping.length(); ++sid) {
            SimpleSpectrum t = traceStorage.getSpectrum(sid);
            for (int i = 0; i < t.size(); ++i) {
                if (t.getIntensityAt(i) > intensityThreshold) {
                    Optional<ContiguousTrace> contiguousTrace = picker.detectTrace(sid, t.getMzAt(i));
                    contiguousTrace.ifPresent(trace -> aso.detectAllAssociatedTraces(picker, apexDetection, trace));
                }
            }
        }
        this.numberOfHighQualityTraces = 0;
        for (int i=0; i < traceStorage.numberOfTraces(); ++i) {
            if (traceStorage.getTraceNode(i).confidenceScore >= 2) ++numberOfHighQualityTraces;
        }
        System.out.println(numberOfHighQualityTraces);
        new TraceConnector().connect(traceStorage);
    }

    public LCMSStorage getTraceStorage() {
        return traceStorage;
    }

    public ScanPointMapping getMapping() {
        return mapping;
    }

    public int getNumberOfHighQualityTraces() {
        return numberOfHighQualityTraces;
    }

    public void inactive() {
        traceStorage.setLowMemoryInactiveMode(true);
    }

    public void active() {
        traceStorage.setLowMemoryInactiveMode(false);
    }

    public MsDataSourceReference getReference() {
        return sourceReference;
    }

    public Range<Double> getRtSpan() {
        return Range.closed(mapping.getRetentionTimeAt(0),mapping.getRetentionTimeAt(mapping.length()-1));
    }
}
