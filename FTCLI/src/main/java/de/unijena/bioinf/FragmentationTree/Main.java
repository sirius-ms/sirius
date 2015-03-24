package de.unijena.bioinf.FragmentationTree;

import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.HelpRequestedException;
import de.unijena.bioinf.ChemistryBase.chem.*;
import de.unijena.bioinf.ChemistryBase.chem.utils.ScoredMolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.MeasurementProfile;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.MutableMeasurementProfile;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FragmentAnnotation;
import de.unijena.bioinf.ChemistryBase.ms.ft.Loss;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.FragmentationPatternAnalysis;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.MultipleTreeComputation;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.TreeIterator;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.filtering.LimitNumberOfPeaksFilter;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.inputValidator.Warning;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.recalibration.AbstractRecalibrationStrategy;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.recalibration.HypothesenDrivenRecalibration;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.CommonLossEdgeScorer;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.TreeSizeScorer;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.DPTreeBuilder;
import de.unijena.bioinf.FragmentationTreeConstruction.inspection.GraphOutput;
import de.unijena.bioinf.FragmentationTreeConstruction.inspection.TreeAnnotation;
import de.unijena.bioinf.FragmentationTreeConstruction.model.*;
import de.unijena.bioinf.IsotopePatternAnalysis.IsotopePattern;
import de.unijena.bioinf.IsotopePatternAnalysis.IsotopePatternAnalysis;
import de.unijena.bioinf.MassDecomposer.Chemistry.MassToFormulaDecomposer;
import de.unijena.bioinf.babelms.GenericParser;
import de.unijena.bioinf.babelms.Parser;
import de.unijena.bioinf.babelms.chemdb.DBMolecularFormulaCache;
import de.unijena.bioinf.babelms.chemdb.Databases;
import de.unijena.bioinf.babelms.dot.FTDotWriter;
import de.unijena.bioinf.babelms.ms.JenaMsParser;
import de.unijena.bioinf.sirius.cli.ProfileOptions;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.io.*;
import java.util.*;

public class Main {

    public static final String VERSION = "1.21";

    public static final boolean DEBUG_MODE = false;

    public static final String CITE = "Computing fragmentation trees from tandem mass spectrometry data\n" +
            "Florian Rasche, Aleš Svatoš, Ravi Kumar Maddula, Christoph Böttcher and Sebastian Böcker\n" +
            "Anal Chem, 83(4):1243-1251, 2011.";

    public static final String USAGE = "tree -n 10 <file1> <file2>";

    public final static String VERSION_STRING = "FragmentationPatternAnalysis " + VERSION + "\n" + CITE + "\nusage:\n" + USAGE;

    private static boolean DEBUG_ONLY_INT = true;

    private static boolean DEBUG = false;
    // fragmentstats
    // file, db, correct, shared, formula, mass, recalibrated, alphabet, ppmdev, mzdev, recppmdev, recmzdev, intensity
    protected PrintStream treeStat, fragStat, lossStats;
    PrintStream measureMZDIFFSTREAM;
    PrintStream measureIsoSTREAM;
    private DBMolecularFormulaCache formulaQuery;
    private File formulaCacheFile;
    private PrintStream DEBUGSTREAM = null;
    private Options options;
    private boolean verbose;
    private PrintStream rankWriter;
    private Profile profile;
    private Databases database;
    private List<PrintStream> openStreams;
    private PrintStream DEBUGWRITER;

    public Main(Options options) {
        this.options = options;
        this.verbose = options.getVerbose();
        this.database = options.getDatabase();
    }

    public Main(String[] args) {
        this(CliFactory.createCli(Options.class).parseArguments(args));
    }

    public static void main(String[] args) {
        try {
            if (args.length == 0) CliFactory.createCli(Options.class).parseArguments("-h");
            new Main(args).run();
        } catch (HelpRequestedException h) {
            System.out.println(VERSION_STRING);
            System.out.println(h.getMessage());
            System.exit(0);
        }
    }

    public static File getFormulaCacheFile(File proposedCachingDirectory, Databases database) {
        if (database == Databases.NONE) return null;
        final File cache = proposedCachingDirectory != null ? proposedCachingDirectory :
                new File(System.getProperty("user.home"), ".sirius-cache");
        return new File(cache, database.name().toLowerCase() + ".db");
    }

    public static DBMolecularFormulaCache initializeFormulaCache(File cacheFile, Databases database) {
        if (database == Databases.NONE) return null;
        final File dir = cacheFile.getParentFile();
        if (!dir.exists()) dir.mkdirs();
        DBMolecularFormulaCache formulaQuery;
        if (cacheFile.exists()) {
            try {
                final FileInputStream input = new FileInputStream(cacheFile);
                formulaQuery = DBMolecularFormulaCache.load(input);
                // TODO: add timestamp!
            } catch (IOException e) {
                System.err.println(e);
                formulaQuery = new DBMolecularFormulaCache(
                        new ChemicalAlphabet(PeriodicTable.getInstance().getAllByName("C", "H", "N", "O", "P", "S",
                                "Cl", "Br", "I", "F")),
                        database
                );
            }
        } else {
            formulaQuery = new DBMolecularFormulaCache(
                    new ChemicalAlphabet(PeriodicTable.getInstance().getAllByName("C", "H", "N", "O", "P", "S",
                            "Cl", "Br", "I", "F")),
                    database
            );
        }
        return formulaQuery;
    }

    public static void updateFormulaCache(File cacheFile, DBMolecularFormulaCache cache) {
        if (cache.getChanges() > 0) {
            synchronized (cache) {
                final File backupFile = new File(cacheFile.getAbsolutePath() + ".backup");
                cacheFile.renameTo(backupFile);
                final FileOutputStream out;
                try {
                    out = new FileOutputStream(cacheFile);
                    cache.store(out);
                    out.close();
                    backupFile.delete();
                } catch (IOException e) {
                    cacheFile.delete();
                    backupFile.renameTo(cacheFile);
                }
            }
        }
    }

    private static double intensityOfTree(FTree tree) {
        double treeIntensity = 0d, maxIntensity = 0d;
        final FragmentAnnotation<ProcessedPeak> pp = tree.getFragmentAnnotationOrThrow(ProcessedPeak.class);
        for (Fragment f : tree.getFragmentsWithoutRoot()) treeIntensity += pp.get(f).getRelativeIntensity();
        final ProcessedInput input = tree.getAnnotationOrThrow(ProcessedInput.class);
        final PeakAnnotation<DecompositionList> decomp = input.getPeakAnnotationOrThrow(DecompositionList.class);
        final MolecularFormula parent = tree.getRoot().getFormula();
        eachPeak:
        for (ProcessedPeak p : input.getMergedPeaks())
            if (p != input.getParentPeak()) {
                for (ScoredMolecularFormula f : decomp.get(p).getDecompositions()) {
                    if (parent.isSubtractable(f.getFormula())) {
                        maxIntensity += p.getRelativeIntensity();
                        continue eachPeak;
                    }
                }
            }
        return treeIntensity / maxIntensity;
    }

    private static String escapeCSV(String s) {
        if (s.indexOf(',') >= 0) {
            return "\"" + s.replaceAll("\"", "\"\"") + "\"";
        } else if (s.indexOf('"') >= 0) {
            return s.replaceAll("\"", "\"\"");
        } else {
            return s;
        }
    }

