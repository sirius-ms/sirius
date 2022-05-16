package de.unijena.bioinf.ms.gui.ms_viewer;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.chemdb.CompoundCandidate;
import de.unijena.bioinf.fragmenter.*;
import de.unijena.bioinf.jjobs.BasicMasterJJob;
import de.unijena.bioinf.projectspace.FormulaResultBean;
import org.openscience.cdk.depict.DepictionGenerator;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;

public class InsilicoFragmenter {

    public Job fragmentJob(FormulaResultBean sre, CompoundCandidate candidate, boolean experimentalMode) {
        return new Job(sre, candidate, experimentalMode);
    }

    public class Job extends BasicMasterJJob<Result> {

        final FormulaResultBean sre;
        final CompoundCandidate candidate;
        final boolean experimental;

        public Job(FormulaResultBean sre, CompoundCandidate candidate, boolean experimental) {
            super(JobType.TINY_BACKGROUND);
            this.sre = sre;
            this.candidate = candidate;
            this.experimental = experimental;
        }


        @Override
        protected Result compute() throws Exception {
            System.out.println("STARTE INSILICO FRAGMENTIERUNG. Das SOLLTE nur einmal aufgerufen werden.");
            final Optional<FTree> tree = sre.getFragTree();

            if (tree.isEmpty()) return null;
            if (candidate == null) return null;

            checkForInterruption();
            final MolecularGraph graph = new MolecularGraph(
                    new SmilesParser(SilentChemObjectBuilder.getInstance()).parseSmiles(candidate.getSmiles())
            );
            checkForInterruption();

            String json = null;
            if (experimental) {
                final EMFragmenterScoring2 scoring = new EMFragmenterScoring2(graph, tree.get());
                final CriticalPathSubtreeCalculator calc = new CriticalPathSubtreeCalculator(tree.get(), graph, scoring, true);
                calc.setMaxNumberOfNodes(50000);
                final HashSet<MolecularFormula> fset = new HashSet<>();
                for (Fragment ft : tree.get().getFragmentsWithoutRoot()) {
                    fset.add(ft.getFormula());
                    fset.add(ft.getFormula().add(MolecularFormula.getHydrogen()));
                    fset.add(ft.getFormula().add(MolecularFormula.getHydrogen().multiply(2)));
                    if (ft.getFormula().numberOfHydrogens()>0) fset.add(ft.getFormula().subtract(MolecularFormula.getHydrogen()));
                    if (ft.getFormula().numberOfHydrogens()>1) fset.add(ft.getFormula().subtract(MolecularFormula.getHydrogen().multiply(2)));
                }
                try {
                    calc.initialize((node, nnodes, nedges) -> {
                        try {
                            checkForInterruption();
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        if (fset.contains(node.getFragment().getFormula())) return true;
                        return (node.getTotalScore() > -5f);
                    });
                } catch (RuntimeException e) {
                    if (e.getCause() instanceof InterruptedException) {
                        throw (InterruptedException)e.getCause();
                    } else throw e;
                }
                checkForInterruption();
                calc.computeSubtree();
                json = CombinatorialSubtreeCalculatorJsonWriter.writeResultForVisualization(tree.get(), calc);
            } else {
                final EMFragmenterScoring2 scoring = new EMFragmenterScoring2(graph,tree.get());//DirectedBondTypeScoring();
                AnnotateFragmentationTree.Job annos = new AnnotateFragmentationTree(tree.get(), graph, scoring).makeJJob();
                submitSubJob(annos.asType(JobType.TINY_BACKGROUND)).awaitResult();
                json = annos.getJson();
            }
            checkForInterruption();

            DepictionGenerator gen = new DepictionGenerator();
            String svg = gen.withAromaticDisplay().withAtomColors().depict(graph.getMolecule()).toSvgStr();

            checkForInterruption();
            System.out.println(json);
            return new Result(json, svg);
        }
    }

    public static class Result {
        private final String json;
        private final String svg;

        public Result(String json, String svg) {
            this.json = json;
            this.svg = svg;
        }

        public String getJson() {
            return json;
        }

        public String getSvg() {
            return svg;
        }
    }
}
