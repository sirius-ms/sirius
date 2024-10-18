package de.unijena.bioinf.lcms.utils;

import de.unijena.bioinf.lcms.align.MoI;
import de.unijena.bioinf.lcms.merge.MergedTrace;
import de.unijena.bioinf.lcms.msms.MsMsTraceReference;
import de.unijena.bioinf.lcms.trace.ContiguousTrace;
import de.unijena.bioinf.lcms.trace.ProcessedSample;
import de.unijena.bioinf.lcms.trace.ProjectedTrace;
import de.unijena.bioinf.lcms.trace.Rect;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import it.unimi.dsi.fastutil.ints.IntArrayList;

public interface Tracker {

    public class NOOP implements Tracker {

        @Override
        public void assignMs2ToMergedTrace(ProcessedSample sample, ContiguousTrace[] sourceTraces, ProcessedSample merged, ProjectedTrace projectedTrace, MsMsTraceReference[] ids) {

        }

        @Override
        public void tracePicked(double mz, double rt, ProcessedSample sample, ContiguousTrace trace) {

        }

        @Override
        public void moiAccepted(double mz, double retentionTime, ProcessedSample sample, ContiguousTrace trace, MoI moi) {

        }

        @Override
        public void moiRejected(double mz, double retentionTime, ProcessedSample sample, ContiguousTrace trace, MoI moi) {

        }

        @Override
        public void alignMois(ProcessedSample rightSample, MoI left, MoI right) {

        }

        @Override
        public void unalignedMoI(ProcessedSample s, MoI moI) {

        }

        @Override
        public void moiDeleted(MoI moi) {

        }

        @Override
        public void createRect(ProcessedSample sample, Rect r) {

        }

        @Override
        public void emptyRect(ProcessedSample sample, Rect r) {

        }

        @Override
        public void mergedTrace(ProcessedSample merged, ProcessedSample sample, Rect r, ProjectedTrace projectedTrace, MoI[] moisForSample) {

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

        @Override
        public void startExtractingCompounds(ProcessedSample mergedSample, MergedTrace mergedTrace) {

        }
    }

    void tracePicked(double mz, double rt, ProcessedSample sample, ContiguousTrace trace);

    void moiAccepted(double mz, double retentionTime, ProcessedSample sample, ContiguousTrace trace, MoI moi);

    void moiRejected(double mz, double retentionTime, ProcessedSample sample, ContiguousTrace trace, MoI moi);

    void alignMois(ProcessedSample rightSample, MoI left, MoI right);

    void unalignedMoI(ProcessedSample s, MoI moI);

    void moiDeleted(MoI moi);

    void createRect(ProcessedSample sample, Rect r);

    void emptyRect(ProcessedSample sample, Rect r);

    void mergedTrace(ProcessedSample merged, ProcessedSample sample, Rect r, ProjectedTrace projectedTrace, MoI[] moisForSample);

    void assignMs2ToMergedTrace(ProcessedSample sample, ContiguousTrace[] sourceTraces, ProcessedSample merged, ProjectedTrace projectedTrace, MsMsTraceReference[] ids);

    void rejectedForFeatureExtraction(Rect r, MergedTrace merged);

    void noFeatureFound(MergedTrace mergedTrace);

    void importFeatures(MergedTrace mergedTrace, AlignedFeatures[] features);

    void startExtractingCompounds(ProcessedSample mergedSample, MergedTrace mergedTrace);
}