    public static Ms2Experiment parseFile(File f, MeasurementProfile profile) throws IOException {
        final GenericParser<Ms2Experiment> parser = new GenericParser<Ms2Experiment>(getParserFor(f));
        final Ms2Experiment experiment = parser.parseFile(f);
        final Ms2ExperimentImpl impl = new Ms2ExperimentImpl(experiment);
        {
            if (impl.getIonization() == null) {
                final MolecularFormula formula = experiment.getMolecularFormula();
                final double ionMass = experiment.getIonMass() - experiment.getMoleculeNeutralMass();
                final Ionization ion = PeriodicTable.getInstance().ionByMass(ionMass, 1e-3, experiment.getIonization().getCharge());
                impl.setIonization(ion);
            } else if (impl.getIonization().getName().equals("[M+H-H2O]X+")) {
                // TODO: QUICKNDIRTY
                System.err.println("Warning: Replace Ionization [M+H-H2O]+ to [M+H]+ by subtracting H2O from correct formula");
                impl.setIonization(PeriodicTable.getInstance().ionByName("[M+H]+"));
                impl.setMolecularFormula(impl.getMolecularFormula().subtract(MolecularFormula.parse("H2O")));
            }
            if (impl.getMs1Spectra() != null && !impl.getMs1Spectra().isEmpty())
                impl.setMergedMs1Spectrum(impl.getMs1Spectra().get(0));
        }
        impl.setMeasurementProfile(profile);
        return impl;
    }

    public static Parser<Ms2Experiment> getParserFor(File f) {
        final String[] extName = f.getName().split("\\.");
        if (extName.length > 1 && extName[1].equalsIgnoreCase("ms")) {
            return new JenaMsParser();
        } else {
            throw new RuntimeException("No parser found for file " + f);
        }

    }

    public static boolean useIsotopes(Options options) {
        return options.getFilterByIsotope() > 0 || options.getMs1();
    }

    public static List<File> getFiles(Options options) {
        final List<File> files = options.getFiles();
        final ArrayList<File> fs = new ArrayList<File>(files.size());
        for (File f : files) {
            if (f.isDirectory()) {
                fs.addAll(Arrays.asList(f.listFiles(new FileFilter() {
                    @Override
                    public boolean accept(File pathname) {
                        return pathname.isFile() && !pathname.isDirectory() && pathname.canRead();
                    }
                })));
            } else if (f.canRead()) {
                fs.add(f);
            }
        }
        return fs;
    }

    private static void TESTKEGG() throws IOException {
        final MolecularFormula[] formulas;
        {
            final ArrayList<MolecularFormula> keggFormula = new ArrayList<MolecularFormula>();
            final BufferedReader keggFormulas = new BufferedReader(new FileReader("keggformulas.csv"));
            String line = null;
            while ((line = keggFormulas.readLine()) != null) {
                final MolecularFormula m = MolecularFormula.parse(line);
                if (m.isCHNOPS() && m.getMass() < 1000d)
                    keggFormula.add(m);
            }
            formulas = keggFormula.toArray(new MolecularFormula[keggFormula.size()]);
        }

        {
            final BufferedWriter writer = new BufferedWriter(new FileWriter("KEGG_real.csv"));
            for (MolecularFormula f : formulas) {
                writer.write(f.getMass() + "," + f.rdbe() + "," + f.heteroWithoutOxygenToCarbonRatio() + "," + f.hydrogen2CarbonRatio() + "," + (f.rdbe() / Math.pow(f.getMass(), 2d / 3d)));
                writer.newLine();
            }
            writer.close();
        }
        {
            final BufferedWriter writer = new BufferedWriter(new FileWriter("KEGG_alldecomps.csv"));
            final ChemicalAlphabet alphabet = ChemicalAlphabet.alphabetFor(MolecularFormula.parse("CHNOPS"));
            final MassToFormulaDecomposer dec = new MassToFormulaDecomposer(alphabet);
            final FormulaConstraints constr = new FormulaConstraints(alphabet);
            for (MolecularFormula g : formulas) {
                final List<MolecularFormula> xs = dec.decomposeToFormulas(g.getMass(), new Deviation(10), constr);
                for (MolecularFormula f : xs) {
                    writer.write(f.getMass() + "," + f.rdbe() + "," + f.heteroWithoutOxygenToCarbonRatio() + "," + f.hydrogen2CarbonRatio() + "," + (f.rdbe() / Math.pow(f.getMass(), 2d / 3d)));
                    writer.newLine();
                }
            }
            writer.close();
        }
    }

