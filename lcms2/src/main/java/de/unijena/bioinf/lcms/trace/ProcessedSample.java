package de.unijena.bioinf.lcms.trace;

import org.apache.commons.lang3.Range;
import de.unijena.bioinf.lcms.ScanPointMapping;
import de.unijena.bioinf.lcms.align.RecalibrationFunction;
import de.unijena.bioinf.lcms.merge.ScanPointInterpolator;
import de.unijena.bioinf.lcms.statistics.NormalizationStrategy;
import de.unijena.bioinf.lcms.statistics.TraceStats;
import de.unijena.bioinf.ms.persistence.model.core.run.AbstractLCMSRun;
import lombok.Getter;
import lombok.Setter;

public class ProcessedSample {

    @Getter @Setter
    private int uid;

    @Getter @Setter
    private AbstractLCMSRun run;

    @Getter
    private LCMSStorage storage;

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

    public ProcessedSample(ScanPointMapping scanPointMapping, LCMSStorage storage, int polarity, int uid)  {
        this.mapping = scanPointMapping;
        this.storage = storage;
        this.polarity = polarity;
        this.uid = uid;
        this.rtRecalibration = RecalibrationFunction.identity();
        this.mzRecalibration = RecalibrationFunction.identity();
    }

    public void inactive() {
        storage.setLowMemoryInactiveMode(true);
    }

    public void active() {
        storage.setLowMemoryInactiveMode(false);
    }

    public Range<Double> getRtSpan() {
        return Range.of(mapping.getRetentionTimeAt(0),mapping.getRetentionTimeAt(mapping.length()-1));
    }
}
