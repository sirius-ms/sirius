package de.unijena.bioinf.ms.cli;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.ChemistryBase.properties.PropertyManager;
import de.unijena.bioinf.ChemistryBase.sirius.projectspace.Index;
import de.unijena.bioinf.babelms.GenericParser;
import de.unijena.bioinf.babelms.MsExperimentParser;
import de.unijena.bioinf.babelms.SpectralParser;
import de.unijena.bioinf.fingerid.FingeridProjectSpaceFactory;
import de.unijena.bioinf.fingerid.net.WebAPI;
import de.unijena.bioinf.fingerworker.WorkerList;
import de.unijena.bioinf.ms.cli.parameters.*;
import de.unijena.bioinf.ms.cli.utils.FormatedTableBuilder;
import de.unijena.bioinf.sirius.Sirius;
import de.unijena.bioinf.sirius.core.ApplicationCore;
import de.unijena.bioinf.sirius.projectspace.*;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.*;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;


/**
 * This is out Commandline tool
 * <p>
 * Here we parse parameters, configure technical stuff,
 * read input, merge workspace, configure Algorithms/Workflows and wirte output.
 * <p>
 * Basic Idea:
 * <p>
 * Note: A Workpace can be input and output at the same time!
 * Some methods will use it as input and check whether
 * the needed input is present (e.g Zodiac).
 * Other Methods only produce output to the Workspace (e.g. SIRIUS).
 * So they need to merge their results with the existing ones.
 */
public class CLI extends ApplicationCore {

    protected ProjectWriter projectWriter;
    protected static boolean shellOutputSurpressed = false; //todo extra Utils class?

    protected org.slf4j.Logger logger = LoggerFactory.getLogger(CombinedCLI.class);

    protected Workflow workflow;
    protected SiriusInstanceProcessor siriusInstanceProcessor;
    protected Sirius sirius;


    BasicOptions basicOptions = new BasicOptions();
    SiriusOptions siriusOptions = new SiriusOptions();
    ZodiacOptions zodiacOptions = new ZodiacOptions();
    FingerIdOptions fingeridOptions = new FingerIdOptions();
    CanopusOptions canopusOptions = new CanopusOptions();

    protected int instanceIdOffset; //index offset if project space is merged


