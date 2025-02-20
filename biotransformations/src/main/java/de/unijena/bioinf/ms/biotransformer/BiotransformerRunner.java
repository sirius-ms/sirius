package de.unijena.bioinf.ms.biotransformer;

import biotransformer.railsappspecific.BiotransformerSequence_rails;
import biotransformer.railsappspecific.SimulateHumanMetabolism_rails;
import biotransformer.transformation.Biotransformation;
import biotransformer.utils.BiotransformerSequence;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import org.openscience.cdk.interfaces.*;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

        IChemObjectBuilder builder = SilentChemObjectBuilder.getInstance();
        SmilesParser smiParser = new SmilesParser(builder);
        IAtomContainer input = smiParser.parseSmiles(smile);



       BioTransformerJJob transformerJob = new BioTransformerJJob();//set input and paramters
        transformerJob.setSubstrates(List.of(input));
        transformerJob.setMetabolicTransformation(MetabolicTransformation.ALL_HUMAN);
        transformerJob.setCyp450Mode(cypmode);
        transformerJob.setP2Mode(p2Mode);
        transformerJob.setIterations(steps);
        transformerJob.setUseDB(useDB);
        transformerJob.setUseSub(useSub);



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