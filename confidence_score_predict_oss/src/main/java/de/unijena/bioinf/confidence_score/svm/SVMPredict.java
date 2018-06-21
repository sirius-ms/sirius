package de.unijena.bioinf.confidence_score.svm;


import de.bwaldvogel.liblinear.Model;

import java.io.File;

/**
 * Created by martin on 21.06.18.
 */
public class SVMPredict {

SVMUtils utils = new SVMUtils();
LibLinearImpl impl =  new LibLinearImpl();
LibLinearImpl.svm_model model;


    public void predict_confidence(double[] features,double frag_energy){//TODO: frag energies?


        try {

           SVMModel model =  utils.load_raw_model_from_json(new File(""));

           impl.getPredictor(model,0,0);


            utils.standardize_features(features);
            utils.normalize_features(features);

        }catch (Exception e){
            e.printStackTrace();
        }









    }




}
