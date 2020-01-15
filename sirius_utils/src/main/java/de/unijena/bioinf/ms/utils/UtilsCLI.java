package de.unijena.bioinf.ms.utils;

import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.HelpRequestedException;
import com.lexicalscope.jewel.cli.Option;
import de.unijena.bioinf.ChemistryBase.SimpleRectangularIsolationWindow;
import de.unijena.bioinf.ChemistryBase.chem.*;
import de.unijena.bioinf.ChemistryBase.chem.utils.biotransformation.BioTransformation;
import de.unijena.bioinf.ChemistryBase.exceptions.InvalidInputData;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.inputValidators.Warning;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.inputValidator.MissingValueValidator;
import de.unijena.bioinf.babelms.DataWriter;
import de.unijena.bioinf.babelms.MsExperimentParser;
import de.unijena.bioinf.babelms.mgf.MgfWriter;
import de.unijena.bioinf.babelms.ms.JenaMsWriter;
import de.unijena.bioinf.fingerid.FingeridProjectSpaceFactory;
import de.unijena.bioinf.ms.cli.ProjectSpaceUtils;
import de.unijena.bioinf.sirius.IdentificationResult;
import de.unijena.bioinf.sirius.Ms2DatasetPreprocessor;
import de.unijena.bioinf.sirius.Sirius;
import de.unijena.bioinf.sirius.projectspace.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class UtilsCLI {
    private final Logger Log = LoggerFactory.getLogger(UtilsCLI.class);

    public static void main(String... args) {
        UtilsCLI utilsCLI = new UtilsCLI();
        utilsCLI.parseArgsAndRun(args);
    }

    private static final Pattern LEADING_DASHES = Pattern.compile("-*(.*)");

    protected void parseArgsAndRun(String[] args) {
        if (args.length == 0 ||
                args.length == 1 && (containsIgnoreCase(args, "-h") || containsIgnoreCase(args, "--help"))) {
            System.out.println(CliFactory.createCli(UtilsOptions.class).getHelpMessage());
            System.exit(0);
        }
        Matcher matcher = LEADING_DASHES.matcher(args[0].toLowerCase());
        if (!matcher.matches()) {
            System.out.println("Please, specify your intended command as first parameter.");
            System.exit(0);
        }
        String arg = matcher.group(1);
        String[] rest = Arrays.copyOfRange(args, 1, args.length);
        switch (arg) {
            case "merge":
                mergeCompounds(rest);
                break;
            case "merge-spectra":
                mergeSpectra(rest);
                break;
            case "convert-to-mgf":
                convertToMgf(rest);
                break;
            case "filter":
                filter(rest);
                break;
            case "map":
                map(rest);
                break;
            case "quality-stats":
                qualityStatistics(rest);
                break;
            case "chemical-noise-detection":
                extractChemicalNoise(rest);
                break;
            case "split":
                splitInputData(rest);
                break;
            case "collect":
                collectWorkspaces(rest);
                break;
            case "library-search":
                librarySearch(rest);
                break;
            case "extend-isotope-pattern":
                //only needed for supplementary of the paper
                extendIsotopePattern(rest);
                break;
            default:
                System.out.println("Please, specify your intended command as first parameter.");
                System.exit(0);
        }

    }

    private boolean containsIgnoreCase(String[] array, String string) {
        for (String s : array) {
            if (s.equalsIgnoreCase(string)) return true;
        }
        return false;
    }

    private void mergeCompounds(String... args) {
        MergingOptions options = null;
        try {
            options = CliFactory.createCli(MergingOptions.class).parseArguments(args);
        } catch (HelpRequestedException e) {
            System.out.println(e.getMessage());
            System.out.println(CliFactory.createCli(MergingOptions.class).getHelpMessage());
            System.exit(0);
        }
        if (options.isHelp()) {
            System.out.println(CliFactory.createCli(MergingOptions.class).getHelpMessage());
            System.exit(0);
        }

        List<String> input = options.getInput();
        List<File> inputFiles = input.stream().map(File::new).collect(Collectors.toList());

        //read data
        List<Ms2Experiment>[] allExperiments = readData(inputFiles);

        Deviation maxDeviation = new Deviation(options.getPPMMax());
        Deviation mergePeaksDeviation = new Deviation(options.getPPMMerge());
        double maxRtDifference = options.getRTMax();
        double cosine = options.getMinCosine();
        Ms2CompoundMerger compoundMerger = new Ms2CompoundMerger(maxDeviation, maxRtDifference, cosine, options.isMergeWithin());

        List<Ms2Experiment> mergedExperiments = compoundMerger.mergeRuns(mergePeaksDeviation, allExperiments);

        JenaMsWriter jenaMsWriter = new JenaMsWriter();

        writeToFile(jenaMsWriter, options.getOutput(), mergedExperiments);


    }

    /**
     * merge spectra per compound. After application each compound only contains at most 1 merged MS1, 1 normal MS1 and 1 MS2. Summing intensities
     * @param args
     */
    private void mergeSpectra(String... args) {
        MergingSpectraOptions options = null;
        try {
            options = CliFactory.createCli(MergingSpectraOptions.class).parseArguments(args);
        } catch (HelpRequestedException e) {
            System.out.println(e.getMessage());
            System.out.println(CliFactory.createCli(MergingSpectraOptions.class).getHelpMessage());
            System.exit(0);
        }
        if (options.isHelp()) {
            System.out.println(CliFactory.createCli(MergingSpectraOptions.class).getHelpMessage());
            System.exit(0);
        }

        List<String> input = options.getInput();
        List<File> inputFiles = input.stream().map(File::new).collect(Collectors.toList());

        //read data
        List<Ms2Experiment> experiments = readData(inputFiles)[0];

        Deviation maxMs1Deviation = new Deviation(options.getPPMMaxMs1());
        Deviation maxMs2Deviation = new Deviation(options.getPPMMaxMs2());

        List<Ms2Experiment> newExperiments = new ArrayList<>();
        for (Ms2Experiment experiment : experiments) {
            MutableMs2Experiment mutableMs2Experiment = new MutableMs2Experiment(experiment);
            mutableMs2Experiment.setMs1Spectra(Collections.singletonList(Spectrums.mergeSpectra(maxMs1Deviation, true, true, experiment.getMs1Spectra())));
            mutableMs2Experiment.setMs2Spectra(Collections.singletonList(new MutableMs2Spectrum(Spectrums.mergeSpectra(maxMs2Deviation, true, true, experiment.getMs2Spectra()))));
            newExperiments.add(mutableMs2Experiment);
        }

        JenaMsWriter jenaMsWriter = new JenaMsWriter();

        writeToFile(jenaMsWriter, options.getOutput(), newExperiments);


    }

    private List<Ms2Experiment>[] readData(List<File> inputFiles) {
        List<Ms2Experiment>[] allExperiments = new List[inputFiles.size()];
        int i = 0;
        for (File inputFile : inputFiles) {
            if (inputFile.isDirectory()){
                try {
                    List<File> subfiles = new ArrayList<>();
                    DirectoryStream<Path> directoryStream = Files.newDirectoryStream(inputFile.toPath());
                    directoryStream.forEach(f -> {
                        if (!Files.isDirectory(f)) subfiles.add(f.toFile());});
                    allExperiments[i] = new ArrayList<>();
                    for (File subfile : subfiles) {
                        try {
                            List<Ms2Experiment> experiments = (new MsExperimentParser()).getParser(subfile).parseFromFile(subfile);
                            allExperiments[i].addAll(experiments);
                        } catch (IOException e) {
                            Log.error("Error parsing input file: " + subfile);
                            Log.error(e.getMessage());
                            System.exit(0);
                        }
                    }
                    ++i;
                } catch (IOException e) {
                    Log.error("Error reading files in directory: " + inputFile);
                    System.exit(0);

                }
            } else {
                try {
                    List<Ms2Experiment> experiments = (new MsExperimentParser()).getParser(inputFile).parseFromFile(inputFile);
                    allExperiments[i++] = experiments;
                } catch (IOException e) {
                    Log.error("Error parsing input file: " + inputFile);
                    Log.error(e.getMessage());
                    System.exit(0);
                }
            }

        }
        return allExperiments;
    }

    private void convertToMgf(String... args) {
        ConvertToMgfOptions options = null;
        try {
            options = CliFactory.createCli(ConvertToMgfOptions.class).parseArguments(args);
        } catch (HelpRequestedException e) {
            System.out.println(e.getMessage());
            System.out.println(CliFactory.createCli(ConvertToMgfOptions.class).getHelpMessage());
            System.exit(0);
        }
        if (options.isHelp()) {
            System.out.println(CliFactory.createCli(ConvertToMgfOptions.class).getHelpMessage());
            System.exit(0);
        }

        List<String> input = options.getInput();
        List<File> inputFiles = input.stream().map(File::new).collect(Collectors.toList());

        List<Ms2Experiment>[] allMsExperiments = readData(inputFiles);


        Deviation mergeMs2Deviation = new Deviation(options.getPPMMerge(), options.getPPMMergeAbs());
        MgfWriter mgfWriter = new MgfWriter(options.isWriteMs1(), options.isMergeMs2(), mergeMs2Deviation);

        writeToFile(mgfWriter, options.getOutput(), allMsExperiments);
    }

    private void writeToFile(DataWriter<Ms2Experiment> spectrumWriter, String output, List<Ms2Experiment>... allMsExperiments) {
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(output))) {
            for (List<Ms2Experiment> experiments : allMsExperiments) {
                for (Ms2Experiment experiment : experiments) {
                    spectrumWriter.write(writer, experiment);
                }
            }
        } catch (IOException e) {
            Log.error("Error writing output: " + output);
            Log.error(e.getMessage());
            System.exit(-1);
        }
    }

    private void filter(String... args) {
        FilterOptions options = null;
        try {
            options = CliFactory.createCli(FilterOptions.class).parseArguments(args);
        } catch (HelpRequestedException e) {
            System.out.println(e.getMessage());
            System.out.println(CliFactory.createCli(FilterOptions.class).getHelpMessage());
            System.exit(0);
        }
        if (options.isHelp()) {
            System.out.println(CliFactory.createCli(FilterOptions.class).getHelpMessage());
            System.exit(0);
        }

        String input = options.getInput();

        List<Ms2Experiment> experiments = readData(Collections.singletonList(new File(input)))[0];
        int numberOfExperimentsBefore = experiments.size();
        int numberOfMs2Before = getNumberOfMs2(experiments);

        if (options.getBlankFeaturesFile() != null) {
            Log.info("blank feature removal.");
            if (options.getMinFoldDifference() == null) {
                Log.error("minimum fold change not provided.");
                System.exit(-1);
            }
            MzRTPeak[] blankFeatures = null;
            try (BufferedReader reader = Files.newBufferedReader(Paths.get(options.getBlankFeaturesFile()))) {
                blankFeatures = ChemicalNoiseRemoval.readFeatureTable(reader);
            } catch (IOException e) {
                Log.error("Error reading file of filtered features: " + options.getBlankFeaturesFile());
                Log.error(e.getMessage());
                System.exit(-1);
            }
            Deviation maxDeviation = new Deviation(options.getPPMMax());
            double maxRtDifference = options.getRTMax();
            double minFoldDifference = options.getMinFoldDifference();
            ChemicalNoiseRemoval blankRemoval = new ChemicalNoiseRemoval(blankFeatures, maxDeviation, maxRtDifference, minFoldDifference);
            experiments = blankRemoval.removeNoiseFeatures(experiments);
        }

        CompoundFilterUtil compoundFilterUtil = new CompoundFilterUtil();
        if (options.getStartRT()!=null || options.getEndRT()!=null) {
            Log.info("filtering early/late compounds from LC.");
            double startRT = options.getStartRT()!=null ? options.getStartRT() : Double.NEGATIVE_INFINITY;
            double endRT = options.getEndRT()!=null ? options.getEndRT() : Double.POSITIVE_INFINITY;
            experiments = compoundFilterUtil.filterByRetentionTime(experiments, startRT, endRT);
        }

        if (options.getMinNumberOfIsotopes() > 0) {
            Log.info("filtering by number of isotopes.");
            int minNumIso = options.getMinNumberOfIsotopes();
            Deviation findPrecursorInMs1Deviation = new Deviation(options.getPPMMax());
            Deviation isoDifferenceDeviation = new Deviation(options.getPPMDiff());
            experiments = compoundFilterUtil.filterByNumberOfIsotopePeaks(experiments, minNumIso, findPrecursorInMs1Deviation, isoDifferenceDeviation);
        }

        if (options.isFilterZeroIntensity()) {
            Log.info("remove zero intensity features");
            Deviation findPrecursorInMs1Deviation = new Deviation(options.getPPMMax());
            experiments = compoundFilterUtil.filterZeroIntensityFeatures(experiments, findPrecursorInMs1Deviation);
        }


        if (options.isFilterChimeric()) {
            Log.info("remove chimeric compounds");
            double max2ndMostIntenseRatio = 0.33;
//            double maxSummedIntensitiesRatio = 1.0;
            //changed
//            double maxSummedIntensitiesRatio = 0.66;
            double maxSummedIntensitiesRatio = 0.5;
            Deviation isoDifferenceDeviation = new Deviation(options.getPPMDiff());



            double isoWindowWidth = options.getIsolationWindowWidth() == null ? -1 : options.getIsolationWindowWidth();
            double isoWindowShift = options.getIsolationWindowWidth() == null ? -1 : options.getIsolationWindowShift();

            IsolationWindow isolationWindow;
            if (isoWindowWidth>0){
                isolationWindow = new SimpleRectangularIsolationWindow(isoWindowShift-isoWindowWidth/2, isoWindowShift+isoWindowWidth/2);
            } else {
                Ms2DatasetPreprocessor preprocessor = new Ms2DatasetPreprocessor(false);
                MutableMs2Dataset dataset = new MutableMs2Dataset(experiments, "default", Double.NaN, (new Sirius()).getMs2Analyzer().getDefaultProfile());
                preprocessor.estimateIsolationWindow(dataset);
                isolationWindow = dataset.getIsolationWindow();
                if (isolationWindow.getEstimatedWindowSize() <= 1) {
                    Log.info("could not estimated isolation window. Setting simple 1 Da window");
                    isolationWindow = new SimpleRectangularIsolationWindow(-0.5, 0.5);
                }
            }


            ChemicalAlphabet alphabet = ChemicalAlphabet.getExtendedAlphabet();
            try {
                experiments = compoundFilterUtil.removeChimericSpectra(experiments, max2ndMostIntenseRatio, maxSummedIntensitiesRatio, isoDifferenceDeviation, isolationWindow, alphabet);
            } catch (InvalidInputData invalidInputData) {
                Log.error(invalidInputData.getMessage());
                System.exit(-1);
            }
        }

        if (options.getMinPrecursorIntAbs() != null || options.getMinPrecursorIntRel() != null) {
            Log.info("remove spectra with low intensity precursor");
            double minAbsInt = options.getMinPrecursorIntAbs() == null ? -1 : options.getMinPrecursorIntAbs();
            double minRelINt = options.getMinPrecursorIntRel() == null ? -1 : options.getMinPrecursorIntRel();
            Deviation findPrecursorInMs1Deviation = new Deviation(options.getPPMMax());
            try {
                experiments = compoundFilterUtil.removeLowIntensityPrecursorSpectra(experiments, minRelINt, minAbsInt, findPrecursorInMs1Deviation);
            } catch (InvalidInputData invalidInputData) {
                Log.error(invalidInputData.getMessage());
                System.exit(-1);
            }
        }

        //run after filtering chimerics since this migh influence behaviour.
        if (options.getMs1Baseline() != null || options.getMs2Baseline() != null) {
            Log.info("apply baseline");
            double ms1Baseline = options.getMs1Baseline() == null ? -1 : options.getMs1Baseline();
            double ms2Baseline = options.getMs2Baseline() == null ? -1 : options.getMs2Baseline();
            experiments = compoundFilterUtil.applyBaseline(experiments, ms1Baseline, ms2Baseline);
        }

        if (options.isRemoveIsotopes()) {
            Log.info("remove isotopes from MS2");
            Deviation isoDifferenceDeviation = new Deviation(options.getPPMDiff());
            ChemicalAlphabet alphabet;
            if (options.isCHNOPSOnly()){
                alphabet = new ChemicalAlphabet(PeriodicTable.getInstance().getAllByName("C", "H", "N", "O", "P", "S"));
                experiments = compoundFilterUtil.removeIsotopesFromMs2(experiments, isoDifferenceDeviation, 0.3, 0.6, 3, alphabet);
            } else {
                alphabet = ChemicalAlphabet.getExtendedAlphabet();
                experiments = compoundFilterUtil.removeIsotopesFromMs2(experiments, isoDifferenceDeviation, 1, 2, 4, alphabet);
            }

        }


        if (options.getMinTicTotal() != null) {
            Log.info("filter compounds by sum of MS2 low Total ion count.");
            double minTic = options.getMinTicTotal();
            experiments = compoundFilterUtil.filterBySumOfMS2TICs(experiments, minTic);
        }

        if (options.getMinTic() != null) {
            Log.info("remove MS2 with low Total ion count.");
            double minTic = options.getMinTic();
            try {
                experiments = compoundFilterUtil.removeMS2WithLowTotalIonCount(experiments, minTic);
            } catch (InvalidInputData invalidInputData) {
                Log.error(invalidInputData.getMessage());
                System.exit(-1);
            }
        }

        if (options.isFilterCompoundsWithoutMs2()) {
            Log.info("remove compounds without MS2");
            experiments = compoundFilterUtil.filterCompoundsWithoutMs2(experiments);
        }
        int numberOfExperimentsAfter = experiments.size();
        int numberOfMs2After = getNumberOfMs2(experiments);
        Log.info("number of compounds before filtering: " + numberOfExperimentsBefore + ", after: " + numberOfExperimentsAfter);
        Log.info("number of MS2 spectra before filtering: " + numberOfMs2Before + ", after: " + numberOfMs2After);
        JenaMsWriter jenaMsWriter = new JenaMsWriter();

        writeToFile(jenaMsWriter, options.getOutput(), experiments);
    }

    private int getNumberOfMs2(List<Ms2Experiment> experiments) {
        int count = 0;
        for (Ms2Experiment experiment : experiments) {
            count += experiment.getMs2Spectra().size();
        }
        return count;
    }


    private void map(String... args) {
        MapOptions options = null;
        try {
            options = CliFactory.createCli(MapOptions.class).parseArguments(args);
        } catch (HelpRequestedException e) {
            System.out.println(e.getMessage());
            System.out.println(CliFactory.createCli(MapOptions.class).getHelpMessage());
            System.exit(0);
        }
        if (options.isHelp()) {
            System.out.println(CliFactory.createCli(MapOptions.class).getHelpMessage());
            System.exit(0);
        }

        Deviation mzDeviation = new Deviation(options.getPPMMax());
        double rtDeviation = options.getRTMax();

        String input1 = options.getDataset1();
        String input2 = options.getDataset2();

        List<Ms2Experiment> experiments1 = readData(Collections.singletonList(new File(input1)))[0];
        List<Ms2Experiment> experiments2 = readData(Collections.singletonList(new File(input2)))[0];


        String[][] mapping = CompoundFilterUtil.mapCompoundIds(experiments1, experiments2, mzDeviation, rtDeviation);

        final String sep = "\t";
        Path output = Paths.get(options.getOutput());
        try (BufferedWriter writer = Files.newBufferedWriter(output)) {
            for (String[] strings : mapping) {
                writer.write(strings[0] + sep + strings[1]);
                writer.newLine();
            }
        } catch (IOException e) {
            Log.error("Error writing output: " + output);
            Log.error(e.getMessage());
            System.exit(-1);
        }

    }

    private void qualityStatistics(String... args) {
        CompoundQualityOptions options = null;
        try {
            options = CliFactory.createCli(CompoundQualityOptions.class).parseArguments(args);
        } catch (HelpRequestedException e) {
            System.out.println(e.getMessage());
            System.out.println(CliFactory.createCli(CompoundQualityOptions.class).getHelpMessage());
            System.exit(0);
        }
        if (options.isHelp()) {
            System.out.println(CliFactory.createCli(CompoundQualityOptions.class).getHelpMessage());
            System.exit(0);
        }


        Path outPath = options.getOutput()==null ? null : Paths.get(options.getOutput()).toAbsolutePath();
        boolean isCombinedOnly = options.isCombinedOnly();
        Path medianNoiseIntensityFile = options.getMedianNoiseIntensityOutputFile()==null ? null : Paths.get(options.getMedianNoiseIntensityOutputFile()).toAbsolutePath();
        boolean isWriteMedianNoiseIntensityOnly = medianNoiseIntensityFile!=null;
        if (isWriteMedianNoiseIntensityOnly){
            //don't write files
            //only estimate for combined data
            outPath = null;
            isCombinedOnly = true;
        }
        try {

            if (outPath!=null && !Files.exists(outPath)) {
                Log.info("create directory: " + outPath);
                Files.createDirectory(outPath);
            }
        } catch (IOException e) {
            Log.error("Error creating output directory: " + options.getOutput());
            Log.error(e.getMessage());
            Log.error("Note: This tool does not create multiple levels of directories.");
            System.exit(-1);
        }


        List<String> input = options.getInput();
        List<File> inputFiles = input.stream().map(File::new).collect(Collectors.toList());
        List<Ms2Experiment>[] experimentLists = readData(inputFiles);

        CompoundQualityUtils compoundQualityUtils = new CompoundQualityUtils();


        double medianMs2Noise = options.getMedianNoiseIntensity() == null ? -1 : options.getMedianNoiseIntensity();
        double isoWindowWidth = options.getIsolationWindowWidth() == null ? -1 : options.getIsolationWindowWidth();
        double isoWindowShift = options.getIsolationWindowWidth() == null ? -1 : options.getIsolationWindowShift();

        if (experimentLists.length==1) {
            try {
                Ms2Dataset dataset = compoundQualityUtils.updateQualityAndWrite(experimentLists[0], medianMs2Noise, isoWindowWidth, isoWindowShift, outPath);

                if (options.getMsOutputFile()!=null) {
                    JenaMsWriter writer = new JenaMsWriter();
                    writeToFile(writer, options.getMsOutputFile(), dataset.getExperiments());
                }
                if (options.getMsGoodQualityOutputFile()!=null) {
                    JenaMsWriter writer = new JenaMsWriter();
                    writeToFile(writer, options.getMsGoodQualityOutputFile(), dataset.getExperiments().stream().filter(e-> CompoundQuality.isNotBadQuality(e)).collect(Collectors.toList()));
                }

            } catch (IOException e) {
                Log.error("Error writing summary: " + options.getOutput());
                Log.error(e.getMessage());
                System.exit(-1);
            }
        } else {
            if (!isCombinedOnly){
                for (int i = 0; i < inputFiles.size(); i++) {
                    List<Ms2Experiment> ms2Experiments = experimentLists[i];
                    File inputFile = inputFiles.get(i);
                    Path subDir = outPath==null? null : outPath.resolve(inputFile.getName());

                    try {
                        if (subDir!=null) Files.createDirectory(subDir);
                    } catch (IOException e) {
                        Log.error("Error creating dir "+subDir);
                        e.printStackTrace();
                        System.exit(-1);
                    }


                    try {
                        compoundQualityUtils.updateQualityAndWrite(ms2Experiments, medianMs2Noise, isoWindowWidth, isoWindowShift, subDir);
                    } catch (IOException e) {
                        Log.error("Error writing summary: " + subDir);
                        Log.error(e.getMessage());
                        System.exit(-1);
                    }
                }
            }

            //write combined summary of all files //todo necessary to read input again to start without annotations?

            experimentLists = readData(inputFiles);
            List<Ms2Experiment> allExperiments = new ArrayList<>();
            for (List<Ms2Experiment> experimentList : experimentLists) {
                allExperiments.addAll(experimentList);
            }

            Path subDir = outPath==null ? null : outPath.resolve("combined");

            try {
                if (subDir!=null) Files.createDirectory(subDir);
            } catch (IOException e) {
                Log.error("Error creating dir "+subDir);
                e.printStackTrace();
                System.exit(-1);
            }

            try {
                if (isWriteMedianNoiseIntensityOnly){
                    double medianNoiseIntensity = compoundQualityUtils.estimateMedianNoise(allExperiments);
                    System.out.println("median noise intensity:\t"+medianNoiseIntensity);
                    BufferedWriter writer = Files.newBufferedWriter(medianNoiseIntensityFile);
                    writer.write(String.valueOf(medianNoiseIntensity));
                    writer.flush();
                    writer.close();
                } else {
                    Ms2Dataset dataset = compoundQualityUtils.updateQualityAndWrite(allExperiments, medianMs2Noise, isoWindowWidth, isoWindowShift, subDir);

                    if (options.getMsOutputFile()!=null) {
                        JenaMsWriter writer = new JenaMsWriter();
                        writeToFile(writer, options.getMsOutputFile(), dataset.getExperiments());
                    }
                    if (options.getMsGoodQualityOutputFile()!=null) {
                        JenaMsWriter writer = new JenaMsWriter();
                        writeToFile(writer, options.getMsGoodQualityOutputFile(), dataset.getExperiments().stream().filter(e-> CompoundQuality.isNotBadQuality(e)).collect(Collectors.toList()));
                    }
                }
            } catch (IOException e) {
                Log.error("Error writing summary: " + subDir);
                Log.error(e.getMessage());
                System.exit(-1);
            }
        }


    }


    private void extractChemicalNoise(String[] args) {
        ExtractChemicalNoiseOptions options = null;
        try {
            options = CliFactory.createCli(ExtractChemicalNoiseOptions.class).parseArguments(args);
        } catch (HelpRequestedException e) {
            System.out.println(e.getMessage());
            System.out.println(CliFactory.createCli(ExtractChemicalNoiseOptions.class).getHelpMessage());
            System.exit(0);
        }
        if (options.isHelp()) {
            System.out.println(CliFactory.createCli(ExtractChemicalNoiseOptions.class).getHelpMessage());
            System.exit(0);
        }


        Path output = Paths.get(options.getOutput());
        Path input = Paths.get(options.getInput());

        double[] frequentMasses = null;
        try {
            MzMLUtils mzMLUtils = MzMLUtils.getInstance(input);
            double binSize = new Deviation(options.getBinSizePPM()).absoluteFor(200);
            frequentMasses = mzMLUtils.getTooFrequentMasses(binSize, 0.01, 0.2);

        } catch (MalformedURLException | InvalidInputData e) {
            Log.error("Error reading input mzML: " + options.getInput());
            Log.error(e.getMessage());
            System.exit(-1);
        }

        try (BufferedWriter writer = Files.newBufferedWriter(output)) {
            writer.write("mz");
            writer.newLine();
            for (double frequentMass : frequentMasses) {
                writer.write(String.valueOf(frequentMass));
                writer.newLine();
            }
        } catch (IOException e) {
            Log.error("Error writing output: " + options.getOutput());
            Log.error(e.getMessage());
            System.exit(-1);
        }

    }


    private void splitInputData(String[] args){
        SplitInputDataOptions options = null;
        try {
            options = CliFactory.createCli(SplitInputDataOptions.class).parseArguments(args);
        } catch (HelpRequestedException e) {
            System.out.println(e.getMessage());
            System.out.println(CliFactory.createCli(SplitInputDataOptions.class).getHelpMessage());
            System.exit(0);
        }
        if (options.isHelp()) {
            System.out.println("This tool splits the input data into n separate files and shuffles compounds (hence, that a single output file does not contains all high mass compounds).");
            System.out.println(CliFactory.createCli(SplitInputDataOptions.class).getHelpMessage());
            System.exit(0);
        }


        Path output = Paths.get(options.getOutputPrefix()).toAbsolutePath();
        Path input = Paths.get(options.getInput());


        List<Ms2Experiment> experiments = readData(Collections.singletonList(input.toFile()))[0];

        //sort by mass
        Collections.sort(experiments, (o1, o2) -> Double.compare(o1.getIonMass(),o2.getIonMass()));

        int n = options.getNumberOfFiles();
        List<Ms2Experiment>[] parts = new List[n];
        for (int i = 0; i < parts.length; i++) {
            parts[i] = new ArrayList();
        }

        int i = 0;
        for (Ms2Experiment experiment : experiments) {
            parts[i++%n].add(experiment);
        }

        //now shuffle so masses are not ordered
        for (List<Ms2Experiment> part : parts) {
            Collections.shuffle(part);
        }


        //output
        Path folder = output.getParent();
        String prefix = output.getFileName().toString();
        if (prefix.endsWith(".ms")) prefix = prefix.substring(0, prefix.length()-3);

        JenaMsWriter writer = new JenaMsWriter();
        i = 1;
        for (List<Ms2Experiment> part : parts) {
            writeToFile(writer, folder.resolve(prefix+"_"+String.valueOf(i++)+".ms").toString(), part);
        }

    }

    private void collectWorkspaces(String[] args){
        CollectWorkspacesOptions options = null;
        try {
            options = CliFactory.createCli(CollectWorkspacesOptions.class).parseArguments(args);
        } catch (HelpRequestedException e) {
            System.out.println(e.getMessage());
            System.out.println(CliFactory.createCli(CollectWorkspacesOptions.class).getHelpMessage());
            System.exit(0);
        }
        if (options.isHelp()) {
            System.out.println(CliFactory.createCli(CollectWorkspacesOptions.class).getHelpMessage());
            System.exit(0);
        }


        Path output = Paths.get(options.getOutput()).toAbsolutePath();
        File[] inputFiles = options.getInput().stream().map(File::new).toArray(l->new File[l]);
        Map<String, Set<MolecularFormula>> selectedIdsWithMFCandidates;
        if (options.getIdList()!=null){
            selectedIdsWithMFCandidates = new HashMap<>();
            Path idFile = Paths.get(options.getIdList());
            try {
                Files.lines(idFile).forEach(l->{
                    String[] row = l.split("\t");
                    String id = row[0];
                    if (row.length==1){
                        selectedIdsWithMFCandidates.put(id, null);
                    } else {
                        Set<MolecularFormula> candidates = new HashSet<>();
                        for (int i = 1; i < row.length; i++) {
                            String s = row[i];
                            candidates.add(MolecularFormula.parse(s));
                        }
                        System.out.println("compound "+id+"with "+candidates+" candidates");
                        selectedIdsWithMFCandidates.put(id, candidates);
                    }

                });
            } catch (IOException e) {
                Log.error("Error reading id list.");
                Log.error(e.getMessage());
                System.exit(-1);
            }
        } else {
            selectedIdsWithMFCandidates = null;
        }

        List<ExperimentResult> experimentResults = new ArrayList<>();
        for (File inputFile : inputFiles) {
            try {
                experimentResults.addAll(loadWorkspace(inputFile, selectedIdsWithMFCandidates));
            } catch (IOException e) {
                Log.error("Error reading workspace: " + inputFile);
                Log.error(e.getMessage());
                System.exit(-1);
            }
        }


        boolean isZip = false;
        String lowercaseName = output.getFileName().toString().toLowerCase();
        if (lowercaseName.endsWith(".workspace") || lowercaseName.endsWith(".zip") || lowercaseName.endsWith(".sirius")) isZip = true;


        FilenameFormatter filenameFormatter = null;
        if (options.getNamingConvention() != null) {
            String formatString = options.getNamingConvention();
            try {
                filenameFormatter = new StandardMSFilenameFormatter(formatString);
            } catch (ParseException e) {
                Log.error("Cannot parse naming convention:\n" + e.getMessage(), e);
                System.exit(-1);
            }
        } else {
            //default
            filenameFormatter = new StandardMSFilenameFormatter();
        }

        String dirOutput = isZip?null:output.toString();
        String siriusOutput = isZip?output.toString():null;

        ProjectWriter projectWriter = null;
        try {
            ProjectSpaceUtils.ProjectWriterInfo projectWriterInfo = ProjectSpaceUtils.getProjectWriter(dirOutput, siriusOutput, new FingeridProjectSpaceFactory(filenameFormatter));
            projectWriter = projectWriterInfo.getProjectWriter();
            for (ExperimentResult experimentResult : experimentResults) {
                projectWriter.writeExperiment(experimentResult);
            }

        } catch (IOException e) {
            Log.error("Error writing workspace: " + output);
            Log.error(e.getMessage());
            System.exit(-1);
        } finally {
            if (projectWriter!=null){
                try {
                    projectWriter.close();

                } catch (IOException e){
                    Log.error("Error writing workspace: " + output);
                    Log.error(e.getMessage());
                    System.exit(-1);
                };
            }

        }



    }

    private void librarySearch(String[] args){
        SpectralLibrarySearchOptions options = null;
        try {
            options = CliFactory.createCli(SpectralLibrarySearchOptions.class).parseArguments(args);
        } catch (HelpRequestedException e) {
            System.out.println(e.getMessage());
            System.out.println(CliFactory.createCli(CollectWorkspacesOptions.class).getHelpMessage());
            System.exit(0);
        }
        if (options.isHelp()) {
            System.out.println(CliFactory.createCli(CollectWorkspacesOptions.class).getHelpMessage());
            System.exit(0);
        }


        Path libraryInput = Paths.get(options.getLibraryHitsDir());
        Path input = Paths.get(options.getInput());
        Path output = Paths.get(options.getOutput()).toAbsolutePath();
        double minCosine = options.getMinCosine();

        List<Ms2Experiment> libraryExperiments = readData(Collections.singletonList(libraryInput.toFile()))[0];
        List<Ms2Experiment> experiments = readData(Collections.singletonList(input.toFile()))[0];

        Deviation deviation = new Deviation(options.getPPMMs2(), 0.005);
//        Deviation parentDeviation = deviation.divide(2.0);
        Deviation parentDeviation = new Deviation(options.getPPMMs1(), 0.0025);

        AbstractSpectralAlignment spectralAlignment;
        if (options.getMethod().equalsIgnoreCase("gaussian")){
            spectralAlignment = new GaussianSpectralAlignment(deviation);
        } else {
            spectralAlignment = new IntensityWeightedSpectralAlignment(deviation);
        }

        MissingValueValidator valueValidator = new MissingValueValidator();
        for (int i = 0; i < libraryExperiments.size(); i++) {
            Ms2Experiment experiment = libraryExperiments.get(i);
            if (experiment.getMolecularFormula()==null || experiment.getPrecursorIonType()==null) {
                MutableMs2Experiment mutableMs2Experiment = valueValidator.validate(libraryExperiments.get(i), new Warning.Noop(), true);
                libraryExperiments.set(i, mutableMs2Experiment);
            }
        }
        SpectralLibrarySearch spectralLibrarySearch = SpectralLibrarySearch.newInstance(libraryExperiments.toArray(new Ms2Experiment[0]), spectralAlignment, parentDeviation, true, true, options.getMinMatchedPeaks(), options.isSearchTransformed());

        AllowedMassDifference allowedMassDifference;
        if (options.isAllowBiotransformations() && options.getMaxDifference()!=null){
            Log.error("don't use --biotransformations and --max-difference at once.");
        }
        if (options.isAllowBiotransformations()){
            if (options.getAllowedTransformations()==null) {
                //use default MFs
                allowedMassDifference = AllowedMassDifference.allowDirectMatchesAndBiotransformations();
            } else {
                List<MolecularFormula> allowedTransformations = new ArrayList<>();
                for (String allowedTransformation : options.getAllowedTransformations()) {
                    MolecularFormula mf = MolecularFormula.parse(allowedTransformation);
                    allowedTransformations.add(mf);
                }
                Log.info("using the following biotransformations: "+allowedTransformations.stream().map(MolecularFormula::formatByHill).collect(Collectors.joining(", ")));
                allowedMassDifference = AllowedMassDifference.allowDirectMatchesAndBiotransformations(allowedTransformations.toArray(new MolecularFormula[0]));
            }
        } else if (options.getMaxDifference()!=null) {
            allowedMassDifference = AllowedMassDifference.allowMaxDifference(options.getMaxDifference());
        } else {
            allowedMassDifference = AllowedMassDifference.onlyAllowDirectMatches();
        }

        List<SpectralLibraryHitWithExperiment> libraryHits = new ArrayList<>();
        for (Ms2Experiment experiment : experiments) {
            SpectralLibraryHit libraryHit = spectralLibrarySearch.findBestHit(experiment, allowedMassDifference);

            if (libraryHit.getCosine()>=minCosine && libraryHit.getLibraryHit()!=null){

                ////////////////////////////////////////////////////////////////////////////////////
                /// not nice yet, and also not working that well!
                /// probably still some problems for insource losses and addcuts
                ////////////////////////////////////////////////////////////////////////////////////
                if (options.isForceMFExplantion()){
                    //todo some way to try and explain mz difference to library hit. THis might corespond to a combination of loss and gain.
                    String possibleTransformationsString = null;
                    if (libraryHit.getMolecularFormula()==null){
                        Sirius sirius = new Sirius();
                        double libMz = libraryHit.getLibraryHit().getIonMass();
                        double compMz = experiment.getIonMass();
                        double mzDiff = Math.abs(libMz-compMz);

                        MolecularFormula measuredLibMF = libraryHit.getIonType().neutralMoleculeToMeasuredNeutralMolecule(libraryHit.getLibraryHit().getMolecularFormula());

                        FormulaConstraints constraints;
                        if (libMz>compMz){
                            constraints = FormulaConstraints.allSubsetsOf(measuredLibMF);
                        } else {
                            ChemicalAlphabet alphabet =  new ChemicalAlphabet(PeriodicTable.getInstance().getAllByName("C", "H", "N", "O", "P", "S", "Cl", "Br", "I", "F", "Si"));
                            constraints = new FormulaConstraints(alphabet);
                        }
                        Ionization ionization = new Charge(1);
                        List<MolecularFormula> formulas = sirius.decompose(ionization.addToMass(mzDiff), ionization, constraints, parentDeviation);
                        List<MolecularFormula> biotransf = Arrays.stream(BioTransformation.values()).map(BioTransformation::getFormula).filter(f->f.getMass()<=80).filter(f->f.getNumberOfElements()<=8).collect(Collectors.toList());
                        biotransf.add(MolecularFormula.parse("Br"));
                        biotransf.add(MolecularFormula.parse("F"));

                        HashMap<MolecularFormula, MolecularFormula[]> mfDifferenceToBiotransfomration = new HashMap<>();
                        for (MolecularFormula biot : biotransf) {
                            List<MolecularFormula> formulas2 = sirius.decompose(ionization.addToMass(mzDiff+biot.getMass()), ionization, constraints, parentDeviation);
                            for (MolecularFormula formula : formulas2) {
                                int numberOfUncommon = formula.numberOf("Br")+formula.numberOf("Cl")+formula.numberOf("I")+formula.numberOf("F")+formula.numberOf("Si");
                                if (numberOfUncommon>1){
                                    continue;
                                } else if (numberOfUncommon==1 && (formula.numberOf("S")>0 || formula.numberOf("P")>0)){
                                    continue;
                                }
                                MolecularFormula mfDiff = formula.subtract(biot);
                                MolecularFormula[] transf = mfDifferenceToBiotransfomration.get(mfDiff);
                                if (transf==null){
                                    mfDifferenceToBiotransfomration.put(mfDiff, new MolecularFormula[]{formula, biot});
                                } else if (biot.getNumberOfElements()<transf[1].getNumberOfElements()) {
                                    mfDifferenceToBiotransfomration.put(mfDiff, new MolecularFormula[]{formula, biot});
                                }
                            }
                        }

                        List<String> possibleTransformations = new ArrayList<>();
                        possibleTransformations.addAll(formulas.stream().map(MolecularFormula::formatByHill).collect(Collectors.toList()));

                        for (MolecularFormula mfDiff : mfDifferenceToBiotransfomration.keySet()) {
                            MolecularFormula[] transf = mfDifferenceToBiotransfomration.get(mfDiff);
                            possibleTransformations.add("["+transf[0]+"]-["+transf[1]+"]");
                        }

                        possibleTransformationsString = possibleTransformations.stream().collect(Collectors.joining(","));

                        if (formulas.size()>1){
                            String alltransf = possibleTransformations.stream().collect(Collectors.joining(","));
                            Log.warn("multiple transformations possible for "+experiment.getName()+":"+alltransf);
                        } else if (formulas.size()==1) {
                            MolecularFormula estimatedMF = (libMz>compMz) ? measuredLibMF.subtract(formulas.get(0)) : measuredLibMF.add(formulas.get(0));
                            libraryHit = new SpectralLibraryHit(libraryHit.getLibraryHit(), estimatedMF, libraryHit.getCosine(), libraryHit.getNumberOfSharedPeaks());
                        }
                    } else {
                        possibleTransformationsString = libraryHit.getLibraryHit().getMolecularFormula().subtract(libraryHit.getMolecularFormula()).toString();
                    }


                    SpectralLibraryHitWithExperiment spectralLibraryHitWithExperiment = new SpectralLibraryHitWithExperiment(libraryHit, experiment);
                    spectralLibraryHitWithExperiment.setPossibleTransformations(possibleTransformationsString);
                    libraryHits.add(spectralLibraryHitWithExperiment);


                ////////////////////////////////////////////////////////////////////////////////////
                ////////////////////////////////////////////////////////////////////////////////////
                } else {
                    SpectralLibraryHitWithExperiment spectralLibraryHitWithExperiment = new SpectralLibraryHitWithExperiment(libraryHit, experiment);
                    libraryHits.add(spectralLibraryHitWithExperiment);
                }


            }
        }

        //sort by cosine
        libraryHits.sort((s1,s2)-> Double.compare(s2.libraryHit.getCosine(), s1.libraryHit.getCosine()));

        try {
            writeHits(output, libraryHits);
        } catch (IOException e) {
            Log.error("Error writing library hits: " + output);
            Log.error(e.getMessage());
            System.exit(-1);
        }

    }

    private void extendIsotopePattern(String[] args){
        DefaultMSInputOutputWithMS1PPM options = null;
        String description = "This tool extracts or extends the compounds' isotope patterns and saves them as mergedMS1. This will remove any peaks of other compounds from the merged MS1.";
        try {
            options = CliFactory.createCli(DefaultMSInputOutputWithMS1PPM.class).parseArguments(args);
        } catch (HelpRequestedException e) {
            System.out.println(description);
            System.out.println(e.getMessage());
            System.out.println(CliFactory.createCli(DefaultMSInputOutputWithMS1PPM.class).getHelpMessage());
            System.exit(0);
        }
        if (options.isHelp()) {
            System.out.println(description);
            System.out.println(CliFactory.createCli(DefaultMSInputOutputWithMS1PPM.class).getHelpMessage());
            System.exit(0);
        }

        String input = options.getInput();

        List<Ms2Experiment> experiments = readData(Collections.singletonList(new File(input)))[0];

        Sirius sirius = new Sirius();
        MutableMeasurementProfile profile = sirius.getMs2Analyzer().getDefaultProfile();
        profile.setAllowedMassDeviation(new Deviation(options.getPPM()));

        ListIterator<Ms2Experiment> iterator = experiments.listIterator();
        while (iterator.hasNext()) {
            Ms2Experiment experiment = iterator.next();

            SimpleSpectrum isotopePattern = sirius.extractIsotopePattern(experiment, profile); //todo different MS1 deviation necessary?

            if (experiment.getMergedMs1Spectrum()!= null) {
                if (experiment.getMergedMs1Spectrum().size()>isotopePattern.size()) {
                    Log.warn("number of peaks in merged MS1 decreased for: "+experiment.getName());
                } else if (experiment.getMergedMs1Spectrum().size() < isotopePattern.size()) {
                    Log.info("number of peaks in merged MS1 increased for: "+experiment.getName());
                }
            }
            MutableMs2Experiment mutableMs2Experiment = new MutableMs2Experiment(experiment);
            mutableMs2Experiment.setMergedMs1Spectrum(isotopePattern);

            iterator.set(mutableMs2Experiment);
        }

        JenaMsWriter jenaMsWriter = new JenaMsWriter();

        writeToFile(jenaMsWriter, options.getOutput(), experiments);


    }

    private interface DefaultMSInputOutputWithMS1PPM extends DefaultMSInputOutputOptions {
        @Option(longName = "ppm", description = "maximum allowed deviation in ppm.", defaultValue = "10")
        Double getPPM();
    }

    private final static String SEP = "\t";
    private void writeHits(Path output, List<SpectralLibraryHitWithExperiment> libraryHits) throws IOException {
        BufferedWriter writer = Files.newBufferedWriter(output);
        String header = Arrays.stream(new String[]{"compoundName", "compoundFile","compoundMz", "compoundAdduct", "libraryName", "libraryMz", "libraryAdduct", "libraryFormula", "cosine", "sharedPeaks", "molecularFormula", "libraryInChI", "librarySMILES", "formulaDiffToLibrary", "libURL"}).collect(Collectors.joining(SEP));
        writer.write(header);
        int idx = 0;
        for (SpectralLibraryHitWithExperiment libraryHit : libraryHits) {
            Ms2Experiment experiment = libraryHit.experiment;
            SpectralLibraryHit hit = libraryHit.libraryHit;
            StringJoiner joiner = new StringJoiner(SEP);
            joiner.add(experiment.getName());
            URL compoundSource  = experiment.getSource();
            joiner.add(compoundSource==null?"":compoundSource.toString());
            joiner.add(Double.toString(experiment.getIonMass()));
            joiner.add(experiment.getPrecursorIonType().toString());
            if (hit.getLibraryHit().getName()!=null){
                joiner.add(hit.getLibraryHit().getName());
            } else joiner.add("");
            joiner.add(Double.toString(hit.getLibraryHit().getIonMass()));
            joiner.add(hit.getLibraryHit().getIonType().toString());
            joiner.add(hit.getLibraryHit().getMolecularFormula().toString());
            joiner.add(Double.toString(hit.getCosine()));
            joiner.add(Integer.toString(hit.getNumberOfSharedPeaks()));
            MolecularFormula estimatedformula = hit.getMolecularFormula();
            joiner.add(estimatedformula==null?"":estimatedformula.formatByHill());
            if (hit.getLibraryHit().getInChI()!=null) {
                joiner.add(hit.getLibraryHit().getInChI().in3D);
            } else joiner.add("");
            if (hit.getLibraryHit().getSmiles()!=null){
                joiner.add(hit.getLibraryHit().getSmiles().smiles);
            } else joiner.add("");

            if (libraryHit.getPossibleTransformations()!=null){
                joiner.add(libraryHit.getPossibleTransformations());
            } else {
                joiner.add(estimatedformula==null?"":libraryHit.libraryHit.getLibraryHit().getMolecularFormula().subtract(estimatedformula).toString());
            }

            URL source  = hit.getLibraryHit().getSource();
            joiner.add(source==null?"":source.toString());
            writer.newLine();
            writer.write(joiner.toString());
            ++idx;
        }
        writer.close();
    }


    protected List<ExperimentResult> loadWorkspace(File file) throws IOException {
        return loadWorkspace(file, null);
    }

    /**
     * reads workspace.
     * @param file
     * @param idSetWithMFCandidates select compounds by id. If instance has a set of molecular formulas, {@link IdentificationResult}s are filtered
     * @return
     * @throws IOException
     */
    protected List<ExperimentResult> loadWorkspace(File file, Map<String, Set<MolecularFormula>> idSetWithMFCandidates) throws IOException {
        final List<ExperimentResult> results = new ArrayList<>();
        final DirectoryReader.ReadingEnvironment env;
        if (file.isDirectory()) {
            env = new SiriusFileReader(file);
        } else {
            env = new SiriusWorkspaceReader(file);
        }
        final DirectoryReader reader = new DirectoryReader(env);

        while (reader.hasNext()) {
            final ExperimentResult result = reader.next();
            if (idSetWithMFCandidates!=null && !idSetWithMFCandidates.containsKey(result.getExperiment().getName())) continue;
            Set<MolecularFormula> mfCandidates = idSetWithMFCandidates!=null ? idSetWithMFCandidates.get(result.getExperiment().getName()) : null;
            if (mfCandidates==null){
                results.add(result);
            } else {
                //if mfCandidates not null, filter results by molecular formula candidates
                Iterator<IdentificationResult> iterator = result.getResults().iterator();
                while (iterator.hasNext()){
                    IdentificationResult identificationResult = iterator.next();
                    if (!mfCandidates.contains(identificationResult.getMolecularFormula())) iterator.remove();
                }
                if (result.getResults().size()!=mfCandidates.size()){
                    System.out.println("Did not find all selected molecular formula candidates for "+result.getExperiment().getName());
                }
            }
        }
        return results;
    }


    private class SpectralLibraryHitWithExperiment {
        SpectralLibraryHit libraryHit;
        Ms2Experiment experiment;
        String possibleTransformations;

        public SpectralLibraryHitWithExperiment(SpectralLibraryHit libraryHit, Ms2Experiment experiment) {
            this.libraryHit = libraryHit;
            this.experiment = experiment;
            this.possibleTransformations = null;
        }

        public String getPossibleTransformations() {
            return possibleTransformations;
        }

        public void setPossibleTransformations(String possibleTransformations) {
            this.possibleTransformations = possibleTransformations;
        }
    }

}
