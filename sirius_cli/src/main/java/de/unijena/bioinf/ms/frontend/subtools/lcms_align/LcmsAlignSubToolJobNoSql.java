/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.frontend.subtools.lcms_align;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.utils.DataQuality;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.lcms.LCMSProcessing;
import de.unijena.bioinf.lcms.adducts.AdductManager;
import de.unijena.bioinf.lcms.adducts.AdductNetwork;
import de.unijena.bioinf.lcms.adducts.ProjectSpaceTraceProvider;
import de.unijena.bioinf.lcms.adducts.assignment.OptimalAssignmentViaBeamSearch;
import de.unijena.bioinf.lcms.align.AlignmentBackbone;
import de.unijena.bioinf.lcms.align.AlignmentThresholds;
import de.unijena.bioinf.lcms.align.MoI;
import de.unijena.bioinf.lcms.projectspace.SiriusProjectDocumentDbAdapter;
import de.unijena.bioinf.lcms.quality.*;
import de.unijena.bioinf.lcms.statistics.UserSpecifiedThresholds;
import de.unijena.bioinf.lcms.trace.ProcessedSample;
import de.unijena.bioinf.lcms.trace.filter.GaussFilter;
import de.unijena.bioinf.lcms.trace.filter.NoFilter;
import de.unijena.bioinf.lcms.trace.filter.SavitzkyGolayFilter;
import de.unijena.bioinf.lcms.trace.filter.WaveletFilter;
import de.unijena.bioinf.lcms.trace.segmentation.PersistentHomology;
import de.unijena.bioinf.lcms.trace.segmentation.TraceSegmentationStrategy;
import de.unijena.bioinf.ms.frontend.subtools.InputFilesOptions;
import de.unijena.bioinf.ms.frontend.subtools.PreprocessingJob;
import de.unijena.bioinf.ms.persistence.model.core.Compound;
import de.unijena.bioinf.ms.persistence.model.core.QualityReport;
import de.unijena.bioinf.ms.persistence.model.core.feature.AbstractFeature;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.core.feature.CorrelatedIonPair;
import de.unijena.bioinf.ms.persistence.model.core.run.MergedLCMSRun;
import de.unijena.bioinf.ms.persistence.model.core.run.RetentionTimeAxis;
import de.unijena.bioinf.ms.persistence.model.core.spectrum.MSData;
import de.unijena.bioinf.ms.persistence.model.properties.ProjectSourceFormats;
import de.unijena.bioinf.ms.persistence.model.properties.ProjectType;
import de.unijena.bioinf.ms.persistence.storage.SiriusProjectDatabaseImpl;
import de.unijena.bioinf.ms.persistence.storage.exceptions.ProjectStateException;
import de.unijena.bioinf.ms.persistence.storage.exceptions.ProjectTypeException;
import de.unijena.bioinf.projectspace.NoSQLProjectSpaceManager;
import de.unijena.bioinf.projectspace.ProjectSpaceManager;
import de.unijena.bioinf.storage.db.nosql.Database;
import de.unijena.bioinf.storage.db.nosql.Filter;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.longs.*;
import lombok.Getter;
import org.apache.commons.io.function.IOSupplier;
import org.apache.commons.lang3.time.StopWatch;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class LcmsAlignSubToolJobNoSql extends PreprocessingJob<ProjectSpaceManager> {
    private static final Logger log = LoggerFactory.getLogger(LcmsAlignSubToolJobNoSql.class);
    List<Path> inputFiles;

    private final IOSupplier<? extends NoSQLProjectSpaceManager> projectSupplier;

    private final TraceSegmentationStrategy mergedTraceSegmenter;

    private final boolean alignRuns;

    private de.unijena.bioinf.lcms.trace.filter.Filter filter = null;

    private boolean inMemoryOnMerged = false;

    @Getter
    @Nullable
    private LongLinkedOpenHashSet importedFeatureIds = null;

    @Getter
    @Nullable
    private LongLinkedOpenHashSet importedCompoundIds = null;


    private final boolean saveImportedCompounds;

    private long progress;

    private final double minSNR;

    private UserSpecifiedThresholds userSpecifiedThresholds = new UserSpecifiedThresholds();

    private long totalProgress;

    private AlignmentThresholds alignmentThresholds;

    public LcmsAlignSubToolJobNoSql(InputFilesOptions input, @NotNull IOSupplier<? extends NoSQLProjectSpaceManager> projectSupplier, LcmsAlignOptions options) {
        this(input.msInput.lcmsFiles.keySet().stream().sorted().collect(Collectors.toList()), projectSupplier, options);
    }

    public LcmsAlignSubToolJobNoSql(@NotNull List<Path> inputFiles, @NotNull IOSupplier<? extends NoSQLProjectSpaceManager> projectSupplier, LcmsAlignOptions options) {
        super();
        this.inputFiles = inputFiles;
        this.projectSupplier = projectSupplier;
        this.alignRuns = !options.noAlign;
        if (options.noiseIntensity>=0) this.userSpecifiedThresholds.setMs1NoiseLevel(options.noiseIntensity);
        if (options.ppmMax!=null) this.userSpecifiedThresholds.setAllowedMassDeviationInMs1(new Deviation(options.ppmMax));

        this.inMemoryOnMerged = options.inMemory;

        this.filter = switch (options.smoothing) {
            case AUTO -> inputFiles.size() < 3 ? new GaussFilter(0.5) : new NoFilter();
            case NOFILTER -> new NoFilter();
            case GAUSSIAN -> new GaussFilter(options.sigma);
            case WAVELET -> new WaveletFilter(options.scaleLevel);
            case SAVITZKY_GOLAY -> new SavitzkyGolayFilter();
        };
        this.saveImportedCompounds = false;
        this.alignmentThresholds = new AlignmentThresholds();
        if (options.alignRtMax>=0) {
            this.alignmentThresholds.setMaximalAllowedRetentionTimeError(options.alignRtMax);
        }
        if (options.alignPpmMax>=0) {
            this.alignmentThresholds.setMaximalAllowedMassError(new Deviation(options.alignPpmMax));
        }

        if (options.snrOptions.sensitive) {
            this.minSNR = 2d;
        } else {
            this.minSNR = options.snrOptions.minSNR;
        }

        this.mergedTraceSegmenter = new PersistentHomology(this.filter, this.minSNR, PersistentHomology.PERSISTENCE_COEFFICIENT, PersistentHomology.MERGE_COEFFICIENT);
    }

    public LcmsAlignSubToolJobNoSql(
            @NotNull List<Path> inputFiles,
            @NotNull IOSupplier<? extends NoSQLProjectSpaceManager> projectSupplier,
            boolean alignRuns,
            DataSmoothing filter,
            double sigma,
            int scale,
            double noiseIntensity,
            double minSNR,
            @Nullable AlignmentThresholds alignmentThresholds,
            @Nullable Deviation ms1Massdev,
            boolean saveImportedCompounds
    ) {
        super();
        this.inputFiles = inputFiles;
        this.projectSupplier = projectSupplier;
        this.alignRuns = alignRuns;
        this.userSpecifiedThresholds = new UserSpecifiedThresholds();
        if (ms1Massdev!=null) userSpecifiedThresholds.setAllowedMassDeviationInMs1(ms1Massdev);
        if (noiseIntensity>=0) userSpecifiedThresholds.setMs1NoiseLevel(noiseIntensity);
        this.filter = switch (filter) {
            case AUTO -> inputFiles.size() < 3 ? new GaussFilter(0.5) : new NoFilter();
            case NOFILTER -> new NoFilter();
            case GAUSSIAN -> new GaussFilter(sigma);
            case WAVELET -> new WaveletFilter(scale);
            case SAVITZKY_GOLAY -> new SavitzkyGolayFilter();
        };
        this.minSNR = minSNR;
        this.mergedTraceSegmenter = new PersistentHomology(this.filter, minSNR, PersistentHomology.PERSISTENCE_COEFFICIENT, PersistentHomology.MERGE_COEFFICIENT);
        this.saveImportedCompounds = saveImportedCompounds;
        if (alignmentThresholds!=null) this.alignmentThresholds = alignmentThresholds;
        else this.alignmentThresholds = new AlignmentThresholds();
    }

    private void compute(SiriusProjectDatabaseImpl<? extends Database<?>> ps, List<Path> files) throws IOException {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        setProjectTypeOrThrow(ps);
        setProjectSourceFormats(ps);

        LCMSProcessing processing = new LCMSProcessing(new SiriusProjectDocumentDbAdapter(ps), saveImportedCompounds, ps.getStorage().location().getParent(), inMemoryOnMerged);
        processing.setMergedTraceSegmentationStrategy(mergedTraceSegmenter);
        // the segmentationStrategy is used for picking the mass traces and corresponding peaks
        // the mergedSegmentationStrategy is then used at the final feature detection step
        // we always want to have the first mass trace detection as sensitive as possible, that's why we use minSNR=2 here
        // the number of features, in contrast, mainly depends on the settings for mergedSegmentationStrategy
        //
        processing.setSegmentationStrategy(new PersistentHomology(this.filter, Math.min(2, this.minSNR), PersistentHomology.PERSISTENCE_COEFFICIENT, PersistentHomology.MERGE_COEFFICIENT));
        processing.setAlignmentThresholds(this.alignmentThresholds);
        if (userSpecifiedThresholds.hasUserInput()) {
            processing.setStatisticsCollector(userSpecifiedThresholds);
        }

        try {
            {
                updateProgress(totalProgress, progress, "Processing Runs");
                List<BasicJJob<ProcessedSample>> jobs = new ArrayList<>();
                int atmost = Integer.MAX_VALUE;
                for (Path f : files) {
                    if (--atmost < 0) break;
                    jobs.add(SiriusJobs.getGlobalJobManager().submitJob(new BasicJJob<ProcessedSample>() {
                        @Override
                        protected ProcessedSample compute() throws Exception {
                            ProcessedSample sample = processing.processSample(f);
                            int hasIsotopes = 0, hasNoIsotopes = 0;
                            for (MoI m : sample.getStorage().getAlignmentStorage()) {
                                if (m.hasIsotopes()) ++hasIsotopes;
                                else ++hasNoIsotopes;
                            }
                            sample.inactive();
                            System.out.println(sample.getUid() + " with " + hasIsotopes + " / " + (hasIsotopes + hasNoIsotopes) + " isotope features");
                            return sample;
                        }
                    }));
                }

                int count = 0;
                for (BasicJJob<ProcessedSample> job : jobs) {
                    System.out.println(job.takeResult().getUid() + " (" + ++count + " / " + jobs.size() + ")");
                    updateProgress(totalProgress, ++progress, "Processing Runs");
                }
            }

            updateProgress(totalProgress, progress, "Aligning runs");
            AlignmentBackbone bac = processing.align();
            HashMap<DataQuality, Integer> countMap, countMapMs2;

            updateProgress(totalProgress, ++progress, "Merging runs");
            ProcessedSample merged = processing.merge(bac);
            /*
            DoubleArrayList avgAl = new DoubleArrayList();
            System.out.println("AVERAGE = " + avgAl.doubleStream().sum() / avgAl.size());
            System.out.println("Good Traces = " + avgAl.doubleStream().filter(x -> x >= 5).sum());
            */
            updateProgress(totalProgress, ++progress, "Importing features");
            if (processing.extractFeaturesAndExportToProjectSpace(merged, bac) == 0) {
                System.err.println("No features found.");
                progress += 2;
                updateProgress(totalProgress, progress, "No features");
                return;
            }
            importedFeatureIds.addAll(processing.getImportedFeatureIds());

            updateProgress(totalProgress, ++progress, "Detecting adducts");
            System.out.printf("\nMerged Run: %s\n\n", merged.getRun().getName());

            final double allowedAdductRtDeviation;
            if (bac.getSamples().length <= 3) {
                FloatArrayList peakWidths = new FloatArrayList();
                for (long fid : processing.getImportedFeatureIds()) {
                    ps.getStorage().getByPrimaryKey(fid, AlignedFeatures.class).ifPresent((feature) -> {
                        // here we can also obtain statistics if we need them
                        Double v = feature.getFwhm();
                        if (v != null) peakWidths.add(v.floatValue());

                    });
                }
                float medianPeakWidth = 1;
                if (!peakWidths.isEmpty()) {
                    peakWidths.sort(null);
                    medianPeakWidth = peakWidths.getFloat(peakWidths.size() / 2);
                }
                allowedAdductRtDeviation = Math.max(1, medianPeakWidth);
            } else {
                allowedAdductRtDeviation = bac.getStatistics().getExpectedRetentionTimeDeviation();
            }
            LoggerFactory.getLogger(LcmsAlignSubToolJobNoSql.class).info("Use {} s as allowed deviation between adducts", allowedAdductRtDeviation);

            AdductManager adductManager = new AdductManager(merged.getPolarity());

            // -_- na toll, die Liste ist nicht identisch mit den Configs. Macht irgendwie auch Sinn. Ich will aber ungern
            // Multimere in die AductSettings reinpacken, das zu debuggen wird die Hoelle. Machen wir ein andern Mal.
            ProjectSpaceTraceProvider provider = new ProjectSpaceTraceProvider(ps);
            {
                final LongList importedCids = new LongArrayList();
                AlignedFeatures[] alignedFeatures = ps.getStorage().findAllStr(AlignedFeatures.class)
                        .filter(f -> f.getApexIntensity() != null)
                        .filter(AbstractFeature::isRTInterval)
                        .toArray(AlignedFeatures[]::new);
                AdductNetwork network = new AdductNetwork(provider, alignedFeatures, adductManager, allowedAdductRtDeviation, bac.getStatistics().getExpectedRetentionTimeDeviation());


                long TIME1 = System.currentTimeMillis();
                network.buildNetworkFromMassDeltas(SiriusJobs.getGlobalJobManager());
                //network.assign(SiriusJobs.getGlobalJobManager(), new OptimalAssignmentViaBeamSearch(), merged.getPolarity(),
                //        (compound) -> groupFeaturesToCompound(store, compound, importedCids));
                long TIME2 = System.currentTimeMillis();
                System.out.printf("Building adduct network took %f seconds\n", (TIME2-TIME1)/1000d);
                network.assignNetworksAndAdductsToFeatures(
                        SiriusJobs.getGlobalJobManager(),
                        new OptimalAssignmentViaBeamSearch(),
                        merged.getPolarity(),
                        x->ps.getStorage().upsert(x),
                        (net)->{ps.getStorage().insert(net); return net.getNetworkId();},
                        (feature)->{Compound c = Compound.singleton(feature); ps.getStorage().insert(c); return c.getCompoundId();}
                );

                long TIME3 = System.currentTimeMillis();
                System.out.printf("Assigning adducts took %f seconds\n", (TIME3-TIME2)/1000d);
                importedCompoundIds.addAll(importedCids);
            }

            updateProgress(totalProgress, ++progress, "Assessing data quality");
            // quality assessment
            countMap = new HashMap<>();
            countMapMs2 = new HashMap<>();
            for (DataQuality q : DataQuality.values()) {
                countMap.put(q, 0);
                countMapMs2.put(q,0);
            }
            final QualityAssessment qa = alignRuns ? new QualityAssessment() : new QualityAssessment(List.of(new CheckPeakQuality(), new CheckIsotopeQuality(), new CheckMs2Quality(), new CheckAdductQuality()));

            record DataQualityItem (DataQuality quality, boolean ms2) {}

            ArrayList<BasicJJob<DataQualityItem>> jobs = new ArrayList<>();
            ps.getStorage().fetchChild(merged.getRun(), "runId", "retentionTimeAxis", RetentionTimeAxis.class);
            ps.fetchLCMSRuns((MergedLCMSRun) merged.getRun());
            long TIME1 = System.currentTimeMillis();
            ps.getStorage().findStr(Filter.where("runId").eq(merged.getRun().getRunId()), AlignedFeatures.class).filter(x -> x.getDataQuality() == DataQuality.NOT_APPLICABLE).forEach(feature -> {
                jobs.add(SiriusJobs.getGlobalJobManager().submitJob(new BasicJJob<DataQualityItem>() {
                    @Override
                    protected DataQualityItem compute() {
                        QualityReport report = QualityReport.withDefaultCategories(alignRuns);
                        ps.fetchFeatures(feature);
                        ps.fetchIsotopicFeatures(feature);
                        ps.fetchMsData(feature);
                        try {
                            qa.addToReport(report, (MergedLCMSRun) merged.getRun(), feature, provider);
                            report.setAlignedFeatureId(feature.getAlignedFeatureId());
                            feature.setDataQuality(report.getOverallQuality());
                            ps.getStorage().insert(report);
                            ps.getStorage().upsert(feature);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        return new DataQualityItem(report.getOverallQuality(), feature.isHasMsMs());
                    }
                }));
            });
            jobs.forEach(x -> {
                DataQualityItem q = x.takeResult();
                countMap.put(q.quality, countMap.get(q.quality) + 1);
                if (q.ms2) countMapMs2.put(q.quality, countMapMs2.get(q.quality)+1);
            });
            long TIME2 = System.currentTimeMillis();
            System.out.printf("Quality Assesment took %f seconds\n", (TIME2-TIME1)/1000d);
            System.out.printf(
                    """
                             -------- Preprocessing Summary ---------
                             Preprocessed data in:      %s
                             # Good Al. Features:           \t%d \t(with MS/MS: %d)
                             # Decent Al. Features:         \t%d \t(with MS/MS: %d)
                             # Bad Al. Features:            \t%d \t(with MS/MS: %d)
                             # Lowest Quality Al. Features: \t%d \t(with MS/MS: %d)
                            
                             # Total Al. Features: \t%d \t(with MS/MS: %d)
                            \s""",
                    stopWatch,

                    countMap.get(DataQuality.GOOD), countMapMs2.get(DataQuality.GOOD),
                    countMap.get(DataQuality.DECENT), countMapMs2.get(DataQuality.DECENT),
                    countMap.get(DataQuality.BAD), countMapMs2.get(DataQuality.BAD),
                    countMap.get(DataQuality.LOWEST), countMapMs2.get(DataQuality.LOWEST),
                    countMap.values().stream().mapToInt(Integer::intValue).sum(),
                    countMapMs2.values().stream().mapToInt(Integer::intValue).sum()
            );
        } finally {
            processing.closeStorages();
        }
    }

    private void setProjectSourceFormats(SiriusProjectDatabaseImpl<? extends Database<?>> ps) {
        ProjectSourceFormats psSources = ps.findProjectSourceFormats().orElseGet(ProjectSourceFormats::new);
        inputFiles.stream().map(Path::toString).map(FileUtils::getFileExt).filter(Objects::nonNull)
                .forEach(psSources::addFormat);
        ps.upsertProjectSourceFormats(psSources);
    }

    private void setProjectTypeOrThrow(SiriusProjectDatabaseImpl<? extends Database<?>> ps) {
        Optional<ProjectType> psType = ps.findProjectType();
        if (psType.map(pst -> pst != ProjectType.UNIMPORTED).orElse(false) ) {
            switch (psType.get()) {
                case DIRECT_IMPORT ->
                        throw new ProjectTypeException("Project already contains data from direct API import. Additional MS run data (.mzml, .mzxml) cannot be added to this project. Please create a new project to import your data.", ProjectType.ALIGNED_RUNS, ProjectType.DIRECT_IMPORT);
                case PEAKLISTS ->
                        throw new ProjectTypeException("Project already contains peak-list data (e.g .ms, .mgf, .mat). Additional MS run data (.mzml, .mzxml) cannot be added to peak-list based projects. Please create a new project to import your data.", ProjectType.ALIGNED_RUNS, ProjectType.PEAKLISTS);
                case UNALIGNED_RUNS -> {
                    if (alignRuns) throw new ProjectStateException("Project already contains preprocessed features from aligning MS runs. Additional data cannot be added. Please create a new project to import your data.");
                }
                default ->
                        throw new ProjectStateException("Project already contains preprocessed features. It is currently not supported to add additional data after preprocessing has been performed. Please create a new project to import your data.");
            }
        } else {
            ps.upsertProjectType(alignRuns && inputFiles.size() > 1 ? ProjectType.ALIGNED_RUNS : ProjectType.UNALIGNED_RUNS);
        }
    }

    @Override
    protected NoSQLProjectSpaceManager compute() throws Exception {
        importedFeatureIds = new LongLinkedOpenHashSet();
        importedCompoundIds = new LongLinkedOpenHashSet();

        NoSQLProjectSpaceManager space = projectSupplier.get();
        SiriusProjectDatabaseImpl<? extends Database<?>> ps = space.getProject();

        progress = 0;
        if (alignRuns) {
            totalProgress = inputFiles.size() + 5L;
            compute(ps, inputFiles);
        } else {
            // TODO parallelize
            totalProgress = inputFiles.size() * 5L + 1;
            int atmost = Integer.MAX_VALUE;
            for (Path f : inputFiles) {
                if (--atmost < 0) break;
                compute(ps, List.of(f));
            }
        }
        updateProgress(totalProgress, totalProgress, "Done");

        return space;
    }

    private static void groupFeaturesToCompound(Database<?> ps, Compound compound, final LongList importedCompoundIds) throws IOException {
        ps.insert(compound);
        if (importedCompoundIds != null)
            importedCompoundIds.add(compound.getCompoundId());

        if (compound.getCorrelatedIonPairs().isPresent()) {
            for (CorrelatedIonPair pair : compound.getCorrelatedIonPairs().get()) {
                ps.insert(pair);
            }
        }

        List<AlignedFeatures> adducts = compound.getAdductFeatures().orElseGet(List::of);
        for (AlignedFeatures f : adducts) {
            if (f.getCompoundId() == null || f.getCompoundId() != compound.getCompoundId())
                f.setCompoundId(compound.getCompoundId());
            ps.upsert(f);
        }
        final SimpleMutableSpectrum ms1Spectra = new SimpleMutableSpectrum();
        List<MSData> msDataList = new ArrayList<>();
        for (AlignedFeatures adduct : adducts) {
            ps.getByPrimaryKey(adduct.getAlignedFeatureId(), MSData.class).ifPresent(m -> {
                msDataList.add(m);
                if (m.getIsotopePattern() != null) {
                    SimpleSpectrum b = m.getIsotopePattern();
                    for (int i = 0; i < b.size(); ++i) {
                        ms1Spectra.addPeak(b.getMzAt(i), b.getIntensityAt(i) * adduct.getApexIntensity());
                    }
                }
            });
        }
        SimpleSpectrum ms1 = new SimpleSpectrum(ms1Spectra);
        for (MSData m : msDataList) {
            m.setMergedMs1Spectrum(ms1);
            ps.upsert(m);
        }
    }
}


