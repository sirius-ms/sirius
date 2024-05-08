package de.unijena.bioinf.lcms;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.lcms.align.*;
import de.unijena.bioinf.lcms.features.*;
import de.unijena.bioinf.lcms.io.LCMSImporter;
import de.unijena.bioinf.lcms.isotopes.IsotopeDetectionByCorrelation;
import de.unijena.bioinf.lcms.isotopes.IsotopeDetectionStrategy;
import de.unijena.bioinf.lcms.merge.MergeTracesWithoutGapFilling;
import de.unijena.bioinf.lcms.merge.MergedTrace;
import de.unijena.bioinf.lcms.merge.ScanPointInterpolator;
import de.unijena.bioinf.lcms.msms.MergeGreedyStrategy;
import de.unijena.bioinf.lcms.msms.MostIntensivePeakInIsolationWindowAssignmentStrategy;
import de.unijena.bioinf.lcms.msms.Ms2MergeStrategy;
import de.unijena.bioinf.lcms.msms.Ms2TraceStrategy;
import de.unijena.bioinf.lcms.projectspace.PickFeaturesAndImportToSirius;
import de.unijena.bioinf.lcms.projectspace.ProjectSpaceImporter;
import de.unijena.bioinf.lcms.projectspace.SiriusDatabaseAdapter;
import de.unijena.bioinf.lcms.projectspace.SiriusProjectDocumentDbAdapter;
import de.unijena.bioinf.lcms.spectrum.Ms2SpectrumHeader;
import de.unijena.bioinf.lcms.statistics.*;
import de.unijena.bioinf.lcms.trace.ProcessedSample;
import de.unijena.bioinf.lcms.trace.*;
import de.unijena.bioinf.lcms.trace.segmentation.PersistentHomology;
import de.unijena.bioinf.lcms.trace.segmentation.TraceSegment;
import de.unijena.bioinf.lcms.trace.segmentation.TraceSegmentationStrategy;
import de.unijena.bioinf.lcms.traceextractor.*;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.core.run.Chromatography;
import de.unijena.bioinf.ms.persistence.model.core.run.LCMSRun;
import de.unijena.bioinf.ms.persistence.model.core.run.MergedLCMSRun;
import de.unijena.bioinf.ms.persistence.model.core.run.RetentionTimeAxis;
import de.unijena.bioinf.ms.persistence.model.core.trace.AbstractTrace;
import de.unijena.bioinf.ms.persistence.model.core.trace.SourceTrace;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntObjectPair;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.text.similarity.LongestCommonSubsequence;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.*;

public class LCMSProcessing {

    /**
     * Creates temporary databases to store traces and spectra
     */
    @Getter @Setter private LCMSStorageFactory storageFactory = LCMSStorage.temporaryStorage();

    /**
     * Calculates noise thresholds from raw Spectra
     */
    @Getter @Setter private StatisticsCollectionStrategy statisticsCollector = new MedianNoiseCollectionStrategy();

    /**
     * Determines which peaks to pick from the LCMS
     */
    @Getter @Setter private TraceDetectionStrategy traceDetectionStrategy = new CombinedTraceDetectionStrategy(
            new PickIntensivePeaksDetectionStrategy(),
            new PickMsPrecursorPeakDetectionStrategy(),
            new AdductAndIsotopeBasedDetectionStrategy()
    );

    @Getter @Setter private Ms2TraceStrategy ms2TraceStrategy = new MostIntensivePeakInIsolationWindowAssignmentStrategy();

    /**
     * This class determines how we ensure that no two peaks are picked in different traces
     */
    @Getter @Setter private TraceCachingStrategy traceCachingStrategy = new RectbasedCachingStrategy();

    @Getter @Setter private TraceSegmentationStrategy segmentationStrategy = new PersistentHomology();

    @Getter @Setter private MassOfInterestConfidenceEstimatorStrategy confidenceEstimatorStrategy = new MassOfInterestCombinedStrategy(
            new IsotopesAndAdductsAreConfidentStrategy(),
            new PrecursorsWithMsMsAreConfidentStrategy(),
            new TracesWithTooManyApexesAreStrange(),
            new IntensivePeaksAreConfident()
    );

