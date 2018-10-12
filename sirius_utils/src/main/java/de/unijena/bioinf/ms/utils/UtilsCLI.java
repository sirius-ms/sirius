package de.unijena.bioinf.ms.utils;

import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.HelpRequestedException;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.MzRTPeak;
import de.unijena.bioinf.babelms.DataWriter;
import de.unijena.bioinf.babelms.MsExperimentParser;
import de.unijena.bioinf.babelms.mgf.MgfWriter;
import de.unijena.bioinf.babelms.ms.JenaMsWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
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
    protected void parseArgsAndRun(String[] args){
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
        switch (arg){
            case "merge":
                mergeCompounds(rest);
                break;
            case "convert-to-mgf":
                convertToMgf(rest);
                break;
            case "filter":
                filter(rest);
                break;
            default:
                System.out.println("Please, specify your intended command as first parameter.");
                System.exit(0);
        }

    }

    private boolean containsIgnoreCase(String[] array, String string){
        for (String s : array) {
            if (s.equalsIgnoreCase(string)) return true;
        }
        return false;
    }

    private void mergeCompounds(String... args){
        MergingOptions options = null;
        try {
            options = CliFactory.createCli(MergingOptions.class).parseArguments(args);
        } catch (HelpRequestedException e) {
            System.out.println(e.getMessage());
            System.out.println(CliFactory.createCli(MergingOptions.class).getHelpMessage());
            System.exit(0);
        }
        if (options.isHelp()){
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
                Log.error("Error parsing input file: "+inputFile);
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
        if (options.isHelp()){
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
        } catch (IOException e){
            Log.error("Error writing output: "+output);
            Log.error(e.getMessage());
            System.exit(0);
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
        if (options.isHelp()){
            System.out.println(CliFactory.createCli(FilterOptions.class).getHelpMessage());
            System.exit(0);
        }

        String input = options.getInput();

        List<Ms2Experiment> experiments = readData(Collections.singletonList(new File(input)))[0];
        int numberOfExperimentsBefore = experiments.size();

        if (options.getBlankFeaturesFile()!=null){
            if (options.getMinFoldDifference()==null){
                Log.error("minimum fold change not provided.");
                System.exit(0);
            }
            MzRTPeak[] blankFeatures = null;
            try(BufferedReader reader = Files.newBufferedReader(Paths.get(options.getBlankFeaturesFile()))){
                blankFeatures = BlankRemoval.readFeatureTable(reader);
            } catch (IOException e){
                Log.error("Error reading file of filtered features: "+options.getBlankFeaturesFile());
                Log.error(e.getMessage());
                System.exit(0);
            }
            Deviation maxDeviation = new Deviation(options.getPPMMax());
            double maxRtDifference = options.getRTMax();
            double minFoldDifference = options.getMinFoldDifference();
            BlankRemoval blankRemoval = new BlankRemoval(blankFeatures, maxDeviation, maxRtDifference, minFoldDifference);
            experiments = blankRemoval.removeBlanks(experiments);
        }

        if (options.getMinNumberOfIsotopes()>0){
            int minNumIso = options.getMinNumberOfIsotopes();
            Deviation findPrecursorInMs1Deviation = new Deviation(options.getPPMMax());
            Deviation isoDifferenceDeviation = new Deviation(options.getPPMDiff());
            CompoundFilterUtil compoundFilterUtil = new CompoundFilterUtil();
            experiments = compoundFilterUtil.filterByNumberOfIsotopePeaks(experiments, minNumIso, findPrecursorInMs1Deviation, isoDifferenceDeviation);
        }

        if (options.isFilterZeroIntensity()){
            Deviation findPrecursorInMs1Deviation = new Deviation(options.getPPMMax());
            CompoundFilterUtil compoundFilterUtil = new CompoundFilterUtil();
            experiments = compoundFilterUtil.filterZeroIntensityFeatures(experiments, findPrecursorInMs1Deviation);
        }

        int numberOfExperimentsAfter = experiments.size();
        Log.info("number of compounds before filtering: "+numberOfExperimentsBefore+", after: "+numberOfExperimentsAfter);
        JenaMsWriter jenaMsWriter = new JenaMsWriter();

        writeToFile(jenaMsWriter, options.getOutput(), experiments);
    }

}
