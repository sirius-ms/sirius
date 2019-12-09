package de.unijena.bioinf.fingerid;

import de.unijena.bioinf.confidence_score.ConfidenceScorer;
import de.unijena.bioinf.fingerid.blast.FingerblastScoringMethod;
import de.unijena.bioinf.chemdb.CachedRESTDB;
import de.unijena.bioinf.fingerid.predictor_types.PredictorType;
import de.unijena.bioinf.WebAPI;

import java.io.IOException;

public interface StructurePredictor {
    PredictorType getPredictorType();

    FingerblastScoringMethod getFingerblastScoring();

    WebAPI getWebAPI();

    CachedRESTDB getDatabase();

    ConfidenceScorer getConfidenceScorer();

    TrainingStructuresSet getTrainingStructures();

    void refreshCacheDir() throws IOException;
}