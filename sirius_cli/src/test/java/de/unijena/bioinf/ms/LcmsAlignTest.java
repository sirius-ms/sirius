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


import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.utils.DataQuality;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.lcms.trace.filter.Filter;
import de.unijena.bioinf.lcms.trace.filter.GaussFilter;
import de.unijena.bioinf.lcms.trace.filter.SavitzkyGolayFilter;
import de.unijena.bioinf.lcms.trace.filter.WaveletFilter;
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
import org.dizitart.no2.mvstore.MVSpatialKey;
import org.h2.mvstore.MVStore;
import org.h2.mvstore.rtree.MVRTreeMap;
import org.h2.mvstore.rtree.Spatial;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

class LcmsAlignTest {

    // TODO SIRIUS 5 test

    private static class Feature {
        private final long featureId;
        private final double mz;
        private final double rtstart;
        private final double rtend;
        private final double rtApex;
        private final DataQuality quality;
        private final double intsum;

        public Feature(long featureId, double mz, double rtstart, double rtend, double rtApex, DataQuality quality, double intsum) {
            this.featureId = featureId;
            this.mz = mz;
            this.rtstart = rtstart;
            this.rtend = rtend;
            this.rtApex = rtApex;
            this.quality = quality;
            this.intsum = intsum;
        }

    }

    private static final SiriusTestDataManager DATA_MANAGER;
    private static final List<String> DATASETS;

    private static final DoubleList GAUSS_SIGMA = DoubleList.of(0.5, 1.0, 3.0);
    private static final IntList WAVELET_SCALE = IntList.of(4, 8, 16);

//    private static final DoubleList GAUSS_SIGMA = DoubleList.of(0.5);
//    private static final IntList WAVELET_SCALE = IntList.of(8);

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
            return Stream.of(Arguments.arguments(null, SavitzkyGolayFilter.SGF.AUTO));
        return DATASETS.stream()
                .map(ds -> new Object[]{ds, SavitzkyGolayFilter.SGF.AUTO})
//                .flatMap(ds -> Arrays.stream(SavitzkyGolayFilter.SGF.values()).map(sgf -> new Object[]{ds, sgf}))
                .map(Arguments::arguments);
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

    private static MVStore STORE;
    private static final Map<String, MVRTreeMap<Feature>> DEFAULT_RTREES = new HashMap<>();
    private static final Map<String, NitriteSirirusProject> DEFAULT_PROJECTS = new HashMap<>();

    private static final Path CSV_PATH = Path.of("/home/mel/workspace/lcms-compare/filter/values");

