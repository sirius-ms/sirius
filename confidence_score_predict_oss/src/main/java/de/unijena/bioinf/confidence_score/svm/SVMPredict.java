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
