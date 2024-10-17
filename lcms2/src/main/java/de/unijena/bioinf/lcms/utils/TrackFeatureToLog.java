package de.unijena.bioinf.lcms.utils;

import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.lcms.align.AlignedMoI;
import de.unijena.bioinf.lcms.align.MoI;
import de.unijena.bioinf.lcms.merge.MergedTrace;
import de.unijena.bioinf.lcms.trace.ContiguousTrace;
import de.unijena.bioinf.lcms.trace.ProcessedSample;
import de.unijena.bioinf.lcms.trace.ProjectedTrace;
import de.unijena.bioinf.lcms.trace.Rect;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import org.apache.commons.lang3.Range;
import org.apache.log4j.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TrackFeatureToLog implements Tracker{

    private final Logger logger;

    private final double massToTrack, fromMz, toMz;
    private final Range<Double> retentionTimeToTrack;

    public TrackFeatureToLog(double massToTrack, Range<Double> retentionTimeToTrack) {
        logger = LoggerFactory.getLogger(TrackFeatureToLog.class);
        this.massToTrack = massToTrack;
        Deviation dev = new Deviation(5);
        fromMz = massToTrack-dev.absoluteFor(massToTrack);
        toMz = massToTrack+dev.absoluteFor(massToTrack);
        this.retentionTimeToTrack = retentionTimeToTrack;
    }

    @Override
    public void tracePicked(double mz, double rt, ProcessedSample sample, ContiguousTrace trace) {
        if (tracked(mz, rt)) {
            logger.debug("trace picked with m/z = %f, rt = %f, in sample %s", mz, rt, sample.getRun().getName());
        }
    }

    private boolean tracked(double mz, double rt) {
        return (mz >= fromMz && mz <= toMz && retentionTimeToTrack.contains(rt));
    }

    @Override
    public void moiAccepted(double mz, double retentionTime, ProcessedSample sample, ContiguousTrace trace, MoI moi) {
        if (tracked(mz, retentionTime)) {
            logger.debug("MOI accepted with  m/z = %f, rt = %f, confidence = %f in sample %s", mz, retentionTime, moi.getConfidence(),  sample.getRun().getName());
        }
    }

    @Override
    public void moiRejected(double mz, double retentionTime, ProcessedSample sample, ContiguousTrace trace, MoI moi) {
        if (tracked(mz, retentionTime)) {
            logger.debug("MOI REJECTEDwith  m/z = %f, rt = %f, confidence = %f in sample %s", mz, retentionTime, moi.getConfidence(),  sample.getRun().getName());
        }
    }

    @Override
    public void alignMois(ProcessedSample rightSample, MoI left, MoI right) {
        if (tracked(left.getMz(), left.getRetentionTime())) {
            logger.debug("ALIGN two MOIS with m/z %f, rt left = %f, rt right = %f in sample %s", left.getMz(), left.getRetentionTime(), right.getRetentionTime(), rightSample.getRun().getName());
        }
    }

    @Override
    public void unalignedMoI(ProcessedSample s, MoI moI) {
        if (tracked(moI.getMz(), moI.getRetentionTime())) {
            logger.debug("Cannot align MOI with m/z %f and rt %f in sample %s", moI.getMz(), moI.getRetentionTime(), moI.getRetentionTime(), s.getRun().getName());
        }
    }

    @Override
    public void moiDeleted(MoI moi) {
        if (tracked(moi.getMz(), moi.getRetentionTime())) {
            if (moi instanceof AlignedMoI) {
                AlignedMoI amo = (AlignedMoI) moi;
                logger.debug("DELETE MOI with m/z %f and rt %f which was aligned in %d samples", moi.getMz(), moi.getRetentionTime(), amo.getAligned().length);
            } else {
                logger.debug("DELETE singleton MOI with m/z %f and rt %f", moi.getMz(), moi.getRetentionTime());
            }
        }
    }

    @Override
    public void emptyRect(ProcessedSample sample, Rect r) {
        if (tracked(r.avgMz, (r.maxRt-r.minRt)/2d)) {
            logger.debug("do not find any traces in rect mz=%f..%f, rt=%f..%f in sample %s",
                    r.minMz,r.maxMz,r.minRt,r.maxRt,sample.getRun().getName());
        }
    }

    @Override
    public void mergedTrace(ProcessedSample merged, ProcessedSample sample, Rect r, ProjectedTrace projectedTrace, MoI[] moisForSample) {
        double rt = merged.getMapping().getRetentionTimeAt(projectedTrace.getProjectedApex());
        if (tracked(projectedTrace.getAveragedMz(), rt)) {
            logger.debug("merge trace m/z = %f, rt = %f", projectedTrace.getAveragedMz(), rt);
        }
    }

    @Override
    public void rejectedForFeatureExtraction(Rect r, MergedTrace merged) {

    }

    @Override
    public void noFeatureFound(MergedTrace mergedTrace) {

    }

    @Override
    public void importFeatures(MergedTrace mergedTrace, AlignedFeatures[] features) {

    }
}
