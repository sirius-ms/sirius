package de.unijena.bioinf.confidence_score_train;

import de.unijena.bioinf.confidence_score.CombinedFeatureCreator;
import de.unijena.bioinf.confidence_score.features.PvalueScoreUtils;
import de.unijena.bioinf.confidence_score.svm.*;
import de.unijena.bioinf.confidence_score_train.svm.LibSVMImpl;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by martin on 27.06.18.
 */



public class TrainConfidenceScore{



CombinedFeatureCreator featureCreator;
double[][] featureMatrix;
double[] labels;
ArrayList<double[][]> cvFeatureMatrix;
ArrayList<double[]> cvLabel;
LibLinearImpl imp;



    public TrainedSVM trainLinearSVMWithCV(double[][] featureMatrix,double[] labels){

        int folds = 10;

        this.featureMatrix=featureMatrix;
        this.labels=labels;

        SVMUtils utils = new SVMUtils();

        SVMScales scales =  utils.calculateScales(featureMatrix);

        utils.standardize_features(featureMatrix,scales);

        utils.normalize_features(featureMatrix,scales);



        List<Integer> range = IntStream.rangeClosed(0, featureMatrix.length-1)
                .boxed().collect(Collectors.toList());

        Collections.shuffle(range);


        int start_index=0;
        int end_index=0;


        ArrayList<Double> scores_all = new ArrayList<>();

        ArrayList<Boolean> label_all = new ArrayList<>();

      for(int i=0;i<folds;i++){

          end_index=start_index+range.size()/folds;

          ArrayList<double[]> featuresTempTest = new ArrayList<>();

          ArrayList<Double>  labelsTempTest = new ArrayList<>();

          ArrayList<double[]> featuresTempTrain = new ArrayList<>();

          ArrayList<Double> labelsTempTrain = new ArrayList<>();



          for(int x=0;x<range.size();x++){

              if(x>start_index && x<end_index) {

                  featuresTempTest.add(featureMatrix[range.get(x)].clone());

                  labelsTempTest.add(labels[range.get(x)]);
              }else {

                  featuresTempTrain.add(featureMatrix[range.get(x)].clone());

                  labelsTempTrain.add(labels[range.get(x)]);
              }

          }

          double[][] featuresFinalTest = new double[featuresTempTest.size()][featureMatrix[0].length];
          double[] labelsFinalTest = new double[labelsTempTest.size()];

          double[][] featuresFinalTrain = new double[featuresTempTrain.size()][featureMatrix[0].length];
          double[] labelsFinalTrain = new double[labelsTempTrain.size()];


          for(int y=0;y<featuresTempTest.size();y++){

                featuresFinalTest[y]=featuresTempTest.get(y);
                labelsFinalTest[y]=labelsTempTest.get(y);


          }

          for(int y=0;y<featuresTempTrain.size();y++){

              featuresFinalTrain[y]=featuresTempTrain.get(y);
              labelsFinalTrain[y]=labelsTempTrain.get(y);


          }




          TrainedSVM svm =  trainLinearSVM(featuresFinalTrain,labelsFinalTrain,featuresFinalTest,labelsFinalTest,null,scales);



          SVMPredict predict = new SVMPredict();

          boolean[] labelsBool = new boolean[labelsFinalTest.length];

          for(int z=0;z<labelsBool.length;z++){
              if(labelsFinalTest[z]==1){
                  labelsBool[z]=true;
              }else {
                  labelsBool[z]=false;
              }


          }

          double[] scores = predict.predict_confidence(featuresFinalTest,svm);



          ArrayList<Double> testscorearray = new ArrayList<>();

          for(int n=0;n<scores.length;n++){
              testscorearray.add(scores[n]+10000);
          }

          PvalueScoreUtils utils2 =  new PvalueScoreUtils();



          svm.bogusDist = utils2.estimate_lognormal_parameters(testscorearray);
          svm.score_shift=10000;


          for(int z=0;z<scores.length;z++){
              scores_all.add(scores[z]);
              label_all.add(labelsBool[z]);


          }



          start_index=end_index;

      }



      double[] scores_all_final = new double[scores_all.size()];
        boolean[] label_all_final = new boolean[label_all.size()];

        for(int i=0;i<scores_all.size();i++){
            scores_all_final[i]=scores_all.get(i);
            label_all_final[i]=label_all.get(i);


        }

        writeScores(scores_all_final,label_all_final);



      Stats stats = new Stats(scores_all_final,label_all_final);

        System.out.println(stats.getAUC());




        TrainedSVM trained = new TrainedSVM(null,null,null);












        return trained;

    }

