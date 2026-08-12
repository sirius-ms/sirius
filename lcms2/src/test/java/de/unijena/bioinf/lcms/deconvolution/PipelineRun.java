package de.unijena.bioinf.lcms.deconvolution;

import de.unijena.bioinf.lcms.LCMSProcessing;
import de.unijena.bioinf.lcms.align.AlignmentBackbone;
import de.unijena.bioinf.lcms.projectspace.SiriusDatabaseAdapter;
import de.unijena.bioinf.lcms.trace.ProcessedSample;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.core.run.LCMSRun;
import de.unijena.bioinf.ms.persistence.model.core.run.MergedLCMSRun;
import de.unijena.bioinf.ms.persistence.model.core.run.RetentionTimeAxis;
import de.unijena.bioinf.ms.persistence.model.core.scan.MSMSScan;
import de.unijena.bioinf.ms.persistence.model.core.scan.Scan;
import de.unijena.bioinf.ms.persistence.model.core.spectrum.MSData;
import de.unijena.bioinf.ms.persistence.model.core.trace.AbstractTrace;
import de.unijena.bioinf.ms.persistence.model.core.trace.MergedTrace;
import de.unijena.bioinf.ms.persistence.model.core.trace.SourceTrace;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * One run of a single file through the whole pipeline - processing, alignment, merging and feature extraction -
 * keeping the features in memory instead of persisting them, so a test can look at them without a project.
 * <p>
 * Shared by the tests that need a whole run to look at: the pipeline smoke test and the comparison against a DDA
 * measurement of the same sample.
 */
final class PipelineRun {

    final String name;
    final List<AlignedFeatures> features;
    final double processingSeconds;
    final double alignmentSeconds;
    /** one per sample, in the order the files were given, and null for a sample that was not deconvoluted */
    final List<DiaReport> diaReports;
    /** what stage two reported, or null if it did not run */
    final String refinement;

    private PipelineRun(String name, List<AlignedFeatures> features, double processingSeconds,
                        double alignmentSeconds, List<DiaReport> diaReports, String refinement) {
        this.name = name;
        this.features = features;
        this.processingSeconds = processingSeconds;
        this.alignmentSeconds = alignmentSeconds;
        this.diaReports = diaReports;
        this.refinement = refinement;
    }

    static PipelineRun run(Path file, String name) throws IOException {
        return run(List.of(file), name, null, true);
    }

    /**
     * Puts every given file through processing and aligns them into one experiment, the way the CLI does with
     * several input files. With one file the alignment is trivial but still runs, so a single sample and a
     * replicate group go through exactly the same code.
     *
     * @param configure applied to the processing before it starts, to vary its settings between runs
     * @param refine    whether to redo the DIA assignment over the final feature boundaries, i.e. stage two. Off is
     *                  what the pipeline did before stage two existed, which is what it has to be compared against.
     */
    static PipelineRun run(List<Path> files, String name, Consumer<LCMSProcessing> configure, boolean refine)
            throws IOException {
        final CollectingAdapter adapter = new CollectingAdapter();
        final LCMSProcessing processing = new LCMSProcessing(adapter, false);
        if (configure != null) configure.accept(processing);
        try {
            final long started = System.currentTimeMillis();
            final List<ProcessedSample> samples = new ArrayList<>(files.size());
            for (Path file : files) samples.add(processing.processSample(file));
            final long afterSample = System.currentTimeMillis();
            final AlignmentBackbone backbone = processing.align();
            final ProcessedSample merged = processing.merge(backbone);
            if (refine) processing.refineDiaAssignments(merged, backbone);
            final int featureCount = processing.extractFeaturesAndExportToProjectSpace(merged, backbone);
            final long finished = System.currentTimeMillis();
            if (featureCount != adapter.features.size()) {
                throw new AssertionError("the importer reported " + featureCount + " features but handed over "
                        + adapter.features.size());
            }
            final List<DiaReport> reports = new ArrayList<>(samples.size());
            for (ProcessedSample sample : samples) reports.add(processing.getDiaReport(sample).orElse(null));
            return new PipelineRun(name, adapter.features, (afterSample - started) / 1000d,
                    (finished - afterSample) / 1000d, reports, processing.getDiaRefinementReport());
        } finally {
            processing.closeStorages();
        }
    }

    /** the features carrying a merged MS/MS spectrum, which is what a comparison of spectra can work with */
    List<AlignedFeatures> featuresWithMsMs() {
        final List<AlignedFeatures> withMsMs = new ArrayList<>();
        for (AlignedFeatures feature : features) {
            final Optional<MSData> data = feature.getMSData();
            if (data.isPresent() && data.get().getMergedMSnSpectrum() != null
                    && !data.get().getMergedMSnSpectrum().isEmpty()) {
                withMsMs.add(feature);
            }
        }
        return withMsMs;
    }

    /** keeps the features instead of persisting them, and hands out the ids the importer expects back */
    static final class CollectingAdapter implements SiriusDatabaseAdapter {
        private final AtomicLong ids = new AtomicLong(1);
        final List<AlignedFeatures> features = new ArrayList<>();

        @Override
        public void importRun(LCMSRun run) {
            run.setRunId(ids.getAndIncrement());
        }

        @Override
        public void updateRun(LCMSRun run) {
        }

        @Override
        public void importMergedRun(MergedLCMSRun mergedRun) {
            mergedRun.setRunId(ids.getAndIncrement());
        }

        @Override
        public void updateMergedRun(MergedLCMSRun mergedRun) {
        }

        @Override
        public void removeMergedRun(MergedLCMSRun run) {
        }

        @Override
        public void importScan(Scan scan) {
        }

        @Override
        public void importMSMSScan(MSMSScan scan) {
        }

        @Override
        public void importTrace(AbstractTrace trace) {
            // the importer needs an id back, and which kind of trace it is decides where it is stored
            final long id = ids.getAndIncrement();
            if (trace instanceof MergedTrace merged) merged.setMergedTraceId(id);
            else if (trace instanceof SourceTrace source) source.setSourceTraceId(id);
        }

        @Override
        public void removeSourceTrace(long id) {
        }

        @Override
        public void removeMergedTrace(long id) {
        }

        @Override
        public synchronized boolean importAlignedFeature(AlignedFeatures alignedFeatures) {
            alignedFeatures.setAlignedFeatureId(ids.getAndIncrement());
            features.add(alignedFeatures);
            return true;
        }

        @Override
        public void importRetentionTimeAxis(RetentionTimeAxis axis, boolean update) {
        }

        @Override
        public synchronized Stream<AlignedFeatures> getImportedFeatureStream(long runId) {
            return new ArrayList<>(features).stream();
        }
    }
}
