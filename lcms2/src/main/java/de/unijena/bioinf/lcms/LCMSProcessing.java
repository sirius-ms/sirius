package de.unijena.bioinf.lcms;

import de.unijena.bioinf.lcms.align2.*;
import de.unijena.bioinf.lcms.features.MergedFeatureExtractionStrategy;
import de.unijena.bioinf.lcms.features.MergedFeatureExtractor;
import de.unijena.bioinf.lcms.io.MZmlSampleParser;
import de.unijena.bioinf.lcms.merge2.MergeTracesWithoutGapFilling;
import de.unijena.bioinf.lcms.merge2.MergedTrace;
import de.unijena.bioinf.lcms.merge2.ScanPointInterpolator;
import de.unijena.bioinf.lcms.msms.MostIntensivePeakInIsolatioNWindowAssignmentStrategy;
import de.unijena.bioinf.lcms.msms.Ms2TraceStrategy;
import de.unijena.bioinf.lcms.projectspace.ImportStrategy;
import de.unijena.bioinf.lcms.spectrum.Ms2SpectrumHeader;
import de.unijena.bioinf.lcms.statistics.*;
import de.unijena.bioinf.lcms.trace.ContiguousTrace;
import de.unijena.bioinf.lcms.trace.LCMSStorage;
import de.unijena.bioinf.lcms.trace.ProcessedSample;
import de.unijena.bioinf.lcms.trace.Rect;
import de.unijena.bioinf.lcms.trace.segmentation.LegacySegmenter;
import de.unijena.bioinf.lcms.trace.segmentation.PersistentHomology;
import de.unijena.bioinf.lcms.trace.segmentation.TraceSegment;
import de.unijena.bioinf.lcms.trace.segmentation.TraceSegmentationStrategy;
import de.unijena.bioinf.lcms.traceextractor.*;
import de.unijena.bioinf.ms.persistence.model.core.AlignedFeatures;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
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

    @Getter @Setter private Ms2TraceStrategy ms2TraceStrategy = new MostIntensivePeakInIsolatioNWindowAssignmentStrategy();

    /**
     * This class determines how we ensure that no two peaks are picked in different traces
     */
    @Getter @Setter private TraceCachingStrategy traceCachingStrategy = new RectbasedCachingStrategy();

    @Getter @Setter private TraceSegmentationStrategy segmentationStrategy = new PersistentHomology();

    @Getter @Setter private MassOfInterestConfidenceEstimatorStrategy confidenceEstimatorStrategy = new MassOfInterestCombinedStrategy(
        new IsotopesAndAdductsAreConfidentStrategy(), new PrecursorsWithMsMsAreConfidentStrategy()
    );

    @Getter @Setter private NormalizationStrategy normalizationStrategy = new AverageOfTop100TracesNormalization();

    @Getter @Setter private AlignmentStrategy alignmentStrategy = new GreedyTwoStageAlignmentStrategy();
    @Getter @Setter private AlignmentAlgorithm alignmentAlgorithm = new GreedyAlgorithm();
    @Getter @Setter private AlignmentScorer alignmentScorerBackbone  = new AlignmentScorer(1.5);
    @Getter @Setter private AlignmentScorer alignmentScorerFull = new AlignmentScorer(4);
    @Getter @Setter private MergeTracesWithoutGapFilling mergeStrategy = new MergeTracesWithoutGapFilling();

    @Getter @Setter private ImportStrategy importStrategy;

    @Getter @Setter private MergedFeatureExtractionStrategy mergedFeatureExtractionStrategy = new MergedFeatureExtractor();

    protected List<ProcessedSample> samples = new ArrayList<>();

    /**
     * parses an MZML file and stores the processed sample. Note: we should add possibility to parse from input
     * stream later
     * TODO: implement other than MZML
     */
    public ProcessedSample processSample(File file) throws IOException {
        // parse file and extract spectra
        ProcessedSample sample = new MZmlSampleParser().parse(file, storageFactory);
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
                null,
                null,
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

    public void extractFeaturesAndExportToProjectSpace(ProcessedSample merged, AlignmentBackbone backbone, Object storage) {
        final HashMap<Integer, ProcessedSample> idx2sample = new HashMap<>();
        for (ProcessedSample s : backbone.getSamples()) idx2sample.put(s.getUid(),s);
        for (MergedTrace trace : merged.getTraceStorage().getMergeStorage()) {
            trace.finishMerging();
            ProcessedSample[] samplesInTrace = new ProcessedSample[trace.getSampleIds().size()];
            trace.getTraceIds().forEach(x->samplesInTrace[x]=idx2sample.get(x));
            Iterator<AlignedFeatures> fiter = mergedFeatureExtractionStrategy.extractFeatures(merged, samplesInTrace, trace);
            while (fiter.hasNext()) {
                importStrategy.importAlignedFeature(storage, fiter.next());
            }
        }
    }
    public void exportFeaturesToFiles(ProcessedSample merged, AlignmentBackbone backbone) {
        int J=0;
        final HashMap<Integer, ProcessedSample> idx2sample = new HashMap<>();
        for (ProcessedSample s : backbone.getSamples()) idx2sample.put(s.getUid(),s);
        try (PrintStream out = new PrintStream("/home/kaidu/analysis/lcms/view/data.js")) {
            out.print("document.lcdata = [");
            for (MergedTrace trace : merged.getTraceStorage().getMergeStorage()) {
                //if (trace.getSampleIds().size()<6 || trace.toTrace(merged).apexIntensity() < 0.01) continue;
                ProcessedSample[] samplesInTrace = new ProcessedSample[trace.getSampleIds().size()];
                for (int i = 0; i < trace.getTraceIds().size(); ++i) {
                    samplesInTrace[i] = idx2sample.get(trace.getSampleIds().getInt(i));
                }
                String line = ((MergedFeatureExtractor) mergedFeatureExtractionStrategy).extractFeaturesToString(merged, samplesInTrace, trace);
                if (line !=null) {
                    out.println(line + ",");
                }
            }
            out.println("\n];");
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void collectStatistics(ProcessedSample sample) {
        // collect statistics
        final StatisticsCollectionStrategy.Calculation calc = statisticsCollector.collectStatistics();;
        for (int idx=0, n=sample.getMapping().length(); idx < n; ++idx) {
            calc.processMs1(sample.getTraceStorage().ms1SpectrumHeader(idx), sample.getTraceStorage().getSpectrum(idx));
        }
        sample.getTraceStorage().setStatistics(calc.done());
    }

    private void collectStatisticsBeforeAlignment(ProcessedSample sample) {
        sample.setTraceStats(TraceStats.collect(sample));
    }

    private void extractTraces(ProcessedSample sample) {
        final TracePicker tracePicker = new TracePicker(sample, traceCachingStrategy, segmentationStrategy);
        tracePicker.setAllowedMassDeviation(sample.getTraceStorage().getStatistics().getMs1MassDeviationWithinTraces());
        traceDetectionStrategy.findPeaksForExtraction(sample, (sample1, spectrumIdx, peakIdx, spectrum) -> {
            Optional<ContiguousTrace> peak = tracePicker.detectMostIntensivePeak(spectrumIdx, spectrum.getMzAt(peakIdx));
        });
    }

    private void assignMs2Trace(ProcessedSample sample) {
        for (Ms2SpectrumHeader ms2SpectrumHeader : sample.getTraceStorage().ms2SpectraHeader()) {
            int traceId = ms2TraceStrategy.getTraceFor(sample, ms2SpectrumHeader);
            if (traceId >= 0) {
                sample.getTraceStorage().setTraceForMs2(ms2SpectrumHeader.getUid(), traceId);
            } else {
                LoggerFactory.getLogger(LCMSProcessing.class).warn("No suitable trace found for MSMS " + ms2SpectrumHeader);
            }
        }
    }

    private void extractMoIsForAlignment(ProcessedSample sample) {
        final AlignmentStorage alignmentStorage = sample.getTraceStorage().getAlignmentStorage();
        NormalizationStrategy.Normalizer normalizer = sample.getNormalizer();
        for (ContiguousTrace trace : sample.getTraceStorage()) {
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
                moi.setConfidence(confidenceEstimatorStrategy.estimateConfidence(sample,trace,moi));
                if (moi.getConfidence() >= 0) {
                    alignmentStorage.addMoI(moi);
                }
            }
        }
    }


}
