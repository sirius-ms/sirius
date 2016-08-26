package de.unijena.bioinf.ConfidenceScore.svm;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ConfidenceScore.Predictor;
import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * Created by Marcus Ludwig on 16.08.16.
 */
public class KernelSVMPredictor implements Predictor {

    private svm_model model;

    public KernelSVMPredictor(svm_model model){
        this.model = model;
        System.out.println("probability model "+svm.svm_check_probability_model(model));
    }



    @Override
    public boolean predict(double[] features) {
        return svm.svm_predict(model, toNodes(features))>=0;
    }

    @Override
    public double score(double[] features) {
        return svm.svm_predict(model, toNodes(features));
    }

    @Override
    public double estimateProbability(double[] features) {
        double[] probabilities = new double[2];
        svm.svm_predict_probability(model, toNodes(features), probabilities);
        return probabilities[0]; //tdodo return which!?!?!?!
//        double[] score = new double[1];
//        svm.svm_predict_values(model, toNodes(features), score);
//        return score[0];
    }

    private svm_node[] toNodes(double[] features){
        svm_node[] nodes = new svm_node[features.length];
        for (int i = 0; i < features.length; i++) {
            double value = features[i];
            if (Double.isNaN(value)){
                throw new IllegalArgumentException("value for node cannot be NaN");
            }
            svm_node svm_node = new libsvm.svm_node();
            svm_node.index = i;
            svm_node.value = value;
            nodes[i] = svm_node;
        }
        return nodes;
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        throw new NotImplementedException();
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        throw new NotImplementedException();
    }
}
