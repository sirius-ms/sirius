package de.unijena.bioinf.lcms.trace;

import org.apache.commons.lang3.Range;
import de.unijena.bioinf.lcms.ScanPointMapping;
import de.unijena.bioinf.lcms.align.RecalibrationFunction;
import de.unijena.bioinf.lcms.merge.ScanPointInterpolator;
import de.unijena.bioinf.lcms.statistics.NormalizationStrategy;
import de.unijena.bioinf.lcms.statistics.TraceStats;
import de.unijena.bioinf.ms.persistence.model.core.run.AbstractLCMSRun;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

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

    /**
     * For DIA runs that co-isolate several windows in one MS2 scan (multiplexed / MSX acquisition): the absolute
     * m/z bounds of all windows of such a scan, interleaved as [lo,hi,lo,hi,...] and keyed by the uid of the
     * MS2 spectrum header. An {@link de.unijena.bioinf.lcms.spectrum.Ms2SpectrumHeader} can only carry a single
     * isolation window, so this is where the remaining ones survive from parsing until the DIA acquisition scheme
     * is reconstructed. Null for everything that is not multiplexed DIA.
     */
    @Getter @Setter
    @Nullable
    private Int2ObjectMap<float[]> multiplexedIsolationWindows;

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

    @Override
    public String toString() {
        return run.getName();
    }
}
