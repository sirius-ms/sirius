package de.unijena.bioinf.fragmenter;

import gurobi.GRBException;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;

public class PCSTFragmentationTreeAnnotatorTest {

    public static void main(String[] args){
        try {
            String smiles = "C1CC1";
            SmilesParser parser = new SmilesParser(SilentChemObjectBuilder.getInstance());
            MolecularGraph molecule = new MolecularGraph(parser.parseSmiles(smiles));

            CombinatorialFragmenterScoring scoring = new CombinatorialFragmenterScoring() {
                @Override
                public double scoreBond(IBond bond, boolean direction) {
                    return 1;
                }

                @Override
                public double scoreFragment(CombinatorialNode fragment) {
                    return 1;
                }

                @Override
                public double scoreEdge(CombinatorialEdge edge) {
                    return edge.cut2 == null ? 1 : 2;
                }
            };

            PCSTFragmentationTreeAnnotator annotator = new PCSTFragmentationTreeAnnotator(null, molecule, scoring, n -> true);
            annotator.initialize();
            CombinatorialSubtree subtree = annotator.computeSubtree();
            System.out.println(subtree.toString());
            System.out.println(subtree.getScore());

        } catch (InvalidSmilesException | GRBException e) {
            e.printStackTrace();
        }
    }
}
