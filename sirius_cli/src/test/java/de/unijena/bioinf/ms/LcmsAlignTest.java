/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2023 Bright Giant GmbH
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.
 *  If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms;


import de.unijena.bioinf.ChemistryBase.utils.RangeUtils;
import org.apache.commons.lang3.Range;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.ms.frontend.subtools.lcms_align.DataSmoothing;
import de.unijena.bioinf.ms.frontend.subtools.lcms_align.LcmsAlignOptions;
import de.unijena.bioinf.ms.frontend.subtools.lcms_align.LcmsAlignSubToolJobNoSql;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.core.trace.MergedTrace;
import de.unijena.bioinf.ms.persistence.model.core.trace.TraceRef;
import de.unijena.bioinf.ms.persistence.storage.SiriusProjectDocumentDatabase;
import de.unijena.bioinf.ms.persistence.storage.nitrite.NitriteSirirusProject;
import de.unijena.bioinf.projectspace.NoSQLProjectSpaceManager;
import de.unijena.bioinf.utils.SiriusTestDataManager;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.floats.FloatList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import org.dizitart.no2.mvstore.MVSpatialKey;
import org.h2.mvstore.MVStore;
import org.h2.mvstore.rtree.MVRTreeMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

class LcmsAlignTest {

    private static class Feature {
        private final double rtstart;
        private final double rtend;
        private final double rtApex;
        private final boolean adduct;

        public Feature(double rtstart, double rtend, double rtApex, boolean adduct) {
            this.rtstart = rtstart;
            this.rtend = rtend;
            this.rtApex = rtApex;
            this.adduct = adduct;
        }

    }

    private static final SiriusTestDataManager DATA_MANAGER;
    private static final List<String> DATASETS;

    private static final DoubleList GAUSS_SIGMA = DoubleList.of(0.5, 1.0);
    private static final IntList WAVELET_SCALE = IntList.of(2, 4);

