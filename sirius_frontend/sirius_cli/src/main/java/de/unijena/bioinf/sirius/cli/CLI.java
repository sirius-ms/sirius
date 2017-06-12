/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2015 Kai Dührkop
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.unijena.bioinf.sirius.cli;

import com.google.common.io.Files;
import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.HelpRequestedException;
import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.FragmentationPatternAnalysis;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.inputValidator.InvalidException;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.inputValidator.Warning;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.TreeBuilder;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.maximumColorfulSubtree.TreeBuilderFactory;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.IsotopePatternAnalysis.IsotopePattern;
import de.unijena.bioinf.IsotopePatternAnalysis.IsotopePatternAnalysis;
import de.unijena.bioinf.babelms.GenericParser;
import de.unijena.bioinf.babelms.MsExperimentParser;
import de.unijena.bioinf.babelms.SpectralParser;
import de.unijena.bioinf.sirius.IdentificationResult;
import de.unijena.bioinf.sirius.IsotopePatternHandling;
import de.unijena.bioinf.sirius.Progress;
import de.unijena.bioinf.sirius.Sirius;
import de.unijena.bioinf.sirius.core.ApplicationCore;
import de.unijena.bioinf.sirius.projectspace.*;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class CLI<Options extends SiriusOptions> extends ApplicationCore{
    protected Sirius sirius;
    protected final boolean shellMode;
    protected ShellProgress progress;

    protected ProjectWriter projectWriter;
    protected boolean shellOutputSurpressed = false;

    protected org.slf4j.Logger logger = LoggerFactory.getLogger(CLI.class);


    public void print(String s) {
        if (!shellOutputSurpressed) System.out.print(s);
    }
    public void println(String s) {
        if (!shellOutputSurpressed) System.out.println(s);
    }
    protected void printf(String msg, Object... args) {
        if (!shellOutputSurpressed)
            System.out.printf(Locale.US, msg, args);
    }

    public static void main(String[] args) {
        final CLI cli = new CLI();
        if (args.length>0 && args[0].toLowerCase().equals("zodiac")){
            ZodiacOptions options = null;
            try {
                options = CliFactory.createCli(ZodiacOptions.class).parseArguments(Arrays.copyOfRange(args, 1, args.length));
            } catch (HelpRequestedException e) {
                cli.println(e.getMessage());
                cli.println("");
                System.exit(0);
            }

            Zodiac zodiac = new Zodiac(options);
            zodiac.run();
        } else {
            cli.parseArgsAndInit(args);
            cli.compute();
        }
    }

    public CLI() {
        this.shellMode = System.console()!=null;
        this.progress = new ShellProgress(System.out, shellMode);
    }

    Options options;
    List<String> inputs, formulas;
    PrecursorIonType[] ionTypes;

    public void compute() {
        try {
            sirius.setProgress(shellOutputSurpressed ? new Progress.Quiet() : progress);
            final Iterator<Instance> instances = handleInput(options);
            while (instances.hasNext()) {
                try {
                    final Instance i = instances.next();
                    sirius.getProgress().info("Compute '" + i.file.getName() + "'");
                    final boolean doIdentify;
                    final List<IdentificationResult> results;

                    //todo hack to include guessing of ionization. If appreciate include in a better way
                    if (options.getPossibleIonizations()!=null) {
                        PrecursorIonType[] specificIontypes = guessIonization(i);

                        Set<MolecularFormula> mfCandidatesSet = new HashSet<MolecularFormula>();
                        FormulaConstraints constraints = sirius.getMs1Analyzer().getDefaultProfile().getFormulaConstraints();
                        for (PrecursorIonType ionType : specificIontypes) {
                            List<MolecularFormula> mfCandidates = sirius.decompose(i.experiment.getIonMass(), ionType.getIonization(), constraints);

                            for (MolecularFormula mfCandidate : mfCandidates) {
                                mfCandidatesSet.add(ionType.measuredNeutralMoleculeToNeutralMolecule(mfCandidate));
                            }

                        }
                        results = sirius.identify(i.experiment, getNumberOfCandidates(), !options.isNotRecalibrating(), options.getIsotopes(), mfCandidatesSet);

                    } else {
                        final List<String> whitelist = formulas;
                        final Set<MolecularFormula> whiteset = getFormulaWhiteset(i, whitelist);
                        if ((whiteset == null) && options.isAutoCharge() && i.experiment.getPrecursorIonType().isIonizationUnknown()) {
                            results = sirius.identifyPrecursorAndIonization(i.experiment, getNumberOfCandidates(), !options.isNotRecalibrating(), options.getIsotopes());
                        } else if (whiteset != null && whiteset.isEmpty()) {
                            results = new ArrayList<>();
                        } else if (whiteset == null || whiteset.size() != 1) {
                            results = sirius.identify(i.experiment, getNumberOfCandidates(), !options.isNotRecalibrating(), options.getIsotopes(), whiteset);
                        } else {
                            results = Arrays.asList(sirius.compute(i.experiment, whiteset.iterator().next(), !options.isNotRecalibrating()));
                        }
                    }



                    //beautify tree (try to explain more peaks)
                    if (options.isBeautifyTrees()){
                        for (IdentificationResult result : results) {
                            sirius.beautifyTree(result, i.experiment, !options.isNotRecalibrating());
                        }
                    }

                    int rank = 1;
                    int n = Math.max(1, (int) Math.ceil(Math.log10(results.size())));
                    for (IdentificationResult result : results) {
                        {
                            final ProcessedInput processedInput = result.getStandardTree().getAnnotationOrNull(ProcessedInput.class);
                            if (processedInput!=null) result.setAnnotation(Ms2Experiment.class, processedInput.getExperimentInformation());
                            else result.setAnnotation(Ms2Experiment.class, i.experiment);
                        }
                        final IsotopePattern pat = result.getRawTree().getAnnotationOrNull(IsotopePattern.class);
                        final int isoPeaks = pat==null ? 0 : pat.getPattern().size()-1;
                        printf("%" + n + "d.) %s\t%s\tscore: %.2f\ttree: %+.2f\tiso: %.2f\tpeaks: %d\texplained intensity: %.2f %%\tisotope peaks: %d\n", rank++, result.getMolecularFormula().toString(), String.valueOf(result.getResolvedTree().getAnnotationOrNull(PrecursorIonType.class)), result.getScore(), result.getTreeScore(), result.getIsotopeScore(), result.getResolvedTree().numberOfVertices(), sirius.getMs2Analyzer().getIntensityRatioOfExplainedPeaks(result.getResolvedTree()) * 100, isoPeaks);
                    }
                    handleResults(i, results);
                    output(i, results);
                } catch (InvalidException e) {
                    LoggerFactory.getLogger(CLI.class).error("Invalid input: " + e.getMessage(),e);
                } catch (RuntimeException e) {
                    LoggerFactory.getLogger(CLI.class).error(e.getMessage(),e);
                }
            }
        } catch (IOException e) {
            LoggerFactory.getLogger(CLI.class).error(e.getMessage(),e);
        } finally {
            if (projectWriter != null) try {
                projectWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private PrecursorIonType[] guessIonization(Instance instance){
        final MutableMs2Experiment experimentMutable = new MutableMs2Experiment(instance.experiment);
        experimentMutable.setPrecursorIonType(PrecursorIonType.unknown(instance.experiment.getPrecursorIonType().getCharge()));

        PrecursorIonType[] specificIontypes = sirius.guessIonization(experimentMutable, ionTypes);
        PrecursorIonType priorIonType = instance.experiment.getPrecursorIonType();
        PrecursorIonType priorIonization = priorIonType.withoutAdduct().withoutInsource();
        if (!priorIonization.isIonizationUnknown()){
            if (!arrayContains(specificIontypes, priorIonization)){
                specificIontypes = Arrays.copyOf(specificIontypes, specificIontypes.length+1);
                specificIontypes[specificIontypes.length-1] = priorIonization;
            } else {
                specificIontypes = new PrecursorIonType[]{priorIonization};
            }

        }
        if (specificIontypes.length==0){
            specificIontypes = ionTypes;
            //todo: do something better: this is  a don't use M+ and M+H+ hack
            PrecursorIonType m_plus  = PrecursorIonType.getPrecursorIonType("[M]+");
            PrecursorIonType m_plus_h = PrecursorIonType.getPrecursorIonType("[M+H]+");

            if (arrayContains(specificIontypes, m_plus) && arrayContains(specificIontypes, m_plus_h)){
                PrecursorIonType[] copy = new PrecursorIonType[specificIontypes.length-1];

                int i = 0;
                for (PrecursorIonType specificIontype : specificIontypes) {
                    if (specificIontype.equals(m_plus)) continue;
                    copy[i++] = specificIontype;
                }
                specificIontypes = copy;
            }
        }




        return specificIontypes;
    }

    protected void handleResults(Instance i, List<IdentificationResult> results) {
        if (results==null || results.isEmpty()) {
            logger.error("Cannot find valid tree that supports the data. You can try to increase the allowed mass deviation with parameter --ppm-max");
            return;
        }
    }

    protected Set<MolecularFormula> getFormulaWhiteset(Instance i, List<String> whitelist) {
        final Set<MolecularFormula> whiteset = new HashSet<MolecularFormula>();
        if (whitelist==null && (options.getNumberOfCandidates()==null) && i.experiment.getMolecularFormula()!=null) {
            whiteset.add(i.experiment.getMolecularFormula());
        } else if (whitelist!=null) for (String s :whitelist) whiteset.add(MolecularFormula.parse(s));
        return whiteset.isEmpty() ? null : whiteset;
    }

    private Integer getNumberOfCandidates() {
        return options.getNumberOfCandidates()!=null ? options.getNumberOfCandidates() : 5;
    }

    private void output(Instance instance, List<IdentificationResult> results) throws IOException {
        if (projectWriter!=null) {
            projectWriter.writeExperiment(new ExperimentResult(instance.experiment, results));
        }
    }

    protected void cite() {
        println("Please cite the following paper when using our method:");
        println(ApplicationCore.CITATION);
    }

    protected void parseArgsAndInit(String[] args) {
        parseArgs(args);
        setup();
        validate();
    }

    protected void validate() {

    }

    public void parseArgs(String[] args) {
        parseArgs(args, (Class<Options>) SiriusOptions.class);
    }

    public void parseArgs(String[] args, Class<Options> optionsClass ) {
        if (args.length==0) {
            println(ApplicationCore.VERSION_STRING);
            println(CliFactory.createCli(optionsClass).getHelpMessage());
            System.exit(0);
        }
        try {
            this.options = CliFactory.createCli(optionsClass).parseArguments(args);
            if (options.isCite()) {
                cite();
                System.exit(0);
            }
        } catch (HelpRequestedException e) {
            println(e.getMessage());
            println("");
            cite();
            System.exit(0);
        }
        if (options.isVersion()) {
            println(ApplicationCore.VERSION_STRING);
            cite();
            System.exit(0);
        }
        handleOutputOptions(options);
    }

    protected void handleOutputOptions(Options options) {
        if (options.isQuiet() || "-".equals(options.getSirius())) {
            this.shellOutputSurpressed = true;
            disableShellLogging();
        }
        if ("-".equals(options.getOutput())) {
            logger.error("Cannot write output files and folders into standard output stream. Please use --sirius t get a zip file of SIRIUS output into the standard output stream");
            System.exit(1);
        }

        final List<ProjectWriter> writers = new ArrayList<>();

        if (options.getOutput() != null) {
            final ProjectWriter pw;
            if (new File(options.getOutput()).exists()) {
                try {
                    checkForValidProjectDirecotry(options.getOutput());
                    pw = new ProjectSpaceMerger(this, options.getOutput(), false);
                    writers.add(pw);
                } catch (IOException e) {

                    logger.error("Cannot merge project " + options.getOutput() + ". Maybe the specified directory is not a valid SIRIUS workspace. You can still specify a new not existing filename to create a new workspace.\n" + e.getMessage(), e);
                    System.exit(1);
                    return;
                }
            } else {
                try {
                    pw = getDirectoryOutputWriter(options.getOutput(), getWorkspaceWritingEnvironmentForDirectoryOutput(options.getOutput()));
                    writers.add(pw);
                } catch (IOException e) {
                    logger.error("Cannot write into " + options.getOutput() + ":\n" + e.getMessage(), e);
                    System.exit(1);
                }
            }
        }
        if (options.getSirius() != null) {
            final ProjectWriter pw;
            if (options.getSirius().equals("-")) {
                pw = getSiriusOutputWriter(options.getSirius(), getWorkspaceWritingEnvironmentForSirius(options.getSirius()));
                shellOutputSurpressed = true;
            } else if (new File(options.getSirius()).exists()) {
                try {
                    pw = new ProjectSpaceMerger(this, options.getSirius(), true);
                } catch (IOException e) {
                    System.err.println("Cannot merge " + options.getSirius() + ". The specified file might be no valid SIRIUS workspace. You can still specify a new not existing filename to create a new workspace.");
                    System.exit(1);
                    return;
                }
            } else {
                pw = getSiriusOutputWriter(options.getSirius(), getWorkspaceWritingEnvironmentForSirius(options.getSirius()));
            }

            writers.add(pw);
        }

        if (writers.size() > 1) {
            this.projectWriter = new MultipleProjectWriter(writers.toArray(new ProjectWriter[writers.size()]));
        } else if (writers.size() > 0){
            this.projectWriter = writers.get(0);
        } else {
            this.projectWriter = new ProjectWriter() {
                @Override
                public void writeExperiment(ExperimentResult result) throws IOException {
                    // dummy stub
                }

                @Override
                public void close() throws IOException {
                    // dummy stub
                }
            };
        }
    }

    private void checkForValidProjectDirecotry(String output) throws IOException {
        final File f = new File(output);
        if (!f.exists()) return;
        if (!f.isDirectory()) throw new IOException("Expect a directory name. But " + output + " is an existing file.");
        final Pattern pat = Pattern.compile("Sirius", Pattern.CASE_INSENSITIVE);
        boolean empty = true;
        for (File g : f.listFiles()) {
            empty = false;
            if (g.getName().equalsIgnoreCase("version.txt")) {
                for (String line : Files.readLines(g, Charset.forName("UTF-8"))) {
                    if (pat.matcher(line).find()) return;
                }
            }
        }
        if (!empty)
            throw new IOException("Given directory is not a valid SIRIUS workspace. Please specify an empty directory or existing SIRIUS workspace!");
    }

    private void disableShellLogging() {
        Handler ch = null;
        for (Handler h : Logger.getGlobal().getHandlers()) {
            if (h instanceof ConsoleHandler) {
                ch = h;
                break;
            }
        }
        if (ch != null) {
            Logger.getGlobal().removeHandler(ch);
        }
    }

    // TODO: implement merge?
    protected DirectoryWriter.WritingEnvironment getWorkspaceWritingEnvironmentForSirius(String value) {
        try {
            if (value.equals("-")) {
                return new SiriusWorkspaceWriter(System.out);
            } else {
                return new SiriusWorkspaceWriter(new FileOutputStream(new File(value)));
            }
        } catch (FileNotFoundException e) {
            System.err.println("Cannot write into " + value + ". The given file name might already exists.");
            System.exit(1);
            return null;
        }
    }

    protected DirectoryWriter.WritingEnvironment getWorkspaceWritingEnvironmentForDirectoryOutput(String value) throws IOException {
        final File root = new File(value);
        if (root.exists()) {
            System.err.println("Cannot create directory " + root.getName() + ". File already exist.");
            System.exit(1);
            return null;
        }
        root.mkdirs();
        return new SiriusFileWriter(root);
    }

    protected ProjectWriter getSiriusOutputWriter(String sirius, DirectoryWriter.WritingEnvironment env) {
        return new DirectoryWriter(env);
    }

    protected ProjectWriter getDirectoryOutputWriter(String sirius, DirectoryWriter.WritingEnvironment env) {
        return new DirectoryWriter(env);
    }

    protected DirectoryReader.ReadingEnvironment getWorkspaceReadingEnvironmentForSirius(String value) {
        try {
            return new SiriusWorkspaceReader(new File(value));
        } catch (IOException e) {
            System.err.println("Cannot read " + value + ":\n" + e.getMessage() );
            System.exit(1);
            return null;
        }
    }

    protected DirectoryReader.ReadingEnvironment getWorkspaceReadingEnvironmentForDirectoryOutput(String value) {
        final File root = new File(value);
        return new SiriusFileReader(root);
    }

    protected ProjectReader getSiriusOutputReader(String sirius, DirectoryReader.ReadingEnvironment env) {
        return new DirectoryReader(env);
    }

    protected ProjectReader getDirectoryOutputReader(String sirius, DirectoryReader.ReadingEnvironment env) {
        return new DirectoryReader(env);
    }

    public void setup() {
        try {
            this.sirius = new Sirius(options.getProfile());
            final FragmentationPatternAnalysis ms2 = sirius.getMs2Analyzer();
            final IsotopePatternAnalysis ms1 = sirius.getMs1Analyzer();
            final MutableMeasurementProfile ms1Prof = new MutableMeasurementProfile(ms1.getDefaultProfile());
            final MutableMeasurementProfile ms2Prof = new MutableMeasurementProfile(ms2.getDefaultProfile());
            final String outerClassName = getClass().getName();
            ms2.setValidatorWarning(new Warning() {
                @Override
                public void warn(String message) {
                    Logger.getLogger(outerClassName).warning(message);
                }
            });
            if (options.getElements()==null) {
                // autodetect and use default set
                ms1Prof.setFormulaConstraints(getDefaultElementSet(options));
                ms2Prof.setFormulaConstraints(getDefaultElementSet(options));
            } else {
                ms2Prof.setFormulaConstraints(options.getElements());
                ms1Prof.setFormulaConstraints(options.getElements());
            }

            if (options.isAutoCharge()) {
                sirius.setAutoIonMode(true);
            }

            if (options.getMedianNoise()!=null) {
                ms2Prof.setMedianNoiseIntensity(options.getMedianNoise());
            }
            if (options.getPPMMax() != null) {
                ms2Prof.setAllowedMassDeviation(new Deviation(options.getPPMMax()));
                ms1Prof.setAllowedMassDeviation(new Deviation(options.getPPMMax()));
            }
            final TreeBuilder builder = sirius.getMs2Analyzer().getTreeBuilder();
            if (builder == null) {
                String noILPSolver = "Could not load a valid ILP solver (TreeBuilder) " + Arrays.toString(TreeBuilderFactory.getBuilderPriorities()) + ". Please read the installation instructions.";
                LoggerFactory.getLogger(CLI.class).error(noILPSolver);
                System.exit(1);
            }
            LoggerFactory.getLogger(CLI.class).info("Compute trees using " + builder.getDescription());

            sirius.getMs2Analyzer().setDefaultProfile(ms2Prof);
            sirius.getMs1Analyzer().setDefaultProfile(ms1Prof);

            if (options.getPossibleIonizations()!=null){
                List<String> ionList = options.getPossibleIonizations();
                if (ionList.size()==1){
                    ionList = Arrays.asList(ionList.get(0).split(","));
                }
                if (ionList.size()==1){
                    LoggerFactory.getLogger(CLI.class).error("Cannot guess ionization when only one ionization/adduct is provided");
                }
                ionTypes = new PrecursorIonType[ionList.size()];
                for (int i = 0; i < ionTypes.length; i++) {
                    String ion = ionList.get(i);
                    ionTypes[i] = PrecursorIonType.getPrecursorIonType(ion);
                }
            }
        } catch (IOException e) {
            LoggerFactory.getLogger(CLI.class).error("Cannot load profile '" + options.getProfile() + "':\n",e);
            System.exit(1);
        }
    }

    protected Instance setupInstance(Instance inst) {
        final MutableMs2Experiment exp = inst.experiment instanceof MutableMs2Experiment ? (MutableMs2Experiment)inst.experiment : new MutableMs2Experiment(inst.experiment);
        if (exp.getPrecursorIonType()==null || exp.getPrecursorIonType().isIonizationUnknown()) exp.setPrecursorIonType(getIonFromOptions(options, exp.getPrecursorIonType()==null ? 0 : exp.getPrecursorIonType().getCharge()));
        if (formulas!=null && formulas.size()==1) exp.setMolecularFormula(MolecularFormula.parse(formulas.get(0)));
        if (options.getParentMz()!=null) exp.setIonMass(options.getParentMz());
        return new Instance(exp, inst.file);
    }

    public Iterator<Instance> handleInput(final SiriusOptions options) throws IOException {
        final ArrayDeque<Instance> instances = new ArrayDeque<Instance>();

        inputs = options.getInput()==null ? new ArrayList<String>() : options.getInput();
        formulas = options.getFormula();
        // WARNING: if you run sirius --formula C6H12O6 C2H2 bla.ms
        // JewelCli will put the bla.ms into the formulas list
        // however, we can simply check if the entries in the formula list
        // are filenames and, if so, move them back into the input list
        if (formulas!=null) {
            formulas = new ArrayList<>(formulas);
            final ListIterator<String> iter = formulas.listIterator(formulas.size());
            while (iter.hasPrevious()) {
                final String s = iter.previous();
                if (new File(s).exists()) {
                    inputs.add(s);
                    iter.remove();
                } else {
                    break;
                }
            }
        }

        final MsExperimentParser parser = new MsExperimentParser();
        // two different input modes:
        // general information that should be used if this fields are missing in the file
        final Double defaultParentMass = options.getParentMz();
        final FormulaConstraints constraints = options.getElements() == null ? null/*getDefaultElementSet(options, ion)*/ : options.getElements();
        // direct input: --ms1 and --ms2 command line options are given
        if (options.getMs2()!=null && !options.getMs2().isEmpty()) {
            final MutableMeasurementProfile profile = new MutableMeasurementProfile();
            profile.setFormulaConstraints(constraints);
            final MutableMs2Experiment exp = new MutableMs2Experiment();
            final PrecursorIonType ionType = getIonFromOptions(options, 0);
            exp.setPrecursorIonType(ionType);
            exp.setMs2Spectra(new ArrayList<MutableMs2Spectrum>());
            for (File f : foreachIn(options.getMs2())) {
                final Iterator<Ms2Spectrum<Peak>> spiter = SpectralParser.getParserFor(f).parseSpectra(f);
                while (spiter.hasNext()) {
                    final Ms2Spectrum<Peak> spec = spiter.next();
                    if (spec.getIonization()==null || spec.getPrecursorMz()==0 || spec.getMsLevel()==0) {
                        final MutableMs2Spectrum ms;
                        if (spec instanceof MutableMs2Spectrum) ms = (MutableMs2Spectrum)spec;
                        else ms = new MutableMs2Spectrum(spec);
                        if (ms.getIonization()==null) ms.setIonization(ionType.getIonization());
                        if (ms.getMsLevel()==0) ms.setMsLevel(2);
                        if (ms.getPrecursorMz()==0) {
                            if (defaultParentMass==null) {
                                if (exp.getMs2Spectra().size()>0) {
                                    ms.setPrecursorMz(exp.getMs2Spectra().get(0).getPrecursorMz());
                                } else {
                                    final MolecularFormula formula;
                                    if (exp.getMolecularFormula()!=null) formula = exp.getMolecularFormula();
                                    else if (formulas!=null && formulas.size()==1) formula = MolecularFormula.parse(formulas.get(0)); else formula=null;
                                    if (formula != null) {
                                        ms.setPrecursorMz(ms.getIonization().addToMass(formula.getMass()));
                                    } else ms.setPrecursorMz(0);
                                }
                            } else {
                                ms.setPrecursorMz(defaultParentMass);
                            }
                        }
                    }
                    exp.getMs2Spectra().add(new MutableMs2Spectrum(spec));
                }
            }
            if (exp.getMs2Spectra().size() <= 0) throw new IllegalArgumentException("SIRIUS expect at least one MS/MS spectrum. Please add a MS/MS spectrum via --ms2 option");

            if (options.getMs2()!=null &&  options.getMs1() != null && !options.getMs1().isEmpty()) {
                exp.setMs1Spectra(new ArrayList<SimpleSpectrum>());
                for (File f : options.getMs1()) {
                    final Iterator<Ms2Spectrum<Peak>> spiter = SpectralParser.getParserFor(f).parseSpectra(f);
                    while (spiter.hasNext()) {
                        exp.getMs1Spectra().add(new SimpleSpectrum(spiter.next()));
                    }
                }
            }

            final double expPrecursor;
            if (options.getParentMz()!=null) {
                expPrecursor = options.getParentMz();
            }else if (exp.getMolecularFormula()!=null) {
                expPrecursor = exp.getPrecursorIonType().neutralMassToPrecursorMass(exp.getMolecularFormula().getMass());
            } else {
                double prec=0d;
                for (int k=1; k < exp.getMs2Spectra().size(); ++k) {
                    final double pmz = exp.getMs2Spectra().get(k).getPrecursorMz();
                    if (pmz!=0 && Math.abs(pmz - exp.getMs2Spectra().get(0).getPrecursorMz()) > 1e-3) {
                        throw new IllegalArgumentException("The given MS/MS spectra have different precursor mass and cannot belong to the same compound");
                    } else if (pmz != 0) prec = pmz;
                }
                if (prec == 0) {
                    if (exp.getMs1Spectra().size()>0) {
                        final SimpleSpectrum patterns = sirius.getMs1Analyzer().extractPattern(exp, exp.getMergedMs1Spectrum().getMzAt(0));
                        if (patterns.size() < exp.getMergedMs1Spectrum().size()) {
                            throw new IllegalArgumentException("SIRIUS cannot infer the parentmass of the measured compound from MS1 spectrum. Please provide it via the -z option.");
                        }
                        expPrecursor = patterns.getMzAt(0);
                    } else throw new IllegalArgumentException("SIRIUS expects the parentmass of the measured compound as parameter. Please provide it via the -z option.");
                } else expPrecursor = prec;
            }


            exp.setIonMass(expPrecursor);
            if (exp.getName()==null) {
                exp.setName("unknown");
            }
            instances.add(new Instance(exp, options.getMs2().get(0)));
        } else if (options.getMs1()!=null && !options.getMs1().isEmpty()) {
            throw new IllegalArgumentException("SIRIUS expect at least one MS/MS spectrum. Please add a MS/MS spectrum via --ms2 option");
        }
        // batch input: files containing ms1 and ms2 data are given
        if (!inputs.isEmpty()) {
            final Iterator<File> fileIter;
            final ArrayList<File> infiles = new ArrayList<File>();
            for (String f : inputs) {
                final File g = new File(f);
                if (g.isDirectory()) {infiles.addAll(Arrays.asList(g.listFiles(new FileFilter() {
                    @Override
                    public boolean accept(File pathname) {
                        return pathname.isFile();
                    }
                })));} else {
                    infiles.add(g);
                }
            }
            fileIter=infiles.iterator();
            return new Iterator<Instance>() {
                Iterator<Ms2Experiment> experimentIterator = fetchNext();
                File currentFile;
                @Override
                public boolean hasNext() {
                    return !instances.isEmpty();
                }

                @Override
                public Instance next() {
                    fetchNext();
                    Instance c = instances.poll();
                    return setupInstance(new Instance(c.experiment, c.file));
                }

                private Iterator<Ms2Experiment> fetchNext() {
                    while (true) {
                        if (experimentIterator==null || !experimentIterator.hasNext()) {
                            if (fileIter.hasNext()) {
                                currentFile = fileIter.next();
                                try {
                                    GenericParser<Ms2Experiment> p = parser.getParser(currentFile);
                                    if (p==null) {
                                        LoggerFactory.getLogger(CLI.class).error("Unknown file format: '" + currentFile + "'");
                                    } else experimentIterator = p.parseFromFileIterator(currentFile);
                                } catch (IOException e) {
                                    LoggerFactory.getLogger(CLI.class).error("Cannot parse file '" + currentFile + "':\n",e);
                                }
                            } else return null;
                        } else {
                            instances.add(new Instance(experimentIterator.next(), currentFile));
                            return experimentIterator;
                        }
                    }
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        } else {
            return instances.iterator();
        }
    }

    private List<File> foreachIn(List<File> ms2) {
        final List<File> queue = new ArrayList<File>();
        for (File f : ms2) {
            if (f.isDirectory()) {
                for (File g : f.listFiles())
                    if (!g.isDirectory())
                        queue.add(g);
            } else queue.add(f);
        }
        return queue;
    }


    private static final Pattern CHARGE_PATTERN = Pattern.compile("(\\d+)[+-]?");
    private static final Pattern CHARGE_PATTERN2 = Pattern.compile("[+-]?(\\d+)");

    protected static PrecursorIonType getIonFromOptions(SiriusOptions opt, int charge) {
        String ionStr = opt.getIon();
        if (ionStr==null) {
            if (opt.isAutoCharge() || opt.getPossibleIonizations()!=null) return PrecursorIonType.unknown(charge);
            else if (charge==0) throw new IllegalArgumentException("Please specify the charge");
            else if (charge == 1) return PrecursorIonType.getPrecursorIonType("[M+H]+");
            else if (charge == -1) return PrecursorIonType.getPrecursorIonType("[M-H]-");
            else throw new IllegalArgumentException("SIRIUS does not support multiple charges");
        } else {
            final PrecursorIonType ionType = PeriodicTable.getInstance().ionByName(opt.getIon());
            if (ionType.isIonizationUnknown() && !opt.isAutoCharge()) {
                if (ionType.getCharge()>0) return PrecursorIonType.getPrecursorIonType("[M+H]+");
                else return PrecursorIonType.getPrecursorIonType("[M-H]-");
            } else return ionType;
        }
    }
    private final static FormulaConstraints DEFAULT_ELEMENTS = new FormulaConstraints("CHNOP[5]S");
    public FormulaConstraints getDefaultElementSet(SiriusOptions opts) {
        final FormulaConstraints cf = (opts.getElements()!=null) ? opts.getElements() : DEFAULT_ELEMENTS;
        return cf;
    }

    protected static String fileNameWithoutExtension(File file) {
        final String name = file.getName();
        final int i = name.lastIndexOf('.');
        if (i>=0) return name.substring(0, i);
        else return name;
    }



    private <T> boolean arrayContains(T[] array, T object) {
        return this.arrayFind(array, object) >= 0;
    }

    private <T> int arrayFind(T[] array, T object) {
        for(int i = 0; i < array.length; ++i) {
            Object t = array[i];
            if(t.equals(object)) {
                return i;
            }
        }

        return -1;
    }


    private int countMatches(String string, String sub){
        int lastIndex = 0;
        int count = 0;

        while (lastIndex != -1) {
            lastIndex = string.indexOf(sub, lastIndex);
            if (lastIndex != -1) {
                count++;
                lastIndex += sub.length();
            }
        }
        return count;
    }
}
