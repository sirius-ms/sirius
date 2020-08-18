package de.unijena.bioinf.fingerid.blast;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.fp.BooleanFingerprint;
import de.unijena.bioinf.ChemistryBase.fp.PredictionPerformance;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;

public class BayesnetScoringTrainingData {
    public final MolecularFormula[] formulasReferenceData;
    public final BooleanFingerprint[] trueFingerprintsReferenceData;
    public final ProbabilityFingerprint[] estimatedFingerprintsReferenceData;

    public final PredictionPerformance[] predictionPerformances;

    public BayesnetScoringTrainingData(MolecularFormula[] formulasReferenceData, BooleanFingerprint[] trueFingerprintsReferenceData, ProbabilityFingerprint[] estimatedFingerprintsReferenceData, PredictionPerformance[] predictionPerformances) {
        this.formulasReferenceData = formulasReferenceData;
        this.trueFingerprintsReferenceData = trueFingerprintsReferenceData;
        this.estimatedFingerprintsReferenceData = estimatedFingerprintsReferenceData;
        this.predictionPerformances = predictionPerformances;
    }


}
