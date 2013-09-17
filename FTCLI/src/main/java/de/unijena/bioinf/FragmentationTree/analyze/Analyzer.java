package de.unijena.bioinf.FragmentationTree.analyze;

import com.lexicalscope.jewel.cli.CliFactory;
import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.chem.utils.ScoredMolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.FragmentationTree.Profile;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.FragmentationPatternAnalysis;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.MultipleTreeComputation;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.TreeIterator;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.filtering.LimitNumberOfPeaksFilter;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.filtering.NoiseThresholdFilter;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.TreeSizeScorer;
import de.unijena.bioinf.FragmentationTreeConstruction.inspection.TreeAnnotation;
import de.unijena.bioinf.FragmentationTreeConstruction.model.*;
import de.unijena.bioinf.IsotopePatternAnalysis.IsotopePattern;
import de.unijena.bioinf.babelms.GenericParser;
import de.unijena.bioinf.babelms.dot.FTDotWriter;
import de.unijena.bioinf.babelms.ms.JenaMsParser;
import de.unijena.bioinf.sirius.cli.BasicProfileOptions;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.THashMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import gnu.trove.procedure.TObjectDoubleProcedure;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Analyzer {

    public static final String CITE = "Computing fragmentation trees from tandem mass spectrometry data\n" +
            "Florian Rasche, Aleš Svatoš, Ravi Kumar Maddula, Christoph Böttcher and Sebastian Böcker\n" +
            "Anal Chem, 83(4):1243-1251, 2011.";
    public static final String VERSION = "1.21";
    public static final String USAGE = "analyze -n 10 <file1> <file2>";
    public final static String VERSION_STRING = "FragmentationPatternAnalysis " + VERSION + "\n" + CITE + "\nusage:\n" + USAGE;

    private Profile profile;
    private List<Parseable> compounds;
    private boolean verbose;
    private File root;

    public static void main(String[] args) {
        AnalyzeOptions opts = CliFactory.createCli(AnalyzeOptions.class).parseArguments(args);
        final List<String> input = opts.getInput();
        final List<Parseable> compounds = new ArrayList<Parseable>();
        parseInput(input, compounds);
        final Analyzer analyzer = new Analyzer(opts, compounds, opts.getVerbose());
        analyzer.setRoot(opts.getTarget());
        if (!opts.getTarget().exists()) opts.getTarget().mkdir();
        for (Parseable c : compounds) {
            try {
                analyzer.analyzeCompound(c, opts.getNumber());
            } catch (IOException e) {
                System.err.println("Error while computing '" + c.getCompoundName() + "': "  + e.getMessage());
            }
        }
    }

    private static void parseInput(List<String> input, List<Parseable> compounds) {
        MultiFileSpectrum mf = null;
        for (int i=0; i < input.size(); ++i) {
            final String s = input.get(i);
            if (s.endsWith(".ms") || s.endsWith(".ms2") || s.endsWith(".msn")) {
                compounds.add(new OneFileSpectrum(new File(s)));
            } else if (s.contains("@")) {
                final String[] values = s.split("@");
                String name = values[0];
                if (name.isEmpty()) name = "Unknown" + (i+1);
                double pm = Double.parseDouble(values[1]);
                mf = new MultiFileSpectrum(name, pm);
                compounds.add(mf);
            } else if (s.startsWith("[")) {
                final Ionization ion = PeriodicTable.getInstance().ionByName(s);
                mf.ionization = ion;
            } else {
                final String[] xs = s.split(":");
                final File f1 = new File(xs[0]);
                mf.files.add(f1);
                mf.collisionEnergyies.add(xs.length>1 ? xs[1] : null);
            }
        }
    }

    public Analyzer(BasicProfileOptions options, List<Parseable> compounds, boolean verbose) {
        this.compounds = compounds;
        this.verbose = verbose;
        try {
            profile = new Profile(options.getProfile());
            updateFromOptions(profile, options);
        } catch (IOException e) {
            throw new RuntimeException("Cannot find profile '" + options.getProfile() + "'");
        }
    }

    public boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public File getRoot() {
        return root;
    }

    public void setRoot(File root) {
        this.root = root;
    }

    public void analyzeCompound(Parseable parseable, SelectionOption selOpt) throws IOException {
        final Ms2Experiment experiment = parseable.getExperiment();
        final String name = parseable.getCompoundName();
        final File dir = new File(root, name);
        if (!dir.exists()) dir.mkdir();
        ProcessedInput pinput = profile.fragmentationPatternAnalysis.preprocessing(experiment);
        final TObjectDoubleHashMap<MolecularFormula> isoScores = new TObjectDoubleHashMap<MolecularFormula>(pinput.getParentMassDecompositions().size());
        FragmentationPatternAnalysis.getOrCreateByClassName(IsotopeScorer.class, profile.fragmentationPatternAnalysis.getRootScorers()).isoScores = new TObjectDoubleHashMap<MolecularFormula>(0, 0);
        final List<FragmentationTree> trees = new ArrayList<FragmentationTree>();
        msg("Analyze '" + name + "'");
        if (experiment.getMs1Spectra() != null && !experiment.getMs1Spectra().isEmpty()) {
            msg("analyze ms1 peaks");
            pinput = ms1Analysis(experiment, pinput, isoScores);
        }
        final ArrayList<ScoredMolecularFormula> candidates = new ArrayList<ScoredMolecularFormula>(pinput.getParentMassDecompositions());
        FragmentationPatternAnalysis.getOrCreateByClassName(IsotopeScorer.class, profile.fragmentationPatternAnalysis.getRootScorers()).isoScores = isoScores;
        if (experiment.getMs2Spectra() != null && !experiment.getMs2Spectra().isEmpty()) {
            msg("analyze ms2 peaks: " + pinput.getParentMassDecompositions().size() + " decompositions");
            MultipleTreeComputation multi = profile.fragmentationPatternAnalysis.computeTrees(pinput);
            if (selOpt.isRestricted())  {
                if (selOpt.maxNumberOfTrees()>0) multi = multi.computeMaximal(selOpt.maxNumberOfTrees());
                else if (selOpt.specificFormula()!=null) multi=multi.onlyWith(selOpt.specificFormula());
            }
            final TreeIterator iter =multi.iterator();
            while (iter.hasNext()) {
                final FragmentationTree tree = iter.next();
                msg("'" + tree.getRoot().getFormula().toString() + "' (" +tree.getScore() + ")" );
                trees.add(tree);
            }
        }
        printStatistics(dir, isoScores, trees, candidates);
        printTrees(dir,trees);
    }

    private void printTrees(File dir, List<FragmentationTree> trees) {
        int rank = 1;
        for (FragmentationTree t : trees) {
            final String name = rank++ + "_" + t.getRoot().getFormula().toString() + ".dot";
            final File f = new File(dir, name);
            FileWriter fw = null;
            try {
                fw =  new FileWriter(new File(dir, name));
                final TreeAnnotation ano = new TreeAnnotation(t, profile.fragmentationPatternAnalysis);
                if (t.getRecalibrationBonus()>0d) ano.getAdditionalProperties().put(t.getRoot(), new ArrayList<String>(Arrays.asList("Rec.Bonus: " + t.getRecalibrationBonus())));
                new FTDotWriter().writeTree(fw, t, ano.getAdditionalProperties(), ano.getVertexAnnotations(), ano.getEdgeAnnotations());
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

    private void printStatistics(File dir, TObjectDoubleHashMap<MolecularFormula> isoScores, List<FragmentationTree> trees, List<ScoredMolecularFormula> candidates) throws FileNotFoundException {
        final PrintStream statistics = new PrintStream(new File(dir, "candidates.csv"));
        final THashMap<MolecularFormula, FragmentationTree> treemap = new THashMap<MolecularFormula, FragmentationTree>(trees.size());
        for (FragmentationTree tree : trees) treemap.put(tree.getRoot().getFormula(), tree);
        for (int i=0; i < candidates.size(); ++i) {
            final MolecularFormula f = candidates.get(i).getFormula();
            final FragmentationTree t =treemap.get(f);
            candidates.set(i, new ScoredMolecularFormula(f, (t!=null ? t.getScore() : 0d) + isoScores.get(f)));
        }
        Collections.sort(candidates, Collections.reverseOrder());
        statistics.println("\formula\",\"massdev ppm\",\"massdev abs\",\"score\",\"isotope score\",\"tree score\",\"\"explained peaks\",\"explained intensity\"");
        for (int i=0; i < candidates.size(); ++i) {
            final MolecularFormula f = candidates.get(i).getFormula();
            final FragmentationTree t =treemap.get(f);
            final int explainedNumberOfPeaks = t.numberOfVertices();
            final double explainedIntensity = getExplainedIntensity(t);
            statistics.print(f.toString());
            statistics.print(",");
            statistics.print(1e6*(t.getRoot().getPeak().getMz()-(t.getIonization().addToMass(f.getMass())))/t.getRoot().getPeak().getMz());
            statistics.print(",");
            statistics.print(t.getRoot().getPeak().getMz()-(t.getIonization().addToMass(f.getMass())));
            statistics.print(",");
            statistics.print(t.getScore());
            statistics.print(",");
            statistics.print(isoScores.get(f));
            statistics.print(",");
            statistics.print(t.getScore()-isoScores.get(f));
            statistics.print(",");
            statistics.print(explainedNumberOfPeaks);
            statistics.print(",");
            statistics.print(explainedIntensity*100);
            statistics.print("\n");
        }
        statistics.close();
    }

    private double getExplainedIntensity(FragmentationTree t) {
        final List<ProcessedPeak> spectrum = t.getInput().getMergedPeaks();
        double maxIntensity = 0d;
        double treeIntensity = 0d;
        final Deviation dev = t.getInput().getExperimentInformation().getMeasurementProfile().getAllowedMassDeviation();
        for (ProcessedPeak p : spectrum) {
            maxIntensity += p.getIntensity();
            for (TreeFragment f : t.getFragments()) {
                if (dev.inErrorWindow(f.getPeak().getMz(), p.getMz())) {
                    treeIntensity += p.getIntensity();
                }
            }
        }
        return treeIntensity/maxIntensity;
    }

    private ProcessedInput ms1Analysis(Ms2Experiment experiment, ProcessedInput pinput, TObjectDoubleHashMap<MolecularFormula> isoScores) {
        Deviation allowedDev = profile.isotopePatternAnalysis.getDefaultProfile().getAllowedMassDeviation();
        final List<IsotopePattern> isoPatterns = profile.isotopePatternAnalysis.deisotope(experiment);
        if (isoPatterns.isEmpty()) return pinput;
        IsotopePattern iso = null;
        for (IsotopePattern pattern : isoPatterns) {
            if (allowedDev.inErrorWindow(pinput.getParentPeak().getMz(), pattern.getMonoisotopicMass())) {
                if (iso == null || iso.getBestScore() < pattern.getBestScore()) {
                    iso=pattern;
                }
            }
        }
        if (iso==null){
            for (IsotopePattern pattern : isoPatterns) {
                if (allowedDev.inErrorWindow(experiment.getIonMass(), pattern.getMonoisotopicMass())) {
                    if (iso == null || iso.getBestScore() < pattern.getBestScore()) {
                        iso=pattern;
                    }
                }
            }
            // replace parent peak in ms2 by isotope peak
            pinput.getParentPeak().setMz(iso.getMonoisotopicMass());
            pinput = new ProcessedInput(pinput.getExperimentInformation(), pinput.getOriginalInput(), pinput.getMergedPeaks(), pinput.getParentPeak(), Collections.<ScoredMolecularFormula>emptyList(), pinput.getPeakScores(), pinput.getPeakPairScores());
        }
        msg("Found ms1 pattern");
        // add iso candidates to tree candidates
        final TObjectDoubleHashMap<MolecularFormula> treeScores = new TObjectDoubleHashMap<MolecularFormula>();
        for (ScoredMolecularFormula f : pinput.getParentMassDecompositions()) treeScores.put(f.getFormula(),f.getScore());
        for (ScoredMolecularFormula f : iso.getCandidates()) isoScores.put(f.getFormula(), f.getScore());
        final ArrayList<ScoredMolecularFormula> formulas = new ArrayList<ScoredMolecularFormula>();
        isoScores.forEachEntry(new TObjectDoubleProcedure<MolecularFormula>() {
            @Override
            public boolean execute(MolecularFormula a, double b) {
                treeScores.adjustOrPutValue(a, b, b);
                return true;
            }
        });
        treeScores.forEachEntry(new TObjectDoubleProcedure<MolecularFormula>() {
            @Override
            public boolean execute(MolecularFormula a, double b) {
                formulas.add(new ScoredMolecularFormula(a, b));
                return true;
            }
        });
        Collections.sort(formulas, Collections.reverseOrder());
        return new ProcessedInput(pinput.getExperimentInformation(), pinput.getOriginalInput(), pinput.getMergedPeaks(), pinput.getParentPeak(), formulas, pinput.getPeakScores(), pinput.getPeakPairScores());
    }

    public void msg(String msg) {
        if (verbose) System.out.println(msg);
    }

    private void updateFromOptions(Profile profile, BasicProfileOptions opts) {
        BasicProfileOptions.Interpreter.merge(profile.fragmentationPatternAnalysis.getDefaultProfile(), opts);
        final FormulaConstraints constraints = BasicProfileOptions.Interpreter.getFormulaConstraints(opts);
        profile.fragmentationPatternAnalysis.getDefaultProfile().setFormulaConstraints(constraints);
        MutableMeasurementProfile prof = new MutableMeasurementProfile(profile.isotopePatternAnalysis.getDefaultProfile());
        BasicProfileOptions.Interpreter.merge(prof, opts);
        prof.setFormulaConstraints(constraints);
        profile.isotopePatternAnalysis.setDefaultProfile(prof);
        if (opts.getLimit()!=null)
            FragmentationPatternAnalysis.getOrCreateByClassName(LimitNumberOfPeaksFilter.class, profile.fragmentationPatternAnalysis.getPostProcessors()).setLimit(opts.getLimit());
        if (opts.getThreshold()!=null)
            FragmentationPatternAnalysis.getOrCreateByClassName(NoiseThresholdFilter.class, profile.fragmentationPatternAnalysis.getPostProcessors()).setThreshold(opts.getThreshold());
        if (opts.getTreeSize()!=null)
            FragmentationPatternAnalysis.getOrCreateByClassName(TreeSizeScorer.class, profile.fragmentationPatternAnalysis.getFragmentPeakScorers()).setTreeSizeScore(opts.getTreeSize());

    }

    private static class OneFileSpectrum implements Parseable {

        private File file;

        private OneFileSpectrum(File file) {
            this.file = file;
        }

        @Override
        public Ms2Experiment getExperiment() throws IOException {
            final JenaMsParser parser = new JenaMsParser();
            return new GenericParser<Ms2Experiment>(parser).parseFile(file);
        }

        @Override
        public String getCompoundName() {
            final String n = file.getName();
            return n.substring(0,n.indexOf('.'));
        }
    }

    private static class MultiFileSpectrum implements Parseable {

        private String name;
        private double parentmass;
        private Ionization ionization;
        private List<File> files;
        private List<String> collisionEnergyies;

        private MultiFileSpectrum(String name, double parentmass) {
            this.name = name;
            this.parentmass = parentmass;
            this.ionization = PeriodicTable.getInstance().ionByName("[M+H]+");
            this.files = new ArrayList<File>();
            this.collisionEnergyies = new ArrayList<String>();
        }

        @Override
        public Ms2Experiment getExperiment() throws IOException {
            final Ms2ExperimentImpl experiment = new Ms2ExperimentImpl();
            experiment.setMs2Spectra(new ArrayList<Ms2Spectrum>());
            experiment.setMs1Spectra(new ArrayList<Spectrum<Peak>>());
            experiment.setIonization(ionization);
            experiment.setIonMass(parentmass);
            for (int i=0; i < files.size(); ++i) {
                final File f = files.get(i);
                final Spectrum<Peak> spectrum = parseCsv(f);
                final String energy = collisionEnergyies.get(i);
                if (energy==null || energy.trim().isEmpty()) {
                    experiment.getMs2Spectra().add(new Ms2SpectrumImpl(spectrum, new CollisionEnergy(0, 0), parentmass, 0));
                } else if (energy.matches("ms1")) {
                    experiment.getMs1Spectra().add(spectrum);
                    experiment.setMergedMs1Spectrum(spectrum);
                } else {
                    final String[] ce = energy.split("-");
                    final double x1 = Double.parseDouble(ce[0]);
                    if (ce.length>1) {
                        final double x2 = Double.parseDouble(ce[1]);
                        experiment.getMs2Spectra().add(new Ms2SpectrumImpl(spectrum, new CollisionEnergy(x1,x2), parentmass, 0));
                    } else {
                        experiment.getMs2Spectra().add(new Ms2SpectrumImpl(spectrum, new CollisionEnergy(x1,x1), parentmass, 0));
                    }
                }
            }
            return experiment;
        }

        private Spectrum<Peak> parseCsv(File f) throws IOException {
            final BufferedReader reader = new BufferedReader(new FileReader(f));
            final SimpleMutableSpectrum spec = new SimpleMutableSpectrum();
            while (reader.ready()) {
                final String line = reader.readLine();
                if (line == null) break;
                final String[] content = line.split("\\s+|,|;");
                final double x1 = Double.parseDouble(content[0]);
                final double x2 = Double.parseDouble(content[1]);
                spec.addPeak(new Peak(x1, x2));
            }
            return spec;
        }

        @Override
        public String getCompoundName() {
            return name;
        }
    }

}
