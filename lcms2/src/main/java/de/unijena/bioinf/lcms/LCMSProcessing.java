package de.unijena.bioinf.lcms;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.math.Statistics;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.lcms.align.*;
import de.unijena.bioinf.lcms.centroiding.CentroidIndividualSpectraStrategy;
import de.unijena.bioinf.lcms.centroiding.CentroidingStrategy;
import de.unijena.bioinf.lcms.deconvolution.*;
import de.unijena.bioinf.lcms.features.IsotopePatternExtractionStrategy;
import de.unijena.bioinf.lcms.features.MergedFeatureExtractionStrategy;
import de.unijena.bioinf.lcms.features.MergedApexIsotopePatternExtractor;
import de.unijena.bioinf.lcms.features.SegmentMergedFeatures;
import de.unijena.bioinf.lcms.io.LCMSImporter;
import de.unijena.bioinf.lcms.isotopes.IsotopeDetectionByCorrelation;
import de.unijena.bioinf.lcms.isotopes.IsotopeDetectionStrategy;
import de.unijena.bioinf.lcms.merge.MergeTracesWithoutGapFilling;
import de.unijena.bioinf.lcms.merge.MergedTrace;
import de.unijena.bioinf.lcms.merge.ScanPointInterpolator;
import de.unijena.bioinf.lcms.msms.*;
import de.unijena.bioinf.lcms.projectspace.PickFeaturesAndImportToSirius;
import de.unijena.bioinf.lcms.projectspace.ProjectSpaceImporter;
import de.unijena.bioinf.lcms.projectspace.SiriusDatabaseAdapter;
import de.unijena.bioinf.lcms.quality.CheckMs2Quality;
import de.unijena.bioinf.lcms.spectrum.Ms2SpectrumHeader;
import de.unijena.bioinf.lcms.statistics.*;
import de.unijena.bioinf.lcms.trace.ProcessedSample;
import de.unijena.bioinf.lcms.trace.*;
import de.unijena.bioinf.lcms.trace.filter.NoFilter;
import de.unijena.bioinf.lcms.trace.segmentation.PersistentHomology;
import de.unijena.bioinf.lcms.trace.segmentation.TraceSegment;
import de.unijena.bioinf.lcms.trace.segmentation.TraceSegmentationStrategy;
import de.unijena.bioinf.lcms.traceextractor.*;
import de.unijena.bioinf.lcms.utils.Tracker;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.core.run.Chromatography;
import de.unijena.bioinf.ms.persistence.model.core.run.MergedLCMSRun;
import de.unijena.bioinf.ms.persistence.model.core.run.RetentionTimeAxis;
import de.unijena.bioinf.ms.persistence.model.core.run.SampleStatistics;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.text.similarity.LongestCommonSubsequence;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class LCMSProcessing {

    /**
     * Creates temporary databases to store traces and spectra
     */
    private final LCMSStorageFactory storageFactory;

    /**
     * Calculates noise thresholds from raw Spectra
     */
    @Getter @Setter private StatisticsCollectionStrategy statisticsCollector = new MedianNoiseCollectionStrategy();

    @Getter @Setter private CentroidingStrategy centroidingStrategy = new CentroidIndividualSpectraStrategy();

    /**
     * Determines which peaks to pick from the LCMS
     */
    @Getter @Setter private TraceDetectionStrategy traceDetectionStrategy = new CombinedTraceDetectionStrategy(
            //new PickIntensivePeaksDetectionStrategy(),
            new PickIntensivePeaksInConsecutiveSpectraStrategy(),

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
    @Getter @Setter private AlignmentThresholds alignmentThresholds = new AlignmentThresholds();
    @Getter @Setter private AlignmentAlgorithm alignmentAlgorithm = new BeamSearchAlgorithm();
    @Getter @Setter private AlignmentScorer alignmentScorerBackbone  = AlignmentScorer.expectSimilarIntensity();
    @Getter @Setter private AlignmentScorer alignmentScorerFull = AlignmentScorer.intensityMayBeDifferent();
    @Getter @Setter private MergeTracesWithoutGapFilling mergeStrategy = new MergeTracesWithoutGapFilling();

    @Getter @Setter private SiriusDatabaseAdapter siriusDatabaseAdapter;

    @Getter @Setter private TraceSegmentationStrategy mergedTraceSegmentationStrategy =
        new PersistentHomology(new NoFilter());

    /**
     * How a merged trace is split into features. The same instance is handed to the importer, so that the stage two
     * DIA assignment measures over exactly the boundaries the importer will use.
     */
    @Getter @Setter private MergedFeatureExtractionStrategy mergedFeatureExtractionStrategy = new SegmentMergedFeatures();

    @Getter @Setter private ProjectSpaceImporter<?> importer = new PickFeaturesAndImportToSirius(
            mergedFeatureExtractionStrategy, new MergedApexIsotopePatternExtractor(), new MergeGreedyStrategy()
    );

    @Getter @Setter private Ms2MergeStrategy ms2MergeStrategy = new MergeGreedyStrategy();

    @Getter @Setter private IsotopePatternExtractionStrategy isotopePatternExtractionStrategy = new MergedApexIsotopePatternExtractor();

    @Getter @Setter private DeconvolutionDecision deconvolutionDecision = new DeconvoluteForLargeIsolationWindows();

    /**
     * Assigns the fragments of a DIA measurement to its precursors, in both phases. Only used when
     * {@link #deconvolutionDecision} says the sample is data independent. Which implementation is best is a question
     * for measurement, see {@link DiaAssignmentStrategy}.
     */
    @Getter @Setter private DiaAssignmentStrategy diaAssignmentStrategy = new CorrelationAssignmentStrategy();

    /** what {@link #refineDiaAssignments} reported, kept for inspection rather than only logged */
    @Getter private String diaRefinementReport;

    /**
     * What the DIA deconvolution did, per sample. Kept for inspection rather than only logged, and keyed by the
     * sample itself: samples are processed in parallel, so a single field would be overwritten by whichever
     * thread finished last.
     */
    private final Map<ProcessedSample, DiaReport> diaReports = new ConcurrentHashMap<>();

    /**
     * @return what the DIA deconvolution did for this sample, or empty if it was not a DIA sample
     */
    public Optional<DiaReport> getDiaReport(ProcessedSample sample) {
        return Optional.ofNullable(diaReports.get(sample));
    }

    protected List<ProcessedSample> samples = new ArrayList<>();

    /**
     * Handed out in the order samples are given to us, not the order their processing finishes.
     * <p>
     * The distinction is the whole point: samples are processed in parallel, so taking a uid when a sample is done
     * makes it a function of the thread scheduler. Everything ordered by uid downstream - which sample seeds the
     * alignment backbone, the order the rest are merged into it - then differs between two runs on identical input.
     * Reserving it in {@link #processSampleAsync} on the calling thread costs nothing and makes it a function of the
     * input alone.
     */
    private final AtomicInteger nextSampleUid = new AtomicInteger();
    private HashMap<Integer, ProcessedSample> sampleByIdx = new HashMap<>();

    private final boolean saveFeatureIds;

    @Getter
    private LongList importedFeatureIds = new LongArrayList();

    @Getter @Setter
    private Tracker tracker = new Tracker.NOOP();

    public LCMSProcessing(SiriusDatabaseAdapter siriusDatabaseAdapter, boolean saveFeatureIds) {
        this(siriusDatabaseAdapter, saveFeatureIds, null, false);
    }

    public LCMSProcessing(SiriusDatabaseAdapter siriusDatabaseAdapter, boolean saveFeatureIds, @Nullable Path tmpDir, boolean inMemoryOnMerged) {
        this.siriusDatabaseAdapter = siriusDatabaseAdapter;
        this.saveFeatureIds = saveFeatureIds;
        this.storageFactory = LCMSStorage.temporaryStorage(tmpDir == null ? null : tmpDir.toFile(), inMemoryOnMerged);
    }

    /**
     * parses an MZML file and stores the processed sample. Note: we should add possibility to parse from input
     * stream later
     */
    public ProcessedSample processSample(Path file) throws IOException {
        return processSample(file, null);
    }

    /**
     * @param sampleName name to be used for the run of this sample. If null, the name is derived from the input file.
     */
    public ProcessedSample processSample(Path file, @Nullable String sampleName) throws IOException {
        return processSample(file, sampleName, false, Chromatography.LC);
    }

    public ProcessedSample processSample(URI input) throws IOException {
        return processSample(input, false, Chromatography.LC);
    }


    public ProcessedSample processSample(
            Path file,
            boolean saveRawScans,
            Chromatography chromatography
    ) throws IOException {
        return processSample(file, null, saveRawScans, chromatography);
    }

    /**
     * @param sampleName name to be used for the run of this sample. If null, the name is derived from the input file.
     */
    public ProcessedSample processSample(
            Path file,
            @Nullable String sampleName,
            boolean saveRawScans,
            Chromatography chromatography
    ) throws IOException {
        return importAndProcess(file, sampleName, saveRawScans, chromatography, nextSampleUid.getAndIncrement());
    }

    /**
     * The parallel form of {@link #processSample(Path, String, boolean, Chromatography)}: the sample's identity is
     * fixed here, on the calling thread, and only the parsing and processing happen in the job.
     * <p>
     * Callers that process several files at once have to use this rather than submitting {@code processSample} into
     * jobs of their own, or the samples are numbered in the order they finish and the alignment is not reproducible.
     * Create the jobs in the order the files should be numbered in; the returned job is not submitted yet, so the
     * caller decides where it runs and can submit it as a sub job of its own. The sample is left in low memory mode,
     * since a caller importing many files does not want them all cached at once - reactivate it before reading.
     */
    public JJob<ProcessedSample> processSampleAsync(
            Path file,
            @Nullable String sampleName,
            boolean saveRawScans,
            Chromatography chromatography
    ) {
        final int uid = nextSampleUid.getAndIncrement();
        return new BasicJJob<ProcessedSample>() {
            @Override
            protected ProcessedSample compute() throws Exception {
                final ProcessedSample sample = importAndProcess(file, sampleName, saveRawScans, chromatography, uid);
                // released here rather than by the caller: with many samples in flight the peak memory is decided by
                // how long each one stays cached after its own work is done, and only this thread knows when that is
                sample.inactive();
                return sample;
            }
        };
    }

    public JJob<ProcessedSample> processSampleAsync(Path file, @Nullable String sampleName) {
        return processSampleAsync(file, sampleName, false, Chromatography.LC);
    }

    private ProcessedSample importAndProcess(Path file, @Nullable String sampleName, boolean saveRawScans,
                                             Chromatography chromatography, int uid) throws IOException {
        ProcessedSample sample = LCMSImporter.importToProject(
                file, sampleName, storageFactory, siriusDatabaseAdapter, centroidingStrategy, saveRawScans, chromatography);
        processSample(sample, uid);
        return sample;
    }

    /**
     * parses an MZML file and stores the processed sample. Note: we should add possibility to parse from input
     * stream later
     */
    public ProcessedSample processSample(
            URI input,
            boolean saveRawScans,
            Chromatography chromatography
    ) throws IOException {
        // parse file and extract spectra
        ProcessedSample sample = LCMSImporter.importToProject(
                input, storageFactory, siriusDatabaseAdapter, saveRawScans, chromatography);
        processSample(sample, nextSampleUid.getAndIncrement());
        return sample;
    }

    private void cleanupSample(ProcessedSample sample) {
        fixMissingParentIdsForMs2(sample);
    }

    private void fixMissingParentIdsForMs2(ProcessedSample sample) {
        final ArrayList<Ms2SpectrumHeader> headers = new ArrayList<>();
        for (Ms2SpectrumHeader header : sample.getStorage().getSpectrumStorage().ms2SpectraHeader()) {
            headers.add(header);
        }
        for (Ms2SpectrumHeader ms2 : headers){
            int parentId = ms2.getParentId();
            if (parentId<0) {
                parentId = sample.getMapping().idForRetentionTime(ms2.getRetentionTime());
                for (int k=parentId; k >= parentId-10 && k >= 0; --k) {
                    SimpleSpectrum ms1Spectrum = sample.getStorage().getSpectrumStorage().getSpectrum(k);
                    int peakIdx = Spectrums.mostIntensivePeakWithin(ms1Spectrum, ms2.getPrecursorMz(), new Deviation(20));
                    if ((peakIdx >= 0) && (ms1Spectrum.getIntensityAt(peakIdx)>sample.getStorage().getStatistics().noiseLevel(k))) {
                        parentId = k;
                        break;
                    }
                }
                sample.getStorage().getSpectrumStorage().updateMs2Header(ms2.withParentId(parentId));
            }
        }
    }

    /**
     * @param sampleUid reserved by the caller before any work started, see {@link #nextSampleUid}. The list itself is
     *                  still filled in completion order, since only successful samples belong in it - a reserved slot
     *                  would leave a hole for a file that failed to parse - and it is ordered by uid before alignment.
     */
    private void processSample(ProcessedSample sample, int sampleUid) throws IOException {
        synchronized (this) {
            this.samples.add(sample);
            this.sampleByIdx.put(sampleUid, sample);
        }
        sample.setUid(sampleUid);
        sample.active();
        collectStatistics(sample);
        cleanupSample(sample);
        extractTraces(sample);
        learnTraceCorrelations(sample);
        if (deconvolutionDecision.performDeconvolution(sample)) {
            deconvoluteDia(sample);
        } else {
            assignMs2Trace(sample);
        }
        sample.setNormalizer(normalizationStrategy.computeNormalization(sample));
        extractMoIsForAlignment(sample);
        collectStatisticsBeforeAlignment(sample);
        importScanPointMapping(sample, sample.getRun().getRunId());
        sample.getStorage().commit();
    }

    public AlignmentBackbone align() throws IOException {
        // Samples were added in the order their parallel jobs finished, which is not the order they were given in.
        // Alignment depends on that order - one sample seeds the backbone and the rest are merged into it greedily -
        // as does the polarity read off the first sample below, so it is restored to the order the caller reserved
        // the uids in. Deterministic because the uid is, see nextSampleUid.
        samples.sort(Comparator.comparingInt(ProcessedSample::getUid));
        LCMSStorage mergedStorage = storageFactory.createNewMergedStorage();
        AlignmentBackbone alignmentBackbone = alignmentStrategy.makeAlignmentBackbone(mergedStorage.getAlignmentStorage(), samples, alignmentAlgorithm, alignmentThresholds, alignmentScorerBackbone);
        ProcessedSample merged = new ProcessedSample(
                alignmentBackbone.getScanPointMapping(),
                mergedStorage,
                samples.get(0).getPolarity(),
                // its own reserved uid: samples.size() would collide with the uid of a sample that failed to parse
                nextSampleUid.getAndIncrement()
        );
        makeMergeStatistics(merged, alignmentBackbone.getSamples());
        samples.add(merged);
        sampleByIdx.put(merged.getUid(), merged);
        alignmentBackbone = alignmentStrategy.align(merged, alignmentBackbone, Arrays.asList(alignmentBackbone.getSamples()), alignmentAlgorithm, alignmentThresholds, alignmentScorerFull, tracker);
        for (ProcessedSample sample : alignmentBackbone.getSamples()) {
            sample.setScanPointInterpolator(new ScanPointInterpolator(merged.getMapping(), sample.getMapping(), sample.getRtRecalibration()));
            updateRetentionTimeAxis(sample);
        }
        merged.getStorage().getAlignmentStorage().setStatistics(alignmentBackbone.getStatistics());
        merged.getStorage().commit();
        return alignmentBackbone;
    }

    private void makeMergeStatistics(ProcessedSample merged, ProcessedSample[] samples) {
        FloatArrayList ms2NoiseLevels = new FloatArrayList();
        FloatArrayList ppmsWithinTraces = new FloatArrayList(), ppmsBetweenTraces = new FloatArrayList();
        FloatArrayList absWithinTraces = new FloatArrayList(), absBetweenTraces = new FloatArrayList();
        DoubleArrayList averagePeakWidth = new DoubleArrayList();
        float mint = Float.POSITIVE_INFINITY;
        for (ProcessedSample sample : samples) {
            SampleStats s = sample.getStorage().getStatistics();
            ms2NoiseLevels.add(s.getMs2NoiseLevel());
            ppmsWithinTraces.add((float)s.getMs1MassDeviationWithinTraces().getPpm());
            ppmsBetweenTraces.add((float)s.getMinimumMs1MassDeviationBetweenTraces().getPpm());
            absWithinTraces.add((float)s.getMs1MassDeviationWithinTraces().getAbsolute());
            absBetweenTraces.add((float)s.getMinimumMs1MassDeviationBetweenTraces().getAbsolute());
            averagePeakWidth.add(sample.getTraceStats().getAveragePeakWidth());
            mint = Math.min(mint, s.getMinimumIntensity());
        }
        // by name rather than by position, so that adding a statistic cannot silently shift the others
        SampleStats statistics = SampleStats.builder()
                .noiseLevelPerScan(new float[0])
                .ms2NoiseLevel((float) Statistics.robustAverage(ms2NoiseLevels.toFloatArray()))
                .minimumIntensity(mint)
                .ms1MassDeviationWithinTraces(new Deviation(
                        Statistics.robustAverage(ppmsWithinTraces.toFloatArray()),
                        Statistics.robustAverage(absWithinTraces.toFloatArray())))
                .minimumMs1MassDeviationBetweenTraces(new Deviation(
                        Statistics.robustAverage(ppmsBetweenTraces.toFloatArray()),
                        Statistics.robustAverage(absBetweenTraces.toFloatArray())))
                .expectedPeakWidth(Statistics.robustAverage(averagePeakWidth.toDoubleArray()))
                .build();
        merged.getStorage().setStatistics(statistics);

    }

    public ProcessedSample merge(AlignmentBackbone backbone) {
        ProcessedSample merged = this.samples.get(samples.size()-1);
        mergeStrategy.merge(merged, backbone, tracker);
        merged.getStorage().commit();
        return merged;
    }

    public int extractFeaturesAndExportToProjectSpace(ProcessedSample merged, AlignmentBackbone backbone) throws IOException {
        LongestCommonSubsequence lcs = new LongestCommonSubsequence();
        String name = Arrays.stream(backbone.getSamples()).map(s -> s.getRun().getName()).reduce((a, b) -> lcs.longestCommonSubsequence(a, b).toString()).orElse("");
        if (name.isBlank())
            name = "merged run";

        MergedLCMSRun mergedRun = MergedLCMSRun.builder()
                .name(name)
                .runIds(Arrays.stream(backbone.getSamples()).mapToLong(x->x.getRun().getRunId()).toArray())
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
        System.out.println("Start importing features and peak picking");
        int featureCount = 0;
        long TIME1 = System.currentTimeMillis();
        List<BasicJJob<long[]>> jobs = new ArrayList<>();
        for (final Rect r : merged.getStorage().getMergeStorage().getRectangleMap()) {
            jobs.add(SiriusJobs.getGlobalJobManager().submitJob(new BasicJJob<long[]>() {
                @Override
                protected long[] compute() throws Exception {
                    MergedTrace mergedTrace = collectMergedTrace(merged, r.id);
                    if (mergedTrace!=null && isSuitableForImport(mergedTrace)) {
                        return Arrays.stream(importer.importMergedTrace(mergedTraceSegmentationStrategy, siriusDatabaseAdapter, obj,merged, mergedTrace, tracker)).mapToLong(AlignedFeatures::getAlignedFeatureId).toArray();
                    } else {
                        tracker.rejectedForFeatureExtraction(r, mergedTrace);
                    }
                    return new long[0];
                }
            }));
        }
        for (BasicJJob<long[]> job : jobs) {
            long[] ids = job.takeResult();
            if (saveFeatureIds) {
                for (long id : ids) {
                    importedFeatureIds.add(id);
                }
            }
            featureCount += ids.length;
        }
        long TIME2 = System.currentTimeMillis();
        System.out.printf("Peak picking and import took %f seconds\n", (TIME2-TIME1)/1000d);
        if (featureCount > 0) {
            mergedRun.setSampleStats(collectFinalStatistics(merged, backbone));
            siriusDatabaseAdapter.updateMergedRun(mergedRun);
        } else {
            siriusDatabaseAdapter.removeMergedRun(mergedRun);
        }

        return featureCount;
    }

    private SampleStatistics collectFinalStatistics(ProcessedSample merged, AlignmentBackbone alignmentBackbone) throws IOException {
        /*
         * this code relies on lazy evaluation of streams. If that is not the case we might have a huge memory peak here :/
         */
        DoubleArrayList fwhms = new DoubleArrayList();
        DoubleArrayList heightDividedByfwhms = new DoubleArrayList();
        siriusDatabaseAdapter.getImportedFeatureStream(merged.getRun().getRunId())
                .filter(x -> x.getMSData().orElseThrow().getIsotopePattern() != null && x.getMSData().get().getIsotopePattern().size() >= 2
                && x.getSnr()>=5)
                .forEach(x->{
                    fwhms.add(x.getFwhm().doubleValue());
                    heightDividedByfwhms.add(x.getApexIntensity()/x.getFwhm());
                });
        SampleStats st = merged.getStorage().getStatistics();
        fwhms.sort(null);
        heightDividedByfwhms.sort(null);
        DoubleList ms2NoiseLevel = new DoubleArrayList();
        for (ProcessedSample sample : alignmentBackbone.getSamples()) {
            SampleStats statistics = sample.getStorage().getStatistics();
           if (statistics.getMs2NoiseLevel()>0) ms2NoiseLevel.add(statistics.getMs2NoiseLevel());
        }
        double ms2NoiseLevelAvg = Statistics.robustAverage(ms2NoiseLevel.toDoubleArray());
        return new SampleStatistics(
                st.getMs1MassDeviationWithinTraces(),
                alignmentBackbone.getStatistics().getExpectedMassDeviationBetweenSamples(),
                alignmentBackbone.getStatistics().getExpectedRetentionTimeDeviation(),
                !fwhms.isEmpty() ? fwhms.getDouble(fwhms.size()/2) : 0,
                !heightDividedByfwhms.isEmpty() ? heightDividedByfwhms.getDouble(heightDividedByfwhms.size()/2) : 0,
                (int)alignmentBackbone.getStatistics().getMedianNumberOfAlignments(),
                Double.isFinite(ms2NoiseLevelAvg) ? ms2NoiseLevelAvg : 0d
        );

    }

    private boolean isSuitableForImport(MergedTrace mergedTrace) {
        return mergedTrace.getTraces().length>0; // todo: implement a proper filter
    }

    protected void importScanPointMapping(ProcessedSample sample, long sampleId) throws IOException {
        RetentionTimeAxis axis = RetentionTimeAxis.builder()
                .runId(sampleId)
                .scanIndizes(sample.getMapping().scanIndizes)
                .retentionTimes(sample.getMapping().retentionTimes)
                .noiseLevelPerScan(sample.getStorage().getStatistics().getNoiseLevelPerScan()).build();
        siriusDatabaseAdapter.importRetentionTimeAxis(axis, false);
    }
    private void updateRetentionTimeAxis(ProcessedSample sample) throws IOException {
        RecalibrationFunction recalibrationFunction = sample.getRtRecalibration();
        RetentionTimeAxis axis = RetentionTimeAxis.builder()
                .runId(sample.getRun().getRunId())
                .scanIndizes(sample.getMapping().scanIndizes)
                .retentionTimes(sample.getMapping().retentionTimes)
                .noiseLevelPerScan(sample.getStorage().getStatistics().getNoiseLevelPerScan())
                .recalbratedRetentionTimes(Arrays.stream(sample.getMapping().retentionTimes).map(recalibrationFunction::value).toArray())
                .normalizationFactor(sample.getNormalizer().normalize(1d))
                .build();
        siriusDatabaseAdapter.importRetentionTimeAxis(axis, true);
    }

    private void collectStatistics(ProcessedSample sample) {
        // collect statistics
        final StatisticsCollectionStrategy.Calculation calc = statisticsCollector.collectStatistics();
        ;
        for (int idx = 0, n = sample.getMapping().length(); idx < n; ++idx) {
            calc.processMs1(sample.getStorage().getSpectrumStorage().ms1SpectrumHeader(idx), sample.getStorage().getSpectrumStorage().getSpectrum(idx));
        }
        for (Ms2SpectrumHeader h : sample.getStorage().getSpectrumStorage().ms2SpectraHeader()) {
            SimpleSpectrum ms2 = sample.getStorage().getSpectrumStorage().getMs2Spectrum(h.getUid());
            calc.processMs2(h, ms2);
        }
        sample.getStorage().setStatistics(calc.done());
    }

    private void collectStatisticsBeforeAlignment(ProcessedSample sample) {
        sample.setTraceStats(TraceStats.collect(sample));
    }


    /**
     * Learns how well two profiles of one and the same compound correlate on this sample, from its isotope pairs.
     * <p>
     * Runs directly after trace picking because that is the earliest point where the traces exist and the latest
     * where nothing has used a correlation yet. Isotope detection proper happens much later, in
     * {@link #extractMoIsForAlignment}, which is why {@link TraceCorrelationStatistics} finds its pairs itself.
     */
    private void learnTraceCorrelations(ProcessedSample sample) {
        final SampleStats stats = sample.getStorage().getStatistics();
        final TraceCorrelationStatistics learned = TraceCorrelationStatistics.learn(sample,
                stats.getMs1MassDeviationWithinTraces(), TraceCorrelationStatistics.medianNoiseLevel(stats));
        sample.getStorage().setStatistics(stats.withTraceCorrelations(learned));
        LoggerFactory.getLogger(LCMSProcessing.class).info("{}: {}", sample.getRun().getName(),
                learned.describe());
    }

    private void extractTraces(ProcessedSample sample) {
        final TracePicker tracePicker = new TracePicker(sample, traceCachingStrategy, segmentationStrategy);
        SampleStats statistics = sample.getStorage().getStatistics();
        tracePicker.setAllowedMassDeviation(sample.getStorage().getStatistics().getMs1MassDeviationWithinTraces());
        traceDetectionStrategy.findPeaksForExtraction(sample, (sample1, spectrumIdx, peakIdx, spectrum) -> {
            Optional<ContiguousTrace> peak = tracePicker.detectMostIntensivePeak(spectrumIdx, spectrum.getMzAt(peakIdx));
            peak.ifPresent(p->tracker.tracePicked(p.averagedMz(), sample.getMapping().getRetentionTimeAt(p.apex()), sample, p));
        });
        sample.getStorage().commit();
    }

    /**
     * DIA: every MS2 scan covers a wide isolation window and therefore holds the fragments of all compounds
     * co-eluting within it. Assigning such a spectrum to the most intensive precursor of its window, as the DDA
     * path does, would attach other compounds' fragments to that feature. Instead the fragments are picked as
     * chromatograms of their own and assigned to precursors by co-elution, and the result is written back as
     * ordinary MS2 spectra so that everything after this point stays unchanged.
     * <p>
     * This runs before the masses of interest are extracted, which means
     * {@code PrecursorsWithMsMsAreConfidentStrategy} rewards precursors whose deconvolution actually found
     * co-eluting fragments - a meaningful confidence signal for DIA, with no code change.
     */
    private void deconvoluteDia(ProcessedSample sample) {
        final Optional<DiaScanIndex> maybeIndex = DiaScanIndexBuilder.build(sample);
        if (maybeIndex.isEmpty()) {
            LoggerFactory.getLogger(LCMSProcessing.class).warn("{} looks like a data independent acquisition, but no "
                    + "usable acquisition scheme could be recovered from its MS/MS scans. Its features will have no "
                    + "MS/MS attached.", sample.getRun().getName());
            return;
        }
        final DiaScanIndex index = maybeIndex.get();
        final DiaReport report = diaAssignmentStrategy.assign(sample, index);
        LoggerFactory.getLogger(LCMSProcessing.class).info("{}: {}", sample.getRun().getName(), report.describe());
        diaReports.put(sample, report);
    }

    /**
     * Stage two of the DIA assignment: redo it over the final feature boundaries and across all samples at once.
     * <p>
     * Called between merging and feature extraction, and that position is what makes it possible at all. The
     * boundaries feature extraction will use come from {@link MergedFeatureExtractionStrategy#featureFinding}, which
     * is pure computation on the merged sample, so they can be had before anything is imported and the assignment
     * can be redone over them without a way of writing corrections into a project. The segmentation is run twice as
     * a result, which costs a fraction of a second and keeps the two from drifting apart.
     * <p>
     * Only the peaks of the pseudo spectra change, never the set of them: their references were copied into the
     * projected traces during merging, and {@code SegmentMergedFeatures} uses their positions as points of interest,
     * so adding or removing one would move the very boundaries this measures over.
     *
     * @return what was refined, or empty if no sample of this experiment is a deconvoluted DIA measurement
     */
    public Optional<String> refineDiaAssignments(ProcessedSample merged, AlignmentBackbone backbone) {
        final List<DiaAssignmentStrategy.SampleIndex> diaSamples = new ArrayList<>();
        for (ProcessedSample sample : backbone.getSamples()) {
            getDiaReport(sample).map(DiaReport::index).ifPresent(index ->
                    diaSamples.add(new DiaAssignmentStrategy.SampleIndex(sample, index)));
        }
        if (diaSamples.isEmpty()) return Optional.empty();

        final List<DiaFinalPrecursors.Aligned> features = new ArrayList<>();
        for (final Rect rect : merged.getStorage().getMergeStorage().getRectangleMap()) {
            final MergedTrace mergedTrace = collectMergedTrace(merged, rect.id);
            if (mergedTrace == null || mergedTrace.getTraces().length == 0) continue;
            features.addAll(DiaFinalPrecursors.of(merged, mergedTrace,
                    mergedFeatureExtractionStrategy.featureFinding(mergedTraceSegmentationStrategy, merged, mergedTrace)));
        }

        final String report = diaAssignmentStrategy.refine(diaSamples, features,
                backbone.getStatistics().getExpectedMassDeviationBetweenSamples(),
                backbone.getStatistics().getExpectedRetentionTimeDeviation());
        if (report != null) LoggerFactory.getLogger(LCMSProcessing.class).info(report);
        for (DiaAssignmentStrategy.SampleIndex sample : diaSamples) sample.sample().getStorage().commit();
        diaRefinementReport = report;
        return Optional.ofNullable(report);
    }

    private void assignMs2Trace(ProcessedSample sample) {
        for (Ms2SpectrumHeader ms2SpectrumHeader : sample.getStorage().getSpectrumStorage().ms2SpectraHeader()) {
            Optional<MsMsTraceReference> traceId = ms2TraceStrategy.getTraceFor(sample, ms2SpectrumHeader);
            if (traceId.isPresent()) {

                ///
                SimpleSpectrum ms2Spectrum = sample.getStorage().getSpectrumStorage().getMs2Spectrum(ms2SpectrumHeader.getUid());
                if (ms2Spectrum.isEmpty()) continue;
                CheckMs2Quality.PeakStats peakStats = CheckMs2Quality.peaksWithCommonMassDeltas(ms2SpectrumHeader.getPrecursorMz(), ms2Spectrum).mergeMax(CheckMs2Quality.signalPeaks(ms2Spectrum));
                if (peakStats.numberOfPeaks()<3 && peakStats.ratioOfIntensity()<0.25) {
                    continue;
                }
                ///

                sample.getStorage().getTraceStorage().assignTraceForMs2(traceId.get());
            } else {
                LoggerFactory.getLogger(LCMSProcessing.class).debug("No suitable trace found for MSMS " + ms2SpectrumHeader);
            }
        }
        sample.getStorage().commit();
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

                if (trace.getSegments().length==1 && !trace.isNoisyTrace()) moi.setSingleApex(true);
                if (trace.getSegments().length>=20 && (trace.endId()-trace.startId()) >= sample.getMapping().length()/4) {
                    moi.setUltraLargeTraceFlag(true);
                }
                detectIsotopesForMoI(sample, trace, segment, moi);
                moi.setConfidence(confidenceEstimatorStrategy.estimateConfidence(sample, trace, moi, null));
                if (trace.isNoisyTrace()) moi.setConfidence(Math.min(0f, moi.getConfidence()));
                if (moi.getConfidence() >= 0) {
                    //System.out.println(moi + " intensity = " + moi.getIntensity() + ", isotopes = " + (moi.getIsotopes()==null ? 0 : moi.getIsotopes().isotopeIntensities.length) + ", confidence = "+ moi.getConfidence());
                    alignmentStorage.addMoI(moi);
                    tracker.moiAccepted(moi.getMz(), moi.getRetentionTime(), sample, trace, moi);
                } else {
                    tracker.moiRejected(moi.getMz(), moi.getRetentionTime(), sample, trace, moi);
                }
            }
        }
        sample.getStorage().commit();
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
        if (projectedTraces.length==0) {
            LoggerFactory.getLogger(LCMSProcessing.class).error("Merged trace disappeared");
            return null;
        }
        return MergedTrace.collect(merged, sampleByIdx, projectedTraces, isotopes);
    }

    public void closeStorages(){
        storageFactory.close();
    }



}
