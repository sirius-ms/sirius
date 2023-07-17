package de.unijena.bioinf.cmlSpectrumPrediction;

import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.babelms.MsIO;
import de.unijena.bioinf.cmlFragmentation.FragmentationPredictor;
import de.unijena.bioinf.cmlFragmentation.FragmentationRules;
import de.unijena.bioinf.cmlFragmentation.RuleBasedFragmentation;
import de.unijena.bioinf.fragmenter.CombinatorialFragment;
import de.unijena.bioinf.fragmenter.CombinatorialFragmenter;
import de.unijena.bioinf.fragmenter.EMFragmenterScoring2;
import de.unijena.bioinf.fragmenter.MolecularGraph;
import de.unijena.bioinf.sirius.ProcessedInput;
import de.unijena.bioinf.sirius.ProcessedPeak;
import de.unijena.bioinf.sirius.Sirius;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class Main {

    private static class SimpleFragmentationRule implements FragmentationRules{

        private final ArrayList<String> allowedElements;

        public SimpleFragmentationRule(String[] allowedElements){
            this.allowedElements = new ArrayList<>(allowedElements.length);
            this.allowedElements.addAll(Arrays.asList(allowedElements));
        }
        public boolean match(IBond bond){
            if(!bond.getOrder().equals(IBond.Order.SINGLE)) return false;
            String atom1Symb = bond.getAtom(0).getSymbol();
            String atom2Symb = bond.getAtom(1).getSymbol();
            return this.allowedElements.contains(atom1Symb) || this.allowedElements.contains(atom2Symb);
        }
    }

    public static void main(String[] args){
        try {
            // 1. Read the measured spectrum and construct an instance of Ms2Spectrum:
            final File measuredSpectrumFile = new File("C:\\Users\\Nutzer\\Documents\\Bioinformatik (B.Sc. + M.Sc.)\\HiWi\\SiriusProjectSpaces\\TEST\\Demo_ProjectSpace\\2_Kaempferol_Kaempferol\\spectrum.ms");
            Ms2Experiment ms2Experiment = MsIO.readExperimentFromFile(measuredSpectrumFile).next();
            ProcessedInput processedInput = getProcessedInput(ms2Experiment);
            List<ProcessedPeak> mergedPeaks = processedInput.getMergedPeaks();

            SimpleMutableSpectrum measuredSimpleSpectrum = new SimpleMutableSpectrum(mergedPeaks.size());
            for(Peak peak : mergedPeaks) measuredSimpleSpectrum.addPeak(peak);
            Ms2Spectrum<Peak> measuredSpectrum = new MutableMs2Spectrum(measuredSimpleSpectrum, processedInput.getParentPeak().getMass(), null, 2);

            // 2. Predict a spectrum regarding a specific molecule:
            final String[] allowedElements = new String[]{"N", "O", "P", "S"};
            final SimpleFragmentationRule fragRule = new SimpleFragmentationRule(allowedElements);
            final CombinatorialFragmenter.Callback2 fragmentationConstraint = (node, numNodes, numEdges) -> true;
            final int NUM_FRAGMENTS = 10;

            final String smiles = "O=c1c(O)c(-c2ccc(O)cc2)oc2cc(O)cc(O)c12";
            final SmilesParser smiParser = new SmilesParser(SilentChemObjectBuilder.getInstance());
            final MolecularGraph mol = new MolecularGraph(smiParser.parseSmiles(smiles));
            final EMFragmenterScoring2 scoring = new EMFragmenterScoring2(mol, null);

            final RuleBasedFragmentation fragmentationPredictor = new RuleBasedFragmentation(mol, scoring, NUM_FRAGMENTS, fragRule, fragmentationConstraint);
            final BarcodeSpectrumPredictor spectrumPredictor = new BarcodeSpectrumPredictor(fragmentationPredictor, true);

            fragmentationPredictor.predictFragmentation(); // has to be called first!
            Ms2Spectrum<Peak> predictedSpectrum = spectrumPredictor.predictSpectrum();
            Map<Peak, CombinatorialFragment> mapping = spectrumPredictor.getPeakFragmentMapping();

            // 3. Create a mirror plot:
            String measuredSpectrumStr = getSpectrumString(measuredSpectrum);
            String predictedSpectrumStr = getSpectrumString(predictedSpectrum);
            ProcessBuilder pb = new ProcessBuilder("python","makeMirrorSpectrumPlot.py", measuredSpectrumStr, predictedSpectrumStr);
            pb.directory(new File("C:\\Users\\Nutzer\\Desktop"));
            pb.start();

        } catch (InvalidSmilesException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String getSpectrumString(Ms2Spectrum<Peak> spectrum){
        StringBuilder strBuilder = new StringBuilder();
        strBuilder.append("[");
        for(int i = 0; i < spectrum.size(); i++){
            Peak peak = spectrum.getPeakAt(i);
            strBuilder.append("(").append(peak.getMass()).append(",").append(peak.getIntensity()).append(")");
            if(i < spectrum.size() - 1){
                strBuilder.append(";");
            }else{
                strBuilder.append("]");
            }
        }
        return strBuilder.toString();
    }

    private static ProcessedInput getProcessedInput(Ms2Experiment ms2Experiment){
        addAnnotationToMs2Experiment(ms2Experiment);
        return new Sirius().preprocessForMs2Analysis(ms2Experiment);
    }

    private static void setMS2MassDeviation(Ms2Experiment ms2Experiment, double allowedMassDeviation, double standardMassDeviation){
        ms2Experiment.setAnnotation(MS2MassDeviation.class, MS2MassDeviation.newInstance(new Deviation(allowedMassDeviation), new Deviation(standardMassDeviation)));
    }

    private static void addAnnotationToMs2Experiment(Ms2Experiment ms2Experiment){
        MsInstrumentation instrumentation = ms2Experiment.getAnnotation(MsInstrumentation.class).orElse(MsInstrumentation.Unknown);
        if(instrumentation.isInstrument("Orbitrap (LCMS)")){
            setMS2MassDeviation(ms2Experiment, 5, 5);
        }else if(instrumentation.isInstrument("Bruker Q-ToF (LCMS)")){
            setMS2MassDeviation(ms2Experiment, 10, 10);
        }else if(instrumentation.isInstrument("Q-ToF (LCMS)")){
            setMS2MassDeviation(ms2Experiment, 10, 10);
        }else if(instrumentation.isInstrument("FTICR (LCMS)")){
            setMS2MassDeviation(ms2Experiment, 5, 5);
        }else if(instrumentation.isInstrument("Ion Trap (LCMS)")){
            setMS2MassDeviation(ms2Experiment, 20, 20);
        }else if(instrumentation.isInstrument("Tripple-Quadrupole")){
            setMS2MassDeviation(ms2Experiment, 100, 100);
        }else{
            setMS2MassDeviation(ms2Experiment, 10, 10);
        }
    }


}
