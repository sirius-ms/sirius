package de.unijena.bioinf.lcms;

import de.unijena.bioinf.lcms.align.*;
import de.unijena.bioinf.lcms.features.IsotopePatternExtractionStrategy;
import de.unijena.bioinf.lcms.features.MergedApexIsotopePatternExtractor;
import de.unijena.bioinf.lcms.features.MergedFeatureExtractionStrategy;
import de.unijena.bioinf.lcms.features.MergedFeatureExtractor;
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
import de.unijena.bioinf.lcms.projectspace.ImportStrategy;
import de.unijena.bioinf.lcms.projectspace.ProjectSpaceImporter;
import de.unijena.bioinf.lcms.spectrum.Ms2SpectrumHeader;
import de.unijena.bioinf.lcms.statistics.*;
import de.unijena.bioinf.lcms.trace.*;
import de.unijena.bioinf.lcms.trace.ProcessedSample;
import de.unijena.bioinf.lcms.trace.segmentation.PersistentHomology;
import de.unijena.bioinf.lcms.trace.segmentation.TraceSegment;
import de.unijena.bioinf.lcms.trace.segmentation.TraceSegmentationStrategy;
import de.unijena.bioinf.lcms.traceextractor.*;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.core.run.Chromatography;
import de.unijena.bioinf.ms.persistence.model.core.run.Run;
import de.unijena.bioinf.ms.persistence.model.core.trace.AbstractTrace;
import de.unijena.bioinf.ms.persistence.model.core.trace.SourceTrace;
import de.unijena.bioinf.ms.persistence.storage.MsProjectDocumentDatabase;
import de.unijena.bioinf.storage.db.nosql.Database;
import it.unimi.dsi.fastutil.ints.*;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
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
    @Getter @Setter private AlignmentAlgorithm alignmentAlgorithm = new GreedyAlgorithm();
    @Getter @Setter private AlignmentScorer alignmentScorerBackbone  = new AlignmentScorer(1.5);
    @Getter @Setter private AlignmentScorer alignmentScorerFull = new AlignmentScorer(4);
    @Getter @Setter private MergeTracesWithoutGapFilling mergeStrategy = new MergeTracesWithoutGapFilling();

    @Getter @Setter private ImportStrategy importStrategy = new ProjectSpaceImporter();

    @Getter @Setter private TraceExtractionStrategy traceExtractionStrategy = new TraceExtractor();

    @Getter @Setter private MergedFeatureExtractionStrategy mergedFeatureExtractionStrategy = new MergedFeatureExtractor();

    @Getter @Setter private Ms2MergeStrategy ms2MergeStrategy = new MergeGreedyStrategy();

    @Getter @Setter private IsotopePatternExtractionStrategy isotopePatternExtractionStrategy = new MergedApexIsotopePatternExtractor();

    protected List<ProcessedSample> samples = new ArrayList<>();

    /**
     * parses an MZML file and stores the processed sample. Note: we should add possibility to parse from input
     * stream later
     */
    public ProcessedSample processSample(File file) throws IOException {
        return processSample(file, false, Run.Type.SAMPLE, Chromatography.LC);
    }

    /**
     * parses an MZML file and stores the processed sample. Note: we should add possibility to parse from input
     * stream later
     */
    public ProcessedSample processSample(
            File file,
            boolean saveRawScans,
            Run.Type runType,
            Chromatography chromatography
    ) throws IOException {
        // parse file and extract spectra
        ProcessedSample sample = LCMSImporter.importToProject(
                file, storageFactory, importStrategy, saveRawScans, runType, chromatography);
        sample.setUid(this.samples.size());
        this.samples.add(sample);
        sample.active();
        collectStatistics(sample);
        extractTraces(sample);
        assignMs2Trace(sample);
        sample.setNormalizer(normalizationStrategy.computeNormalization(sample));
        extractMoIsForAlignment(sample);
        collectStatisticsBeforeAlignment(sample);
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
        alignmentBackbone = alignmentStrategy.align(merged, alignmentBackbone, Arrays.asList(alignmentBackbone.getSamples()), alignmentAlgorithm, alignmentScorerFull);
        for (ProcessedSample sample : alignmentBackbone.getSamples()) {
            sample.setScanPointInterpolator(new ScanPointInterpolator(merged.getMapping(), sample.getMapping(), sample.getRtRecalibration()));
        }
        return alignmentBackbone;
    }

    public ProcessedSample merge(AlignmentBackbone backbone) {
        ProcessedSample merged = this.samples.get(samples.size()-1);
        mergeStrategy.merge(merged, backbone);
        return merged;
    }

    public void extractFeaturesAndExportToProjectSpace(ProcessedSample merged, AlignmentBackbone backbone) throws IOException {
        final Int2ObjectMap<ProcessedSample> idx2sample = new Int2ObjectOpenHashMap<>();
        for (ProcessedSample s : backbone.getSamples()) idx2sample.put(s.getUid(),s);

        Int2LongMap trace2trace = new Int2LongOpenHashMap();
        for (MergedTrace trace : merged.getStorage().getMergeStorage()) {
            trace.finishMerging();
            ProcessedSample[] samplesInTrace = new ProcessedSample[trace.getSampleIds().size()];
            for (int i = 0; i < trace.getSampleIds().size(); ++i) {
                samplesInTrace[i] = idx2sample.get(trace.getSampleIds().getInt(i));
            }

            Iterator<IntObjectPair<AbstractTrace>> titer = traceExtractionStrategy.extractTrace(merged, samplesInTrace, trace);
            while (titer.hasNext()) {
                IntObjectPair<AbstractTrace> pair = titer.next();
                AbstractTrace atrace = pair.right();
                importStrategy.importTrace(atrace);
                if (atrace instanceof de.unijena.bioinf.ms.persistence.model.core.trace.MergedTrace) {
                    trace2trace.put(pair.leftInt(), ((de.unijena.bioinf.ms.persistence.model.core.trace.MergedTrace) atrace).getMergedTraceId());
                } else if (atrace instanceof SourceTrace) {
                    trace2trace.put(pair.leftInt(), ((SourceTrace) atrace).getSourceTraceId());
                }
            }
        }

        for (MergedTrace trace : merged.getStorage().getMergeStorage()) {
            Iterator<AlignedFeatures> fiter = mergedFeatureExtractionStrategy.extractFeatures(merged, trace, ms2MergeStrategy, isotopePatternExtractionStrategy, trace2trace, idx2sample);
            while (fiter.hasNext()) {
                importStrategy.importAlignedFeature(fiter.next());
            }
        }
    }

    public void exportFeaturesToFiles(ProcessedSample merged, AlignmentBackbone backbone) {
        int J=0;
        final HashMap<Integer, ProcessedSample> idx2sample = new HashMap<>();
        for (ProcessedSample s : backbone.getSamples()) idx2sample.put(s.getUid(),s);

        try {
            Path p = Path.of(System.getProperty("lcms.logdir"), "data.js");
            Files.createDirectories(p.getParent());
            try (PrintStream out = new PrintStream(p.toAbsolutePath().toString())) {
                out.print("document.traces = {");
                for (MergedTrace trace : merged.getStorage().getMergeStorage()) {
                    //if (trace.getSampleIds().size()<6 || trace.toTrace(merged).apexIntensity() < 0.01) continue;
                    ProcessedSample[] samplesInTrace = new ProcessedSample[trace.getSampleIds().size()];
                    for (int i = 0; i < trace.getSampleIds().size(); ++i) {
                        samplesInTrace[i] = idx2sample.get(trace.getSampleIds().getInt(i));
                    }
                    String line = ((MergedFeatureExtractor) mergedFeatureExtractionStrategy).extractFeaturesToString(merged, samplesInTrace, trace, ms2MergeStrategy);
                    if (line !=null) {
                        out.println("\"" + trace.getUid() + "\": " +  line + ",");
                    }
                }
                out.println("\n};");
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void collectStatistics(ProcessedSample sample) {
        // collect statistics
        final StatisticsCollectionStrategy.Calculation calc = statisticsCollector.collectStatistics();;
        for (int idx=0, n=sample.getMapping().length(); idx < n; ++idx) {
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
            double minMz=trace.averagedMz(), maxMz = trace.averagedMz();
            for (TraceSegment segment : trace.getSegments()) {
                minMz = Math.min(trace.mz(segment.apex), minMz);
                maxMz = Math.max(trace.mz(segment.apex), maxMz);
            }
            for (TraceSegment segment : trace.getSegments()) {
                Rect rect = trace.rectWithRts();
                rect.minMz = (float)minMz;
                rect.maxMz = (float)maxMz;
                MoI moi = new MoI(rect, segment.apex, sample.getMapping().getRetentionTimeAt(segment.apex),
                        (float)normalizer.normalize(trace.intensity(segment.apex)), sample.getUid());

                detectIsotopesForMoI(sample, trace, segment, moi);

                moi.setConfidence(confidenceEstimatorStrategy.estimateConfidence(sample,trace,moi, null));
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


}
