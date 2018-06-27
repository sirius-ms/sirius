package de.unijena.bioinf.confidence_score.svm;

import de.unijena.bioinf.ChemistryBase.utils.FileUtils;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.json.JsonWriter;
import java.io.File;
import java.io.IOException;

/**
 * Created by martin on 27.06.18.
 */

//Trained SVM object containing all the feature weights, names and scaling information
public class TrainedSVM {
    double[] weights;
    String[] names;
    SVMScales scales;



    public TrainedSVM(SVMScales scales,double[] weights, String[] names){
        this.weights=weights;
        this.scales=scales;
        this.names=names;



    }



    public void exportAsJSON(File file){


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
        javax.json.JsonObject json_obj = json.build();
        try {


            JsonWriter write_json = Json.createWriter(FileUtils.getWriter(file));
            write_json.writeObject(json_obj);
            write_json.close();
        }catch (IOException e){
            e.printStackTrace();
        }




    }
}
