package de.unijena.bioinf.cmlSpectrumPrediction;

import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.babelms.MsIO;
import de.unijena.bioinf.cmlFragmentation.*;
import de.unijena.bioinf.fragmenter.CombinatorialNode;
import de.unijena.bioinf.fragmenter.MolecularGraph;
import de.unijena.bioinf.sirius.ProcessedInput;
import de.unijena.bioinf.sirius.ProcessedPeak;
import de.unijena.bioinf.sirius.Sirius;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

public class Main {

    public static class SimpleFragmentationRule implements FragmentationRules {

        private List<String> allowedElements;

        public SimpleFragmentationRule(String[] allowedElements){
            this.allowedElements = Arrays.asList(allowedElements);
        }

        @Override
        public boolean match(IBond bond) {
            if(!bond.getOrder().equals(IBond.Order.SINGLE)) return false;
            String atom1Symbol = bond.getAtom(0).getSymbol();
            String atom2Symbol = bond.getAtom(1).getSymbol();
            return (this.allowedElements.contains(atom1Symbol) || this.allowedElements.contains(atom2Symbol));
        }
    }

    public enum FragmentationPredictorType {
        ITERATIVE, RULE_BASED;
    }

    public static void main(String[] args) {
        try {
            // GENERAL INITIALISATION:
            String smiles = "CCC(CC)N1C2=C(C=C(C=C2)C(=O)NC(C(C)C)C(=O)N)N=C1C=CC3=CC=CC=C3";
            File msFile = new File("C:\\Users\\Nutzer\\Documents\\Bioinformatik_PhD\\AS-MS-Project\\LCMS_Benzimidazole\\BAMS-14-3\\ProjectSpaces\\filtered_by_hand_PS_5.7.2\\1695_230220_BAMS-14-3_01_1695\\spectrum.ms");

            SmilesParser smiParser = new SmilesParser(SilentChemObjectBuilder.getInstance());
            MolecularGraph molecule = new MolecularGraph(smiParser.parseSmiles(smiles));
            FragStepDependentScoring scoring = new FragStepDependentScoring(molecule);
            int numFragments = 50;

            // PREDICT THE FRAGMENTATION PROCESS & A SPECTRUM:
            FragmentationPredictorType type = FragmentationPredictorType.ITERATIVE;
            AbstractFragmentationPredictor fragmentationPredictor;
            switch (type) {
                case RULE_BASED -> {
                    String[] allowedElements = new String[]{"N", "O", "P", "S"};
                    SimpleFragmentationRule fragRule = new SimpleFragmentationRule(allowedElements);
                    fragmentationPredictor = new RuleBasedFragmentation(molecule, scoring, numFragments, fragRule, (node, nnodes, nedges) -> true);
                }
                default -> fragmentationPredictor = new PrioritizedIterativeFragmentation(molecule, scoring, numFragments);
            }
            fragmentationPredictor.predictFragmentation();
            for(CombinatorialNode node : fragmentationPredictor.getFragmentationGraph().getNodes()){
                System.out.println(node.getIncomingEdges().size()+"\t"+node.getDepth());
            }

            // Build a barcode spectrum:
            BarcodeSpectrumPredictor spectrumPredictor = new BarcodeSpectrumPredictor(fragmentationPredictor, true);
            SimpleSpectrum predictedSpectrum = new SimpleSpectrum(spectrumPredictor.predictSpectrum());

            // PARSE THE MEASURED SPECTRUM:
            Ms2Experiment ms2Experiment = MsIO.readExperimentFromFile(msFile).next();
            ProcessedInput processedMs2Experiment = new Sirius().preprocessForMs2Analysis(ms2Experiment);
            List<ProcessedPeak> mergedPeaks = processedMs2Experiment.getMergedPeaks();
            SimpleMutableSpectrum s = new SimpleMutableSpectrum(mergedPeaks.size());
            for(ProcessedPeak peak : mergedPeaks) s.addPeak(peak);
            SimpleSpectrum measuredSpectrum = new SimpleSpectrum(s);

            // COMPARE PREDICTED AND MEASURED SPECTRUM:
            Deviation deviation = new Deviation(5d);
            RecallSpectralAlignment recallScorer = new RecallSpectralAlignment(deviation);
            WeightedRecallSpectralAlignment weightedRecallScorer = new WeightedRecallSpectralAlignment(deviation);

            System.out.println("Recall: " + recallScorer.score(predictedSpectrum, measuredSpectrum).similarity);
            System.out.println("Weighted Recall: " + weightedRecallScorer.score(predictedSpectrum, measuredSpectrum).similarity);

            // OUTPUT: save the measured and predicted spectrum in a .csv file
            // Additional to the mz values and intensities, save which measured peaks were matched and
            // which fragments explain which peak
            File outputFile = new File("C:\\Users\\Nutzer\\Documents\\Repositories\\sirius-libs\\affinity_selection_ms\\src\\main\\resources\\msrdPredSpectrum.csv");
            try(BufferedWriter fileWriter = Files.newBufferedWriter(outputFile.toPath())){
                fileWriter.write("mz,intensity,type,matchedMsrdPeak,smiles,atomIndices");
                fileWriter.newLine();

                //Write the measured spectrum:
                List<Peak> matchedMsrdPeaks = recallScorer.getPreviousMatchedMeasuredPeaks();
                for(Peak peak : measuredSpectrum){
                    double mz = peak.getMass();
                    double intensity = peak.getIntensity();
                    int matchedMsrdPeak = matchedMsrdPeaks.contains(peak) ? 1 : 0;
                    fileWriter.write(mz + "," + intensity + ",0," + matchedMsrdPeak + ",NaN,NaN");
                    fileWriter.newLine();
                }

                // Write the predicted spectrum:
                for(Peak peak : predictedSpectrum){
                    double mz = peak.getMass();
                    double intensity = peak.getIntensity();
                    fileWriter.write(mz + "," + intensity + ",1,0,NaN,NaN");
                    fileWriter.newLine();
                }
            }
        } catch (InvalidSmilesException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }



}
