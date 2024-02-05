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


/**
 * Created by martin on 21.06.18.
 */
public class SVMPredict {

SVMUtils utils = new SVMUtils();
LibLinearImpl impl =  new LibLinearImpl();
LibLinearImpl.svm_model model;


    public double[] predict_confidence(double[][] features,TrainedSVM svm){//TODO: frag energies?


        double[] scores =  new double[features.length];

        try {

//TODO: assert only normalized stuff comes in here

            LinearSVMPredictor predictor=  new LinearSVMPredictor(svm.weights,0);

            for(int i=0;i<features.length;i++) {

                if(svm.probAB!=null) {
                    scores[i] =1.0/(1+Math.exp(svm.probAB[0]*predictor.score(features[i])+svm.probAB[1]));
                }else{
                    scores[i] = predictor.score(features[i]);


                }
            }


        }catch (Exception e){
            e.printStackTrace();
        }

        return scores;




    }

    public double[] predict_SVM_Value(double[][] features,TrainedSVM svm){

        double[] scores = new double[features.length];
        try{
            LinearSVMPredictor predictor = new LinearSVMPredictor(svm.weights,0);
            for(int i=0;i<features.length;i++) {

                    scores[i] = predictor.score(features[i]);

            }

        }catch (Exception e){
            e.printStackTrace();
        }

        return scores;

    }

    public double applyPlatt(double svmValue,TrainedSVM svm){

        double score;
        score =1.0/(1+Math.exp(svm.probAB[0]*svmValue+svm.probAB[1]));
        return score;

    }



    public boolean[] predict_classes(double[][] features, TrainedSVM svm){

        boolean[] classes =  new boolean[features.length];

        try {


            //TODO need scales

            utils.standardize_features(features,svm.scales);
            utils.normalize_features(features,svm.scales);

            LinearSVMPredictor predicor=  new LinearSVMPredictor(svm.weights,0);

            for(int i=0;i<features.length;i++) {

                classes[i]=(predicor.predict(features[i]));
            }

        }catch (Exception e){
            e.printStackTrace();
        }

        return classes;




    }




}
