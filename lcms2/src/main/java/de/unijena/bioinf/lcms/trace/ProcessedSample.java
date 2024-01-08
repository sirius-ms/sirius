package de.unijena.bioinf.lcms.trace;

import com.google.common.collect.Range;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.MsInstrumentation;
import de.unijena.bioinf.ChemistryBase.ms.lcms.MsDataSourceReference;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.lcms.ScanPointMapping;
import de.unijena.bioinf.lcms.align2.RecalibrationFunction;
import de.unijena.bioinf.lcms.merge2.ScanPointInterpolator;
import de.unijena.bioinf.lcms.spectrum.Ms2SpectrumHeader;
import de.unijena.bioinf.lcms.statistics.NormalizationStrategy;
import de.unijena.bioinf.lcms.statistics.TraceStats;
import de.unijena.bioinf.lcms.trace.segmentation.ApexDetection;
import de.unijena.bioinf.lcms.trace.segmentation.LegacySegmenter;
import de.unijena.bioinf.lcms.traceextractor.RectbasedCachingStrategy;
import de.unijena.bioinf.lcms.traceextractor.TracePicker;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

public class ProcessedSample {

    @Getter @Setter
    private int uid;

    @Getter
    private LCMSStorage traceStorage;
    @Getter
    private MsDataSourceReference sourceReference;
    @Getter
    private MsInstrumentation instrumentation;
    @Getter
    private ScanPointMapping mapping;

    @Getter @Setter
    private TraceStats traceStats;

    @Getter @Setter
    private NormalizationStrategy.Normalizer normalizer;

    @Getter @Setter
    RecalibrationFunction rtRecalibration, mzRecalibration;

    @Getter @Setter
    ScanPointInterpolator scanPointInterpolator;

    @Getter
    private int polarity;

    public ProcessedSample(MsDataSourceReference reference, MsInstrumentation instrumentation, ScanPointMapping scanPointMapping, LCMSStorage storage, int polarity, int uid)  {
        this.mapping = scanPointMapping;
        this.sourceReference = reference;
        this.instrumentation = instrumentation;
        this.traceStorage = storage;
        this.polarity = polarity;
        this.rtRecalibration = RecalibrationFunction.identity();
        this.mzRecalibration = RecalibrationFunction.identity();
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
