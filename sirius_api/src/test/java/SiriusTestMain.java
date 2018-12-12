import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.Decomposition;
import de.unijena.bioinf.FragmentationTreeConstruction.model.DecompositionList;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.babelms.MsIO;
import de.unijena.bioinf.sirius.Sirius;

import java.io.File;
import java.io.IOException;

public class SiriusTestMain {
    public static void main(String[] args) {
        try {
            Sirius sirius = new Sirius();
            // input file
            Ms2Experiment experiment = MsIO.readExperimentFromFile(new File("someFile.ms")).next();
            // intermediate object
            ProcessedInput pinput = sirius.getMs2Analyzer().preprocessing(experiment);
            Decomposition decomposition = pinput.getAnnotationOrThrow(DecompositionList.class).find(experiment.getMolecularFormula());
            FGraph graph = sirius.getMs2Analyzer().buildGraphWithoutReduction(pinput, decomposition);


        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