    static {
        try {
            DATA_MANAGER = new SiriusTestDataManager();
            DATASETS = DATA_MANAGER.getDataSets();
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    static Stream<Arguments> provideGaussArgs() {
        if (DATASETS.isEmpty())
            return Stream.of(Arguments.arguments(null, 0d));
        return DATASETS.stream()
                .flatMap(ds -> GAUSS_SIGMA.doubleStream().mapToObj(sigma -> new Object[]{ds, sigma}))
                .map(Arguments::arguments);
    }

    static Stream<Arguments> provideWaveletArgs() {
        if (DATASETS.isEmpty())
            return Stream.of(Arguments.arguments(null, 0));
        return DATASETS.stream()
                .flatMap(ds -> WAVELET_SCALE.intStream().mapToObj(scale -> new Object[]{ds, scale}))
                .map(Arguments::arguments);
    }

    static Stream<Arguments> provideSGArgs() {
        if (DATASETS.isEmpty())
            return Stream.of(Arguments.arguments(""));
        return DATASETS.stream().map(Arguments::arguments);
    }

    private static final Deviation MZ_DEV = new Deviation(10);

    private static final LcmsAlignOptions DEFAULT_OPTIONS = new LcmsAlignOptions();

    static {
        DEFAULT_OPTIONS.noAlign = false;
        DEFAULT_OPTIONS.forbidMs1Only = false;
        DEFAULT_OPTIONS.smoothing = DataSmoothing.NOFILTER;
        DEFAULT_OPTIONS.noiseCoefficient = 2.0;
        DEFAULT_OPTIONS.persistenceCoefficient = 0.1;
        DEFAULT_OPTIONS.mergeCoefficient = 0.8;
    }

    private static final Map<String, Map<Feature, MVSpatialKey>> NOFILTER_FEATURES = new HashMap<>();

    private static final Object2IntMap<String> SIRIUS_5_FEATURES = new Object2IntArrayMap<>(
            new String[]{"tomato", "polluted_citrus"}, new int[]{3151, 2636}
    );
    private static final Object2IntMap<String> SIRIUS_5_ADDUCTS = new Object2IntArrayMap<>(
            new String[]{"tomato", "polluted_citrus"}, new int[]{1642, 678}
    );

    @BeforeAll
    public static void setUp() throws IOException, ExecutionException {
        for (String dataset : DATASETS) {
            Path location = FileUtils.createTmpProjectSpaceLocation(SiriusProjectDocumentDatabase.SIRIUS_PROJECT_SUFFIX);
            try (NitriteSirirusProject project = new NitriteSirirusProject(location)) {
                NOFILTER_FEATURES.put(dataset, runPreprocessing(project, DEFAULT_OPTIONS, dataset));
            }
        }
    }

    @ParameterizedTest
    @MethodSource("provideGaussArgs")
    void testGaussian(String dataset, double sigma) throws IOException, ExecutionException {
        if (dataset == null || dataset.isEmpty() || dataset.isBlank())
            return;

        final LcmsAlignOptions options = new LcmsAlignOptions();
        options.noAlign = false;
        options.forbidMs1Only = false;
        options.smoothing = DataSmoothing.GAUSSIAN;
        options.sigma = sigma;
        options.noiseCoefficient = 2.0;
        options.persistenceCoefficient = 0.1;
        options.mergeCoefficient = 0.8;

        Path location = FileUtils.createTmpProjectSpaceLocation(SiriusProjectDocumentDatabase.SIRIUS_PROJECT_SUFFIX);
        try (NitriteSirirusProject project = new NitriteSirirusProject(location)) {
            compareFeatures(
                    NOFILTER_FEATURES.get(dataset),
                    runPreprocessing(project, options, dataset),
                    dataset
            );
        }
    }

    @ParameterizedTest
    @MethodSource("provideWaveletArgs")
    void testWavelet(String dataset, int scale) throws IOException, ExecutionException {
        if (dataset == null || dataset.isEmpty() || dataset.isBlank())
            return;

        final LcmsAlignOptions options = new LcmsAlignOptions();
        options.noAlign = false;
        options.forbidMs1Only = false;
        options.smoothing = DataSmoothing.WAVELET;
        options.scaleLevel = scale;
        options.noiseCoefficient = 2.0;
        options.persistenceCoefficient = 0.1;
        options.mergeCoefficient = 0.8;

        Path location = FileUtils.createTmpProjectSpaceLocation(SiriusProjectDocumentDatabase.SIRIUS_PROJECT_SUFFIX);
        try (NitriteSirirusProject project = new NitriteSirirusProject(location)) {
            compareFeatures(
                    NOFILTER_FEATURES.get(dataset),
                    runPreprocessing(project, options, dataset),
                    dataset
            );
        }
    }

    @ParameterizedTest
    @MethodSource("provideSGArgs")
    void testSavitzkyGolay(String dataset) throws IOException, ExecutionException {
        if (dataset == null || dataset.isEmpty() || dataset.isBlank())
            return;

        final LcmsAlignOptions options = new LcmsAlignOptions();
        options.noAlign = false;
        options.forbidMs1Only = false;
        options.smoothing = DataSmoothing.SAVITZKY_GOLAY;
        options.noiseCoefficient = 2.0;
        options.persistenceCoefficient = 0.1;
        options.mergeCoefficient = 0.8;

        Path location = FileUtils.createTmpProjectSpaceLocation(SiriusProjectDocumentDatabase.SIRIUS_PROJECT_SUFFIX);
        try (NitriteSirirusProject project = new NitriteSirirusProject(location)) {
            compareFeatures(
                    NOFILTER_FEATURES.get(dataset),
                    runPreprocessing(project, options, dataset),
                    dataset
            );
        }
    }

    private static Map<Feature, MVSpatialKey> runPreprocessing(NitriteSirirusProject project, LcmsAlignOptions options, String dataset) throws IOException, ExecutionException {
        Map<Feature, MVSpatialKey> featureMap = new HashMap<>();
        List<Path> inputFiles = DATA_MANAGER.getPaths(dataset);
        int counter = 0;

        SiriusJobs.getGlobalJobManager().submitJob(new LcmsAlignSubToolJobNoSql(inputFiles, () -> new NoSQLProjectSpaceManager(project), options, new HashSet<>())).awaitResult();

        for (AlignedFeatures f : project.getStorage().findAll(AlignedFeatures.class)) {
            float mzd = (float) MZ_DEV.absoluteFor(f.getAverageMass());

            TraceRef traceRef = f.getTraceRef();
            Optional<MergedTrace> optTrace = project.getStorage().getByPrimaryKey(f.getTraceRef().getTraceId(), MergedTrace.class);
            Assertions.assertTrue(optTrace.isPresent());
            MergedTrace trace = optTrace.get();
            FloatList sublist = trace.getIntensities().subList(traceRef.getStart(), traceRef.getEnd() + 1);
            double max = 0;
            int maxIndex = 0;
            for (int i = 0; i < sublist.size(); i++) {
                if (sublist.getFloat(i) > max) {
                    max = sublist.getFloat(i);
                    maxIndex = i;
                }
            }

            float start = (float) f.getRetentionTime().getStartTime();
            float end = (float) f.getRetentionTime().getEndTime();
            float rtApex = (((float) maxIndex / (float) sublist.size()) * (end - start)) + start;

            featureMap.put(
                    new Feature(f.getRetentionTime().getStartTime(), f.getRetentionTime().getEndTime(), rtApex, f.hasDetectedAdducts()),
                    new MVSpatialKey(counter++, (float) f.getAverageMass() - mzd, (float) f.getAverageMass() + mzd, start, end));
        }
        return featureMap;
    }

    private static void compareFeatures(
            Map<Feature, MVSpatialKey> noFilterFeatures,
            Map<Feature, MVSpatialKey> features,
            String dataset
    ) {

        // count changed features
        int lostFeatures = 0;
        int totalFeatures = noFilterFeatures.size();
        int lostAdducts = 0;
        int totalAdducts = 0;

        try (MVStore store = MVStore.open(null)) {
            MVRTreeMap<Feature> tree = store.openMap("features", new MVRTreeMap.Builder<>());
            features.forEach((f, key) -> tree.add(key, f));

            for (Map.Entry<Feature, MVSpatialKey> entry : noFilterFeatures.entrySet()) {
                Feature feature = entry.getKey();
                List<Feature> matches = StreamSupport
                        .stream(Spliterators.spliteratorUnknownSize(tree.findIntersectingKeys(entry.getValue()), Spliterator.ORDERED), false)
                        .map(tree::get).toList();

                boolean apexCovered = matches.stream().anyMatch(f -> f.rtstart <= feature.rtApex && feature.rtApex <= f.rtend);

                Range<Double> rtNoFilter = Range.of(feature.rtstart, feature.rtend);
                Deque<Range<Double>> intersections = new ArrayDeque<>(
                        matches.stream()
                                .map(f -> Range.of(f.rtstart, f.rtend).intersectionWith(rtNoFilter))
                                .sorted(Comparator.comparingDouble(Range::getMinimum))
                                .toList());
                double coverage = 0;
                if (!intersections.isEmpty()) {
                    double covered = 0;
                    Range<Double> current = intersections.pollFirst();
                    while (!intersections.isEmpty()) {
                        if (intersections.peekFirst().isOverlappedBy(current)) {
                            current = RangeUtils.span(current, intersections.pollFirst());
                        } else {
                            covered += current.getMaximum() - current.getMinimum();
                            current = intersections.pollFirst();
                        }
                    }
                    covered += current.getMaximum() - current.getMinimum();
                    if (covered > 0) {
                        coverage = (feature.rtend - feature.rtstart) / covered;
                    }
                }

                if (!apexCovered || coverage < 0.666) {
                    lostFeatures++;
                    if (feature.adduct) {
                        lostAdducts++;
                    }
                } else {
                    if (feature.adduct && matches.stream().noneMatch(f -> f.adduct)) {
                        lostAdducts++;
                    }
                }
                if (feature.adduct) {
                    totalAdducts++;
                }

            }

            if (SIRIUS_5_FEATURES.containsKey(dataset)) {
                Assertions.assertTrue((double) totalFeatures / (double) SIRIUS_5_FEATURES.getInt(dataset) > 1.5);
            }
            if (SIRIUS_5_ADDUCTS.containsKey(dataset)) {
                Assertions.assertTrue((double) totalAdducts / (double) SIRIUS_5_ADDUCTS.getInt(dataset) > 0.33);
            }

            Assertions.assertTrue(totalAdducts > 0);
            Assertions.assertTrue(totalFeatures > 0);
            Assertions.assertTrue((double) lostAdducts / (double) totalAdducts < 0.06);
            Assertions.assertTrue((double) lostFeatures / (double) totalFeatures < 0.06);

        }

    }

}
