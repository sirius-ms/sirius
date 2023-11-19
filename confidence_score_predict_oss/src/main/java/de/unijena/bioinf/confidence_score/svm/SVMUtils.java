/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.confidence_score.svm;




import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unijena.bioinf.confidence_score.CombinedFeatureCreator;
import de.unijena.bioinf.confidence_score.CombinedFeatureCreatorBIODISTANCE2TO5;
import de.unijena.bioinf.confidence_score.CombinedFeatureCreatorBIODISTANCE6TO10;
import de.unijena.bioinf.confidence_score.FeatureCreator;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;


/**
 * Created by martin on 21.06.18.
 */
public class SVMUtils {


    public static SVMScales loadScalesFromJson(File file) {

    try {
        JsonNode obj = new ObjectMapper().readTree(file);
    //TODO: Load the JSON stuff here
    }catch (Exception e){
      e.printStackTrace();
    }
        return null;
    }


    public static SVMScales calculateScales(double[][] features, CombinedFeatureCreator comb) {


        //Normalisation

        ArrayList<FeatureCreator> creator_list = comb.featureCreators;


        double[] max_values_for_feature = new double[features[0].length];
        double[] min_values_for_feature = new double[features[0].length];
        for (int i = 0; i < max_values_for_feature.length; i++) {
            max_values_for_feature[i] = -Double.MAX_VALUE;
            min_values_for_feature[i] = Double.MAX_VALUE;
        }

        for (int i = 0; i < features[0].length; i++) {
            ArrayList<Double> value_list = new ArrayList<>();

            for (int j = 0; j < features.length; j++) {
                value_list.add(features[j][i]);

               // if (features[j][i] > max_values_for_feature[i]) {
                 //   max_values_for_feature[i] = features[j][i];
              //  }
                //if (features[j][i] < min_values_for_feature[i]) {
                  //  min_values_for_feature[i] = features[j][i];
               // }

            }
            Collections.sort(value_list);
            int min_quartile = creator_list.get(i).min_quartil();
            int max_quartile = creator_list.get(i).max_quartil();
            int index_min = (int) Math.ceil(((double)min_quartile/100d)*value_list.size());
            int index_max= (int) Math.ceil(((double)max_quartile/100d)*value_list.size());
            min_values_for_feature[i]=value_list.get(index_min);
            max_values_for_feature[i]=value_list.get(index_max);
            System.out.println(min_quartile+ " "+max_quartile+" "+index_min+" "+index_max+" "+min_values_for_feature[i]+" "+max_values_for_feature[i]);


        }



        //standardisazion:


        int feature_amount = features[0].length;

        ArrayList<Double>[] value_lists = new ArrayList[feature_amount];
        for (int i = 0; i < value_lists.length; i++) value_lists[i] = new ArrayList<>();

        double[] medians = new double[feature_amount];
        double[] stddevs = new double[feature_amount];
        double[] stddevs_mean = new double[feature_amount];
        double[] means = new double[feature_amount];

        for (int i = 0; i < feature_amount; i++) {

            for (int j = 0; j < features.length; j++) {

                if(features[j][i]<min_values_for_feature[i])value_lists[i].add(min_values_for_feature[i]);
                else if( features[j][i]>max_values_for_feature[i])value_lists[i].add(max_values_for_feature[i]);
                else  value_lists[i].add(features[j][i]);

            }


        }

       /**
         *
         * collect medians
         */
/*
        for (int i = 0; i < value_lists.length; i++) {

            Collections.sort(value_lists[i]);
            medians[i] = value_lists[i].get(value_lists[i].size() / 2);
        }*/

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



        for (int i = 0; i < value_lists.length; i++) {
            double sum = 0;
            for (int j = 0; j < value_lists[i].size(); j++) {
                sum += Math.pow(value_lists[i].get(j) - means[i], 2);
            }
            stddevs_mean[i] = Math.sqrt((sum / value_lists[i].size()));
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

                if(element[i]<scales.getMin_feature_values()[i])element[i]=scales.getMin_feature_values()[i];
                if(element[i]>scales.getMax_feature_values()[i])element[i]=scales.getMax_feature_values()[i];

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
                //if(element[i]<scales.getMin_feature_values()[i])element[i]=scales.getMin_feature_values()[i];
               // if(element[i]>scales.getMax_feature_values()[i])element[i]=scales.getMax_feature_values()[i];

                if(scales.max_feature_values[i]-scales.min_feature_values[i] ==0){
                    System.out.println("Something is wrong with the SVM!- "+i);
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





