package de.unijena.bioinf.ms.rest.model.fingerid;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.fingerid.predictor_types.PredictorType;
import de.unijena.bioinf.fingerid.predictor_types.UserDefineablePredictorType;
import de.unijena.bioinf.sirius.IdentificationResult;

import java.util.EnumSet;

public class FingerprintJobInput {
    public final Ms2Experiment experiment;
    public final FTree ftree;
    public final IdentificationResult<?> identificationResult;
    public final EnumSet<PredictorType> predictors;


    public FingerprintJobInput(final Ms2Experiment experiment, final IdentificationResult<?> result, final FTree ftree, EnumSet<PredictorType> predictors) {
        this.experiment = experiment;
        this.ftree = ftree;
        this.identificationResult = result;

        if (predictors == null || predictors.isEmpty())
            this.predictors = EnumSet.of(UserDefineablePredictorType.CSI_FINGERID.toPredictorType(experiment.getPrecursorIonType()));
        else
            this.predictors = predictors;
    }
}
