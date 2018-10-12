package de.unijena.bioinf.ms.utils;

import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.MzRTPeak;
import de.unijena.bioinf.babelms.MsExperimentParser;
import de.unijena.bioinf.babelms.ms.JenaMsWriter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

public class TestUtils {

    public static void main(String... args) throws IOException {
        testMs2CompoundMerger(args);
    }

    //Ms2CompoundMerger
    public static void testMs2CompoundMerger(String... args) throws IOException {
//        Path path = Paths.get("/home/ge28quv/Data/nobackup/test_ms2compoundmerger/part.mgf");
//        GenericParser<Ms2Experiment> parser = (new MsExperimentParser()).getParser(path.toFile());
//        List<Ms2Experiment> experiments = parser.parseFromFile(path.toFile());
//        List<Ms2Experiment> experiments = parser.parseFromFile(path.toFile());
//        List<Ms2Experiment> experiments2 = parser.parseFromFile(path.toFile());
//        List<Ms2Experiment> experiments3 = parser.parseFromFile(path.toFile());

//        Collections.shuffle(experiments);
//        experiments = experiments.subList(0,200);
//
//        Collections.shuffle(experiments2);
//        experiments2 = experiments2.subList(0,200);
//
//        Collections.shuffle(experiments3);
//        experiments3 = experiments3.subList(0,200);

        Path path1 = Paths.get("/home/ge28quv/Data/gibbsSample/with_openMS/dendroides/1.ms");
        Path path2 = Paths.get("/home/ge28quv/Data/gibbsSample/with_openMS/dendroides/2.ms");
        Path path3 = Paths.get("/home/ge28quv/Data/gibbsSample/with_openMS/dendroides/3.ms");
        List<Ms2Experiment> experiments = (new MsExperimentParser()).getParser(path1.toFile()).parseFromFile(path1.toFile());
        List<Ms2Experiment> experiments2 = (new MsExperimentParser()).getParser(path2.toFile()).parseFromFile(path2.toFile());
        List<Ms2Experiment> experiments3 = (new MsExperimentParser()).getParser(path3.toFile()).parseFromFile(path3.toFile());

        Deviation maxMzDeviation = new Deviation(20);
        Deviation deviation = new Deviation(10);
        double maxRTShift = 20;
        double cosineSimilarity = 0.8;
        Ms2CompoundMerger ms2CompoundMerger = new Ms2CompoundMerger(maxMzDeviation, maxRTShift, cosineSimilarity, false);

        //todo diff deviation, maxMzDeviation;
        System.out.println("before: "+(experiments.size()+experiments2.size()));
//        List<Ms2Experiment> mergedExperiments = ms2CompoundMerger.mergeRuns(deviation, experiments, experiments2, experiments3);
        List<Ms2Experiment> mergedExperiments = ms2CompoundMerger.mergeRuns(deviation, experiments, experiments2, experiments3);
        System.out.println("after: "+mergedExperiments.size());


        String output = "/home/ge28quv/Data/gibbsSample/with_openMS/dendroides/out.ms";
        JenaMsWriter spectrumWriter = new JenaMsWriter();

        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(output))) {
            for (Ms2Experiment experiment : mergedExperiments) {
                spectrumWriter.write(writer, experiment);
            }
        }

    }


    public static void testMs2CompoundMerger2(String... args) throws IOException {
        String loc = "/home/ge28quv/Data/gibbsSample/with_openMS/dendroides/processing/";
        String glob = "glob:**/*ME*.ms";
        List<Path> paths = match(glob, loc);
        System.out.println(paths.size());
        List<Ms2Experiment>[] experiments = new List[paths.size()];
        int i = 0;
        int before = 0;
        for (Path path : paths) {
            List<Ms2Experiment> exp = (new MsExperimentParser()).getParser(path.toFile()).parseFromFile(path.toFile());
            experiments[i] = exp;
            before += exp.size();
            ++i;
        }
        Deviation maxMzDeviation = new Deviation(20);
        Deviation deviation = new Deviation(10);
        double maxRTShift = 20;
        double cosineSimilarity = 0.8;
        Ms2CompoundMerger ms2CompoundMerger = new Ms2CompoundMerger(maxMzDeviation, maxRTShift, cosineSimilarity, false);

        //todo diff deviation, maxMzDeviation;
        System.out.println("before: "+(before));
//        List<Ms2Experiment> mergedExperiments = ms2CompoundMerger.mergeRuns(deviation, experiments, experiments2, experiments3);
        List<Ms2Experiment> mergedExperiments = ms2CompoundMerger.mergeRuns(deviation, experiments);
        System.out.println("after: "+mergedExperiments.size());

    }

    public static List<Path> match(String glob, String location) throws IOException {

        final PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher(
                glob);

        List<Path> paths = new ArrayList<>();
        Files.walkFileTree(Paths.get(location), new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult visitFile(Path path,
                                             BasicFileAttributes attrs) throws IOException {
                if (pathMatcher.matches(path)) {
                    paths.add(path);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc)
                    throws IOException {
                return FileVisitResult.CONTINUE;
            }
        });
        return paths;
    }







    ///////////////////////////////
    /////CompoundFilterUtil
    /////////////////////////////

    public static void testCompoundFilterUtil(String... args) throws IOException {
//        Path path = Paths.get("/home/ge28quv/Data/nobackup/test_ms2compoundmerger/part.mgf");
//        GenericParser<Ms2Experiment> parser = (new MsExperimentParser()).getParser(path.toFile());
//        List<Ms2Experiment> experiments = parser.parseFromFile(path.toFile());
//        List<Ms2Experiment> experiments = parser.parseFromFile(path.toFile());
//        List<Ms2Experiment> experiments2 = parser.parseFromFile(path.toFile());
//        List<Ms2Experiment> experiments3 = parser.parseFromFile(path.toFile());

//        Collections.shuffle(experiments);
//        experiments = experiments.subList(0,200);
//
//        Collections.shuffle(experiments2);
//        experiments2 = experiments2.subList(0,200);
//
//        Collections.shuffle(experiments3);
//        experiments3 = experiments3.subList(0,200);

//        Path path1 = Paths.get("/home/ge28quv/Data/gibbsSample/with_openMS/dendroides/processing/sirius_140221_ME_14_10.ms");
//        Path path1 = Paths.get("/home/ge28quv/Data/gibbsSample/with_openMS/dendroides/1.ms");
        Path path1 = Paths.get("/home/ge28quv/Data/gibbsSample/with_openMS/dendroides/processing/sirius_merged.ms");
//        Path path1 = Paths.get("/home/ge28quv/Data/gibbsSample/with_openMS/dendroides/processing/sirius_140221_Blanc8.ms");
        List<Ms2Experiment> experiments = (new MsExperimentParser()).getParser(path1.toFile()).parseFromFile(path1.toFile());

        int minNumIso = 2;
        Deviation findPrecursorInMs1Deviation = new Deviation(20);
        Deviation isoDifferenceDeviation = new Deviation(10);
        CompoundFilterUtil compoundFilterUtil = new CompoundFilterUtil();

        //todo diff deviation, maxMzDeviation;
        System.out.println("before: "+(experiments.size()));
//        List<Ms2Experiment> mergedExperiments = ms2CompoundMerger.mergeRuns(deviation, experiments, experiments2, experiments3);
        List<Ms2Experiment> without = compoundFilterUtil.filterByNumberOfIsotopePeaks(experiments, minNumIso, findPrecursorInMs1Deviation, isoDifferenceDeviation);
//        List<Ms2Experiment> without = compoundFilterUtil.filterZeroIntensityFeatures(experiments, findPrecursorInMs1Deviation);
        System.out.println("after: "+without.size());

        String output = "/home/ge28quv/Data/gibbsSample/with_openMS/dendroides/out.ms";
        JenaMsWriter spectrumWriter = new JenaMsWriter();

        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(output))) {
            for (Ms2Experiment experiment : without) {
                spectrumWriter.write(writer, experiment);
            }
        }

    }




    ////////////////////
    //test BlnankRemoval

    public static void testBlankRemoval(String... args) throws IOException {
//        Path path = Paths.get("/home/ge28quv/Data/nobackup/test_ms2compoundmerger/part.mgf");
//        GenericParser<Ms2Experiment> parser = (new MsExperimentParser()).getParser(path.toFile());
//        List<Ms2Experiment> experiments = parser.parseFromFile(path.toFile());
//        List<Ms2Experiment> experiments = parser.parseFromFile(path.toFile());
//        List<Ms2Experiment> experiments2 = parser.parseFromFile(path.toFile());
//        List<Ms2Experiment> experiments3 = parser.parseFromFile(path.toFile());

//        Collections.shuffle(experiments);
//        experiments = experiments.subList(0,200);
//
//        Collections.shuffle(experiments2);
//        experiments2 = experiments2.subList(0,200);
//
//        Collections.shuffle(experiments3);
//        experiments3 = experiments3.subList(0,200);

        Path path1 = Paths.get("/home/ge28quv/Data/gibbsSample/with_openMS/dendroides/processing/sirius_140221_ME_14_10.ms");
//        Path path1 = Paths.get("/home/ge28quv/Data/gibbsSample/with_openMS/dendroides/processing/sirius_140221_Blanc8.ms");
        List<Ms2Experiment> experiments = (new MsExperimentParser()).getParser(path1.toFile()).parseFromFile(path1.toFile());

        Deviation maxMzDeviation = new Deviation(20);
        double maxRTShift = 20;
        double minFoldChange = 2d;
        BufferedReader reader = Files.newBufferedReader(Paths.get("/home/ge28quv/Data/gibbsSample/with_openMS/dendroides/processing/blank_features.csv"));
        MzRTPeak[] blankFeatures = BlankRemoval.readFeatureTable(reader);
        reader.close();
        BlankRemoval blankRemoval = new BlankRemoval(blankFeatures, maxMzDeviation, maxRTShift, minFoldChange);

        //todo diff deviation, maxMzDeviation;
        System.out.println("before: "+(experiments.size()));
//        List<Ms2Experiment> mergedExperiments = ms2CompoundMerger.mergeRuns(deviation, experiments, experiments2, experiments3);
        List<Ms2Experiment> withoutBlank = blankRemoval.removeBlanks(experiments);
        System.out.println("after: "+withoutBlank.size());

    }
}
