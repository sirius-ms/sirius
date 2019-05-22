package de.unijena.bioinf.confidence_score.svm;

import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import org.apache.commons.math3.distribution.LogNormalDistribution;
import org.json.JSONArray;

import javax.json.*;
import java.io.File;
import java.io.IOException;

/**
 * Created by martin on 27.06.18.
 */

//Trained SVM object containing all the feature weights, names and scaling information
public class TrainedSVM {
    double[] weights;
    String[] names;
    public SVMScales scales;
    public double[] probAB;
    public int score_shift;



    public TrainedSVM(SVMScales scales,double[] weights, String[] names){
        this.weights=weights;
        this.scales=scales;
        this.names=names;



    }

    public TrainedSVM(File file){
        import_parameters(file);


    }



    public void exportAsJSON(File file){


        //TODO: false score distribution in here?

        JsonObjectBuilder json = Json.createObjectBuilder();

        for(int i=0;i<weights.length;i++) {

            JsonArrayBuilder json_array = Json.createArrayBuilder();

            json_array.add(names[i]);
            json_array.add(weights[i]);
            json_array.add(scales.medians[i]);
            json_array.add(scales.deviations[i]);
            json_array.add(scales.min_feature_values[i]);
            json_array.add(scales.max_feature_values[i]);


            json.add("feature "+i, json_array.build());

        }


        JsonArrayBuilder sigmoid_array =  Json.createArrayBuilder();



        sigmoid_array.add(probAB[0]);
        sigmoid_array.add(probAB[1]);


        json.add("sigmoid",sigmoid_array.build());

        javax.json.JsonObject json_obj = json.build();

        try {


            JsonWriter write_json = Json.createWriter(FileUtils.getWriter(file));
            write_json.writeObject(json_obj);
            write_json.close();
        }catch (IOException e){
            e.printStackTrace();
        }




    }

    public void import_parameters(File file){


        try {


            JsonReader parse_json_marvin = Json.createReader(FileUtils.getReader(file));
            javax.json.JsonObject object_marvin = parse_json_marvin.readObject();
            parse_json_marvin.close();

            names= new String[object_marvin.keySet().size()];
            weights= new double[names.length];



            double[] medians = new double[names.length];
            double[] devs =  new double[names.length];
            double[] mins = new double[names.length];
            double[] maxs =  new double[names.length];

            int counter=0;

            for (String key : object_marvin.keySet()){

                JsonArray curr =  object_marvin.getJsonArray(key);

                if(key.contains("feature")) {


                    names[counter] = curr.get(0).toString();
                    weights[counter] = Double.parseDouble(curr.get(1).toString());
                    medians[counter] = Double.parseDouble(curr.get(2).toString());
                    devs[counter] = Double.parseDouble(curr.get(3).toString());
                    mins[counter] = Double.parseDouble(curr.get(4).toString());
                    maxs[counter] = Double.parseDouble(curr.get(5).toString());

                    counter++;

                }

                if(key.contains("sigmoid")){
                    probAB = new double[2];

                    probAB[0]= Double.parseDouble(curr.get(0).toString());
                    probAB[1]=Double.parseDouble(curr.get(1).toString());



                }







            }

            scales= new SVMScales(medians,devs,mins,maxs);


        }catch (IOException e){
            e.printStackTrace();
        }


    }

    public void import_parameters(JsonObject jsonObject){
        names= new String[jsonObject.keySet().size()];
        weights= new double[names.length];
        probAB = new double[2];


        double[] medians = new double[names.length];
        double[] devs =  new double[names.length];
        double[] mins = new double[names.length];
        double[] maxs =  new double[names.length];

        int counter=0;

        for (String key : jsonObject.keySet()){

            JsonArray curr =  jsonObject.getJsonArray(key);

            if(key.contains("feature")) {


                names[counter] = curr.get(0).toString();
                weights[counter] = Double.parseDouble(curr.get(1).toString());
                medians[counter] = Double.parseDouble(curr.get(2).toString());
                devs[counter] = Double.parseDouble(curr.get(3).toString());
                mins[counter] = Double.parseDouble(curr.get(4).toString());
                maxs[counter] = Double.parseDouble(curr.get(5).toString());

                counter++;

            }

            if(key.contains("sigmoid")){

                probAB[0]= Double.parseDouble(curr.get(0).toString());
                probAB[1]=Double.parseDouble(curr.get(1).toString());



            }







        }

        scales= new SVMScales(medians,devs,mins,maxs);






}


}
