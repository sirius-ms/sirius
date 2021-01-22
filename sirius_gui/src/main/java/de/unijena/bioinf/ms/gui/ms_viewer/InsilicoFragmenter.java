package de.unijena.bioinf.ms.gui.ms_viewer;

import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.chemdb.CompoundCandidate;
import de.unijena.bioinf.fragmenter.AnnotateFragmentationTree;
import de.unijena.bioinf.fragmenter.DirectedBondTypeScoring;
import de.unijena.bioinf.fragmenter.MolecularGraph;
import de.unijena.bioinf.ms.gui.mainframe.result_panel.tabs.SpectraVisualizationPanel;
import de.unijena.bioinf.projectspace.FormulaResultBean;
import org.openscience.cdk.depict.DepictionGenerator;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

public class InsilicoFragmenter {
    SpectraVisualizationPanel panel;
    public InsilicoFragmenter(SpectraVisualizationPanel panel) {
        this.panel = panel;
    }

    public boolean fragment(FormulaResultBean sre, CompoundCandidate candidate) {

        final Optional<FTree> tree = sre.getFragTree();
        if (tree.isEmpty()) return false;
        if (candidate==null) return false;
        new Worker(tree.get(), candidate).execute();
        return true;
    }

    public static class Result {
        private AnnotateFragmentationTree ano;
        private String json, svg;

        public Result(AnnotateFragmentationTree ano, String json, String svg) {
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

    protected class Worker extends SwingWorker<Result, Object> {
        private FTree tree;
        private CompoundCandidate candidate;
        public Worker(FTree tree, CompoundCandidate candidate) {
            this.tree = tree;
            this.candidate = candidate;
        }

        @Override
        protected void done() {
            try {
                final Result annotation = get();
                panel.setInsilicoResult(annotation);
            } catch (InterruptedException | ExecutionException e) {
                LoggerFactory.getLogger(InsilicoFragmenter.class).error(e.getMessage(),e);
            }

        }

        @Override
        protected Result doInBackground() throws Exception {
            // We should do this via our jobsystem, but I do not find any
            // integration to swing?
            final DirectedBondTypeScoring scoring = new DirectedBondTypeScoring();
            final MolecularGraph graph = new MolecularGraph(
                    new SmilesParser(SilentChemObjectBuilder.getInstance()).parseSmiles(candidate.getSmiles())
            );
            final AnnotateFragmentationTree annotateFragmentationTree = new AnnotateFragmentationTree(tree, graph, scoring);
            DepictionGenerator gen = new DepictionGenerator();
            return new Result(annotateFragmentationTree,
                    annotateFragmentationTree.getJson(),
                    gen.withAromaticDisplay().withAtomColors().depict(graph.getMolecule()).toSvgStr()
                    );

        }
    }

}
