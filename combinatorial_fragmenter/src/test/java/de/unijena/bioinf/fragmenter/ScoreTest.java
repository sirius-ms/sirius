package de.unijena.bioinf.fragmenter;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.Smiles;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.babelms.MsIO;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.sirius.Sirius;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;

public class ScoreTest {

    public static void main(String[] args) {

        PropertyManager.loadSiriusCredentials();
        int cores = Math.max(1, (int)Math.round(Math.floor(Runtime.getRuntime().availableProcessors()*2/3d)));
        SiriusJobs.setGlobalJobManager(cores);
        final TObjectDoubleHashMap<String> bondScores = new TObjectDoubleHashMap<>(1000,0.75f, 0);
        int counter=0;
        final Sirius sirius = new Sirius("qtof");
        for (File file : new File("/home/kaidu/data/ms/sample").listFiles()) {
            if (!file.getName().endsWith(".ms"))
                continue;

            try {
                final Ms2Experiment experiment = MsIO.readExperimentFromFile(file).next();
                if (experiment.getIonMass()>600)
                    continue;
                final FTree tree = sirius.compute(experiment, experiment.getMolecularFormula()).getTree();

                final Set<MolecularFormula> formulaSet = new HashSet<>();
                for (Fragment f : tree.getFragmentsWithoutRoot()) formulaSet.add(f.getFormula().withoutHydrogen());

                final IAtomContainer M = new SmilesParser(SilentChemObjectBuilder.getInstance()).parseSmiles(experiment.getAnnotation(Smiles.class).get().smiles);
                final MolecularGraph graph = new MolecularGraph(M);

                final CombinatorialGraph comb = new CombinatorialFragmenter(graph).createCombinatorialFragmentationGraph((x)->x.depth < 5 && formulaSet.contains(x.fragment.getFormula()));
                comb.pruneLongerPaths();
                final HashMap<MolecularFormula, List<CombinatorialNode>> explainations = new HashMap<>();

                for (CombinatorialNode node : comb.nodes) {
                    if (formulaSet.contains(node.fragment.getFormula())) {
                        explainations.computeIfAbsent(node.fragment.formula, (x)->new ArrayList<>()).add(node);
                    }
                }

                // now add scoring to edges
                final TObjectDoubleHashMap<IBond> bond2score = new TObjectDoubleHashMap<>(graph.bonds.length, 0.75f, 0);
                for (Map.Entry<MolecularFormula,List<CombinatorialNode>> entry : explainations.entrySet()) {
                    final double weightPerEntry = 1d/entry.getValue().size();
                    for (CombinatorialNode node : entry.getValue()) {
                        final double weightPerCut = weightPerEntry/node.depth;
                        // get all bonds which have to be cut to get to this node
                        scoreAllBondsToRoot(node, weightPerCut, bond2score);
                    }
                }

                bond2score.transformValues(x->x/tree.numberOfVertices());
                final TObjectDoubleHashMap<String> reweight = new TObjectDoubleHashMap<>(bond2score.size());
                for (IBond b : graph.bonds) {
                    reweight.adjustOrPutValue(explainBond(b), 1, 1);
                }
                bond2score.forEachEntry((bond,score)->{
                    final String bondName = explainBond(bond);
                    final double scoring = score/reweight.get(bondName);
                    bondScores.adjustOrPutValue(bondName, scoring,scoring);
                    return true;
                });


                if (++counter % 50 == 0) {
                    System.out.println("-----------------------");
                    final ArrayList<String> alist = new ArrayList<>(bondScores.keySet());
                    alist.sort(Comparator.comparingDouble(s->-bondScores.get(s)));
                    for (String s : alist) {
                        System.out.println(s + "\t" + bondScores.get(s));
                    }
                    System.out.println("-----------------------");
                }

            } catch (IOException | InvalidSmilesException e) {
                e.printStackTrace();
            }

        }

    }

    public static void trySingleOne() {
        try {
            final Ms2Experiment experiment = MsIO.readExperimentFromFile(new File("/home/kaidu/temp/example.ms")).next();
            final FTree tree = MsIO.readTreeFromFile(new File("/home/kaidu/temp/example.json"));

            final Set<MolecularFormula> formulaSet = new HashSet<>();
            for (Fragment f : tree.getFragmentsWithoutRoot()) formulaSet.add(f.getFormula().withoutHydrogen());

            final IAtomContainer M = new SmilesParser(SilentChemObjectBuilder.getInstance()).parseSmiles(experiment.getAnnotation(Smiles.class).get().smiles);
            final MolecularGraph graph = new MolecularGraph(M);

            final CombinatorialGraph comb = new CombinatorialFragmenter(graph).createCombinatorialFragmentationGraph((x)->x.depth < 5 && formulaSet.contains(x.fragment.getFormula()));
            comb.pruneLongerPaths();
            final HashMap<MolecularFormula, List<CombinatorialNode>> explainations = new HashMap<>();

            for (CombinatorialNode node : comb.nodes) {
                if (formulaSet.contains(node.fragment.getFormula())) {
                    explainations.computeIfAbsent(node.fragment.formula, (x)->new ArrayList<>()).add(node);
                }
            }

            // now add scoring to edges
            final TObjectDoubleHashMap<IBond> bond2score = new TObjectDoubleHashMap<>(graph.bonds.length, 0.75f, 0);
            for (Map.Entry<MolecularFormula,List<CombinatorialNode>> entry : explainations.entrySet()) {
                final double weightPerEntry = 1d/entry.getValue().size();
                for (CombinatorialNode node : entry.getValue()) {
                    final double weightPerCut = weightPerEntry/node.depth;
                    // get all bonds which have to be cut to get to this node
                    scoreAllBondsToRoot(node, weightPerCut, bond2score);
                }
            }

            bond2score.transformValues(x->x/tree.numberOfVertices());

            for (IBond b : bond2score.keySet()) {
                System.out.println(explainBond(b) + "\t" + bond2score.get(b));
            }

        } catch (IOException | InvalidSmilesException e) {
            e.printStackTrace();
        }

    }

    private static String explainBond(IBond b) {
        String[] atomLabels = new String[]{b.getAtom(0).getAtomTypeName(),b.getAtom(1).getAtomTypeName()};
        Arrays.sort(atomLabels);
        String s = atomLabels[0];
        if (b.isAromatic()) {
            s += ":";
        } else {
            switch (b.getOrder()) {
                case SINGLE:
                    s += "-";
                    break;
                case DOUBLE:
                    s += "=";
                    break;
                case TRIPLE:
                    s += "#";
                    break;
                default:
                    s += "?";
            }
        }
        s += atomLabels[1];
        return s;
    }

    private static void scoreAllBondsToRoot(CombinatorialNode node, double weightPerCut, TObjectDoubleHashMap<IBond> bond2score) {
        weightPerCut /= node.incomingEdges.size();
        for (CombinatorialEdge edge : node.incomingEdges) {
            if (edge.cut2!=null) {
                double s = weightPerCut/2d;
                bond2score.adjustOrPutValue(edge.cut1, s,s);
                bond2score.adjustOrPutValue(edge.cut2, s,s);
                if (edge.source.depth>0) scoreAllBondsToRoot(edge.source, weightPerCut,bond2score);
            } else {
                bond2score.adjustOrPutValue(edge.cut1, weightPerCut,weightPerCut);
                if (edge.source.depth>0) scoreAllBondsToRoot(edge.source, weightPerCut,bond2score);
            }
        }

    }

}