    public TrainedSVM trainLinearSVM(double[][] trainfeatureMatrix, double[] labels, double[][] testFeatureMatrix, double[] testLabels,String[] feature_names,SVMScales scales){

        imp = new LibLinearImpl();
        SVMUtils utils = new SVMUtils();
        LibLinearImpl.svm_model model =null;

      //  SVMScales scales =  utils.calculateScales(featureMatrix);

      //  utils.standardize_features(featureMatrix,scales);

      //  utils.normalize_features(featureMatrix,scales);






        List<List<LibLinearImpl.svm_nodeImpl>> features =  new ArrayList<>();
        LibLinearImpl.svm_parameter para;
        int positive_examples=0;
        int negative_examples=0;




        for(int i=0;i<trainfeatureMatrix.length;i++){
            int index=1;
            List<LibLinearImpl.svm_nodeImpl> temp_list= new ArrayList<>();
            for(int j=0;j<trainfeatureMatrix[i].length;j++) {


                temp_list.add(imp.createSVM_Node(index, trainfeatureMatrix[i][j]));
                index++;
            }
            features.add(temp_list);
        }

        for(int i=0;i<labels.length;i++){
            if(labels[i]==1){
                positive_examples+=1;
            }else {
                negative_examples+=1;
            }

        }





        //grid search here

        double[] c_values = new double[]{0.0001,0.001,0.01,0.1,1,10,100,1000};

        double best_AUC=-1;
        LibLinearImpl.svm_model best_model=null;

        for(int i=0;i<c_values.length;i++) {


            LibLinearImpl.svm_problemImpl prob = imp.createSVM_Problem();


            prob.svm_problem.bias = 0;


            prob.setX(features);
            prob.setY(labels);
            prob.setL(features.size());


            para = new LibSVMImpl.svm_parameter();

            para.C = c_values[i];
            para.kernel_type = 0;
            para.eps = 0.0001;
            para.weight = new double[]{1, negative_examples / positive_examples};
            para.weight_label = new int[]{-1, 1};


             model = imp.svm_train(prob, para); //trains the svm model

            SVMPredict predict = new SVMPredict();

            TrainedSVM trained = new TrainedSVM(scales,model.getModel().getFeatureWeights(),feature_names);



            double[] scores = predict.predict_confidence(testFeatureMatrix,trained);

            boolean[] testLabelToBool = new boolean[testLabels.length];

            for(int j=0;j<testLabels.length;j++){
                if(testLabels[j]==1){
                    testLabelToBool[j]=true;
                }else {
                    testLabelToBool[j]=false;
                }


            }



            Stats stats = new Stats(scores,testLabelToBool);

            //writeBogusScores(scores, testLabelToBool);

            if(stats.getAUC()>best_AUC){
                best_AUC=stats.getAUC();
               best_model=model;
            }



        }



        TrainedSVM trained = new TrainedSVM(scales,best_model.getModel().getFeatureWeights(),feature_names);

        System.out.println(best_AUC);

        return trained;






    }

    public void writeBogusScores(double[] scores, boolean[] label){
        try {
            FileWriter writeBogus = new FileWriter(new File("/vol/clusterdata/fingerid_martin/fingerid-112_noldn/bogus_dist.txt"),true);

            for(int i=0;i<scores.length;i++){

                if(label[i]==false){
                    writeBogus.write(scores[i]+"\n");
                }
            }


            writeBogus.close();

        }catch (Exception e){
            e.printStackTrace();
        }




    }


