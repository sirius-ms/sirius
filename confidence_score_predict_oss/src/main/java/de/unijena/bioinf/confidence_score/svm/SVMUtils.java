package de.unijena.bioinf.confidence_score.svm;




import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;


/**
 * Created by martin on 21.06.18.
 */
public class SVMUtils {


    public static SVMScales loadScalesFromJson(File file) {

    try {
        JsonReader jreader = Json.createReader(new FileReader(file));

        JsonObject obj = jreader.readObject();




    //TODO: Load the JSON stuff here
    }catch (Exception e){
      e.printStackTrace();
    }

        return null;
    }


    public static SVMScales calculateScales(double[][] features) {


        //Normalisation

        double[] max_values_for_feature = new double[features[0].length];
        double[] min_values_for_feature = new double[features[0].length];
        for (int i = 0; i < max_values_for_feature.length; i++) {
            max_values_for_feature[i] = -Double.MAX_VALUE;
            min_values_for_feature[i] = Double.MAX_VALUE;
        }


        for (int i = 0; i < features[0].length; i++) {

            for (int j = 0; j < features.length; j++) {

                if (features[j][i] > max_values_for_feature[i]) {
                    max_values_for_feature[i] = features[j][i];
                }
                if (features[j][i] < min_values_for_feature[i]) {
                    min_values_for_feature[i] = features[j][i];
                }

            }


        }



        //standardisazion:


        int feature_amount = features[0].length;

        ArrayList<Double>[] value_lists = new ArrayList[feature_amount];
        for (int i = 0; i < value_lists.length; i++) value_lists[i] = new ArrayList<>();

        ArrayList<Double>[] mAD_lists = new ArrayList[feature_amount];
        for (int i = 0; i < mAD_lists.length; i++) mAD_lists[i] = new ArrayList<>();

        double[] medians = new double[feature_amount];
        double[] stddevs = new double[feature_amount];
        double[] stddevs_mean = new double[feature_amount];
        double[] means = new double[feature_amount];

        for (int i = 0; i < feature_amount; i++) {

            for (int j = 0; j < features.length; j++) {

                value_lists[i].add(features[j][i]);

            }


        }

        /**
         *
         * collect medians
         */

        for (int i = 0; i < value_lists.length; i++) {

            Collections.sort(value_lists[i]);
            medians[i] = value_lists[i].get(value_lists[i].size() / 2);
        }

        /**
         *
         * collect means
         */


        for (int i = 0; i < value_lists.length; i++) {
            double sum = 0;
            for (int j = 0; j < value_lists[i].size(); j++) {
                sum += value_lists[i].get(j);

            }
            means[i] = sum / value_lists[i].size();
        }


        for (int i = 0; i < feature_amount; i++) {

            for (int j = 0; j < features.length; j++) {

                mAD_lists[i].add(Math.abs(medians[i] - features[j][i]));

            }

        }


        for (int i = 0; i < mAD_lists.length; i++) {

            Collections.sort(mAD_lists[i]);
            stddevs[i] = 1.462 * mAD_lists[i].get(mAD_lists[i].size() / 2);

        }

        for (int i = 0; i < value_lists.length; i++) {
            double sum = 0;
            for (int j = 0; j < value_lists[i].size(); j++) {
                sum += Math.pow(value_lists[i].get(j) - means[i], 2);
            }
            stddevs_mean[i] = Math.sqrt((sum / value_lists.length));
        }





            for (int i = 0; i < features[0].length; i++) {


                if (stddevs[i] != 0) {

                } else {
                    if (stddevs_mean[i] != 0) {
                        medians[i] = means[i];
                        stddevs[i] = stddevs_mean[i];
                    }
                }


            }



            SVMScales scales = new SVMScales(medians, stddevs, min_values_for_feature, max_values_for_feature);

            return scales;


        }


    public static double[][] standardize_features(double[][] features, SVMScales scales) {


        for(double[] element : features){



            for(int i=0;i<element.length;i++) {

                if(scales.deviations[i]==0){
                    System.out.println("?");
                    System.out.println(i);
                }

                element[i] = (element[i] - scales.medians[i]) / scales.deviations[i];

            }
        }


        return features;
    }


    public static double[][] normalize_features(double[][] features, SVMScales scales) {



        for (double[] element: features){

            for(int i=0;i<element.length;i++){

                if(scales.max_feature_values[i]-scales.min_feature_values[i] ==0){
                    System.out.println("=");
                }

                element[i]=(element[i]-scales.min_feature_values[i])/(scales.max_feature_values[i]-scales.min_feature_values[i]);
            }


        }




        return features;
    }

    public static LinearSVMPredictor getPredictorFromJson(File file) {


        return null;

    }







}





