package de.unijena.bioinf.fragmenter;

import de.unijena.bioinf.ChemistryBase.chem.ElectronIonization;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.Smiles;
import de.unijena.bioinf.ChemistryBase.chem.utils.UnknownElementException;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.babelms.MsIO;
import gurobi.GRBException;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;
import uk.ac.ebi.beam.Bond;

import javax.swing.*;
import java.io.File;
import java.io.IOException;

import static org.openscience.cdk.interfaces.IBond.Order.SINGLE;

public class Main {

    private static CombinatorialFragmenterScoring DEFAULT_SCORING = new CombinatorialFragmenterScoring() {
        @Override
        public double scoreBond(IBond bond, boolean direction) {
            return -1d;
        }

        @Override
        public double scoreFragment(CombinatorialNode fragment) {
            return 0;
        }

        @Override
        public double scoreEdge(CombinatorialEdge edge) {
            if(edge.target.fragment.isRealFragment()){
                return this.scoreBond(edge.getCut1(), edge.getDirectionOfFirstCut()) + (edge.getCut2() != null ? scoreBond(edge.getCut2(), edge.getDirectionOfSecondCut()) : 0);
            }else{
                return 10 - Math.abs(edge.source.fragment.hydrogenRearrangements(edge.target.fragment.getFormula()));
            }
        }
    };

    private static double getMinimalMassInFTree(FTree fTree){
        double minMass = Double.POSITIVE_INFINITY;
        for(Fragment frag : fTree){
            double mass = frag.getFormula().getMass();
            if(mass < minMass){
                minMass = mass;
            }
        }
        return minMass;
    }

    public static void main(String[] args) {
        try{
            String smiles = "C1CC1";
            SmilesParser parser = new SmilesParser(SilentChemObjectBuilder.getInstance());
            MolecularGraph molecule = new MolecularGraph(parser.parseSmiles(smiles));

            FTree fTree = new FTree(MolecularFormula.parse("C3H6"), new ElectronIonization());
            fTree.addFragment(fTree.getRoot(), new Fragment(1, MolecularFormula.parse("C2H2"), new ElectronIonization()));
            fTree.addFragment(fTree.getFragmentAt(1), new Fragment(2,MolecularFormula.parse("C"), new ElectronIonization()));
            fTree.addFragment(fTree.getRoot(), new Fragment(3, MolecularFormula.parse("CH5"), new ElectronIonization()));

            EMFragmenterScoring scoring = new EMFragmenterScoring(molecule);
            EMFragmenterScoring.rearrangementProb = 0.5;
            CombinatorialFragmenter fragmenter = new CombinatorialFragmenter(molecule, scoring);
            CombinatorialGraph graph = fragmenter.createCombinatorialFragmentationGraph(n -> true);
            CombinatorialGraphManipulator.addTerminalNodes(graph, scoring, fTree);

            double[][] matrix = graph.getAdjacencyMatrix();
            for(double[] row : matrix){
                for(int c = 0; c < row.length; c++){
                    System.out.print(row[c]+" ");
                }
                System.out.println();
            }
        }catch(InvalidSmilesException | UnknownElementException e){
            e.printStackTrace();
        }

    }

}
