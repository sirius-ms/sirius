package de.unijena.bioinf.lcms.align;

import de.unijena.bioinf.lcms.ScanPointMapping;
import de.unijena.bioinf.lcms.trace.ProcessedSample;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;


public class AlignmentBackbone implements AlignWithRecalibration{
    @Getter @Setter
    private ProcessedSample[] samples;
    @Getter @Setter
    private ScanPointMapping scanPointMapping;
    @Getter @Setter
    private AlignmentStatistics statistics;
    private final HashMap<Integer, ProcessedSample> idx2sample;

    @Builder
    public AlignmentBackbone(ProcessedSample[] samples, ScanPointMapping scanPointMapping, AlignmentStatistics statistics) {
        this.samples = samples;
        this.scanPointMapping = scanPointMapping;
        this.statistics = statistics;
        this.idx2sample = new HashMap<>();
        for (ProcessedSample s : samples) idx2sample.put(s.getUid(), s);
    }

    @Override
    public double getRecalibratedRt(MoI moi) {
        return (moi instanceof AlignedMoI) ? moi.getRetentionTime() : idx2sample.get(moi.getSampleIdx()).getRtRecalibration().value(moi.getRetentionTime());
    }

    @Override
    public RecalibrationFunction getRecalibrationFor(MoI moi) {
        return (moi instanceof AlignedMoI) ? RecalibrationFunction.identity() : idx2sample.get(moi.getSampleIdx()).getRtRecalibration();
    }
}