    @BeforeAll
    public static void setUp() throws IOException, ExecutionException {
        STORE = MVStore.open(null);
        DEFAULT_RTREES.clear();
        for (String dataset : DATASETS) {
            Path location = FileUtils.createTmpProjectSpaceLocation(SiriusProjectDocumentDatabase.SIRIUS_PROJECT_SUFFIX);
            NitriteSirirusProject project = new NitriteSirirusProject(location);
            MVRTreeMap<Feature> tree = STORE.openMap("default_" + dataset, new MVRTreeMap.Builder<>());
            runPreprocessing(project, DEFAULT_OPTIONS, dataset).forEach((f, key) -> {
                tree.add(key, f);
            });
            DEFAULT_RTREES.put(dataset, tree);
            DEFAULT_PROJECTS.put(dataset, project);
        }
        if (!Files.exists(CSV_PATH))
            Files.createDirectory(CSV_PATH);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(CSV_PATH.resolve("filter_results.csv").toFile()))) {
            writer.write("filter,dataset,parameter,match,merge,split,lost,new,match_ints,merge_ints,split_ints,lost_ints,new_ints\n");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void tearDown() {
        STORE.close();
        for (NitriteSirirusProject project : DEFAULT_PROJECTS.values()) {
            try {
                project.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @ParameterizedTest
    @MethodSource("provideGaussArgs")
    void testGaussian(String dataset, double sigma) throws IOException, ExecutionException {
        if (dataset == null)
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
                    DEFAULT_PROJECTS.get(dataset), project,
                    runPreprocessing(project, options, dataset),
                    new GaussFilter(sigma),
                    dataset, "gaussian", Double.toString(sigma)
            );
        }
    }

    @ParameterizedTest
    @MethodSource("provideWaveletArgs")
    void testWavelet(String dataset, int scale) throws IOException, ExecutionException {
        if (dataset == null)
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
                    DEFAULT_PROJECTS.get(dataset), project,
                    runPreprocessing(project, options, dataset),
                    new WaveletFilter(scale),
                    dataset, "wavelet", Integer.toString(scale)
            );
        }
    }

    @ParameterizedTest
    @MethodSource("provideSGArgs")
    void testSavitzkyGolay(String dataset, SavitzkyGolayFilter.SGF sgf) throws IOException, ExecutionException {
        if (dataset == null)
            return;

        final LcmsAlignOptions options = new LcmsAlignOptions();
        options.noAlign = false;
        options.forbidMs1Only = false;
        options.smoothing = DataSmoothing.SAVITZKY_GOLAY;
        options.savitzkyGolayType = sgf;
        options.noiseCoefficient = 2.0;
        options.persistenceCoefficient = 0.1;
        options.mergeCoefficient = 0.8;

        Path location = FileUtils.createTmpProjectSpaceLocation(SiriusProjectDocumentDatabase.SIRIUS_PROJECT_SUFFIX);
        try (NitriteSirirusProject project = new NitriteSirirusProject(location)) {
            compareFeatures(
                    DEFAULT_PROJECTS.get(dataset), project,
                    runPreprocessing(project, options, dataset),
                    new SavitzkyGolayFilter(sgf),
                    dataset, "sg", sgf.toString()
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
            double intsum = 0;
            double max = 0;
            int maxIndex = 0;
            for (int i = 0; i < sublist.size(); i++) {
                intsum += sublist.getFloat(i);
                if (sublist.getFloat(i) > max) {
                    max = sublist.getFloat(i);
                    maxIndex = i;
                }
            }


            float start = (float) f.getRetentionTime().getStartTime();
            float end = (float) f.getRetentionTime().getEndTime();
            float rtApex = (((float) maxIndex / (float) sublist.size()) * (end - start)) + start;

            featureMap.put(
                    new Feature(f.getAlignedFeatureId(), f.getAverageMass(), f.getRetentionTime().getStartTime(), f.getRetentionTime().getEndTime(), rtApex, f.getDataQuality(), intsum),
                    new MVSpatialKey(counter++, (float) f.getAverageMass() - mzd, (float) f.getAverageMass() + mzd, start, end));
        }
        return featureMap;
    }

    private static void compareFeatures(
            NitriteSirirusProject oldProject, NitriteSirirusProject project,
            Map<Feature, MVSpatialKey> features,
            Filter filter,
            String dataset, String filterName, String parameter
    ) throws IOException {
        MVRTreeMap<Feature> oldTree = DEFAULT_RTREES.get(dataset);

        // count changed features
        long matchF = 0;
        long mergeF = 0;
        long splitF = 0;
        long lostF = 0;
        long newF = 0;
        // count intensities of changed features
        double matchInts = 0;
        double lostInts = 0;
        double mergedInts = 0;
        double splitInts = 0;
        double newInts = 0;
        double totalInts = 0;
        double newTotalInts = 0;
        // count quality changes of matched features
        List<String> qualities = Arrays.stream(DataQuality.values()).map(DataQuality::toString).toList();

        PriorityQueue<Feature> lostQ = new PriorityQueue<>(Comparator.<Feature, Double>comparing(f -> f.intsum).reversed());
        PriorityQueue<Feature[]> splitQ = new PriorityQueue<>(Comparator.<Feature[], Double>comparing(f -> f[0].intsum).reversed());
        PriorityQueue<Feature[]> mergeQ = new PriorityQueue<>(Comparator.<Feature[], Double>comparing(f -> IntStream.range(0, f.length - 1).mapToDouble(i -> f[i].intsum).sum()).reversed());

        try (MVStore store = MVStore.open(null)) {
            MVRTreeMap<Feature> tree = STORE.openMap("features", new MVRTreeMap.Builder<>());
            features.forEach((f, key) -> tree.add(key, f));

            for (Map.Entry<Spatial, Feature> entry : oldTree.entrySet()) {
                Feature feature = entry.getValue();
                List<Feature> candidates0 = StreamSupport.stream(
                        Spliterators.spliteratorUnknownSize(tree.findIntersectingKeys(entry.getKey()), Spliterator.ORDERED),
                        false
                ).map(tree::get).toList();

                List<Feature> candidates = candidates0.stream().filter(c -> feature.rtstart <= c.rtApex && c.rtApex <= feature.rtend).toList();

                if (candidates.isEmpty()) {
                    // still might not mean its lost! -> might be merged! check later
                    lostF++;
                    lostInts += feature.intsum;
                    lostQ.add(feature);
                } else if (candidates.size() == 1) {
                    matchF++;
                    matchInts += feature.intsum;
                } else {
                    splitF++;
                    splitInts += feature.intsum;
                    List<Feature> splits = new ArrayList<>(candidates);
                    splits.addFirst(feature);
                    splitQ.add(splits.toArray(Feature[]::new));
                }

            }

        }


//        MutableGraph<Feature> alignment =  GraphBuilder.undirected().expectedNodeCount(features.size() + tree.size()).build();
//        for (Feature f : tree.values()) {
//            alignment.addNode(f);
//        }

        for (Map.Entry<Feature, MVSpatialKey> entry : features.entrySet()) {
            Feature feature = entry.getKey();
//            alignment.addNode(feature);

            List<Feature> candidates = StreamSupport.stream(
                    Spliterators.spliteratorUnknownSize(oldTree.findIntersectingKeys(entry.getValue()), Spliterator.ORDERED),
                    false
            ).map(oldTree::get).filter(c -> feature.rtstart <= c.rtApex && c.rtApex <= feature.rtend).toList();

            if (candidates.isEmpty()) {
                newF++;
                newInts += feature.intsum;
            } else if (candidates.size() > 1) {
                for (Feature c : candidates) {
                    if (lostQ.contains(c)) {
                        lostQ.remove(c);
                        lostF--;
                        lostInts -= c.intsum;
                    }
                }
                mergeF++;
                mergedInts += candidates.stream().mapToDouble(f -> f.intsum).sum();
                List<Feature> merges = new ArrayList<>(candidates);
                merges.add(feature);
                mergeQ.add(merges.toArray(Feature[]::new));
            }

//            for (Spatial k; it.hasNext();) {
//                k = it.next();
//                Feature candidate = tree.get(k);
//
////                alignment.putEdge(feature, candidate);
////                if (feature.rtstart <= candidate.rtApex && candidate.rtApex <= feature.rtend) {
////                    alignment.putEdge(feature, candidate);
////                } else if (candidate.rtstart <= feature.rtApex && feature.rtApex <= candidate.rtend) {
////                    alignment.putEdge(feature, candidate);
////                }
//            }
        }


//        long[][] qualChange = new long[qualities.size()][qualities.size()];
//        for (Feature f : oldTree.values()) {
//            totalInts += f.intsum;
//            if (alignment.degree(f) == 0) {
//                lostF++;
//                lostInts += f.intsum;
//                lostQ.add(f);
//            } else if (alignment.degree(f) > 1) {
//                splitF++;
//                splitInts += f.intsum;
//                splitQ.add(Stream.concat(
//                        Stream.of(f),
//                        alignment.adjacentNodes(f).stream()
//                ).toArray(Feature[]::new));
//            } else if (alignment.degree(f) == 1) {
//                Feature neighbor = alignment.adjacentNodes(f).iterator().next();
//                if (alignment.degree(neighbor) == 1) {
//                    matchF++;
//                    int qIndexOld = qualities.indexOf(neighbor.quality.toString());
//                    int qIndexNew = qualities.indexOf(f.quality.toString());
//                    if (qIndexOld != qIndexNew) {
//                        qualChange[qIndexOld][qIndexNew]++;
//                    }
//                    matchInts += f.intsum;
//                }
//            }
//        }
//        for (Feature f : features.keySet()) {
//            newTotalInts += f.intsum;
//            if (alignment.degree(f) == 0) {
//                newF++;
//                newInts += f.intsum;
//            } else if (alignment.degree(f) > 1) {
//                List<Feature> flist = new ArrayList<>();
//                for (Feature neighbor : alignment.adjacentNodes(f)) {
//                    if (alignment.degree(neighbor) == 1) {
//                        mergeF++;
//                        mergedInts += neighbor.intsum;
//                        flist.add(neighbor);
//                    }
//                }
//                if (!flist.isEmpty()) {
//                    flist.add(f);
//                    mergeQ.add(flist.toArray(Feature[]::new));
//                }
//            }
//        }

        System.out.printf(
                "%nFEATURE COUNTS:%n OLD: matched: %d lost: %d merged: %d split: %d %n NEW: %d%n",
                matchF, lostF, mergeF, splitF, newF
                );

//        System.out.printf(
//                "%nFEATURE INTENSITIES:%n OLD: matched: %.3f%% lost: %.3f%% merged: %.3f%% split: %.3f%%%n NEW: %.3e%%%n",
//                100 * matchInts / totalInts, 100 * lostInts / totalInts, 100 * mergedInts / totalInts, 100 * splitInts / totalInts, 100 * newInts / newTotalInts
//        );

//        System.out.println("\nQUALITY CHANGES FOR MATCHED FEATURES:");
//        for (int i = 0; i < qualities.size(); i++) {
//            boolean changed = false;
//            StringBuffer buffer = new StringBuffer(" " + qualities.get(i) + " -> ");
//            for (int j = 0; j < qualities.size(); j++) {
//                if (i == j)
//                    continue;
//                if (qualChange[i][j] > 0) {
//                    changed = true;
//                    buffer.append(qualities.get(j)).append(": ").append(qualChange[i][j]).append(" ");
//                }
//            }
//            if (changed) {
//                System.out.println(buffer);
//            }
//        }
//        System.out.println();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(CSV_PATH.resolve("filter_results.csv").toFile(), true))) {
            writer.write(
                    filterName + "," + dataset + "," + parameter + "," + matchF + "," + mergeF + "," + splitF + "," + lostF + "," + newF +
                    matchInts + "," + mergedInts + "," + splitInts + "," + lostInts + "," + newInts + "\n"
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        for (int i = 0; i < 3; i++) {
            Feature lost = lostQ.poll();
            if (lost == null)
                break;

            Optional<AlignedFeatures> optF = oldProject.getStorage().getByPrimaryKey(lost.featureId, AlignedFeatures.class);
            Assertions.assertTrue(optF.isPresent());
            TraceRef traceRef = optF.get().getTraceRef();
            Optional<MergedTrace> optTrace = oldProject.getStorage().getByPrimaryKey(traceRef.getTraceId(), MergedTrace.class);
            Assertions.assertTrue(optTrace.isPresent());
            FloatList ints = optTrace.get().getIntensities();
            double[] filtered = filter.apply(ints.doubleStream().toArray());

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(CSV_PATH.resolve("lost_trace_" + filterName + "_" + dataset + "_" + parameter + "_" + i + ".csv").toFile()))) {
                for (int j = 0; j < ints.size() - 1; j++) {
                    writer.write(ints.getFloat(j) + ",");
                }
                writer.write(ints.getFloat(ints.size() - 1) + "\n");
                for (int j = 0; j < filtered.length - 1; j++) {
                    writer.write(filtered[j] + ",");
                }
                writer.write(filtered[filtered.length - 1] + "\n");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(CSV_PATH.resolve("lost_peaks_" + filterName + "_" + dataset + "_" + parameter + "_" + i + ".csv").toFile()))) {
                writer.write("m/z,start,end,ints\n");
                writer.write( lost.mz + "," + traceRef.getStart() + "," + traceRef.getEnd() + "\n");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

//        for (int i = 0; i < 3; i++) {
//            Feature[] split = splitQ.poll();
//            if (split == null)
//                break;
//
//            Optional<AlignedFeatures> optF = oldProject.getStorage().getByPrimaryKey(split[0].featureId, AlignedFeatures.class);
//            Assertions.assertTrue(optF.isPresent());
//            TraceRef traceRef = optF.get().getTraceRef();
//            Optional<MergedTrace> optTrace = oldProject.getStorage().getByPrimaryKey(traceRef.getTraceId(), MergedTrace.class);
//            Assertions.assertTrue(optTrace.isPresent());
//            FloatList ints = optTrace.get().getIntensities();
//            double[] filtered = filter.apply(ints.doubleStream().toArray());
//
//            try (BufferedWriter writer = new BufferedWriter(new FileWriter(CSV_PATH.resolve("split_trace_" + dataset + "_" + parameter + "_" + i + ".csv").toFile()))) {
//                for (int j = 0; j < ints.size() - 1; j++) {
//                    writer.write(ints.getFloat(j) + ",");
//                }
//                writer.write(ints.getFloat(ints.size() - 1) + "\n");
//                for (int j = 0; j < filtered.length - 1; j++) {
//                    writer.write(filtered[j] + ",");
//                }
//                writer.write(filtered[filtered.length - 1] + "\n");
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
//            int traceLength = traceRef.getEnd() - traceRef.getStart();
//            try (BufferedWriter writer = new BufferedWriter(new FileWriter(CSV_PATH.resolve("split_peaks_" + dataset + "_" + parameter + "_" + i + ".csv").toFile()))) {
//                writer.write("m/z,start,end\n");
//                writer.write(split[0].mz + "," + traceRef.getStart() + "," + traceRef.getEnd() + "\n");
//                for (int j = 1; j < split.length; j++) {
//                    double startNewFeature = ((split[j].rtstart - split[0].rtstart) / (split[0].rtend - split[0].rtstart)) * traceLength + traceRef.getStart();
//                    double endNewFeature = ((split[j].rtend - split[0].rtstart) / (split[0].rtend - split[0].rtstart)) * traceLength + traceRef.getStart();
//                    writer.write(split[j].mz + "," + startNewFeature + "," + endNewFeature + "\n");
//                }
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
//
//        }

        for (int i = 0; i < 3; i++) {
            Feature[] merge = mergeQ.poll();
            if (merge == null)
                break;

            Optional<AlignedFeatures> optF = project.getStorage().getByPrimaryKey(merge[merge.length-1].featureId, AlignedFeatures.class);
            Assertions.assertTrue(optF.isPresent());
            TraceRef traceRef = optF.get().getTraceRef();
            Optional<MergedTrace> optTrace = project.getStorage().getByPrimaryKey(traceRef.getTraceId(), MergedTrace.class);
            Assertions.assertTrue(optTrace.isPresent());
            FloatList ints = optTrace.get().getIntensities();
            double[] filtered = filter.apply(ints.doubleStream().toArray());

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(CSV_PATH.resolve("merge_trace_" + filterName + "_" + dataset + "_" + parameter + "_" + i + ".csv").toFile()))) {
                for (int j = 0; j < ints.size() - 1; j++) {
                    writer.write(ints.getFloat(j) + ",");
                }
                writer.write(ints.getFloat(ints.size() - 1) + "\n");
                for (int j = 0; j < filtered.length - 1; j++) {
                    writer.write(filtered[j] + ",");
                }
                writer.write(filtered[filtered.length - 1] + "\n");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            int traceLength = traceRef.getEnd() - traceRef.getStart();
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(CSV_PATH.resolve("merge_peaks_" + filterName + "_" + dataset + "_" + parameter + "_" + i + ".csv").toFile()))) {
                writer.write("m/z,start,end\n");
                for (int j = 0; j < merge.length - 1; j++) {
                    double startOldFeature = ((merge[j].rtstart - merge[merge.length-1].rtstart) / (merge[merge.length-1].rtend - merge[merge.length-1].rtstart)) * traceLength + traceRef.getStart();
                    double endOldFeature = ((merge[j].rtend - merge[merge.length-1].rtstart) / (merge[merge.length-1].rtend - merge[merge.length-1].rtstart)) * traceLength + traceRef.getStart();
                    writer.write(merge[j].mz + "," + startOldFeature + "," + endOldFeature + "\n");
                }
                writer.write(merge[merge.length - 1].mz + "," + traceRef.getStart() + "," + traceRef.getEnd() + "\n");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }

        // TODO find filter + parameter settings that minimize lost and merge
        // TODO more datasets

    }

}
