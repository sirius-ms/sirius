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

        final Options options = CliFactory.createCli(Options.class).parseArguments(args);

        final List<File> files = InterpretOptions.getFiles(options);
        final MeasurementProfile profile = InterpretOptions.getProfile(options);

        final FragmentationPatternAnalysis analyzer = FragmentationPatternAnalysis.defaultAnalyzer();
        final IsotopePatternAnalysis deIsotope = IsotopePatternAnalysis.defaultAnalyzer();

        for (File f : files) {
            try {
                final List<FragmentationTree> trees = new ArrayList<FragmentationTree>();
                final Ms2Experiment experiment = parseFile(f, profile);
                final ProcessedInput input = analyzer.preprocessing(experiment);
                /*
                final List<ScoredMolecularFormula> formulas = deIsotope.deisotope(experiment).get(0);
                Collections.sort(formulas, Collections.reverseOrder());

                final Iterator<ScoredMolecularFormula> iter = formulas.iterator();
                while (iter.hasNext()) {
                    if (!iter.next().getFormula().equals(experiment.getMolecularFormula())) iter.remove();
                }
                */
                final ArrayList<ScoredMolecularFormula> formulas = new ArrayList<ScoredMolecularFormula>(Arrays.asList(new ScoredMolecularFormula(experiment.getMolecularFormula(), 0d)));

                for (int i=0; i <formulas.size(); ++i) {
                    ScoredMolecularFormula s = formulas.get(i);
                    ScoredMolecularFormula t = null;
                    for (ScoredMolecularFormula x : input.getParentMassDecompositions())
                        if (x.getFormula().equals(s.getFormula())) {
                            t = x;
                            break;
                        }
                    System.out.println("Compute " + s.toString());
                    final FragmentationGraph graph = analyzer.buildGraph(input, new ScoredMolecularFormula(s.getFormula(), s.getScore() + (t == null ? Math.log(0.1d) : t.getScore())));
                    final FragmentationTree tree = analyzer.computeTree(graph);
                    trees.add(tree);
                }
                Collections.sort(trees, Collections.reverseOrder());
                for (int i=0; i < trees.size(); ++i) {
                    System.out.println(trees.get(i).getRoot().getFormula() + " => " + trees.get(i).getScore());
                    writeTreeToFile(new File(f.getName().split("\\.")[0] + "_" + (i+1) + trees.get(i).getRoot().getFormula().toString() + ".dot"), trees.get(i), analyzer);
                }

            } catch (IOException e) {
                System.err.println("Error while parsing " + f + ":\n" + e);
            } catch (Exception e) {
                System.err.println("Error while processing " + f + ":\n" + e);
                e.printStackTrace();
            }
        }


    }

    private static Ms2Experiment parseFile(File f, MeasurementProfile profile) throws IOException {
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

    private static Parser<Ms2Experiment> getParserFor(File f) {
        final String[] extName = f.getName().split("\\.");
        if (extName.length>1 && extName[1].equalsIgnoreCase("ms")){
            return new JenaMsParser();
        } else {
            throw new RuntimeException("No parser found for file " + f);
        }

    }

    protected static void writeTreeToFile(File f, FragmentationTree tree, FragmentationPatternAnalysis pipeline) {
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
