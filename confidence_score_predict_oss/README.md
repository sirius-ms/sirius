this module lets you predict confidence scores for structure assignments of csi:fingerid

##### Prediction phase #####
- load predictor
    QueryPredictor queryPredictor = QueryPredictor.loadFromFile(modelFile);

- estimate
    double platt = queryPredictor.estimateProbability(Query query, Candidate[] rankedCandidates);