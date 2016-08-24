package de.unijena.bioinf.ConfidenceScore;

import de.unijena.bioinf.ChemistryBase.algorithm.Parameterized;

/**
 * Created by Marcus Ludwig on 11.03.16.
 */
public interface Predictor extends Parameterized{

    public boolean predict(double[] features);

    public double score(double[] features);

    public double estimateProbability(double[] kernel);
}
