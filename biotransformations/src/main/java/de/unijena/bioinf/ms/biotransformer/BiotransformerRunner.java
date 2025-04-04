package de.unijena.bioinf.ms.biotransformer;

import biotransformer.railsappspecific.BiotransformerSequence_rails;
import biotransformer.utils.BiotransformerSequence;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import org.openscience.cdk.interfaces.*;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;

import java.util.List;

public class BiotransformerRunner {

    public static void main(String[] args) throws Exception {
        BiotransformerSequence_rails biotransformerSeqeuence = null;
        BiotransformerSequence btq_for_mf = null;
        // default
        int p2Mode = 1;
        boolean useDB = true;
        boolean useSub = false;
        Double massThreshold = (double) 1500.0F;

        String smile = "CC(C)C1=CC=C(C)C=C1O";
        int steps = 2;
        Cyp450Mode cypmode = Cyp450Mode.COMBINED;

        IChemObjectBuilder builder = SilentChemObjectBuilder.getInstance();
        SmilesParser smiParser = new SmilesParser(builder);
        IAtomContainer input = smiParser.parseSmiles(smile);


        BioTransformerJJob transformerJob = new BioTransformerJJob();
        transformerJob.setSubstrates(List.of(input)); //set input structures
        transformerJob.setSettings(BioTransformerSettings.builder() //set parameters
                .metabolicTransformation(MetabolicTransformation.ALL_HUMAN)
                .cyp450Mode(cypmode)
                .p2Mode(p2Mode)
                .iterations(steps)
                .useDB(useDB)
                .useSub(useSub)
                .build()
        );

        List<BioTransformerResult> results = SiriusJobs.getGlobalJobManager().submitJob(transformerJob).awaitResult();
        System.out.println(results.getFirst().getBiotranformations().getFirst().getProducts());



      /*  SimulateHumanMetabolism_rails humanMetabolismRails =new SimulateHumanMetabolism_rails(cypmode, p2Mode, useDB, "hmdb", false, 30, useSub);
        ArrayList<Biotransformation> transformations;
        transformations = humanMetabolismRails.simulateHumanMetabolism(input,steps);
        // Iteration und Ausgabe der Eigenschaften jedes Ergebnisses
      for (Biotransformation transformation : transformations) {
            System.out.println("---------------");
            //System.out.println("Substrat: " + transformation.getSubstrates());
           System.out.println("Produkt: " + transformation.getProducts());
            System.out.println("Reaktion: " + transformation.getReactionType());
            System.out.println("Score: " + transformation.getScore());
            System.out.println("Enzymes: "+ transformation.getEnzymeNames());
            System.out.println("---------------");
           // transformation.display();
            //IAtomContainerSet products = transformation.getProducts();
            //System.out.println("Products: " + products.toString());
            transformation.display();
        }
         */


    }
}