    public void compute() {
        final long time = System.currentTimeMillis();
        try {
            if (workflow instanceof ZodiacWorkflow) {
                Path workspace = Paths.get(basicOptions.workspaceZip);
                List<ExperimentResult> experimentResults = loadWorkspace(workspace.toFile());
                workflow.compute(experimentResults.iterator());
            } else {
                Iterator<Instance> instanceIterator = handleInput();
                workflow.compute(instanceIterator);
            }
        } catch (IOException e) {
            logger.error("Error while handling the input data", e);
        } finally {
            if (projectWriter != null) try {
                projectWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            logger.info("Computation time: " + (double) (System.currentTimeMillis() - time) / 1000d + "s");
        }
    }


    //////////////////////////////////////////////////
    // init
    ////////////////////////////////////////////////////

    protected void cite(final CommandLine.Model.CommandSpec spec) {
        System.out.println(spec.usageMessage().footerHeading());
        for (String footerLine : spec.usageMessage().footer()) {
            System.out.println(footerLine);
        }
    }

    private void fingerIDInfo() {
        System.out.println();

        WorkerList info = WebAPI.INSTANCE.getWorkerInfo();
        if (info != null) {
            System.out.println("Active worker instances: ");
            System.out.println();
            final FormatedTableBuilder align = new FormatedTableBuilder();
            // header
            align.addLine("ID", "Type", "Predictors", "Version", "Host", "Pulse");

            info.forEach((workerInfo) ->
                    // data
                    align.addLine(String.valueOf(workerInfo.id), workerInfo.workerType.name(), workerInfo.predictors.toString(), workerInfo.version, workerInfo.hostname, String.valueOf(workerInfo.getPulse()))
            );

            // output
            align.output(System.out::println);

//            System.out.println();
            System.out.println("Number of pending jobs: " + info.getPendingJobs());
        }
    }

    protected void parseArgsAndInit(String[] args) {
        parseArgs(args);
        if (!workflow.setup()) System.exit(1);
        if (!workflow.validate()) System.exit(1);
    }

    protected CommandLine.Model.CommandSpec configureCommandLine() {
        final CommandLine.Model.CommandSpec spec = CommandLine.Model.CommandSpec.forAnnotatedObject(basicOptions);
        spec.addMixin("SIRIUS", CommandLine.Model.CommandSpec.forAnnotatedObject(siriusOptions));
        spec.addMixin("Zodiac", CommandLine.Model.CommandSpec.forAnnotatedObject(zodiacOptions));
        spec.addMixin("FingerID", CommandLine.Model.CommandSpec.forAnnotatedObject(fingeridOptions));
        spec.addMixin("Canopus", CommandLine.Model.CommandSpec.forAnnotatedObject(canopusOptions));

        spec.usageMessage().footerHeading("Please cite the following publications when using our tool:");
        spec.usageMessage().footer(ApplicationCore.CITATION);

        return spec;
    }

    protected void parseArgs(String[] args) {

        final CommandLine.Model.CommandSpec spec = configureCommandLine();
        final CommandLine.ParseResult parseResult = spec.commandLine().parseArgs(args);
        //printing version or usage help
        if (CommandLine.printHelpIfRequested(parseResult))
            System.exit(0);
        if (basicOptions.cite) { //todo this should be the header or footer of the tool
            cite(spec);
            System.exit(0);
        }
        if (fingeridOptions.fingeridInfo) {
            fingerIDInfo();
            cite(spec);
            System.exit(0);
        }

        //run application
        if (basicOptions.numOfCores > 0) {
            PropertyManager.PROPERTIES.setProperty("de.unijena.bioinf.sirius.cpu.cores", String.valueOf(basicOptions.numOfCores));
        }

        //configure file formatter for workspace
        FilenameFormatter filenameFormatter = null;
        if (basicOptions.workspaceNamingConvention != null) {
            try {
                filenameFormatter = new StandardMSFilenameFormatter(basicOptions.workspaceNamingConvention);
            } catch (ParseException e) {
                logger.error("Cannot parse naming convention: " + basicOptions.workspaceNamingConvention + System.lineSeparator() + e.getMessage(), e);
                System.exit(1);
            }
        } else {
            //default
            filenameFormatter = new StandardMSFilenameFormatter();
        }


        //configure output

        handleOutputOptions(new FingeridProjectSpaceFactory(filenameFormatter));

        siriusInstanceProcessor = new SiriusInstanceProcessor(options);
        siriusInstanceProcessor.setup(); //todo don't setup twice
        sirius = siriusInstanceProcessor.getSirius();


        //decide for workflow
        if (zodiacOptions.zodiac && fingeridOptions.fingerid) {
            logger.error("ZODIAC + CSI:FingerID is currently not supported");
            System.exit(1);
        } else if (zodiacOptions.zodiac) {
            //todo better differentiate between reading from workspace and additionally running SIRIUS
            if (options.getInput() == null || options.getInput().size() == 0) {
                //todo tis might fail due to jewelCLI bug: see handleInput
                workflow = new ZodiacWorkflow(options);
            } else {
                logger.error("SIRIUS + ZODIAC in one run is currently not supported");
                System.exit(1);
            }

//                ...;
//            } else if (options.isFingerid()){
//                ...;
        } else {
            FingerIdInstanceProcessor fingerIdInstanceProcessor = new FingerIdInstanceProcessor(options);
            workflow = new FingerIdWorkflow(siriusInstanceProcessor, fingerIdInstanceProcessor, options, projectWriter);
        }


    }


    /*private String[] fixBuggyJewelCliLibrary(String[] args) {
        final List<String> argsCopy = new ArrayList<>();
        final List<String> ionModeStrings = new ArrayList<>();
        boolean ionIn = false;
        for (int i = 0; i < args.length; ++i) {
            String arg = args[i];
            if (arg.equals("--ion") || arg.equals("-i")) {
                if (!ionIn) {
                    ionModeStrings.add(arg);
                    ionIn = true;
                }
                final Pattern ionPattern = Pattern.compile("^\\s*\\[?\\s*M\\s*[+-\\]]");
                // if a list parameter is last parameter, we have to distinguish it from the rest parameter
                for (i = i + 1; i < args.length; ++i) {
                    arg = args[i];
                    if (ionPattern.matcher(arg).find()) {
                        ionModeStrings.add(arg);
                    } else {
                        break;
                    }
                }
            }
            argsCopy.add(arg);
        }
        if (ionModeStrings.size() > 0) ionModeStrings.add("--placeholder");
        ionModeStrings.addAll(argsCopy);
        return ionModeStrings.toArray(new String[ionModeStrings.size()]);
    }*/

    protected void handleOutputOptions(ReaderWriterFactory readerWriterFactory) {

        //output and logging
        if (basicOptions.quiet || "-".equals(basicOptions.workspaceZip)) {
            this.shellOutputSurpressed = true;
            disableShellLogging();
        }

        if ("-".equals(basicOptions.workspaceDir)) {
            logger.error("Cannot write output files and folders into standard output stream. Please use --workspace|-w to write a zip file of the SIRIUS output into the standard output stream.");
            System.exit(1);
        }

        //todo ZODIAC hack
        if (!zodiacOptions.zodiac) {
            try {
                ProjectSpaceUtils.ProjectWriterInfo projectWriterInfo = ProjectSpaceUtils.getProjectWriter(options.getOutput(), options.getSirius(), readerWriterFactory);
                this.projectWriter = projectWriterInfo.getProjectWriter();
                this.instanceIdOffset = projectWriterInfo.getNumberOfWrittenExperiments();
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
                System.exit(1);
            }
        }


    }

    private ConsoleHandler getConsoleHandler() {
        for (Handler h : Logger.getGlobal().getHandlers()) {
            if (h instanceof ConsoleHandler) {
                return (ConsoleHandler) h;
            }

        }
        return null;
    }

    private void disableShellLogging() {
        //todo shellOutputLogger will have specific ConsoleHandler?

        Handler ch = getConsoleHandler();
        if (ch != null) {
            Logger.getGlobal().removeHandler(ch);
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // parse input, setup each instance
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    //load workspace for Zodiac //todo integrate better
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

    protected Iterator<Instance> handleInput() throws IOException {


        //todo users should use delimiter (--) instead
        /*formulas = options.getFormula();
        // WARNING: if you run sirius --formula C6H12O6 C2H2 bla.ms
        // JewelCli will put the bla.ms into the formulas list
        // however, we can simply check if the entries in the formula list
        // are filenames and, if so, move them back into the input list
        if (formulas != null) {
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
        }*/


        // two different input modes:
        // general information that should be used if this fields are missing in the file
//        final Double defaultParentMass = options.getParentMz(); todo not needed becaus part of options class
//        final FormulaConstraints constraints = options.getElements() == null ? null/*getDefaultElementSet(options, ion)*/ : options.getElements();todo not needed becaus part of options class

        final ArrayDeque<Instance> instances = new ArrayDeque<Instance>();

        ///////////////////////////////////////////////////////////////////
        // direct input: --ms1 and --ms2 command line options are given
        ///////////////////////////////////////////////////////////////////
        if (basicOptions.ms2 != null && !basicOptions.ms2.isEmpty()) {
            final MutableMs2Experiment exp = new MutableMs2Experiment();
            exp.setSource(basicOptions.ms2.get(0));
            exp.setPrecursorIonType(siriusOptions.getIonFromOption(0));

            exp.setMs2Spectra(new ArrayList<MutableMs2Spectrum>());
            for (File f : foreachIn(basicOptions.ms2)) {
                final Iterator<Ms2Spectrum<Peak>> spiter = SpectralParser.getParserFor(f).parseSpectra(f);
                while (spiter.hasNext()) {
                    final Ms2Spectrum<Peak> spec = spiter.next();
                    if (spec.getIonization() == null || spec.getPrecursorMz() == 0 || spec.getMsLevel() == 0) {
                        final MutableMs2Spectrum ms;
                        if (spec instanceof MutableMs2Spectrum) ms = (MutableMs2Spectrum) spec;
                        else ms = new MutableMs2Spectrum(spec);
                        if (ms.getIonization() == null) ms.setIonization(exp.getPrecursorIonType().getIonization());
                        if (ms.getMsLevel() == 0) ms.setMsLevel(2);
                        if (ms.getPrecursorMz() == 0) {
                            if (siriusOptions.parentMz == null) {
                                if (exp.getMs2Spectra().size() > 0) {
                                    ms.setPrecursorMz(exp.getMs2Spectra().get(0).getPrecursorMz());
                                } else {
                                    final MolecularFormula formula;
                                    if (exp.getMolecularFormula() != null) formula = exp.getMolecularFormula();
                                    else if (siriusOptions.formula != null && siriusOptions.formula.size() == 1)
                                        formula = MolecularFormula.parse(siriusOptions.formula.get(0));
                                    else formula = null;
                                    if (formula != null) {
                                        ms.setPrecursorMz(ms.getIonization().addToMass(formula.getMass()));
                                    } else ms.setPrecursorMz(0);
                                }
                            } else {
                                ms.setPrecursorMz(siriusOptions.parentMz);
                            }
                        }
                    }
                    exp.getMs2Spectra().add(new MutableMs2Spectrum(spec));
                }
            }

            if (exp.getMs2Spectra().size() <= 0)
                throw new IllegalArgumentException("SIRIUS expect at least one MS/MS spectrum. Please add a MS/MS spectrum via --ms2 option");

            if (basicOptions.ms1 != null && !basicOptions.ms1.isEmpty()) {
                exp.setMs1Spectra(new ArrayList<SimpleSpectrum>());
                for (File f : basicOptions.ms1) {
                    final Iterator<Ms2Spectrum<Peak>> spiter = SpectralParser.getParserFor(f).parseSpectra(f);
                    while (spiter.hasNext()) {
                        exp.getMs1Spectra().add(new SimpleSpectrum(spiter.next()));
                    }
                }
                exp.setMergedMs1Spectrum(exp.getMs1Spectra().get(0));
            }


            final double expPrecursor;
            if (siriusOptions.parentMz != null) {
                expPrecursor = siriusOptions.parentMz;
            } else if (exp.getMolecularFormula() != null) {
                expPrecursor = exp.getPrecursorIonType().neutralMassToPrecursorMass(exp.getMolecularFormula().getMass());
            } else {
                double prec = 0d;
                for (int k = 1; k < exp.getMs2Spectra().size(); ++k) {
                    final double pmz = exp.getMs2Spectra().get(k).getPrecursorMz();
                    if (pmz != 0 && Math.abs(pmz - exp.getMs2Spectra().get(0).getPrecursorMz()) > 1e-3) {
                        throw new IllegalArgumentException("The given MS/MS spectra have different precursor mass and cannot belong to the same compound");
                    } else if (pmz != 0) prec = pmz;
                }
                if (prec == 0) {
                    if (exp.getMs1Spectra().size() > 0) {
                        final SimpleSpectrum patterns = sirius.getMs1Analyzer().extractPattern(exp, exp.getMergedMs1Spectrum().getMzAt(0));
                        if (patterns.size() < exp.getMergedMs1Spectrum().size()) {
                            throw new IllegalArgumentException("SIRIUS cannot infer the parentmass of the measured compound from MS1 spectrum. Please provide it via the -z option.");
                        }
                        expPrecursor = patterns.getMzAt(0);
                    } else
                        throw new IllegalArgumentException("SIRIUS expects the parentmass of the measured compound as parameter. Please provide it via the -z option.");
                } else expPrecursor = prec;
            }


            exp.setIonMass(expPrecursor);
            if (exp.getName() == null) {
                exp.setName("unknown");
            }

            Instance instance = new Instance(exp, basicOptions.ms2.get(0), ++instanceIdOffset);
            instances.add(setupInstance(instance));
        } else if (basicOptions.ms1 != null && !basicOptions.ms1.isEmpty()) {
            throw new IllegalArgumentException("SIRIUS expect at least one MS/MS spectrum. Please add a MS/MS spectrum via --ms2 option");
        }


        ///////////////////////////////////////////////////////////////////
        // batch input: files containing ms1 and ms2 data are given
        ///////////////////////////////////////////////////////////////////
        if (!basicOptions.input.isEmpty()) {
            final Iterator<File> fileIter;
            final ArrayList<File> infiles = new ArrayList<File>();
            Collections.sort(basicOptions.input);
            for (String f : basicOptions.input) {
                final File g = new File(f);
                if (g.isDirectory()) {
                    File[] ins = g.listFiles(pathname -> pathname.isFile());
                    if (ins != null) {
                        Arrays.sort(ins, Comparator.comparing(File::getName));
                        infiles.addAll(Arrays.asList(ins));
                    }
                } else {
                    infiles.add(g);
                }
            }
            fileIter = infiles.iterator();


            return new Iterator<Instance>() {
                final MsExperimentParser parser = new MsExperimentParser();
                File currentFile;
                int index = instanceIdOffset;
                Iterator<Ms2Experiment> experimentIterator = fetchNext();

                @Override
                public boolean hasNext() {
                    return !instances.isEmpty();
                }

                @Override
                public Instance next() {
                    fetchNext();
                    Instance c = instances.poll();
                    return setupInstance(c);
                }

                private Iterator<Ms2Experiment> fetchNext() {
                    start:
                    while (true) {
                        if (experimentIterator == null || !experimentIterator.hasNext()) {
                            if (fileIter.hasNext()) {
                                currentFile = fileIter.next();
                                try {
                                    GenericParser<Ms2Experiment> p = parser.getParser(currentFile);
                                    if (p == null) {
                                        logger.error("Unknown file format: '" + currentFile + "'");
                                    } else experimentIterator = p.parseFromFileIterator(currentFile);
                                } catch (IOException e) {
                                    logger.error("Cannot parse file '" + currentFile + "':\n", e);
                                }
                            } else return null;
                        } else {
                            MutableMs2Experiment experiment = sirius.makeMutable(experimentIterator.next());
                            if (basicOptions.maxMz != null)
                                //skip high-mass compounds
                                if (experiment.getIonMass() > basicOptions.maxMz) continue start;

                            Index expIndex = experiment.getAnnotation(Index.class);
                            int currentIndex;
                            if (instanceIdOffset == 0 && expIndex != null && expIndex.index >= 0) {
                                //if no workspaces are merged and parser provides real index, use if
                                currentIndex = expIndex.index;
                            } else {
                                //normal fallback
                                currentIndex = ++index;
                            }

                            Instance instance = new Instance(experiment, currentFile, currentIndex);
                            instances.add(instance);
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


    /////////////////////////////////////////////////////////////////////////////////////////////
    /// set data and Sirius dependent parameters
    ////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * add here (instance specific) parameters
     *
     * @param inst
     * @return
     */
    protected Instance setupInstance(Instance inst) {
        final MutableMs2Experiment exp = inst.experiment instanceof MutableMs2Experiment ? (MutableMs2Experiment) inst.experiment : new MutableMs2Experiment(inst.experiment);
        siriusOptions.setParamatersToExperiment(exp);

        return new Instance(exp, inst.file, inst.index);
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


//    private static final Pattern CHARGE_PATTERN = Pattern.compile("(\\d+)[+-]?");
//    private static final Pattern CHARGE_PATTERN2 = Pattern.compile("[+-]?(\\d+)");


}
