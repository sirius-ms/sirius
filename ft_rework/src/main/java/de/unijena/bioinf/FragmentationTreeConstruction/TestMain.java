package de.unijena.bioinf.FragmentationTreeConstruction;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.sirius.Sirius;

import java.io.File;
import java.io.IOException;

public class TestMain {

    public static void main(String[] args) {

        final Sirius sirius;
        try {
            sirius = new Sirius("qtof");
            final FasterMultithreadedTreeComputation fmtc = new FasterMultithreadedTreeComputation(sirius.getMs2Analyzer());

            final Ms2Experiment experiment = sirius.parseExperiment(new File("someFile.ms")).next();

            final ProcessedInput input = sirius.getMs2Analyzer().preprocessing(experiment);

            fmtc.setInput(input);

            fmtc.startComputation();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
