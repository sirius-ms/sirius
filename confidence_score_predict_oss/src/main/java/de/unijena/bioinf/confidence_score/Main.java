package de.unijena.bioinf.confidence_score;

import de.unijena.bioinf.confidence_score.svm.SVMPredict;

/**
 * Created by martin on 20.06.18.
 */
public class Main {


    public static void main(String[] args){

       //TODO: Read a compound and see if everything works

        SVMPredict pred = new SVMPredict();
        pred.predict_confidence(new double[3],2);


    }
}