    @Getter @Setter private NormalizationStrategy normalizationStrategy = new AverageOfTop100TracesNormalization();

    @Getter @Setter private AlignmentStrategy alignmentStrategy = new GreedyTwoStageAlignmentStrategy();
    @Getter @Setter private AlignmentAlgorithm alignmentAlgorithm = new BeamSearchAlgorithm();
    @Getter @Setter private AlignmentScorer alignmentScorerBackbone  = AlignmentScorer.expectSimilarIntensity();
    @Getter @Setter private AlignmentScorer alignmentScorerFull = AlignmentScorer.intensityMayBeDifferent();
    @Getter @Setter private MergeTracesWithoutGapFilling mergeStrategy = new MergeTracesWithoutGapFilling();

    @Getter @Setter private SiriusDatabaseAdapter siriusDatabaseAdapter;

    @Getter @Setter private ProjectSpaceImporter<?> importer = new PickFeaturesAndImportToSirius(
            new SegmentMergedFeatures(), new MergedApexIsotopePatternExtractor(), new MergeGreedyStrategy()
    );

    @Getter @Setter private Ms2MergeStrategy ms2MergeStrategy = new MergeGreedyStrategy();

    @Getter @Setter private IsotopePatternExtractionStrategy isotopePatternExtractionStrategy = new MergedApexIsotopePatternExtractor();

    protected List<ProcessedSample> samples = new ArrayList<>();
    private HashMap<Integer, ProcessedSample> sampleByIdx = new HashMap<>();

    public LCMSProcessing(SiriusDatabaseAdapter siriusDatabaseAdapter) {
        this.siriusDatabaseAdapter = siriusDatabaseAdapter;
    }

    /**
     * parses an MZML file and stores the processed sample. Note: we should add possibility to parse from input
     * stream later
     */
    public ProcessedSample processSample(File file) throws IOException {
        return processSample(file, false, LCMSRun.Type.SAMPLE, Chromatography.LC);
    }

    public ProcessedSample processSample(URI input) throws IOException {
        return processSample(input, false, LCMSRun.Type.SAMPLE, Chromatography.LC);
    }


    public ProcessedSample processSample(
            File file,
            boolean saveRawScans,
            LCMSRun.Type runType,
            Chromatography chromatography
    ) throws IOException {
        return processSample(file.toURI(), saveRawScans, runType, chromatography);
    }

    /**
     * parses an MZML file and stores the processed sample. Note: we should add possibility to parse from input
     * stream later
     */
    public ProcessedSample processSample(
            URI input,
            boolean saveRawScans,
            LCMSRun.Type runType,
            Chromatography chromatography
    ) throws IOException {
        // parse file and extract spectra
        ProcessedSample sample = LCMSImporter.importToProject(
                input, storageFactory, siriusDatabaseAdapter, saveRawScans, runType, chromatography);
        sample.setUid(this.samples.size());
        this.samples.add(sample);
        this.sampleByIdx.put(sample.getUid(), sample);
        sample.active();
        collectStatistics(sample);
        extractTraces(sample);
        assignMs2Trace(sample);
        sample.setNormalizer(normalizationStrategy.computeNormalization(sample));
        extractMoIsForAlignment(sample);
        collectStatisticsBeforeAlignment(sample);
        importScanPointMapping(sample, sample.getRun().getRunId());
        return sample;
    }

    public AlignmentBackbone align() throws IOException {
        LCMSStorage mergedStorage = storageFactory.createNewStorage();
        AlignmentBackbone alignmentBackbone = alignmentStrategy.makeAlignmentBackbone(mergedStorage.getAlignmentStorage(), samples, alignmentAlgorithm, alignmentScorerBackbone);
        ProcessedSample merged = new ProcessedSample(
                alignmentBackbone.getScanPointMapping(),
                mergedStorage,
                samples.get(0).getPolarity(),
                samples.size() // TODO: better use sample idx 0?
        );
        samples.add(merged);
        sampleByIdx.put(merged.getUid(), merged);
        alignmentBackbone = alignmentStrategy.align(merged, alignmentBackbone, Arrays.asList(alignmentBackbone.getSamples()), alignmentAlgorithm, alignmentScorerFull);
        for (ProcessedSample sample : alignmentBackbone.getSamples()) {
            sample.setScanPointInterpolator(new ScanPointInterpolator(merged.getMapping(), sample.getMapping(), sample.getRtRecalibration()));
        }
        merged.getStorage().getAlignmentStorage().setStatistics(alignmentBackbone.getStatistics());
        return alignmentBackbone;
    }

