package de.unijena.bioinf.fingerid;

import de.unijena.bioinf.WebAPI;
import de.unijena.bioinf.chemdb.CachedRESTDB;
import de.unijena.bioinf.confidence_score.ConfidenceScorer;
import de.unijena.bioinf.fingerid.blast.FingerblastScoringMethod;
import de.unijena.bioinf.fingerid.predictor_types.PredictorType;

public abstract class AbstractStructurePredictor implements StructurePredictor {
    protected final PredictorType predictorType;
    protected final WebAPI csiWebAPI;
    protected CachedRESTDB database;
    protected FingerblastScoringMethod fingerblastScoring;
    protected ConfidenceScorer confidenceScorer;
    protected TrainingStructuresSet trainingStructures;

    protected AbstractStructurePredictor(PredictorType predictorType, WebAPI api) {
        this.predictorType = predictorType;
        this.csiWebAPI = api;
    }

    public PredictorType getPredictorType() {
        return predictorType;
    }

    public CachedRESTDB getDatabase() {
        return database;
    }

    public FingerblastScoringMethod getFingerblastScoring() {
        return fingerblastScoring;
    }

    public WebAPI getWebAPI() {
        return csiWebAPI;
    }

    public ConfidenceScorer getConfidenceScorer() {
        return confidenceScorer;
    }

    public TrainingStructuresSet getTrainingStructures() {
        return trainingStructures;
    }
}
