package de.unijena.bioinf.confidence_score.svm;

import de.unijena.bioinf.ChemistryBase.algorithm.Parameterized;

/**
 * Created by Marcus Ludwig on 11.03.16.
 */
public interface Predictor extends Parameterized{

    boolean predict(double[] features);

    double score(double[] features);

    double estimateProbability(double[] kernel);
}
