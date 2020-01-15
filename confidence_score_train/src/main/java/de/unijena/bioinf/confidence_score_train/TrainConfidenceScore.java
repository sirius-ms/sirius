package de.unijena.bioinf.confidence_score_train;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.Scored;
import de.unijena.bioinf.confidence_score.CombinedFeatureCreator;
import de.unijena.bioinf.confidence_score.FeatureCreator;
import de.unijena.bioinf.confidence_score.features.PvalueScoreUtils;
import de.unijena.bioinf.confidence_score.svm.*;
import de.unijena.bioinf.confidence_score_train.svm.LibSVMImpl;

import de.unijena.bioinf.fingerid.svm.Svm;
import org.libsvm.SVM;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
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



    public TrainedSVM trainLinearSVMWithCV(double[][] featureMatrix,double[] labels, String[] IDs, double[][] synth_features, double[] synth_labels,String[] feature_names,String dist,String fe)throws IOException{

        FileWriter write_features= new FileWriter("/vol/clusterdata/fingerid_martin/exp2_nfp/features.csv");


        int folds = 10;

        this.featureMatrix=featureMatrix;
        this.labels=labels;

        SVMUtils utils = new SVMUtils();

        System.out.println("starting scale calc");
        SVMScales scales =  utils.calculateScales(featureMatrix);
        System.out.println("starting standard");

        utils.standardize_features(featureMatrix,scales);

       // utils.updateForNormalization(scales,featureMatrix);
      //  utils.normalize_features(featureMatrix,scales);
        //System.out.println("Normalizing");
      //  for(int i=0;i<featureMatrix.length;i++){
       //     System.out.print("Normal "+Arrays.toString(featureMatrix[i]));
     //   }






        List<Integer> range = IntStream.rangeClosed(0, featureMatrix.length-1)
                .boxed().collect(Collectors.toList());

       // Collections.shuffle(range);


        int start_index=0;
        int end_index=0;


        ArrayList<Double> scores_all = new ArrayList<>();

        ArrayList<Boolean> label_all = new ArrayList<>();

      for(int i=0;i<folds;i++){

          end_index=start_index+range.size()/folds;
          System.out.println("in fold "+i+" - start: "+start_index+" - end: "+end_index);


          ArrayList<double[]> featuresTempTest = new ArrayList<>();

          ArrayList<String> ids_Test= new ArrayList<>();
          ArrayList<String> ids_Train= new ArrayList<>();

          ArrayList<Double>  labelsTempTest = new ArrayList<>();

          ArrayList<double[]> featuresTempTrain = new ArrayList<>();

          ArrayList<Double> labelsTempTrain = new ArrayList<>();



          for(int x=0;x<range.size();x++){

              if(x>start_index && x<end_index) {

                  featuresTempTest.add(featureMatrix[range.get(x)].clone());

                  labelsTempTest.add(labels[range.get(x)]);
                  ids_Test.add(IDs[range.get(x)]);
              }else {

                  featuresTempTrain.add(featureMatrix[range.get(x)].clone());
                  ids_Train.add(IDs[range.get(x)]);
                  labelsTempTrain.add(labels[range.get(x)]);
              }

          }

          //add synthetic features
          for(int u=0;u<synth_features.length;u++) {
              System.out.println(Arrays.toString(synth_features[u]));
              featuresTempTrain.add(synth_features[u].clone());
              labelsTempTrain.add(synth_labels[u]);
              ids_Train.add("synth");
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
          String[] id_train_final = new String[ids_Train.size()];
          for (int z=0;z<ids_Train.size();z++){
              id_train_final[z]=ids_Train.get(z);
          }



          System.out.println("train size: "+featuresFinalTrain.length);

          TrainedSVM svm =  trainLinearSVM(featuresFinalTrain,labelsFinalTrain,featuresFinalTest,labelsFinalTest,feature_names,scales,String.valueOf(i),id_train_final);

          writeFold(ids_Test,svm,String.valueOf(i),dist,fe);

          for(int h=0;h<svm.weights.length;h++){
              System.out.print(svm.weights[h]+" , ");

          }
          System.out.println();

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

          PvalueScoreUtils utils2 =  new PvalueScoreUtils();






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










        write_features.close();


        return trained;

    }

    public TrainedSVM trainLinearSVM(double[][] trainfeatureMatrix, double[] labels, double[][] testFeatureMatrix, double[] testLabels,String[] feature_names,SVMScales scales, String id,String[] ids_train){

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

        System.out.println("before gridsearch");

        double[] c_values = new double[]{10,100,1000};
        double[] epsilon_values=  new double[]{0.000001,0.00001,0.0001,0.001,0.01,0.1,1};

        double best_TPR=-1;
        double[] best_probAB = new double[2];
        LibLinearImpl.svm_model best_model=null;

        for(int i=0;i<c_values.length;i++) {
            for(int r=0;r<epsilon_values.length;r++) {
                System.out.println("Computing C: "+c_values[i]+"  Computing epsilon: "+epsilon_values[r]);


                LibLinearImpl.svm_problemImpl prob = imp.createSVM_Problem();


                prob.svm_problem.bias = 0;


                prob.setX(features);
                prob.setY(labels);
                prob.setL(features.size());


                para = new LibSVMImpl.svm_parameter();

                para.C = c_values[i];
                para.kernel_type = 0;
                para.eps =epsilon_values[r];
                para.weight = new double[]{1, 1};
                para.weight_label = new int[]{-1, 1};


                model = imp.svm_train(prob, para); //trains the svm model
                System.out.println("trained");

                SVMPredict predict = new SVMPredict();

                TrainedSVM trained = new TrainedSVM(scales, model.getModel().getFeatureWeights(), feature_names);

                //TODO: testing
                double[] probAB = new double[2];


                //remove all synth features for sigmoid training
                int counter=0;
                for (int t=0;t<ids_train.length;t++){
                    if (!ids_train[t].equals("synth")){
                        counter++;
                    }
                }


                double[][] reduced_f_matrix = new double[counter][trainfeatureMatrix[0].length];
                double[] reduced_labels = new double[counter];

                for (int q=0;q<trainfeatureMatrix.length;q++){
                    if (!ids_train[q].equals("synth")){
                        reduced_f_matrix[q]=trainfeatureMatrix[q];
                        reduced_labels[q]=labels[q];
                    }
                }
                System.out.println("reduced: "+reduced_f_matrix.length+" - orig: "+trainfeatureMatrix);

                //end


                trainSigmoid(reduced_f_matrix, reduced_labels, trained, probAB,false);
                System.out.println("trained sigmoid"+" - "+probAB[0]+" - "+probAB[1]);

                //trained.probAB=null;
                //System.out.println("Platt scaling is off!");



                double[] scores = predict.predict_confidence(testFeatureMatrix, trained);

                boolean[] testLabelToBool = new boolean[testLabels.length];

                for (int j = 0; j < testLabels.length; j++) {
                    if (testLabels[j] == 1) {
                        testLabelToBool[j] = true;
                    } else {
                        testLabelToBool[j] = false;
                    }


                }


                Stats stats = new Stats(scores, testLabelToBool);


                double current_AUC=stats.getAUC();


                //writeBogusScores(scores, testLabelToBool);

                if (current_AUC > best_TPR) {
                    best_TPR = current_AUC;
                    best_model = model;
                    best_probAB = probAB;
                }

            }

        }



        TrainedSVM trained = new TrainedSVM(scales,best_model.getModel().getFeatureWeights(),feature_names);


        trained.probAB=best_probAB;
        SVMPredict predict = new SVMPredict();


        double[] scores = predict.predict_confidence(testFeatureMatrix, trained);

        boolean[] testLabelToBool = new boolean[testLabels.length];

        for (int j = 0; j < testLabels.length; j++) {
            if (testLabels[j] == 1) {
                testLabelToBool[j] = true;
            } else {
                testLabelToBool[j] = false;
            }


        }
        System.out.println("fold complete"+id);


        //writeScores(scores,testLabelToBool,id);

        System.out.println(best_TPR+" - "+best_model.getParam().C+" - "+best_model.getParam().eps);

        return trained;






    }

    public void writeBogusScores(double[] scores, boolean[] label){
        try {
            FileWriter writeBogus = new FileWriter(new File("/vol/clusterdata/fingerid_martin/exp2/bogus_dist.txt"));

            for(int i=0;i<scores.length;i++){

              //  if(label[i]==false){
                    writeBogus.write(scores[i]+"\n");
              //  }
            }


            writeBogus.close();

        }catch (Exception e){
            e.printStackTrace();
        }




    }


    public TrainedSVM trainLinearSVMNoEval(double[][] featureMatrix,double[] labels, String[] feature_names,double[][] synth_matrix, double[] synth_labels,double[] directions,double c,double epsilon){


        imp = new LibLinearImpl();
        SVMUtils utils = new SVMUtils();
        LibLinearImpl.svm_model model =null;



        SVMScales scales = utils.calculateScales(featureMatrix);

        utils.standardize_features(featureMatrix,scales);
        //utils.normalize_features(featureMatrix,scales);


        //split into test/train





        List<List<LibLinearImpl.svm_nodeImpl>> features =  new ArrayList<>();
        LibLinearImpl.svm_parameter para;
        int positive_examples=0;
        int negative_examples=0;




        for(int i=0;i<featureMatrix.length;i++){
            int index=1;
            List<LibLinearImpl.svm_nodeImpl> temp_list= new ArrayList<>();
            for(int j=0;j<featureMatrix[i].length;j++) {


                temp_list.add(imp.createSVM_Node(index, featureMatrix[i][j]));
                index++;
            }

            features.add(temp_list);
        }
        for(int u=0;u<synth_matrix.length;u++) {
            int index=1;
            List<LibLinearImpl.svm_nodeImpl> temp_list= new ArrayList<>();
            for(int j=0;j<synth_matrix[u].length;j++) {
                temp_list.add(imp.createSVM_Node(index,synth_matrix[u][j]));
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

        double[] labels_complete= new double[labels.length+synth_labels.length];
        for(int i=0;i<labels.length;i++){
            labels_complete[i]=labels[i];
        }

        for(int i=labels.length;i<labels_complete.length;i++){
            labels_complete[i]=synth_labels[i-labels.length];
        }



            LibLinearImpl.svm_problemImpl prob = imp.createSVM_Problem();


            prob.svm_problem.bias = 0;


            prob.setX(features);
            prob.setY(labels_complete);
            prob.setL(features.size());


            para = new LibSVMImpl.svm_parameter();

            para.C = c;
            para.kernel_type = 0;
            para.eps = epsilon;
            para.weight = new double[]{1, 1};
            para.weight_label = new int[]{-1, 1};


            model = imp.svm_train(prob, para); //trains the svm model



            TrainedSVM trained = new TrainedSVM(scales,model.getModel().getFeatureWeights(),feature_names);


            double[] probAB = new double[2];

            trainSigmoid(featureMatrix,labels,trained,probAB,false);



            //getUpperFeatures(featureMatrix,0.6,trained);



           // SVM.sigmoid_train(featureMatrix.length,predict.predict_confidence(featureMatrix,trained),labels,probAB);



        trained.probAB=probAB;

        for(int h=0;h<trained.weights.length;h++){
            System.out.print(trained.weights[h]+" , ");

        }
        System.out.println();


        return trained;




    }


    public void trainSigmoid(double[][] featureMatrix, double[] labels,TrainedSVM svm ,double[] probAB, Boolean cut){

        HashMap<double[], Double> feature_to_label = new HashMap<>();

        for(int i=0;i<featureMatrix.length;i++){
            feature_to_label.put(featureMatrix[i],labels[i]);
        }

        double[][] cutFeatures = getUpperFeatures(featureMatrix,1,svm);

        double[] cutlabel = new double[cutFeatures.length];

        for(int i=0;i<cutFeatures.length;i++){
            cutlabel[i]=feature_to_label.get(cutFeatures[i]);


        }
        SVMPredict pred = new SVMPredict();


        if (cut==true)
        SVM.sigmoid_train(cutFeatures.length,pred.predict_confidence(cutFeatures,svm),cutlabel,probAB);
        if (cut==false)
        SVM.sigmoid_train(featureMatrix.length,pred.predict_confidence(featureMatrix,svm),labels,probAB);






    }

    public void writeFold(ArrayList<String> ids_test, TrainedSVM svm, String foldID,String dist,String fe){
        try {
            System.out.println("writing fold"+foldID);
            File folder = new File("/vol/clusterdata/fingerid_martin/fingerid_confidence_120/cv_folds/fold"+foldID);
            BufferedWriter write_folds = new BufferedWriter(new FileWriter(new File(folder+"/testids_"+dist+"_"+fe+"_"+foldID)));

            for(int i=0;i<ids_test.size();i++){
                write_folds.write(ids_test.get(i)+"\n");
            }
            write_folds.close();
            svm.exportAsJSON(new File(folder+"/svm_"+dist+"_"+fe+"_"+foldID));




        }catch (Exception e){
            e.printStackTrace();
        }
    }


    public double[][] getUpperFeatures(double[][] matrix, double perc, TrainedSVM trained){

        SVMPredict pred = new SVMPredict();

        double[][] outputMatrix = new double[(int) Math.round(matrix.length*perc)][matrix[0].length];

        double[] scores = pred.predict_confidence(matrix,trained);

        ArrayList<Scored<double[]>> scoredlist = new ArrayList<>();




        for(int i=0;i<scores.length;i++){
            Scored<double[]> scored = new Scored<>(matrix[i],scores[i]);
            scoredlist.add(scored);
        }

        Collections.sort(scoredlist);
        Collections.reverse(scoredlist);
        
        for(int i=0;i<(int) Math.round(matrix.length*perc);i++){
            outputMatrix[i]=scoredlist.get(i).getCandidate();

        }

        return outputMatrix;




    }

    public void writeScores(double[] scores, boolean[] labels){

try {
    FileWriter write_true = new FileWriter("/vol/clusterdata/fingerid_martin/fingerid_confidence_120/scores_true.txt");
    FileWriter write_bogus = new FileWriter("/vol/clusterdata/fingerid_martin/fingerid_confidence_120/scores_bogus.txt");


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






}
