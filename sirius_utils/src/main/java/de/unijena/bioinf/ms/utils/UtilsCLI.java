package de.unijena.bioinf.ms.utils;

import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.HelpRequestedException;
import de.unijena.bioinf.ChemistryBase.SimpleRectangularIsolationWindow;
import de.unijena.bioinf.ChemistryBase.chem.ChemicalAlphabet;
import de.unijena.bioinf.ChemistryBase.exceptions.InvalidInputData;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.babelms.DataWriter;
import de.unijena.bioinf.babelms.MsExperimentParser;
import de.unijena.bioinf.babelms.mgf.MgfWriter;
import de.unijena.bioinf.babelms.ms.JenaMsWriter;
import de.unijena.bioinf.fingerid.FingeridProjectSpaceFactory;
import de.unijena.bioinf.ms.cli.ProjectSpaceUtils;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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

    private List<Ms2Experiment>[] readData(List<File> inputFiles) {
        List<Ms2Experiment>[] allExperiments = new List[inputFiles.size()];
        int i = 0;
        for (File inputFile : inputFiles) {
            try {
                List<Ms2Experiment> experiments = (new MsExperimentParser()).getParser(inputFile).parseFromFile(inputFile);
                allExperiments[i++] = experiments;
            } catch (IOException e) {
                Log.error("Error parsing input file: " + inputFile);
                Log.error(e.getMessage());
                System.exit(0);
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

        if (options.getMs1Baseline() != null || options.getMs2Baseline() != null) {
            Log.info("apply baseline");
            double ms1Baseline = options.getMs1Baseline() == null ? -1 : options.getMs1Baseline();
            double ms2Baseline = options.getMs2Baseline() == null ? -1 : options.getMs2Baseline();
            experiments = compoundFilterUtil.applyBaseline(experiments, ms1Baseline, ms2Baseline);
        }

        if (options.isFilterChimeric()) {
            Log.info("remove chimeric compounds");
            double max2ndMostIntenseRatio = 0.33;
            double maxSummedIntensitiesRatio = 1.0;
            Deviation isoDifferenceDeviation = new Deviation(options.getPPMDiff());


            Ms2DatasetPreprocessor preprocessor = new Ms2DatasetPreprocessor(false);
            MutableMs2Dataset dataset = new MutableMs2Dataset(experiments, "default", Double.NaN, (new Sirius()).getMs2Analyzer().getDefaultProfile());
            preprocessor.estimateIsolationWindow(dataset);
            IsolationWindow isolationWindow = dataset.getIsolationWindow();
            if (isolationWindow.getEstimatedWindowSize() <= 1) {
                Log.info("could not estimated isolation window. Setting simple 1 Da window");
                isolationWindow = new SimpleRectangularIsolationWindow(-0.5, 0.5);
            }

            ChemicalAlphabet alphabet = ChemicalAlphabet.getExtendedAlphabet();
            try {
                experiments = compoundFilterUtil.removeChimericSpectra(experiments, max2ndMostIntenseRatio, maxSummedIntensitiesRatio, isoDifferenceDeviation, isolationWindow, alphabet);
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


        Path outPath = Paths.get(options.getOutput()).toAbsolutePath();
        try {

            if (!Files.exists(outPath)) {
                Log.info("create directory: " + outPath);
                Files.createDirectory(outPath);
            }
        } catch (IOException e) {
            Log.error("Error creating output directory: " + options.getOutput());
            Log.error(e.getMessage());
            Log.error("Note: This tool does not create multiple levels of directories.");
            System.exit(-1);
        }


//        List<String> input = options.getInput();
//        List<File> inputFiles = input.stream().map(File::new).collect(Collectors.toList());
        String input = options.getInput();
        List<Ms2Experiment> experiments = readData(Collections.singletonList(new File(input)))[0];

        CompoundQualityUtils compoundQualityUtils = new CompoundQualityUtils();


        double medianMs2Noise = options.getMedianNoiseIntensity() == null ? -1 : options.getMedianNoiseIntensity();
        double isoWindowWidth = options.getIsolationWindowWidth() == null ? -1 : options.getIsolationWindowWidth();
        double isoWindowShift = options.getIsolationWindowWidth() == null ? -1 : options.getIsolationWindowShift();
        try {
            compoundQualityUtils.updateQualityAndWrite(experiments, medianMs2Noise, isoWindowWidth, isoWindowShift, outPath);
        } catch (IOException e) {
            Log.error("Error writing summary: " + options.getOutput());
            Log.error(e.getMessage());
            System.exit(-1);
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

        List<ExperimentResult> experimentResults = new ArrayList<>();
        for (File inputFile : inputFiles) {
            try {
                experimentResults.addAll(loadWorkspace(inputFile));
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


    protected List<ExperimentResult> loadWorkspace(File file) throws IOException {
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
            results.add(result);
        }
        return results;
    }




}
