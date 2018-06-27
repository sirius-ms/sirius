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


    public void predict_confidence(double[][] features,double frag_energy){//TODO: frag energies?


        try {




            utils.standardize_features(features,null);
            utils.normalize_features(features,null);

        }catch (Exception e){
            e.printStackTrace();
        }









    }




}
