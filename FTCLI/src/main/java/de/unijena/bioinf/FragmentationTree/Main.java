package de.unijena.bioinf.FragmentationTree;

import com.lexicalscope.jewel.cli.CliFactory;
import de.unijena.bioinf.ChemistryBase.chem.Charge;
import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.chem.utils.ScoredMolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.MeasurementProfile;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.FragmentationPatternAnalysis;
import de.unijena.bioinf.FragmentationTreeConstruction.inspection.TreeAnnotation;
import de.unijena.bioinf.FragmentationTreeConstruction.model.FragmentationGraph;
import de.unijena.bioinf.FragmentationTreeConstruction.model.FragmentationTree;
import de.unijena.bioinf.FragmentationTreeConstruction.model.Ms2ExperimentImpl;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.IsotopePatternAnalysis.IsotopePatternAnalysis;
import de.unijena.bioinf.babelms.GenericParser;
import de.unijena.bioinf.babelms.Parser;
import de.unijena.bioinf.babelms.dot.FTDotWriter;
import de.unijena.bioinf.babelms.ms.JenaMsParser;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class Main {

    public static void main(String[] args) {
        new Main().run(args);
    }

    private Options options;

    void run(String[] args) {
        options = CliFactory.createCli(Options.class).parseArguments(args);

        final List<File> files = InterpretOptions.getFiles(options);
        final MeasurementProfile profile = InterpretOptions.getProfile(options);

        final FragmentationPatternAnalysis analyzer = FragmentationPatternAnalysis.defaultAnalyzer();
        final IsotopePatternAnalysis deIsotope = IsotopePatternAnalysis.defaultAnalyzer();

        final File target = options.getTarget();
        if (!target.exists()) target.mkdirs();

        final int maxNumberOfTrees = InterpretOptions.maxNumberOfTrees(options);

        for (File f : files) {
            try {
                if (options.getTrees()>0) {
                    final File tdir = new File(options.getTarget(), removeExtname(f));
                    if (tdir.exists() && !tdir.isDirectory()) {
                        throw new RuntimeException("Cannot create directory '" + tdir.getAbsolutePath() +"': File still exists!");
                    }
                    tdir.mkdir();
                }

                final Ms2Experiment experiment = parseFile(f, profile);
                final MolecularFormula correctFormula = experiment.getMolecularFormula(); // TODO: charge
                final ProcessedInput input = analyzer.preprocessing(experiment);

                // First: Compute correct tree
                FragmentationTree correctTree = null;
                if (experiment.getMolecularFormula() != null) {
                    correctTree = analyzer.computeTrees(input).onlyWith(Arrays.asList(correctFormula)).optimalTree();
                }

                double lowerbound = options.getLowerbound();
                if (options.getWrongPositive() && correctTree != null) lowerbound = Math.max(lowerbound, correctTree.getScore());

                if (options.getTrees()>0) {
                    final List<FragmentationTree> trees = analyzer.computeTrees(input).inParallel(options.getThreads()).computeMaximal(maxNumberOfTrees).withLowerbound(lowerbound)
                            .without(correctFormula!=null ? Arrays.asList(correctFormula) : (List<MolecularFormula>)Arrays.asList()).list();
                    if (correctTree != null) {
                        trees.add(correctTree);
                        Collections.sort(trees);
                    }
                    for (int i=0; i < trees.size(); ++i) {
                        final FragmentationTree tree = trees.get(i);
                        System.out.println(trees.get(i).getRoot().getFormula() + " => " + trees.get(i).getScore());
                        writeTreeToFile(prettyNameSuboptTree(tree, f, i+1, tree==correctTree), tree, analyzer);
                    }
                }

            } catch (IOException e) {
                System.err.println("Error while parsing " + f + ":\n" + e);
            } catch (Exception e) {
                System.err.println("Error while processing " + f + ":\n" + e);
                e.printStackTrace();
            }
        }
    }

    private File prettyNameOptTree(FragmentationTree tree, File fileName) {
        return new File(removeExtname(fileName) + ".gv");
    }
    private File prettyNameSuboptTree(FragmentationTree tree, File fileName, int rank, boolean correct) {
        return new File(removeExtname(fileName), rank + (correct ? "_opt_" : "_") + tree.getRoot().getFormula() + ".gv");
    }

    private String removeExtname(File f) {
        final String name = f.getName();
        final int i= name.lastIndexOf('.');
        return i<0 ? name : name.substring(0, i);
    }

    private Ms2Experiment parseFile(File f, MeasurementProfile profile) throws IOException {
        final GenericParser<Ms2Experiment> parser = new GenericParser<Ms2Experiment>(getParserFor(f));
        final Ms2Experiment experiment = parser.parseFile(f);
        final Ms2ExperimentImpl impl = new Ms2ExperimentImpl(experiment);
        {
            final MolecularFormula formula = experiment.getMolecularFormula();
            final double ionMass = experiment.getIonMass() - experiment.getMoleculeNeutralMass();
            final Ionization ion = PeriodicTable.getInstance().ionByMass(ionMass, 1e-3, experiment.getIonization().getCharge());
            impl.setIonization(ion);
            impl.setMergedMs1Spectrum(impl.getMs1Spectra().get(0));
        }
        impl.setMeasurementProfile(profile);
        return impl;
    }

    private Parser<Ms2Experiment> getParserFor(File f) {
        final String[] extName = f.getName().split("\\.");
        if (extName.length>1 && extName[1].equalsIgnoreCase("ms")){
            return new JenaMsParser();
        } else {
            throw new RuntimeException("No parser found for file " + f);
        }

    }

    protected void writeTreeToFile(File f, FragmentationTree tree, FragmentationPatternAnalysis pipeline) {
        FileWriter fw = null;
        try {
            fw =  new FileWriter(f);
            final TreeAnnotation ano = new TreeAnnotation(tree, pipeline);
            new FTDotWriter().writeTree(fw, tree, ano.getVertexAnnotations(), ano.getEdgeAnnotations());
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
