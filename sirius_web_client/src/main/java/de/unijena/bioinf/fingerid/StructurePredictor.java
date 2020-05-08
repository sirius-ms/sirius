package de.unijena.bioinf.fingerid;

import de.unijena.bioinf.webapi.WebAPI;
import de.unijena.bioinf.chemdb.RestWithCustomDatabase;
import de.unijena.bioinf.confidence_score.ConfidenceScorer;
import de.unijena.bioinf.fingerid.blast.FingerblastScoringMethod;
import de.unijena.bioinf.fingerid.predictor_types.PredictorType;

import java.io.IOException;

public interface StructurePredictor {
    PredictorType getPredictorType();

    FingerblastScoringMethod getFingerblastScoring();

    WebAPI getWebAPI();

    RestWithCustomDatabase getDatabase();

    ConfidenceScorer getConfidenceScorer();

    TrainingStructuresSet getTrainingStructures();

    void refreshCacheDir() throws IOException;
}