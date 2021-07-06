package de.unijena.bioinf.ms.gui.ms_viewer;

import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.chemdb.CompoundCandidate;
import de.unijena.bioinf.fragmenter.AnnotateFragmentationTree;
import de.unijena.bioinf.fragmenter.DirectedBondTypeScoring;
import de.unijena.bioinf.fragmenter.MolecularGraph;
import de.unijena.bioinf.jjobs.BasicMasterJJob;
import de.unijena.bioinf.projectspace.FormulaResultBean;
import org.openscience.cdk.depict.DepictionGenerator;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;

import java.util.ArrayList;
import java.util.Optional;

public class InsilicoFragmenter {

    public Job fragmentJob(FormulaResultBean sre, CompoundCandidate candidate) {
        return new Job(sre, candidate);
    }

    public class Job extends BasicMasterJJob<Result> {

        final FormulaResultBean sre;
        final CompoundCandidate candidate;

        public Job(FormulaResultBean sre, CompoundCandidate candidate) {
            super(JobType.TINY_BACKGROUND);
            this.sre = sre;
            this.candidate = candidate;
        }


        @Override
        protected Result compute() throws Exception {
            final Optional<FTree> tree = sre.getFragTree();

            if (tree.isEmpty()) return null;
            if (candidate == null) return null;

            checkForInterruption();

            final DirectedBondTypeScoring scoring = new DirectedBondTypeScoring();
            final MolecularGraph graph = new MolecularGraph(
                    new SmilesParser(SilentChemObjectBuilder.getInstance()).parseSmiles(candidate.getSmiles())
            );

            checkForInterruption();

            AnnotateFragmentationTree.Job annos = new AnnotateFragmentationTree(tree.get(), graph, scoring).makeJJob();
            submitSubJob(annos.asType(JobType.TINY_BACKGROUND)).awaitResult();

            checkForInterruption();

            DepictionGenerator gen = new DepictionGenerator();
            String svg = gen.withAromaticDisplay().withAtomColors().depict(graph.getMolecule()).toSvgStr();

            checkForInterruption();

            return new Result(annos.result(), annos.getJson(), svg);
        }
    }

    public static class Result {
        private final ArrayList<AnnotateFragmentationTree.Entry> ano;
        private final String json;
        private final String svg;

        public Result(ArrayList<AnnotateFragmentationTree.Entry> ano, String json, String svg) {
            this.ano = ano;
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
