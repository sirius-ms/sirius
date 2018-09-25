package de.unijena.bioinf.fingerid;

import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.fingerid.net.WebAPI;
import de.unijena.bioinf.fingerid.predictor_types.PredictorType;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

public class TrainingStructuresPerPredictor {

    private static TrainingStructuresPerPredictor singleton;

    private final Map<PredictorType, TrainingStructuresSet> predictorTypeToInchiKeys2D;

    private TrainingStructuresPerPredictor() {
        predictorTypeToInchiKeys2D = new HashMap<>();
    }

    public static synchronized TrainingStructuresPerPredictor getInstance() {
        if (singleton==null){
            singleton = new TrainingStructuresPerPredictor();
        }
        return singleton;
    }

    public void addAvailablePredictorTypes(PredictorType... predictorTypes) {
        for (PredictorType predictorType : predictorTypes) {
            if (!predictorTypeToInchiKeys2D.containsKey(predictorType)){
                try {
                    InChI[] inchis = WebAPI.INSTANCE.getTrainingStructures(predictorType);
                    TrainingStructuresSet trainingSet = new TrainingStructuresSet(inchis);
                    addTrainingStructuresSet(predictorType, trainingSet);
                } catch (Exception e) {
                    LoggerFactory.getLogger(TrainingStructuresPerPredictor.class).error("Cannot retrieve training structures for predictor type "+predictorType+".\nError is: "+e.getMessage());
                    e.printStackTrace();
                    addTrainingStructuresSet(predictorType, new TrainingStructuresSet(new InChI[]{}));
                }

            }
        }
    }

    private void addTrainingStructuresSet(PredictorType predictorType, TrainingStructuresSet trainingStructuresSet){
        if (predictorTypeToInchiKeys2D.containsKey(predictorType)) throw new IllegalArgumentException("PredictorType already known");
        predictorTypeToInchiKeys2D.put(predictorType, trainingStructuresSet);
    }

    public TrainingStructuresSet getTrainingStructuresSet(PredictorType predictorType) {
        TrainingStructuresSet trainingSet = predictorTypeToInchiKeys2D.get(predictorType);
        if (trainingSet==null){
            throw new IllegalArgumentException("Unknown PredictorType: "+predictorType);
        }
        return trainingSet;
    }
}
