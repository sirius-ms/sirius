package de.unijena.bioinf.ms.utils;

import com.google.common.collect.Iterables;
import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.babelms.MsExperimentParser;
import de.unijena.bioinf.babelms.ms.JenaMsWriter;
import de.unijena.bioinf.sirius.IdentificationResult;
import de.unijena.bioinf.sirius.IsotopePatternHandling;
import de.unijena.bioinf.sirius.Sirius;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TestUtils {

    public static void main(String... args) throws IOException {
        testMs2CompoundMerger2(args);
//        testGuessIon(args);
    }

    public static void testGuessIon(String... args) throws IOException {
        Path path1 = Paths.get("/home/ge28quv/Data/gibbsSample/with_openMS/dendroides/1.ms");
        List<Ms2Experiment> experiments = (new MsExperimentParser()).getParser(path1.toFile()).parseFromFile(path1.toFile());
        Sirius sirius = new Sirius();
        for (Ms2Experiment exp : experiments) {
            PrecursorIonType[] ionTypes = Iterables.toArray(PeriodicTable.getInstance().getKnownLikelyPrecursorIonizations(exp.getPrecursorIonType().getCharge()), PrecursorIonType.class);
            List<Ionization> ionizations = new ArrayList<>();
            for (PrecursorIonType ionType : ionTypes) {
                ionizations.add(ionType.getIonization());
            }
            PossibleIonModes im = exp.getAnnotation(PossibleIonModes.class, new PossibleIonModes());
            im.setGuessFromMs1(PossibleIonModes.GuessingMode.SELECT);

            im.enableGuessFromMs1WithCommonIonModes(exp.getPrecursorIonType().getCharge());
            final Set<Ionization> ionModes = new HashSet<>(ionizations);
            for (Ionization ion : ionModes) {
                im.add(ion, 0.02);
            }
            if (exp.getPrecursorIonType().getCharge() > 0) {
                im.add(PrecursorIonType.getPrecursorIonType("[M+H]+").getIonization(), 1d);
            } else {
                im.add(PrecursorIonType.getPrecursorIonType("[M-H]-").getIonization(), 1d);

            }
            exp.setAnnotation(PossibleIonModes.class, im);

            ProcessedInput input = new ProcessedInput(new MutableMs2Experiment(exp), exp, sirius.getMs2Analyzer().getProfile(exp));
            sirius.detectPossibleIonModesFromMs1(input);

            if (exp.getIonMass()>400) continue;

            FormulaConstraints constraints = new FormulaConstraints("CHNOPS");
            List<IdentificationResult> results = sirius.identify(exp, 20, 10 , true, IsotopePatternHandling.both, constraints);
            System.out.println("..................");
            for (IdentificationResult result : results) {
                System.out.println(result.getPrecursorIonType()+" "+result.getMolecularFormula());
            }
            System.out.println("......");

            //            System.out.println(exp.getAnnotation(PossibleIonModes.class));
        }
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
        String glob = "glob:**/sirius_chemicalNoiseAndChimericsRemoved_140221*ME*.ms";
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
        Deviation maxMzDeviation = new Deviation(15);//20);
        Deviation deviation = new Deviation(15);
        double maxRTShift = 100;//20;
        double cosineSimilarity = 0.3;
        Ms2CompoundMerger ms2CompoundMerger = new Ms2CompoundMerger(maxMzDeviation, maxRTShift, cosineSimilarity, false);

        //todo diff deviation, maxMzDeviation;
        System.out.println("before: "+(before));
//        List<Ms2Experiment> mergedExperiments = ms2CompoundMerger.mergeRuns(deviation, experiments, experiments2, experiments3);
        List<Ms2Experiment> mergedExperiments = ms2CompoundMerger.mergeRuns(deviation, experiments);
        System.out.println("after: "+mergedExperiments.size());

        String output = "/home/ge28quv/Data/gibbsSample/with_openMS/dendroides/out.ms";
        JenaMsWriter spectrumWriter = new JenaMsWriter();

        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(output))) {
            for (Ms2Experiment experiment : mergedExperiments) {
                spectrumWriter.write(writer, experiment);
            }
        }

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
        MzRTPeak[] blankFeatures = ChemicalNoiseRemoval.readFeatureTable(reader);
        reader.close();
        ChemicalNoiseRemoval chemicalNoiseRemoval = new ChemicalNoiseRemoval(blankFeatures, maxMzDeviation, maxRTShift, minFoldChange);

        //todo diff deviation, maxMzDeviation;
        System.out.println("before: "+(experiments.size()));
//        List<Ms2Experiment> mergedExperiments = ms2CompoundMerger.mergeRuns(deviation, experiments, experiments2, experiments3);
        List<Ms2Experiment> withoutBlank = chemicalNoiseRemoval.removeNoiseFeatures(experiments);
        System.out.println("after: "+withoutBlank.size());

    }
}