    void run() {

        if (options.getCite() || options.getVersion()) {
            System.out.println(VERSION_STRING);
            return;
        }

        if (options.getThreads() > 1) {
            System.err.println("Multiple threads are currently not supported. Please restart the program without the option -n");
            System.exit(1);
        }

        this.openStreams = new ArrayList<PrintStream>();
        if (options.getRanking() != null) {
            try {
                rankWriter = new PrintStream(options.getRanking());
                openStreams.add(rankWriter);
                rankWriter.print("name,formula,mass,decompositions,rank,score,optScore,explainedPeaks,computationTime");
                if (options.isIsotopeFilteringCheat()) {
                    rankWriter.print("," + "iso20" + "," + "iso10" + "," + "iso5" + "," + "isoX");
                }
                rankWriter.println("");
            } catch (FileNotFoundException e) {
                System.err.println(e.getMessage());
                System.exit(1);
            }
        }
        initializeFormulaCache();
        int dbchanges = 0;

        final List<File> files = getFiles(options);
        final MeasurementProfile defaultProfile = ProfileOptions.Interpret.getMeasurementProfile(options);

        FragmentationPatternAnalysis analyzer;

        if (options.getProfile() != null) {
            try {
                profile = new Profile(options.getProfile());
                analyzer = profile.fragmentationPatternAnalysis;
            } catch (IOException e) {
                System.err.println(e);
                System.exit(1);
                return;
            }
        } else {
            try {
                profile = new Profile("default");
                analyzer = profile.fragmentationPatternAnalysis;
            } catch (IOException e) {
                System.err.println("Can't find default profile");
                return;
            }
        }
        if (options.isOldSirius()) {
            SiriusProfile sirius = new SiriusProfile();
            if (!options.getProfile().equals("default"))
                sirius.fragmentationPatternAnalysis.setDefaultProfile(profile.fragmentationPatternAnalysis.getDefaultProfile());
            profile = sirius;
            analyzer = profile.fragmentationPatternAnalysis;
        }
        profile.fragmentationPatternAnalysis.setDefaultProfile(MutableMeasurementProfile.merge(profile.fragmentationPatternAnalysis.getDefaultProfile(), defaultProfile));

        if (options.getTreeSize() != null)
            FragmentationPatternAnalysis.getOrCreateByClassName(TreeSizeScorer.class, analyzer.getFragmentPeakScorers()).setTreeSizeScore(options.getTreeSize());

        if (options.getPeakLimit() != null) {
            FragmentationPatternAnalysis.getOrCreateByClassName(LimitNumberOfPeaksFilter.class, analyzer.getPostProcessors()).setLimit(options.getPeakLimit().intValue());
        }

        if (options.isDp()) {
            System.out.println("Use DP for tree computation");
            analyzer.setTreeBuilder(new DPTreeBuilder(17));
        }


        analyzer.setRepairInput(true);
        final IsotopePatternAnalysis deIsotope = (profile.isotopePatternAnalysis != null) ? profile.isotopePatternAnalysis : IsotopePatternAnalysis.defaultAnalyzer();

        if (options.getRecalibrate() && analyzer.getRecalibrationMethod() == null) {
            analyzer.setRecalibrationMethod(new HypothesenDrivenRecalibration());
        }

        final File target = options.getTarget();
        if (!target.exists()) target.mkdirs();

        // write used profile

        {
            try {
                profile.writeToFile(new File(target, "profile.json"));
            } catch (IOException e) {
                System.err.println("Cannot create profile file: " + e);
            }
        }


        eachFile:
        for (int fnum = 0; fnum < files.size(); ++fnum) {

            dbchanges = updateFormulaCache(formulaQuery, formulaCacheFile, dbchanges);

            final File f = files.get(fnum);
            try {
                long computationTime = System.nanoTime();
                if (verbose) System.out.println("parse " + f);
                System.out.flush();
                if (options.getTrees() > 0) {
                    final File tdir = new File(options.getTarget(), removeExtname(f));
                    if (tdir.exists() && !tdir.isDirectory()) {
                        throw new RuntimeException("Cannot create directory '" + tdir.getAbsolutePath() + "': File still exists!");
                    }
                    tdir.mkdir();
                }
                MeasurementProfile profile = defaultProfile;

                Ms2Experiment experiment = parseFile(f, profile);

                if (options.isNaive()) {
                    useNaiveApproach(analyzer, profile, experiment);
                    continue eachFile;
                }

                if (false) {

                    measureMzDiff(analyzer, profile, experiment);
                    continue eachFile;

                }

                final MolecularFormula correctFormula = experiment.getMolecularFormula(); // TODO: charge
                if (correctFormula != null) {
                    if (verbose) System.out.println("correct formula is given: " + correctFormula);
                    final List<Element> elements = correctFormula.elements();
                    elements.removeAll(profile.getFormulaConstraints().getChemicalAlphabet().getElements());
                    if (elements.size() > 0) {
                        if (verbose) {
                            System.out.print("Missing characters in chemical alphabet! Add ");
                            for (Element e : elements) System.out.print(e.getSymbol());
                            System.out.println(" to alphabet");
                        }
                        elements.addAll(defaultProfile.getFormulaConstraints().getChemicalAlphabet().getElements());
                        MutableMeasurementProfile mmp = new MutableMeasurementProfile(profile);
                        mmp.setFormulaConstraints(new FormulaConstraints(new ChemicalAlphabet(elements.toArray(new Element[0])), defaultProfile.getFormulaConstraints().getFilters()));
                        profile = mmp;
                        Ms2ExperimentImpl ms2 = new Ms2ExperimentImpl(experiment);
                        ms2.setMeasurementProfile(profile);
                        experiment = ms2;
                    }
                }
                analyzer.setValidatorWarning(new Warning() {
                    @Override
                    public void warn(String message) {
                        System.err.println(f.getName() + ": " + message);
                    }
                });

                experiment = analyzer.validate(experiment);

                ProcessedInput input = analyzer.preprocessing(experiment);

                if (false) {
                    final ArrayList<ScoredMolecularFormula> list = new ArrayList<ScoredMolecularFormula>();
                    for (ScoredMolecularFormula x : input.getAnnotationOrThrow(DecompositionList.class).getDecompositions()) {
                        final double mzdev = Math.abs(input.getExperimentInformation().getIonization().addToMass(x.getFormula().getMass()) - input.getParentPeak().getMz());
                        list.add(new ScoredMolecularFormula(x.getFormula(), mzdev));
                    }
                    Collections.sort(list);
                    int pos = 0;
                    for (; pos < list.size(); ++pos) {
                        if (list.get(pos).getFormula().equals(correctFormula)) break;
                    }
                    if (DEBUGSTREAM == null) {
                        DEBUGSTREAM = new PrintStream("mzdev.csv");
                        openStreams.add(DEBUGSTREAM);
                        DEBUGSTREAM.println("name,formula,mass,decompositions,rank");
                    }
                    DEBUGSTREAM.println(f.getName() + "," + correctFormula.toString() + "," + input.getParentPeak().getMz() + "," + list.size() + "," + (pos + 1));
                    if (true) continue eachFile;

                }

                /*
                    FILTER BY DATABASE (if activated)
                 */
                final int decompositionListSize = input.getAnnotationOrThrow(DecompositionList.class).getDecompositions().size();
                if (database != Databases.NONE) {
                    final ArrayList<ScoredMolecularFormula> formulas =
                            new ArrayList<ScoredMolecularFormula>(input.getAnnotationOrThrow(DecompositionList.class).getDecompositions());
                    final Iterator<ScoredMolecularFormula> iter = formulas.iterator();
                    while (iter.hasNext()) {
                        if (!formulaQuery.isFormulaExist(iter.next().getFormula())) iter.remove();
                    }

                    input.setAnnotation(DecompositionList.class, new DecompositionList(formulas));
                    if (verbose)
                        System.out.println("Filter by " + database.name() + " leaving " + formulas.size() + " out of "
                                + decompositionListSize + " candidates.");
                }

                /*
                TODO: Push into separate branch "newScores2013"
                 */
                final TObjectIntHashMap<MolecularFormula> isoRankingMap;
                // isoN = Rank of correct compound if you remove all explanations before it that have an isotope rank of
                // worse than N % relative to all decompositions
                int iso20 = 0, iso10 = 0, iso5 = 0;
                double isoX = 0d;

                final int NumberOfDecompositions = input.getAnnotationOrThrow(DecompositionList.class).getDecompositions().size();
                {
                    if (options.isIsotopeFilteringCheat()) {
                        final EvalIsotopeScorer isoScorer = new EvalIsotopeScorer(experiment.getMolecularFormula());
                        final ArrayList<ScoredMolecularFormula> scoredList = new ArrayList<ScoredMolecularFormula>();
                        isoRankingMap = new TObjectIntHashMap<MolecularFormula>(input.getAnnotationOrThrow(DecompositionList.class).getDecompositions().size());
                        for (ScoredMolecularFormula scf : input.getAnnotationOrThrow(DecompositionList.class).getDecompositions()) {
                            scoredList.add(new ScoredMolecularFormula(scf.getFormula(), isoScorer.score(scf.getFormula())));
                        }
                        Collections.sort(scoredList, Collections.reverseOrder());
                        for (int i = 0; i < scoredList.size(); ++i) {
                            isoRankingMap.put(scoredList.get(i).getFormula(), i);
                        }
                    } else {
                        isoRankingMap = null;
                    }
                }


                // use corrected input information
                experiment = analyzer.validate(experiment);
                assert experiment.getIonization() != null;

                // isotope pattern analysis
                final List<IsotopePattern> patterns = useIsotopes(options) ? deIsotope.getPatternExtractor().extractPattern(experiment.getMergedMs1Spectrum())
                        : new ArrayList<IsotopePattern>();
                IsotopePattern pattern = null;
                for (IsotopePattern iso : patterns) {
                    if (Math.abs(iso.getMonoisotopicMass() - experiment.getIonMass()) < 2e-2d) {
                        if (pattern == null || Math.abs(iso.getMonoisotopicMass() - experiment.getIonMass()) < Math.abs(pattern.getMonoisotopicMass() - experiment.getIonMass()))
                            pattern = iso;
                    }
                }
                final HashMap<MolecularFormula, Double> isotopeScores = new HashMap<MolecularFormula, Double>();
                if (pattern != null) {
                    if (verbose) System.out.println("analyze isotope pattern in MS1");
                    pattern = deIsotope.deisotope(experiment, pattern);
                    // change fragmentation candidates according to isotope pattern
                    final HashMap<MolecularFormula, Double> scores = new HashMap<MolecularFormula, Double>();
                    for (ScoredMolecularFormula g : input.getAnnotationOrThrow(DecompositionList.class).getDecompositions()) {
                        scores.put(g.getFormula(), g.getScore());
                    }
                    List<ScoredMolecularFormula> list = new ArrayList<ScoredMolecularFormula>(scores.size());
                    for (ScoredMolecularFormula g : pattern.getCandidates()) {
                        final Double treeScore = scores.get(g.getFormula());
                        final double isoScore = (options.getMs1() ? g.getScore() * 5 : Double.NEGATIVE_INFINITY);
                        list.add(new ScoredMolecularFormula(g.getFormula(), (treeScore == null ? 0d : treeScore.doubleValue()) + isoScore));
                        isotopeScores.put(g.getFormula(), isoScore);
                    }
                    Collections.sort(list, Collections.reverseOrder());
                    if (verbose) {
                        System.out.println("Isotope scores:");
                        for (ScoredMolecularFormula formula : list) {
                            final Double treeScore = scores.get(formula.getFormula());
                            final double isoScore = formula.getScore() - (treeScore == null ? 0d : treeScore.doubleValue());
                            System.out.println(formula.getFormula() + ": " + isoScore);
                        }
                    }
                    if (options.getFilterByIsotope() > 0 && options.getFilterByIsotope() < list.size())
                        list = list.subList(0, options.getFilterByIsotope());

                    // TODO: WORKAROUND =(
                    for (int i = 0; i < list.size(); ++i) {
                        boolean inf = true;
                        do {
                            final ScoredMolecularFormula s = list.get(i);
                            inf = Double.isInfinite(s.getScore());
                            if (inf) {
                                list.remove(i);
                            } else {
                                list.set(i, new ScoredMolecularFormula(s.getFormula(), s.getScore() - isotopeScores.get(s.getFormula())));
                            }
                        } while (inf && list.size() > i);
                    }

                    input.setAnnotation(DecompositionList.class, new DecompositionList(list));
                }

                // DEBUG!!!!! FSTAT

                if (DEBUG_MODE) {
                    final ArrayList<ScoredMolecularFormula> allowed = new ArrayList<ScoredMolecularFormula>(input.getAnnotationOrThrow(DecompositionList.class).getDecompositions());
                    final MolecularFormula correct = correctFormula;
                    final MolecularFormula bestWrong;
                    {
                        final File dir = new File("D:/daten/arbeit/analysis_2014/28", f.getName().substring(0, f.getName().indexOf('.')));
                        final ArrayList<File> get = new ArrayList<File>();
                        for (File g : dir.listFiles()) {
                            if ((g.getName().startsWith("1_") || g.getName().startsWith("2_")) && !g.getName().contains("_correct_")) {
                                get.add(g);
                            }
                        }
                        if (get.size() > 1) {
                            if (get.get(0).getName().startsWith("2_")) get.remove(0);
                            else get.remove(1);
                        }
                        if (get.size() == 0) {
                            bestWrong = null;
                        } else {
                            final File bestWrongFile = get.get(0);
                            String[] parts = bestWrongFile.getName().split("_");
                            bestWrong = MolecularFormula.parse(parts[parts.length - 1]);
                        }
                    }
                    final Iterator<ScoredMolecularFormula> iter = allowed.iterator();
                    while (iter.hasNext()) {
                        final MolecularFormula h = iter.next().getFormula();
                        if (h.equals(correctFormula) || h.equals(bestWrong)) continue;
                        else {
                            if (h.equals(correctFormula)) {
                                assert false;
                                System.err.println("'Correct' is not found in " + database.name() + "!");
                            }
                            iter.remove();
                        }
                    }
                    input.setAnnotation(DecompositionList.class, new DecompositionList(allowed));
                }


                // First: Compute correct tree
                // DONT USE LOWERBOUNDS
                FTree correctTree = null;
                int correctRankInPmds = 0;
                if (correctFormula != null) {
                    for (ScoredMolecularFormula pmd : input.getAnnotationOrThrow(DecompositionList.class).getDecompositions())
                        if (pmd.getFormula().equals(correctFormula)) break;
                        else ++correctRankInPmds;
                }
                //if (correctRankInPmds >= options.getTrees()) System.err.println("Correct formula not in top " + options.getTrees());
                double lowerbound = options.getLowerbound() == null ? 0d : options.getLowerbound();

                if (DEBUG_MODE) lowerbound = 0d;

                if (experiment.getMolecularFormula() != null /*&& correctRankInPmds < 1000*/ /* TODO: What does this mean? */) {
                    correctTree = analyzer.computeTrees(input).onlyWith(Arrays.asList(correctFormula)).withoutRecalibration().optimalTree();
                    /*
                    if (correctTree != null) {
                        final TreeScoring scoring = correctTree.getAnnotationOrThrow(TreeScoring.class);
                        if (options.getWrongPositive() && correctTree != null)
                            lowerbound = Math.max(lowerbound, scoring.getOverallScore() - scoring.getRecalibrationBonus());
                        final TreeIterator iter = analyzer.computeTrees(input).onlyWith(Arrays.asList(correctFormula)).withoutRecalibration().iterator();
                        iter.next();

                        final FGraph g = iter.lastGraph();
                        new GraphOutput().printToFile(iter.lastGraph(), scoring.getOverallScore() - scoring.getRootScore(),
                                new File("graph.txt"));

                    }
                    */
                    if (verbose) {
                        if (correctTree != null) {
                            printResult(correctTree);
                        } else {
                            System.out.println("correct tree not found. Please increase allowed mass deviation.");
                            if (options.getWrongPositive() || options.getTrees()==0) continue eachFile;
                        }
                    }
                }

                if (verbose) {
                    System.out.println(input.getAnnotationOrThrow(DecompositionList.class).getDecompositions().size() + " further candidate formulas.");
                    System.out.flush();
                }

                TreeSizeScorer origScorer = FragmentationPatternAnalysis.getByClassName(TreeSizeScorer.class, analyzer.getFragmentPeakScorers());
                double origScore = origScorer == null ? 0d : origScorer.getTreeSizeScore();

                final ArrayList<MolecularFormula> blacklist = new ArrayList<MolecularFormula>();
                if (options.isIsotopeFilteringCheat()) {
                    final int consider = input.getAnnotationOrThrow(DecompositionList.class).getDecompositions().size();
                    final int co20 = (int)Math.ceil(consider*0.2);
                    for (ScoredMolecularFormula scf : input.getAnnotationOrThrow(DecompositionList.class).getDecompositions()) {
                        if (isoRankingMap.get(scf.getFormula()) > co20) {
                            blacklist.add(scf.getFormula());
                        }
                    }
                }
                if (correctFormula != null) blacklist.add(correctFormula);
                final int NumberOfTreesToCompute = (/*options.isIsotopeFilteringCheat() ? input.getParentMassDecompositions().size() :*/ options.getTrees());
                final int TreesToConsider = options.getTrees();
                int rank = 1;
                double optScore = Double.NEGATIVE_INFINITY;//(correctTree==null) ? Double.NEGATIVE_INFINITY : correctTree.getScore();
                final boolean printGraph = options.isWriteGraphInstances();
                lowerbound = 0d; // don't use correct tree as lowerbound! This crashs with the recalibration
                if (NumberOfTreesToCompute > 0) {
                    List<FTree> trees;
                    final MultipleTreeComputation m = analyzer.computeTrees(input).withRoots(input.getAnnotationOrThrow(DecompositionList.class).getDecompositions()).inParallel(options.getThreads()).computeMaximal(NumberOfTreesToCompute).withLowerbound(lowerbound)
                            .without(blacklist).withoutRecalibration();
                    if (!verbose && !printGraph) {
                        trees = m.list();
                    } else {
                        final TreeSet<FTree> bestTrees = new TreeSet<FTree>(TreeScoring.OrderedByscoreAsc());
                        final TreeIterator treeIter = m.iterator();
                        double lb = lowerbound;
                        if (DEBUG_MODE) treeIter.setLowerbound(0d);
                        treeIteration:
                        while (treeIter.hasNext()) {
                            if (verbose) System.out.print("Compute next tree: ");
                            FTree tree;

                            if (printGraph) {
                                treeIter.setLowerbound(0d);
                                do {
                                    final long now = System.nanoTime();
                                    tree = treeIter.next();
                                    final TreeScoring treeScoring = tree.getAnnotationOrThrow(TreeScoring.class);
                                    final long runtime = System.nanoTime() - now;
                                    if (runtime > 8000000000l) {
                                        final int numberOfSeconds = (int) Math.round(runtime / 1000000000d);
                                        System.out.println("OUTPUT GRAPH!!!!!");
                                        new GraphOutput().printToFile(treeIter.lastGraph(), treeScoring.getOverallScore() - treeScoring.getRootScore(),
                                                new File(options.getTarget(), removeExtname(f) + tree.getRoot().getFormula().toString() + "_" + numberOfSeconds + ".txt"));
                                    }
                                    if (treeScoring.getOverallScore() < lb) tree = null;
                                } while (tree == null);
                                treeIter.setLowerbound(lb);
                            } else {
                                tree = treeIter.next();
                            }

                            if (tree == null && verbose) System.out.println("To low score");
                            else {
                                printResult(tree);
                                bestTrees.add(tree);
                                if (bestTrees.size() > NumberOfTreesToCompute) {
                                    bestTrees.pollFirst();
                                    final TreeScoring worstScoring = bestTrees.first().getAnnotationOrThrow(TreeScoring.class);
                                    if (correctTree != null) {
                                        if (DEBUG_ONLY_INT && worstScoring.getOverallScore() >= correctTree.getAnnotationOrThrow(TreeScoring.class).getOverallScore()) {
                                            break treeIteration;
                                        }
                                        treeIter.setLowerbound(DEBUG_MODE ? 0d : lb);
                                        if (verbose) System.out.println("Increase lowerbound to " + lb);
                                        if (worstScoring.getOverallScore() > correctTree.getAnnotationOrThrow(TreeScoring.class).getOverallScore()) {
                                            break treeIteration;
                                        }
                                    }
                                    lb = worstScoring.getOverallScore() - worstScoring.getRecalibrationBonus();
                                }
                            }
                            if (DEBUG_MODE) treeIter.setLowerbound(0d);
                        }
                        trees = new ArrayList<FTree>(bestTrees.descendingSet());
                    }
                    if (correctTree != null) {
                        trees.add(correctTree);
                    }

                    Collections.sort(trees, (pattern != null ? new Comparator<FTree>() {
                        @Override
                        public int compare(FTree o1, FTree o2) {
                            return new Double(o2.getAnnotationOrThrow(TreeScoring.class).getOverallScore() +
                                    isotopeScores.get(o2.getRoot().getFormula())).compareTo(o1.getAnnotationOrThrow(TreeScoring.class).getOverallScore() + isotopeScores.get(o1.getRoot().getFormula()));
                        }
                    } : TreeScoring.OrderedByscoreDesc()));
                    trees = new ArrayList<FTree>(trees.subList(0, Math.min(TreesToConsider, trees.size())));
                    if (correctTree != null && !trees.contains(correctTree)) trees.add(correctTree);
                    // recalibrate best trees
                    if (!trees.isEmpty() && analyzer.getRecalibrationMethod() != null) {

                        final double DEVIATION_SCALE = 2 / 3d;
                        final int MIN_NUMBER_OF_PEAKS = 8;
                        final double MIN_INTENSITY = 0d;
                        final Deviation EPSILON = new Deviation(4, 5e-4d);
                        // only recalibrate if at least one tree has more than 5 nodes
                        boolean doRecalibrate = false;
                        for (FTree t : trees)
                            if (t.numberOfVertices() >= MIN_NUMBER_OF_PEAKS) doRecalibrate = true;
                        if (doRecalibrate) {
                            for (int i = 0; i < Math.min(TreesToConsider + 1, trees.size()); ++i) {
                                ((HypothesenDrivenRecalibration) analyzer.getRecalibrationMethod()).setDeviationScale(1d);
                                if (verbose)
                                    System.out.print("Recalibrate " + trees.get(i).getRoot().getFormula().toString() + "(" + trees.get(i).getAnnotationOrThrow(TreeScoring.class).getOverallScore() + ")");
                                /*
                                {
                                    AbstractRecalibrationStrategy method = (AbstractRecalibrationStrategy)((HypothesenDrivenRecalibration)analyzer.getRecalibrationMethod()).getMethod();
                                    //method.setMaxDeviation(analyzer.getDefaultProfile().getAllowedMassDeviation().multiply(1d));
                                    method.setMinNumberOfPeaks(MIN_NUMBER_OF_PEAKS);
                                    method.setEpsilon(EPSILON);
                                    method.setMinIntensity(MIN_INTENSITY);
                                    method.setMaxDeviation(new Deviation(20, 0.1)); // "disable" max deviation
                                    ((HypothesenDrivenRecalibration)analyzer.getRecalibrationMethod()).setDeviationScale(1d);
                                }
                                */
                                {
                                    AbstractRecalibrationStrategy method = (AbstractRecalibrationStrategy) ((HypothesenDrivenRecalibration) analyzer.getRecalibrationMethod()).getMethod();
                                    method.setMaxDeviation(new Deviation(10, 5e-4d));
                                    method.setMinNumberOfPeaks(8);
                                    method.setEpsilon(EPSILON);
                                }
                                if (trees.get(i) == correctTree) {
                                    correctTree = analyzer.recalibrate(correctTree);
                                    AbstractRecalibrationStrategy method = (AbstractRecalibrationStrategy) ((HypothesenDrivenRecalibration) analyzer.getRecalibrationMethod()).getMethod();
                                    method.setMinNumberOfPeaks(MIN_NUMBER_OF_PEAKS);
                                    method.setEpsilon(EPSILON);
                                    ((HypothesenDrivenRecalibration) analyzer.getRecalibrationMethod()).setDeviationScale(DEVIATION_SCALE);
                                    correctTree = analyzer.recalibrate(correctTree, true);

                                    trees.set(i, correctTree);

                                } else {
                                    final FTree t = analyzer.recalibrate(trees.get(i));
                                    AbstractRecalibrationStrategy method = (AbstractRecalibrationStrategy) ((HypothesenDrivenRecalibration) analyzer.getRecalibrationMethod()).getMethod();
                                    method.setMinNumberOfPeaks(MIN_NUMBER_OF_PEAKS);
                                    method.setEpsilon(EPSILON);
                                    method.setForceParentPeakIn(true);
                                    ((HypothesenDrivenRecalibration) analyzer.getRecalibrationMethod()).setDeviationScale(DEVIATION_SCALE);
                                    trees.set(i, analyzer.recalibrate(t, true));
                                }
                                if (verbose) {
                                    if (trees.get(i).getAnnotationOrThrow(TreeScoring.class).getRecalibrationBonus() != 0)
                                        System.out.println(" -> " + trees.get(i).getAnnotationOrThrow(TreeScoring.class).getOverallScore());
                                    else System.out.println("");
                                }
                            }
                        }
                    }

                    // TODO: Workaround =(
                    if (pattern != null) {
                        for (FTree t : trees) {
                            final TreeScoring ts = t.getAnnotationOrThrow(TreeScoring.class);
                            ts.setOverallScore(ts.getOverallScore() + isotopeScores.get(t.getRoot().getFormula()));
                        }
                    }

                    Collections.sort(trees, TreeScoring.OrderedByscoreDesc());
                    final boolean correctTreeContained = trees.contains(correctTree);
                    for (int i = 0; i < Math.min(TreesToConsider + 1, trees.size()); ++i) {
                        final FTree tree = trees.get(i);
                        if (!correctTreeContained || correctTree.getAnnotationOrThrow(TreeScoring.class).getOverallScore() < tree.getAnnotationOrThrow(TreeScoring.class).getOverallScore()) {
                            ++rank;
                        }
                        optScore = Math.max(optScore, tree.getAnnotationOrThrow(TreeScoring.class).getOverallScore());
                        if (i < Math.min(10, TreesToConsider) || tree == correctTree)    // TODO: FIX Math.min(10
                            writeTreeToFile(prettyNameSuboptTree(tree, f, i + 1, tree == correctTree), tree, analyzer, isotopeScores.get(tree.getRoot().getFormula()));
                    }

                    // FSTAT
                    if (DEBUG_MODE) {
                        if (trees.size() == 1) statistics(f, correctTree, null);
                        else statistics(f, correctTree, trees.get(0) == correctTree ? trees.get(1) : trees.get(0));
                    }


                    /*
                   TODO: Push into separate branch "newScores2013"
                    */
                    if (isoRankingMap != null) {
                        iso20 = 1;
                        iso10 = 1;
                        iso5 = 1;
                        int isoXRank = NumberOfDecompositions;
                        final int threshold20 = (int) Math.round(NumberOfDecompositions * 0.2), threshold10 = (int) Math.round(NumberOfDecompositions * 0.1), threshold5 = (int) Math.round(NumberOfDecompositions * 0.05);
                        for (int i = 0; i < trees.size(); ++i) {
                            final FTree tree = trees.get(i);
                            final MolecularFormula tf = tree.getRoot().getFormula();
                            if (correctTree != null && correctTree.getAnnotationOrThrow(TreeScoring.class).getOverallScore() < tree.getAnnotationOrThrow(TreeScoring.class).getOverallScore()) {
                                isoXRank = Math.min(isoRankingMap.get(tf), isoXRank);
                                if (isoRankingMap.get(tf) <= threshold20) ++iso20;
                                if (isoRankingMap.get(tf) <= threshold10) ++iso10;
                                if (isoRankingMap.get(tf) <= threshold5) ++iso5;
                            } else {
                                break;
                            }
                        }
                        isoX = ((double) isoXRank) / NumberOfDecompositions;
                    }
                } else {
                    FTree tree;
                    if (correctTree == null) {
                        if (verbose) {
                            System.out.print("Compute optimal tree ");
                            System.out.flush();
                        }
                        tree = analyzer.computeTrees(input).inParallel(options.getThreads()).computeMaximal(NumberOfTreesToCompute).withLowerbound(lowerbound)
                                .without(blacklist).withRecalibration().optimalTree();
                        if (verbose) printResult(tree);
                    } else if (analyzer.getRecalibrationMethod() != null) {
                        if (options.getForceExplainedIntensity() > 0 || options.getForceExplainedPeaks() > 0) {
                            while (true) {
                                final FTree t = analyzer.recalibrate(correctTree);
                                AbstractRecalibrationStrategy method = (AbstractRecalibrationStrategy) ((HypothesenDrivenRecalibration) analyzer.getRecalibrationMethod()).getMethod();
                                ((HypothesenDrivenRecalibration) analyzer.getRecalibrationMethod()).setDeviationScale(2d / 3d);
                                correctTree = analyzer.recalibrate(t, true);
                                final double intensity = intensityOfTree(correctTree) * 100d;
                                final TreeSizeScorer scorer = FragmentationPatternAnalysis.getOrCreateByClassName(TreeSizeScorer.class, analyzer.getFragmentPeakScorers());
                                if ((intensity < options.getForceExplainedIntensity() || (t.numberOfVertices() < options.getForceExplainedPeaks() && input.getMergedPeaks().size() > options.getForceExplainedPeaks())) && scorer.getTreeSizeScore() <= 5) {
                                    scorer.setTreeSizeScore(scorer.getTreeSizeScore() + 0.5d);
                                    input = analyzer.preprocessing(experiment);
                                    correctTree = analyzer.computeTrees(input).onlyWith(Arrays.asList(correctFormula)).withoutRecalibration().optimalTree();
                                } else {
                                    break;
                                }
                            }
                        }
                        tree = correctTree;
                    } else tree = correctTree;
                    if (tree == null) {
                        System.err.println("Can't find any tree");
                    } else {
                        writeTreeToFile(prettyNameOptTree(tree, f), tree, analyzer, isotopeScores.get(tree.getRoot().getFormula()));
                    }
                    FragmentationPatternAnalysis.getOrCreateByClassName(TreeSizeScorer.class, analyzer.getFragmentPeakScorers()).setTreeSizeScore(origScore);
                }
                computationTime = System.nanoTime() - computationTime;
                computationTime /= 1000000;
                System.out.println("Computation time: " + computationTime + " ms");
                if (correctTree != null && rankWriter != null) {
                    rankWriter.print(escapeCSV(f.getName()) + "," + correctTree.getRoot().getFormula() + "," + correctTree.getRoot().getFormula().getMass() + "," + input.getAnnotationOrThrow(DecompositionList.class).getDecompositions().size() + "," +
                            rank +
                            "," + correctTree.getAnnotationOrThrow(TreeScoring.class).getOverallScore() + "," + optScore + "," + correctTree.numberOfVertices() + "," + computationTime);
                    if (options.isIsotopeFilteringCheat()) {
                        rankWriter.print("," + iso20 + "," + iso10 + "," + iso5 + "," + ((int) Math.round(isoX * 100)));
                    }
                    rankWriter.println("");
                    if (verbose) rankWriter.flush();
                }

            } catch (IOException e) {
                System.err.println("Error while parsing " + f + ":\n" + e);
            } catch (Exception e) {
                System.err.println("Error while processing " + f + ":\n" + e);
                e.printStackTrace();
            }
        }
        updateFormulaCache(formulaQuery, formulaCacheFile, -10);
        for (PrintStream writer : openStreams) {
            writer.close();
        }
    }