    public ProcessedSample merge(AlignmentBackbone backbone) {
        ProcessedSample merged = this.samples.get(samples.size()-1);
        mergeStrategy.merge(merged, backbone);
        return merged;
    }

    public void extractFeaturesAndExportToProjectSpace(ProcessedSample merged, AlignmentBackbone backbone) throws IOException {
        final Int2ObjectMap<ProcessedSample> idx2sample = new Int2ObjectOpenHashMap<>();
        for (ProcessedSample s : backbone.getSamples()) {
            idx2sample.put(s.getUid(), s);
        }

        LongestCommonSubsequence lcs = new LongestCommonSubsequence();
        String name = Arrays.stream(backbone.getSamples()).map(s -> s.getRun().getName()).reduce((a, b) -> lcs.longestCommonSubsequence(a, b).toString()).orElse("");
        if (name.isBlank())
            name = "merged run";

        MergedLCMSRun mergedRun = MergedLCMSRun.builder()
                .name(name)
                .sampleStats(merged.getStorage().getStatistics())
                .build();
        merged.setRun(mergedRun);

        siriusDatabaseAdapter.importMergedRun(mergedRun);

        importScanPointMapping(merged, mergedRun.getRunId());
        ProjectSpaceImporter<Object> importer = (ProjectSpaceImporter<Object>) this.importer;
        Object obj = importer.initializeImport(siriusDatabaseAdapter);
        for (ProcessedSample sample : samples) {
            if (sample!=merged) importer.importRun(siriusDatabaseAdapter, obj, sample);
            else importer.importMergedRun(siriusDatabaseAdapter, obj, sample);
        }
        List<BasicJJob<long[]>> jobs = new ArrayList<>();
        for (final Rect r : merged.getStorage().getMergeStorage().getRectangleMap()) {
            jobs.add(SiriusJobs.getGlobalJobManager().submitJob(new BasicJJob<long[]>() {
                @Override
                protected long[] compute() throws Exception {
                    MergedTrace mergedTrace = collectMergedTrace(merged, r.id);
                    if (isSuitableForImport(mergedTrace)) {
                        return Arrays.stream(importer.importMergedTrace(siriusDatabaseAdapter, obj,merged, mergedTrace)).mapToLong(AlignedFeatures::getAlignedFeatureId).toArray();
                    }
                    return new long[0];
                }
            }));
        }
        jobs.forEach(JJob::takeResult);
    }

    private boolean isSuitableForImport(MergedTrace mergedTrace) {
        return mergedTrace.getTraces().length>0; // todo: implement a proper filter
    }

    protected void importScanPointMapping(ProcessedSample sample, long sampleId) throws IOException {
        RetentionTimeAxis axis = RetentionTimeAxis.builder()
                .runId(sampleId)
                .scanNumbers(sample.getMapping().scanids)
                .retentionTimes(sample.getMapping().retentionTimes)
                .noiseLevelPerScan(sample.getStorage().getStatistics().getNoiseLevelPerScan()).build();
        siriusDatabaseAdapter.importRetentionTimeAxis(axis);
    }

    private void collectStatistics(ProcessedSample sample) {
        // collect statistics
        final StatisticsCollectionStrategy.Calculation calc = statisticsCollector.collectStatistics();
        ;
        for (int idx = 0, n = sample.getMapping().length(); idx < n; ++idx) {
            calc.processMs1(sample.getStorage().getSpectrumStorage().ms1SpectrumHeader(idx), sample.getStorage().getSpectrumStorage().getSpectrum(idx));
        }
        sample.getStorage().setStatistics(calc.done());
    }

