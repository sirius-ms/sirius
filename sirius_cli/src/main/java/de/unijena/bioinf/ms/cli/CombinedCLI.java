package de.unijena.bioinf.ms.cli;

import de.unijena.bioinf.sirius.core.ApplicationCore;

public class CombinedCLI extends ApplicationCore {

   /* protected ProjectWriter projectWriter;
    protected static boolean shellOutputSurpressed = false; //todo extra Utils class?

    protected org.slf4j.Logger logger = LoggerFactory.getLogger(CombinedCLI.class);

    protected Workflow workflow;
    protected SiriusInstanceProcessor siriusInstanceProcessor;
    protected Sirius sirius;


    public CombinedCLI() {
    }

    CombinedOptions options;
    List<String> inputs, formulas;
    protected int instanceIdOffset; //index offset if project space is merged

    public void compute() {
        final long time = System.currentTimeMillis();
        try {
            if (workflow instanceof ZodiacWorkflow) {
//                if (!(new ZodiacInstanceProcessor(options)).validate()){
//                    System.exit(1);
//                }
                Path workspace = Paths.get(options.getSirius());
                List<ExperimentResult> experimentResults = newLoad(workspace.toFile());
                workflow.compute(experimentResults.iterator());
            } else {
                Iterator<Instance> instanceIterator = handleInput(options);
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

    protected void cite() {
        System.out.println("Please cite the following paper when using our method:");
        System.out.println(ApplicationCore.CITATION);
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

    protected void parseArgs(String[] args) {
        if (args.length == 0) {
            System.out.println(CliFactory.createCli(FingerIdOptions.class).getHelpMessage());
            System.exit(0);
        }
        try {
            args = fixBuggyJewelCliLibrary(args);
            this.options = CliFactory.createCli(CombinedOptions.class).parseArguments(args);
            if (options.isCite()) {
                cite();
                System.exit(0);
            } else if (options.isZodiac() && args.length == 1) {
                System.out.println(CliFactory.createCli(ZodiacOptions.class).getHelpMessage());
                System.exit(0);
            }
        } catch (HelpRequestedException e) {
            System.out.println(e.getMessage());
            System.out.println("");
            cite();
            System.exit(0);
        }
        if (options.isVersion()) {
            cite();
            System.exit(0);
        }
        if (options.isFingeridInfo()) {
            fingerIDInfo();
            System.exit(0);
        }

        FilenameFormatter filenameFormatter = null;
        if (options.getNamingConvention() != null) {
            String formatString = options.getNamingConvention();
            try {
                filenameFormatter = new StandardMSFilenameFormatter(formatString);
            } catch (ParseException e) {
                logger.error("Cannot parse naming convention:\n" + e.getMessage(), e);
                System.exit(1);
            }
        } else {
            //default
            filenameFormatter = new StandardMSFilenameFormatter();
        }

        handleOutputOptions(options, new FingeridProjectSpaceFactory(filenameFormatter));

        siriusInstanceProcessor = new SiriusInstanceProcessor(options);
        siriusInstanceProcessor.setup(); //todo don't setup twice
        sirius = siriusInstanceProcessor.getSirius();

        //decide for workflow
        if (options.isZodiac() && options.isFingerid()) {
            logger.error("ZODIAC + CSI:FingerID is currently not supported");
            System.exit(1);
        } else if (options.isZodiac()) {
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


    private String[] fixBuggyJewelCliLibrary(String[] args) {
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
    }

    protected void handleOutputOptions(CombinedOptions options, ReaderWriterFactory readerWriterFactory) {
        if (options.getNumOfCores() > 0) {
            PropertyManager.PROPERTIES.setProperty("de.unijena.bioinf.sirius.cpu.cores", String.valueOf(options.getNumOfCores()));
        }

        //output and logging
        if (options.isQuiet() || "-".equals(options.getSirius())) {
            this.shellOutputSurpressed = true;
            disableShellLogging();
        } else {
            //setup former print() to shell by logging
            //todo do it right
//            ConsoleHandler ch = getConsoleHandler();//todo this one always outputs into err
//            Logger logger = Logger.getLogger("shellOutputLogger");//todo do better
//            logger.removeHandler(ch);
//            Handler shellResultHandler = new ResultOutputHandler();
//            //todo both necessary?
//            Logger.getGlobal().addHandler(shellResultHandler);
//            logger.addHandler(shellResultHandler);
        }

        if ("-".equals(options.getOutput())) {
            logger.error("Cannot write output files and folders into standard output stream. Please use --sirius t get a zip file of SIRIUS output into the standard output stream");
            System.exit(1);
        }

        //todo ZODIAC hack
        if (!options.isZodiac()) {
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
    protected List<ExperimentResult> newLoad(File file) throws IOException {
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

    protected Iterator<Instance> handleInput(final SiriusOptions options) throws IOException {
        final ArrayDeque<Instance> instances = new ArrayDeque<Instance>();

        inputs = options.getInput() == null ? new ArrayList<String>() : options.getInput();
        formulas = options.getFormula();
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
        }

        final MsExperimentParser parser = new MsExperimentParser();
        // two different input modes:
        // general information that should be used if this fields are missing in the file
        final Double defaultParentMass = options.getParentMz();
        final FormulaConstraints constraints = options.getElements() == null ? null*//*getDefaultElementSet(options, ion)*//* : options.getElements();
        // direct input: --ms1 and --ms2 command line options are given
        if (options.getMs2() != null && !options.getMs2().isEmpty()) {
            final MutableMeasurementProfile profile = new MutableMeasurementProfile();
            profile.setFormulaConstraints(constraints);
            final MutableMs2Experiment exp = new MutableMs2Experiment();
            exp.setSource(options.getMs2().get(0));
            final PrecursorIonType ionType = getIonFromOptions(options, 0);
            exp.setPrecursorIonType(ionType);
            exp.setMs2Spectra(new ArrayList<MutableMs2Spectrum>());
            for (File f : foreachIn(options.getMs2())) {
                final Iterator<Ms2Spectrum<Peak>> spiter = SpectralParser.getParserFor(f).parseSpectra(f);
                while (spiter.hasNext()) {
                    final Ms2Spectrum<Peak> spec = spiter.next();
                    if (spec.getIonization() == null || spec.getPrecursorMz() == 0 || spec.getMsLevel() == 0) {
                        final MutableMs2Spectrum ms;
                        if (spec instanceof MutableMs2Spectrum) ms = (MutableMs2Spectrum) spec;
                        else ms = new MutableMs2Spectrum(spec);
                        if (ms.getIonization() == null) ms.setIonization(ionType.getIonization());
                        if (ms.getMsLevel() == 0) ms.setMsLevel(2);
                        if (ms.getPrecursorMz() == 0) {
                            if (defaultParentMass == null) {
                                if (exp.getMs2Spectra().size() > 0) {
                                    ms.setPrecursorMz(exp.getMs2Spectra().get(0).getPrecursorMz());
                                } else {
                                    final MolecularFormula formula;
                                    if (exp.getMolecularFormula() != null) formula = exp.getMolecularFormula();
                                    else if (formulas != null && formulas.size() == 1)
                                        formula = MolecularFormula.parse(formulas.get(0));
                                    else formula = null;
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
            if (exp.getMs2Spectra().size() <= 0)
                throw new IllegalArgumentException("SIRIUS expect at least one MS/MS spectrum. Please add a MS/MS spectrum via --ms2 option");

            if (options.getMs2() != null && options.getMs1() != null && !options.getMs1().isEmpty()) {
                exp.setMs1Spectra(new ArrayList<SimpleSpectrum>());
                for (File f : options.getMs1()) {
                    final Iterator<Ms2Spectrum<Peak>> spiter = SpectralParser.getParserFor(f).parseSpectra(f);
                    while (spiter.hasNext()) {
                        exp.getMs1Spectra().add(new SimpleSpectrum(spiter.next()));
                    }
                }
                exp.setMergedMs1Spectrum(exp.getMs1Spectra().get(0));
            }

            final double expPrecursor;
            if (options.getParentMz() != null) {
                expPrecursor = options.getParentMz();
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
            if (constraints != null) {
                sirius.setFormulaConstraints(exp, constraints);
            }
            if (options.isDisableElementDetection()) {
                sirius.enableAutomaticElementDetection(exp, false);
            }
            Instance instance = new Instance(exp, options.getMs2().get(0), ++instanceIdOffset);
            instances.add(setupInstance(instance));
        } else if (options.getMs1() != null && !options.getMs1().isEmpty()) {
            throw new IllegalArgumentException("SIRIUS expect at least one MS/MS spectrum. Please add a MS/MS spectrum via --ms2 option");
        }
        // batch input: files containing ms1 and ms2 data are given
        if (!inputs.isEmpty()) {
            final Iterator<File> fileIter;
            final ArrayList<File> infiles = new ArrayList<File>();
            Collections.sort(inputs);
            for (String f : inputs) {
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
                            if (options.getMaxMz() != null) {
                                //skip high-mass compounds
                                if (experiment.getIonMass() > options.getMaxMz()) continue start;
                            }
                            if (constraints != null) {
                                sirius.setFormulaConstraints(experiment, constraints);
                            }
                            if (options.isDisableElementDetection()) {
                                sirius.enableAutomaticElementDetection(experiment, false);

                            }

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

    *//**
     * add here (instance specific) parameters
     *
     * @param inst
     * @return
     *//*
    protected Instance setupInstance(Instance inst) {
        final MutableMs2Experiment exp = inst.experiment instanceof MutableMs2Experiment ? (MutableMs2Experiment) inst.experiment : new MutableMs2Experiment(inst.experiment);
        if (exp.getPrecursorIonType() == null || exp.getPrecursorIonType().isIonizationUnknown())
            exp.setPrecursorIonType(getIonFromOptions(options, exp.getPrecursorIonType() == null ? 0 : exp.getPrecursorIonType().getCharge()));

        if (formulas != null && formulas.size() == 1) exp.setMolecularFormula(MolecularFormula.parse(formulas.get(0)));

        Set<MolecularFormula> whiteSet = getFormulaWhiteset(exp, formulas);
        if (whiteSet != null) sirius.setFormulaSearchList(exp, whiteSet);


        if (options.getParentMz() != null) exp.setIonMass(options.getParentMz());

        if (options.getIsolationWindowWidth() != null) {
            final double width = options.getIsolationWindowWidth();
            final double shift = options.getIsolationWindowShift();
            final double right = width / 2d + shift;
            final double left = -width / 2d + shift;
            SimpleRectangularIsolationWindow isolationWindow = new SimpleRectangularIsolationWindow(left, right);
            exp.setAnnotation(IsolationWindow.class, isolationWindow);
        }


        //only keep most intense ms2 (hack used for bad data)
        if (options.isMostIntenseMs2()) {
            onlyKeepMostIntenseMS2(inst.experiment);
        }


        sirius.setTimeout(exp, options.getInstanceTimeout(), options.getTreeTimeout());

        setIonModeStuff(exp);

        sirius.enableRecalibration(exp, !options.isNotRecalibrating());
        sirius.setIsotopeMode(exp, options.getIsotopes());

        return new Instance(exp, inst.file, inst.index);
    }

    private void setIonModeStuff(MutableMs2Experiment exp) {
        PossibleIonModes.GuessingMode enabledGuessingMode = options.isTrustGuessIonFromMS1() ? PossibleIonModes.GuessingMode.SELECT : PossibleIonModes.GuessingMode.ADD_IONS;
        if (options.isAutoCharge()) { //TODO: add optiosn.getIon into this case
            if (exp.getPrecursorIonType().isIonizationUnknown()) {
                exp.setAnnotation(PossibleAdducts.class, null);
                setPrecursorIonTypes(exp, new PossibleAdducts(Iterables.toArray(PeriodicTable.getInstance().getKnownLikelyPrecursorIonizations(exp.getPrecursorIonType().getCharge()), PrecursorIonType.class)), enabledGuessingMode, true);
            } else {
                setPrecursorIonTypes(exp, new PossibleAdducts(exp.getPrecursorIonType()), PossibleIonModes.GuessingMode.DISABLED, false);
            }
            //todo whats with options.getIon().size() == 1 ?
        } else if (options.getIon() != null && options.getIon().size() > 1) {
            if (exp.getPrecursorIonType().isIonizationUnknown()) {
                final List<PrecursorIonType> ionTypes = new ArrayList<>();
                for (String ion : options.getIon()) ionTypes.add(PrecursorIonType.getPrecursorIonType(ion));
                setPrecursorIonTypes(exp, new PossibleAdducts(ionTypes), enabledGuessingMode, false);
            } else {
                setPrecursorIonTypes(exp, new PossibleAdducts(exp.getPrecursorIonType()), PossibleIonModes.GuessingMode.DISABLED, false);
            }
        } else {
            if (exp.getPrecursorIonType().isIonizationUnknown()) {
                setPrecursorIonTypes(exp, new PossibleAdducts(exp.getPrecursorIonType().getCharge() > 0 ? PrecursorIonType.getPrecursorIonType("[M+H]+") : PrecursorIonType.getPrecursorIonType("[M-H]-")), enabledGuessingMode, true); // TODO: ins MS1 gucken
            } else {
                setPrecursorIonTypes(exp, new PossibleAdducts(exp.getPrecursorIonType()), PossibleIonModes.GuessingMode.DISABLED, false);
            }
        }
    }

    private void setPrecursorIonTypes(MutableMs2Experiment exp, PossibleAdducts pa, PossibleIonModes.GuessingMode guessingMode, boolean preferProtonation) {
        exp.setAnnotation(PossibleAdducts.class, pa);
        PossibleIonModes im = exp.getAnnotation(PossibleIonModes.class, new PossibleIonModes());
        im.setGuessFromMs1(guessingMode);


        if (preferProtonation) {
            if (guessingMode.isEnabled())
                im.enableGuessFromMs1WithCommonIonModes(exp.getPrecursorIonType().getCharge());
            final Set<Ionization> ionModes = new HashSet<>(pa.getIonModes());
            for (Ionization ion : ionModes) {
                im.add(ion, 0.02);
            }
            if (exp.getPrecursorIonType().getCharge() > 0) {
                im.add(PrecursorIonType.getPrecursorIonType("[M+H]+").getIonization(), 1d);
            } else {
                im.add(PrecursorIonType.getPrecursorIonType("[M-H]-").getIonization(), 1d);

            }
        } else {
            final Set<Ionization> ionModes = new HashSet<>(pa.getIonModes());
            for (Ionization ion : ionModes) {
                im.add(ion, 1d);
            }
        }

        exp.setAnnotation(PossibleIonModes.class, im);
    }


    protected Set<MolecularFormula> getFormulaWhiteset(Ms2Experiment experiment, List<String> whitelist) {
        if ((new FingerIdInstanceProcessor(options)).isOffline()) {
            return getFormulaWhitesetNoDB(experiment, whitelist);
        } else {
            return getFormulaWhitesetWithDB(experiment, whitelist);
        }
    }


    *//*
    remove all but the most intense ms2
     *//*
    protected void onlyKeepMostIntenseMS2(MutableMs2Experiment experiment) {
        if (experiment.getMs2Spectra().size() == 0) return;
        double precursorMass = experiment.getIonMass();
        int mostIntensiveIdx = -1;
        double maxIntensity = -1d;
        int pos = -1;
        if (experiment.getMs1Spectra().size() == experiment.getMs2Spectra().size()) {
            //one ms1 corresponds to one ms2. we take ms2 with most intense ms1 precursor peak
            for (Spectrum<Peak> spectrum : experiment.getMs1Spectra()) {
                ++pos;
                Deviation dev = new Deviation(100);
                int idx = Spectrums.mostIntensivePeakWithin(spectrum, precursorMass, dev);
                if (idx < 0) continue;
                double intensity = spectrum.getIntensityAt(idx);
                if (intensity > maxIntensity) {
                    maxIntensity = intensity;
                    mostIntensiveIdx = pos;
                }
            }
        }
        if (mostIntensiveIdx < 0) {
            //take ms2 with highest summed intensity
            pos = -1;
            for (Spectrum<Peak> spectrum : experiment.getMs2Spectra()) {
                ++pos;
                final int n = spectrum.size();
                double sumIntensity = 0d;
                for (int i = 0; i < n; ++i) {
                    sumIntensity += spectrum.getIntensityAt(i);
                }
                if (sumIntensity > maxIntensity) {
                    maxIntensity = sumIntensity;
                    mostIntensiveIdx = pos;
                }
            }
        }

        List<SimpleSpectrum> ms1List = new ArrayList<>();
        List<MutableMs2Spectrum> ms2List = new ArrayList<>();
        if (experiment.getMs1Spectra().size() == experiment.getMs2Spectra().size()) {
            ms1List.add(experiment.getMs1Spectra().get(mostIntensiveIdx));
        } else {
            ms1List.addAll(experiment.getMs1Spectra());
        }
        ms2List.add(experiment.getMs2Spectra().get(mostIntensiveIdx));
        experiment.setMs1Spectra(ms1List);
        experiment.setMs2Spectra(ms2List);
    }


    protected Set<MolecularFormula> getFormulaWhitesetNoDB(Ms2Experiment experiment, List<String> whitelist) {
        final Set<MolecularFormula> whiteset = new HashSet<MolecularFormula>();
        if (whitelist == null && (options.getNumberOfCandidates() == null) && experiment.getMolecularFormula() != null) {
            whiteset.add(experiment.getMolecularFormula());
        } else if (whitelist != null) for (String s : whitelist) whiteset.add(MolecularFormula.parse(s));
        return whiteset.isEmpty() ? null : whiteset;
    }

    protected Set<MolecularFormula> getFormulaWhitesetWithDB(Ms2Experiment experiment, List<String> whitelist) {
        final String dbOptName = options.getDatabase().toLowerCase();
        if (dbOptName.equals("all")) return getFormulaWhitesetNoDB(experiment, whitelist);
        else {
            FingerIdInstanceProcessor fingerIdInstanceProcessor = new FingerIdInstanceProcessor(options);
            fingerIdInstanceProcessor.initializeDatabaseCache();
            //todo may create extra DB class
            final HashMap<String, Long> aliasMap = fingerIdInstanceProcessor.getDatabaseAliasMap();
            final SearchableDatabase searchableDatabase = fingerIdInstanceProcessor.getDatabase();
            final long flag;

            if (aliasMap.containsKey(dbOptName)) {
                flag = aliasMap.get(dbOptName).longValue() == DatasourceService.Sources.BIO.flag ? 0 : aliasMap.get(dbOptName);
            } else {
                flag = 0L;
            }
            final Deviation dev;
            if (options.getPPMMax() != null) dev = new Deviation(options.getPPMMax());
            else
                dev = siriusInstanceProcessor.getSirius().getMs2Analyzer().getDefaultProfile().getAllowedMassDeviation();
            final Set<PrecursorIonType> allowedIonTypes = new HashSet<>();
            if (experiment.getPrecursorIonType() == null || experiment.getPrecursorIonType().isIonizationUnknown()) {
                allowedIonTypes.addAll(experiment.getAnnotation(PossibleAdducts.class).getAdducts());
            } else {
                allowedIonTypes.add(experiment.getPrecursorIonType());
            }
            final FormulaConstraints allowedAlphabet;
            if (options.getElements() != null) allowedAlphabet = options.getElements();
            else allowedAlphabet = new FormulaConstraints("CHNOPSBBrClIF");

            List<List<FormulaCandidate>> candidates = new ArrayList<>();
            try {
                if (searchableDatabase.searchInBio()) {
                    try (final RESTDatabase db = WebAPI.INSTANCE.getRESTDb(BioFilter.ONLY_BIO, fingerIdInstanceProcessor.bioDatabase.getDatabasePath())) {
                        candidates.addAll(db.lookupMolecularFormulas(experiment.getIonMass(), dev, allowedIonTypes.toArray(new PrecursorIonType[allowedIonTypes.size()])));
                    }
                }
                if (searchableDatabase.searchInPubchem()) {
                    try (final RESTDatabase db = WebAPI.INSTANCE.getRESTDb(BioFilter.ONLY_NONBIO, fingerIdInstanceProcessor.pubchemDatabase.getDatabasePath())) {
                        candidates.addAll(db.lookupMolecularFormulas(experiment.getIonMass(), dev, allowedIonTypes.toArray(new PrecursorIonType[allowedIonTypes.size()])));
                    }
                }
                if (searchableDatabase.isCustomDb()) {
                    candidates.addAll(fingerIdInstanceProcessor.getFileBasedDb(searchableDatabase).lookupMolecularFormulas(experiment.getIonMass(), dev, allowedIonTypes.toArray(new PrecursorIonType[allowedIonTypes.size()])));
                }
            } catch (IOException e) {
                LoggerFactory.getLogger(this.getClass()).error("Connection to database fails. Probably our webservice is currently offline. You can still use SIRIUS in offline mode - you just have to remove the database flags -d or --database because database search is not available in offline mode.", e);
                System.exit(1);
                return null;
            }

            final HashSet<MolecularFormula> allowedSet = new HashSet<>();
            for (List<FormulaCandidate> fc : candidates) {
                for (FormulaCandidate f : fc) {
                    final long bitset = f.getBitset();
                    if (flag == 0 || (bitset & flag) != 0)
                        if (allowedAlphabet.isSatisfied(f.getFormula()))
                            allowedSet.add(f.getFormula());
                }
            }
            return allowedSet;
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


//    private static final Pattern CHARGE_PATTERN = Pattern.compile("(\\d+)[+-]?");
//    private static final Pattern CHARGE_PATTERN2 = Pattern.compile("[+-]?(\\d+)");

    protected static PrecursorIonType getIonFromOptions(SiriusOptions opt, int charge) {
        List<String> ionStr = opt.getIon();
        if (ionStr == null) {
            return PrecursorIonType.unknown(charge);
        } else if (ionStr.size() == 1) {
            final PrecursorIonType ionType = PeriodicTable.getInstance().ionByName(opt.getIon().get(0));
            if (ionType.isIonizationUnknown() && !opt.isAutoCharge()) {
                if (ionType.getCharge() > 0) return PrecursorIonType.getPrecursorIonType("[M+H]+");
                else return PrecursorIonType.getPrecursorIonType("[M-H]-");
            } else return ionType;
        } else {
            final List<PrecursorIonType> ionTypes = new ArrayList<>();
            for (String ion : ionStr) ionTypes.add(PrecursorIonType.getPrecursorIonType(ion));
            int ch = ionTypes.get(0).getCharge();
            for (PrecursorIonType pi : ionTypes)
                if (pi.getCharge() != ch)
                    throw new IllegalArgumentException("SIRIUS does not support different charge states for the same compound");
            return PrecursorIonType.unknown(ch);
        }
    }*/
}