    private void measureMzDiff(FragmentationPatternAnalysis analyzer, MeasurementProfile profile, Ms2Experiment experiment) {
        if (measureMZDIFFSTREAM == null) try {
            measureMZDIFFSTREAM = new PrintStream(new File("mzdiff.csv"));
            measureIsoSTREAM = new PrintStream(new File("intensity.csv"));
            openStreams.add(measureMZDIFFSTREAM);
            openStreams.add(measureIsoSTREAM);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        final ProcessedInput input = analyzer.preprocessing(experiment);
        final FTree tree = analyzer.computeTrees(input).onlyWith(Arrays.asList(experiment.getMolecularFormula())).optimalTree();
        FragmentAnnotation<ProcessedPeak> ano = tree.getFragmentAnnotationOrThrow(ProcessedPeak.class);
        final Ionization ion = tree.getAnnotationOrThrow(Ionization.class);
        for (Fragment f : tree.getFragments()) {
            final double mz = ano.get(f).getOriginalMz();
            final double hypo = ion.addToMass(f.getFormula().getMass());
            measureMZDIFFSTREAM.print(String.valueOf(mz));
            measureMZDIFFSTREAM.print("\t");
            measureMZDIFFSTREAM.print(String.valueOf(hypo));
            measureMZDIFFSTREAM.print("\t");
            measureMZDIFFSTREAM.print(String.valueOf(mz - hypo));
            measureMZDIFFSTREAM.print("\t");
            measureMZDIFFSTREAM.print(String.valueOf((mz - hypo) * 1e6 / (mz)));
            measureMZDIFFSTREAM.print("\n");
        }

        // find noise
        final Deviation dev = analyzer.getDefaultProfile().getAllowedMassDeviation();
        final double[] usedMzs = new double[tree.getFragments().size()];
        int k = 0;
        for (Fragment f : tree.getFragments()) usedMzs[k++] = ano.get(f).getOriginalMz();
        Arrays.sort(usedMzs);
        for (ProcessedPeak peak : input.getMergedPeaks()) {
            int j = Arrays.binarySearch(usedMzs, peak.getOriginalMz());
            if (j < 0) {
                j = -(j + 1);
                if (j < usedMzs.length && (dev.inErrorWindow(usedMzs[j], peak.getOriginalMz()) || dev.inErrorWindow(peak.getOriginalMz(), usedMzs[j]))) {
                    continue;
                }
                --j;
                if (j >= 0 && (dev.inErrorWindow(usedMzs[j], peak.getOriginalMz()) || dev.inErrorWindow(peak.getOriginalMz(), usedMzs[j]))) {
                    continue;
                }

                measureIsoSTREAM.println(peak.getGlobalRelativeIntensity());
            }
        }
    }

    public void initializeFormulaCache() {
        this.formulaCacheFile = getFormulaCacheFile(options.getCachingDirectory(), options.getDatabase());
        this.formulaQuery = initializeFormulaCache(formulaCacheFile, options.getDatabase());
    }

    private int updateFormulaCache(DBMolecularFormulaCache formulaQuery, File formulaCacheFile, int dbchanges) {
        if (formulaQuery != null && (formulaQuery.getChanges() - dbchanges) > 10) {
            updateFormulaCache(formulaCacheFile, formulaQuery);
            dbchanges = formulaQuery.getChanges();
        }
        return dbchanges;
    }

    private void useNaiveApproach(FragmentationPatternAnalysis analyzer, MeasurementProfile profile, Ms2Experiment experiment) {
        experiment = analyzer.validate(experiment);
        final ProcessedInput pinput = analyzer.preprocessing(experiment);
        final DecompositionList list = pinput.getAnnotationOrThrow(DecompositionList.class);
        final ArrayList<MolecularFormula> formulas = new ArrayList<MolecularFormula>();
        for (ScoredMolecularFormula f : list.getDecompositions()) formulas.add(f.getFormula());
        final Ionization ion = experiment.getIonization();
        final double pm = ion.subtractFromMass(pinput.getParentPeak().getMass());
        Collections.sort(formulas, new Comparator<MolecularFormula>() {
            @Override
            public int compare(MolecularFormula elements, MolecularFormula elements2) {
                final double d1 = Math.abs(elements.getMass() - pm);
                final double d2 = Math.abs(elements2.getMass() - pm);
                return Double.compare(d1, d2);
            }
        });
        int rank = 1;
        for (int i = 0; i < formulas.size(); ++i) {
            if (formulas.get(i).equals(experiment.getMolecularFormula())) break;
            else ++rank;
        }
        System.out.println("RANK " + rank);
    }

    private void printResult(FTree tree) {
        final TreeScoring scoring = tree.getAnnotationOrThrow(TreeScoring.class);
        System.out.print(tree.getRoot().getFormula() + " (" + (scoring.getOverallScore() - scoring.getRecalibrationBonus()));
        if (scoring.getRecalibrationBonus() > 1e-6) {
            System.out.print(" -> " + scoring.getOverallScore());
        }
        System.out.println(") explaining " + tree.getFragments().size() + " peaks");
    }

    // treeinformation
    // file,db,correct,score,optscore,mass, ppmdev, mzdev, recppmdev, recmzdev

    private File prettyNameOptTree(FTree tree, File fileName, String suffix) {
        return new File(options.getTarget(), removeExtname(fileName) + suffix);
    }

    private File prettyNameOptTree(FTree tree, File fileName) {
        return prettyNameOptTree(tree, fileName, ".dot");
    }

    private File prettyNameSuboptTree(FTree tree, File fileName, int rank, boolean correct) {
        return new File(new File(options.getTarget(), removeExtname(fileName)), rank + (correct ? "_correct_" : "_") + tree.getRoot().getFormula() + ".dot");
    }

    private String removeExtname(File f) {
        final String name = f.getName();
        final int i = name.lastIndexOf('.');
        return i < 0 ? name : name.substring(0, i);
    }

    protected void statistics(File filename, FTree correct, FTree bestWrong) {
        if (treeStat == null) {
            try {
                treeStat = new PrintStream("treestats.csv");
                openStreams.add(treeStat);
                treeStat.append("file,db,correct,score,optscore,mass,ppmdev,mzdev,recppmdev,recmzdev\n");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        final TreeScoring corSc = correct.getAnnotationOrThrow(TreeScoring.class);
        final TreeScoring wrSc = bestWrong.getAnnotationOrThrow(TreeScoring.class);
        double optScore = (bestWrong == null) ? corSc.getOverallScore() : Math.max(corSc.getOverallScore(), wrSc.getOverallScore());
        treeStat(filename, correct, optScore, true);
        if (bestWrong != null) treeStat(filename, bestWrong, optScore, false);


        if (fragStat == null) {
            try {
                fragStat = new PrintStream("fragstats.csv");
                openStreams.add(fragStat);
                fragStat.println("file,db,correct,shared,formula,mass,recalibrated,alphabet,ppmdev,mzdev,recppmdev,recmzdev,intensity");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        final HashSet<MolecularFormula> shared = new HashSet<MolecularFormula>();
        for (Fragment f : correct.getFragmentsWithoutRoot()) shared.add(f.getFormula());
        fragStats(filename, correct, true, shared);
        if (bestWrong != null) fragStats(filename, bestWrong, false, shared);

        if (lossStats == null) {
            try {
                lossStats = new PrintStream("lossStats.csv");
                openStreams.add(lossStats);
                lossStats.println("file,db,correct,formula,mass,common,alphabet");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        lossStat(filename, correct, true);
        if (bestWrong != null) lossStat(filename, bestWrong, false);
    }

    private void lossStat(File filename, FTree tree, boolean correct) {
        final char db = filename.getName().startsWith("mpos") ? 'm' : 'a';
        final CommonLossEdgeScorer le = FragmentationPatternAnalysis.getByClassName(CommonLossEdgeScorer.class, profile.fragmentationPatternAnalysis.getLossScorers());
        for (Fragment f : tree.getFragmentsWithoutRoot()) {
            final Loss l = f.getIncomingEdge();
            final int common = (le.isCommonLoss(l.getFormula())) ? 1 : (le.isRecombinatedLoss(l.getFormula()) ? 2 : 0);
            final int isChnops = l.getFormula().isCHNO() ? 0 : (l.getFormula().isCHNOPS() ? 1 : 2);
            lossStats.append(filename.getName()).append(',').append(db).append(',').append(correct ? '1' : '0').append(',')
                    .append(l.getFormula().toString()).append(',').append(Double.toString(l.getFormula().getMass())).append(',')
                    .append(String.valueOf(common)).append(',').append(String.valueOf(isChnops)).append('\n');
        }
    }

    private void fragStats(File filename, FTree tree, boolean correct, HashSet<MolecularFormula> shared) {
        final char db = filename.getName().startsWith("mpos") ? 'm' : 'a';
        final FragmentAnnotation<ProcessedPeak> pp = tree.getFragmentAnnotationOrThrow(ProcessedPeak.class);
        final Ionization ion = tree.getAnnotationOrThrow(Ionization.class);
        for (Fragment f : tree.getFragmentsWithoutRoot()) {
            final boolean isShared = shared.contains(f.getFormula());
            final Deviation dev = Deviation.fromMeasurementAndReference(pp.get(f).getOriginalMz(), ion.addToMass(f.getFormula().getMass()));
            final Deviation recdev = Deviation.fromMeasurementAndReference(pp.get(f).getMz(), ion.addToMass(f.getFormula().getMass()));
            final int isChnops = f.getFormula().isCHNO() ? 0 : (f.getFormula().isCHNOPS() ? 1 : 2);
            fragStat.append(filename.getName()).append(',').append(db).append(',').append(correct ? '1' : '0').append(',').append(isShared ? '1' : '0')
                    .append(',').append(f.getFormula().toString()).append(',').append(String.valueOf(pp.get(f).getOriginalMz())).append(',')
                    .append(String.valueOf(pp.get(f).getMz())).append(',').append(String.valueOf(isChnops)).append(',')
                    .append(String.valueOf(dev.getPpm())).append(',').append(String.valueOf(dev.getAbsolute())).append(',').append(String.valueOf(recdev.getPpm())).append(',')
                    .append(String.valueOf(recdev.getAbsolute())).append(',').append(String.valueOf(pp.get(f).getRelativeIntensity())).append('\n');
        }
    }

    private void treeStat(File filename, FTree tree, double optScore, boolean correct) {
        final Ionization ion = tree.getAnnotationOrThrow(Ionization.class);
        final FragmentAnnotation<ProcessedPeak> pp = tree.getFragmentAnnotationOrThrow(ProcessedPeak.class);
        final char db = filename.getName().startsWith("mpos") ? 'm' : 'a';
        final double mass = pp.get(tree.getRoot()).getOriginalMz();
        final Deviation dev = Deviation.fromMeasurementAndReference(pp.get(tree.getRoot()).getOriginalMz(), ion.addToMass(tree.getRoot().getFormula().getMass()));
        final Deviation recdev = Deviation.fromMeasurementAndReference(pp.get(tree.getRoot()).getMz(), ion.addToMass(tree.getRoot().getFormula().getMass()));
        treeStat.append(filename.getName()).append(',').append(db).append(',').append(correct ? '1' : '0').append(',').append(String.valueOf(tree.getAnnotationOrThrow(TreeScoring.class).getOverallScore())).append(',').append(String.valueOf(optScore)).append(',')
                .append(String.valueOf(mass)).append(',').append(String.valueOf(dev.getPpm())).append(',').append(String.valueOf(dev.getAbsolute())).append(',')
                .append(String.valueOf(recdev.getPpm())).append(',').append(String.valueOf(recdev.getAbsolute())).append('\n');
    }

    protected void writeTreeToFile(File f, FTree tree, FragmentationPatternAnalysis pipeline, Double isoScore) {
        tree.normalizeStructure();
        FileWriter fw = null;
        try {
            fw = new FileWriter(f);
            final TreeAnnotation ano = new TreeAnnotation(tree, pipeline);
            final TreeScoring scoring = tree.getAnnotationOrThrow(TreeScoring.class);
            ano.getAdditionalProperties().put(tree.getRoot(), new ArrayList<String>(Arrays.asList("CompoundScore: " + scoring.getOverallScore(), "Explained: " + (int)Math.round(intensityOfTree(tree)*100) + " %")));
            if (isoScore != null) ano.getAdditionalProperties().get(tree.getRoot()).add("Isotope: " + isoScore);
            if (scoring.getRecalibrationBonus() > 0d)
                ano.getAdditionalProperties().put(tree.getRoot(), new ArrayList<String>(Arrays.asList("Rec.Bonus: " + scoring.getRecalibrationBonus())));
            new FTDotWriter().writeTree(fw, tree, ano.getAdditionalProperties(), ano.getVertexAnnotations(), ano.getEdgeAnnotations());
        } catch (IOException e) {
            System.err.println("Error while writing in " + f + " for input ");
            e.printStackTrace();
        } finally {
            if (fw != null) try {
                fw.close();
            } catch (IOException e) {
                System.err.println("Error while writing in " + f + " for input ");
                e.printStackTrace();
            }
        }
    }

}