    private void collectStatisticsBeforeAlignment(ProcessedSample sample) {
        sample.setTraceStats(TraceStats.collect(sample));
    }

    private void extractTraces(ProcessedSample sample) {
        final TracePicker tracePicker = new TracePicker(sample, traceCachingStrategy, segmentationStrategy);
        tracePicker.setAllowedMassDeviation(sample.getStorage().getStatistics().getMs1MassDeviationWithinTraces());
        traceDetectionStrategy.findPeaksForExtraction(sample, (sample1, spectrumIdx, peakIdx, spectrum) -> {
            Optional<ContiguousTrace> peak = tracePicker.detectMostIntensivePeak(spectrumIdx, spectrum.getMzAt(peakIdx));
        });
    }

    private void assignMs2Trace(ProcessedSample sample) {
        for (Ms2SpectrumHeader ms2SpectrumHeader : sample.getStorage().getSpectrumStorage().ms2SpectraHeader()) {
            int traceId = ms2TraceStrategy.getTraceFor(sample, ms2SpectrumHeader);
            if (traceId >= 0) {
                sample.getStorage().getTraceStorage().setTraceForMs2(ms2SpectrumHeader.getUid(), traceId);
            } else {
                LoggerFactory.getLogger(LCMSProcessing.class).debug("No suitable trace found for MSMS " + ms2SpectrumHeader);
            }
        }
    }

    private void extractMoIsForAlignment(ProcessedSample sample) {
        final AlignmentStorage alignmentStorage = sample.getStorage().getAlignmentStorage();
        NormalizationStrategy.Normalizer normalizer = sample.getNormalizer();
        for (ContiguousTrace trace : sample.getStorage().getTraceStorage()) {
            // calculate rect
            double minMz = trace.averagedMz(), maxMz = trace.averagedMz();
            for (TraceSegment segment : trace.getSegments()) {
                minMz = Math.min(trace.mz(segment.apex), minMz);
                maxMz = Math.max(trace.mz(segment.apex), maxMz);
            }
            for (TraceSegment segment : trace.getSegments()) {
                Rect rect = trace.rectWithRts();
                rect.minMz = (float) minMz;
                rect.maxMz = (float) maxMz;
                MoI moi = new MoI(rect, segment.apex, sample.getMapping().getRetentionTimeAt(segment.apex),
                        (float) normalizer.normalize(trace.intensity(segment.apex)), sample.getUid());

                if (trace.getSegments().length==1) moi.setSingleApex(true);
                detectIsotopesForMoI(sample, trace, segment, moi);

                moi.setConfidence(confidenceEstimatorStrategy.estimateConfidence(sample, trace, moi, null));
                if (moi.getConfidence() >= 0) {
                    //System.out.println(moi + " intensity = " + moi.getIntensity() + ", isotopes = " + (moi.getIsotopes()==null ? 0 : moi.getIsotopes().isotopeIntensities.length) + ", confidence = "+ moi.getConfidence());
                    alignmentStorage.addMoI(moi);
                }
            }
        }
    }

    private void detectIsotopesForMoI(ProcessedSample sample, ContiguousTrace trace, TraceSegment segment, MoI moi) {
        IsotopeDetectionStrategy.Result result = new IsotopeDetectionByCorrelation().detectIsotopesFor(sample, moi, trace, segment);
        if (result.isMonoisotopic()) {
            moi.setIsotopes(result.getIsotopePeaks().get());
        } else {
            moi.setIsotopePeakFlag(true);
        }
    }

    public MergedTrace collectMergedTrace(ProcessedSample merged, int uid) {
        // collect all projectedTraces that have the given uid
        ProjectedTrace[] projectedTraces = merged.getStorage().getMergeStorage().getAllProjectedTracesOf(uid);
        ProjectedTrace[][] isotopes = merged.getStorage().getMergeStorage().getIsotopePatternFor(uid);
        return MergedTrace.collect(merged, sampleByIdx, projectedTraces, isotopes);
    }


}
