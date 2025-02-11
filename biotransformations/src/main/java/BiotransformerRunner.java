import biotransformer.railsappspecific.BiotransformerSequence_rails;
import biotransformer.transformation.Biotransformation;
import biotransformer.utils.BiotransformerSequence;
import executable.BiotransformerExecutable3;
import org.openscience.cdk.interfaces.IAtomContainerSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class BiotransformerRunner {

    public static void main(String[] args) throws Exception {
        BiotransformerSequence_rails biotransformerSeqeuence = null;
        BiotransformerSequence btq_for_mf = null;
        // default
        int cyp450Mode = 1;
        int p2Mode = 1;
        boolean useDB = true;
        boolean useSub = false;
        Double massThreshold = (double)1500.0F;

        String smile = "CC(C)C1=CC=C(C)C=C1O";
        int steps= 2;
        int cypmode = 3;

     ArrayList<BiotransformationObject> transformations=  BiotransformerWrapper.AllHumanTransformer(smile, steps, p2Mode, cypmode, useDB, useSub);

        // Iteration und Ausgabe der Eigenschaften jedes Ergebnisses
        for (Biotransformation transformation : transformations) {
            System.out.println("---------------");
            //System.out.println("Substrat: " + transformation.getSubstrates());
          /*  System.out.println("Produkt: " + transformation.getProducts());
            System.out.println("Reaktion: " + transformation.getReactionType());
            System.out.println("Score: " + transformation.getScore());
            System.out.println("Enzymes: "+ transformation.getEnzymeNames());
            System.out.println("---------------");*/
           // transformation.display();
            //IAtomContainerSet products = transformation.getProducts();
            //System.out.println("Products: " + products.toString());
            transformation.display();
        }





    }
}