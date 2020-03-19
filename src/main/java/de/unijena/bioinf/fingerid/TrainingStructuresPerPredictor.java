package de.unijena.bioinf.fingerid;

import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.webapi.WebAPI;
import de.unijena.bioinf.fingerid.predictor_types.PredictorType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TrainingStructuresPerPredictor {

    private static volatile TrainingStructuresPerPredictor singleton;

    private final Map<PredictorType, TrainingStructuresSet> predictorTypeToInchiKeys2D;

    private TrainingStructuresPerPredictor() {
        predictorTypeToInchiKeys2D = new ConcurrentHashMap<>();
    }

    public static synchronized TrainingStructuresPerPredictor getInstance() {
        if (singleton==null){
            singleton = new TrainingStructuresPerPredictor();
        }
        return singleton;
    }

    private TrainingStructuresSet addAvailablePredictorTypes(PredictorType predictorType, WebAPI api) {
        synchronized (predictorTypeToInchiKeys2D) {
            if (!predictorTypeToInchiKeys2D.containsKey(predictorType)) {
                try {
                    InChI[] inchis = api.getTrainingStructures(predictorType);
                    TrainingStructuresSet trainingSet = new TrainingStructuresSet(inchis);
                    addTrainingStructuresSet(predictorType, trainingSet);
                } catch (Exception e) {
                    LoggerFactory.getLogger(TrainingStructuresPerPredictor.class).error("Cannot retrieve training structures for predictor type " + predictorType + ".\nError is: " + e.getMessage());
                    e.printStackTrace();
                    addTrainingStructuresSet(predictorType, new TrainingStructuresSet(new InChI[]{}));
                }
            }
        }
        return predictorTypeToInchiKeys2D.get(predictorType);
    }

    private TrainingStructuresSet addTrainingStructuresSet(PredictorType predictorType, TrainingStructuresSet trainingStructuresSet) {
        if (predictorTypeToInchiKeys2D.containsKey(predictorType)) throw new IllegalArgumentException("PredictorType already known");
        predictorTypeToInchiKeys2D.put(predictorType, trainingStructuresSet);
        return predictorTypeToInchiKeys2D.get(predictorType);
    }

    public TrainingStructuresSet getTrainingStructuresSet(PredictorType predictorType, @Nullable WebAPI api) {
        if (api == null)
            return getTrainingStructuresSet(predictorType);
        else
            return addAvailablePredictorTypes(predictorType, api);

    }

    public TrainingStructuresSet getTrainingStructuresSet(@NotNull PredictorType predictorType) {
        TrainingStructuresSet trainingSet = predictorTypeToInchiKeys2D.get(predictorType);
        if (trainingSet==null){
            throw new IllegalArgumentException("Unknown PredictorType: " + predictorType);
        }
        return trainingSet;
    }
}
