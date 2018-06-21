package de.unijena.bioinf.confidence_score.svm;


import de.bwaldvogel.liblinear.Model;
import de.bwaldvogel.liblinear.SolverType;
import de.unijena.bioinf.chemdb.JSONReader;
import jdk.nashorn.internal.parser.JSONParser;
import org.json.JSONObject;

import javax.json.JsonReader;
import java.io.File;
import java.io.FileReader;

/**
 * Created by martin on 21.06.18.
 */
public class SVMUtils {

    protected SVMInterface.svm_model model;




    public SVMScales loadScalesFromJson(File file){

try {
    JSONObject json = new JSONObject();
    //TODO: Load the JSON stuff here
}catch (Exception e){
    e.printStackTrace();
}

        return null;
    }

    public  double[][] standardize_features(double[][] features, SVMScales scales){


        for(double[] element : features){



            for(int i=0;i<element.length;i++) {

                element[i] = (element[i] - scales.medians[i]) / scales.deviations[i];

            }
        }


        return features;
    }


    public  double[][] normalize_features(double[][] features,SVMScales scales){



        for (double[] element: features){

            for(int i=0;i<element.length;i++){

                element[i]=(element[i]-scales.min_feature_values[i])/(scales.max_feature_values[i]-scales.min_feature_values[i]);
            }


        }



        return features;
    }

    public LinearSVMPredictor getPredictorFromJson(File file){


        return null;

    }







}

class SVMScales{

    double[] medians;
    double[] deviations;
    double[] min_feature_values;
    double[] max_feature_values;

    public SVMScales(double[] medians,double[] deviations,double[] mins, double[] maxs){

        this.medians=medians;
        this.deviations=deviations;
        this.min_feature_values=mins;
        this.max_feature_values=maxs;


    }


}