    public TrainedSVM trainLinearSVMNoEval(double[][] featureMatrix,double[] labels, String[] feature_names){


        imp = new LibLinearImpl();
        SVMUtils utils = new SVMUtils();
        LibLinearImpl.svm_model model =null;



        SVMScales scales = utils.calculateScales(featureMatrix);

        utils.standardize_features(featureMatrix,scales);
        utils.normalize_features(featureMatrix,scales);


        //split into test/train

        ArrayList<double[]> trainfeatures = new ArrayList<>();
        ArrayList<Double> trainlabels =  new ArrayList<>();
        ArrayList<double[]> testfeatures = new ArrayList<>();
        ArrayList<Double> testlabels = new ArrayList<>();


        List<Integer> range = IntStream.rangeClosed(0, featureMatrix.length-1)
                .boxed().collect(Collectors.toList());

        Collections.shuffle(range);

        for(int i=0;i<range.size()*0.9;i++){

            trainfeatures.add(featureMatrix[range.get(i)]);
            trainlabels.add(labels[range.get(i)]);


        }

        for(int i=(int) Math.round(range.size()*0.9);i<range.size();i++){

            testfeatures.add(featureMatrix[range.get(i)]);
            testlabels.add(labels[range.get(i)]);


        }



        double[][] trainfeatureMatrix = new double[trainfeatures.size()][trainfeatures.get(0).length];
        double[][] testfeatureMatrix = new double[testfeatures.size()][trainfeatures.get(0).length];

        for(int i=0;i<trainfeatures.size();i++){
            trainfeatureMatrix[i]=trainfeatures.get(i);

        }

        for(int i=0;i<testfeatures.size();i++){
            testfeatureMatrix[i]=testfeatures.get(i);

        }


        double[] trainlabelsarray = new double[trainlabels.size()];

        for(int i=0;i<trainlabels.size();i++){

            trainlabelsarray[i] = trainlabels.get(i);
        }







        List<List<LibLinearImpl.svm_nodeImpl>> features =  new ArrayList<>();
        LibLinearImpl.svm_parameter para;
        int positive_examples=0;
        int negative_examples=0;




        for(int i=0;i<trainfeatureMatrix.length;i++){
            int index=1;
            List<LibLinearImpl.svm_nodeImpl> temp_list= new ArrayList<>();
            for(int j=0;j<trainfeatureMatrix[i].length;j++) {


                temp_list.add(imp.createSVM_Node(index, trainfeatureMatrix[i][j]));
                index++;
            }
            features.add(temp_list);
        }

        for(int i=0;i<trainlabels.size();i++){
            if(trainlabels.get(i)==1){
                positive_examples+=1;
            }else {
                negative_examples+=1;
            }

        }





        //grid search here

        double[] c_values = new double[]{0.0001,0.001,0.01,0.1,1,10,100,1000};

        double best_AUC=-1;
        LibLinearImpl.svm_model best_model=null;

        for(int i=0;i<c_values.length;i++) {


            LibLinearImpl.svm_problemImpl prob = imp.createSVM_Problem();


            prob.svm_problem.bias = 0;


            prob.setX(features);
            prob.setY(trainlabelsarray);
            prob.setL(features.size());


            para = new LibSVMImpl.svm_parameter();

            para.C = c_values[i];
            para.kernel_type = 0;
            para.eps = 0.0001;
            para.weight = new double[]{1, negative_examples / positive_examples};
            para.weight_label = new int[]{-1, 1};


            model = imp.svm_train(prob, para); //trains the svm model

            SVMPredict predict = new SVMPredict();

            TrainedSVM trained = new TrainedSVM(scales,model.getModel().getFeatureWeights(),feature_names);



            double[] scores = predict.predict_confidence(testfeatureMatrix,trained);

            boolean[] testLabelToBool = new boolean[testlabels.size()];




            for(int j=0;j<testlabels.size();j++){
                if(testlabels.get(j)==1){
                    testLabelToBool[j]=true;
                }else {
                    testLabelToBool[j]=false;
                }


            }



            Stats stats = new Stats(scores,testLabelToBool);

            if(stats.getAUC()>best_AUC){
                best_AUC=stats.getAUC();
                best_model=model;
            }



        }



        TrainedSVM trained = new TrainedSVM(scales,best_model.getModel().getFeatureWeights(),feature_names);

        int score_shift=10000;
        /**
         * this saves the bogus score dist for this svm
         */

        SVMPredict predict = new SVMPredict();
        double[] testscores = predict.predict_confidence(testfeatureMatrix,trained);
        ArrayList<Double> testscoresarray = new ArrayList<>();
        for(int i=0;i<testscores.length;i++){
            testscoresarray.add(testscores[i]+score_shift);
        }

        PvalueScoreUtils utils2 =  new PvalueScoreUtils();



       trained.bogusDist = utils2.estimate_lognormal_parameters(testscoresarray);
       trained.score_shift=score_shift;



        System.out.println(best_AUC);

        return trained;




    }


    public void writeScores(double[] scores, boolean[] labels){

try {
    FileWriter write_true = new FileWriter("/vol/clusterdata/fingerid_martin/fingerid-112_noldn/scores_true.txt");
    FileWriter write_bogus = new FileWriter("/vol/clusterdata/fingerid_martin/fingerid-112_noldn/scores_bogus.txt");


    for(int i=0;i<scores.length;i++){

        if (labels[i]==true){
            write_true.write(scores[i]+"\n");
        }else {
            write_bogus.write(scores[i]+"\n");
        }

    }

    write_bogus.close();
    write_true.close();



}catch (Exception e){
    e.printStackTrace();
}
    }


    public int posExampleAmount(double[] labels){
        int counter=0;

        for (double current: labels){
            if (current==1){
             counter+=1;
            }

        }

        return counter;

    }

    public int negExampleAmount(double[] labels){
        int counter=0;

        for (double current: labels){
            if (current==-1){
                counter+=1;
            }

        }

        return counter;

    }

  /*  public double evalTrainedPredictorAUC(TrainedSVM svm,double[][] features, double[] label){



        SVMPredict predict = new SVMPredict();

        double[] scores= predict.predict_confidence(features,svm);

        boolean[] label_as_boolean = new boolean[label.length];

        for(int i=0;i<label.length;i++){
            if(label[i]==1){
                label_as_boolean[i]=true;
            }else {
                label_as_boolean[i]=false;
            }

        }


        Stats stats = new Stats(scores,label_as_boolean);

        return(stats.getAUC());




    }*/




}
