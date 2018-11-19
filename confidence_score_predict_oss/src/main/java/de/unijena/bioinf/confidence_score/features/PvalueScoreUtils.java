package de.unijena.bioinf.confidence_score.features;

import de.unijena.bioinf.ChemistryBase.algorithm.Scored;
import de.unijena.bioinf.ChemistryBase.ms.ft.Score;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.confidence_score.Utils;
import de.unijena.bioinf.sirius.IdentificationResult;
import org.apache.commons.math3.distribution.LogNormalDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.ParetoDistribution;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.mllib.clustering.GaussianMixture;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.mllib.clustering.GaussianMixtureModel;
import org.apache.spark.mllib.linalg.Vector;
import org.apache.spark.mllib.linalg.Vectors;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * Created by martin on 25.06.18.
 */
public class PvalueScoreUtils {

    int score_shift=10000;



    public PvalueScoreUtils(){

    }


    public double computePvalueScore(Scored<FingerprintCandidate>[] ranked_candidates, Scored<FingerprintCandidate> current_candidate){





        ArrayList<Double> score_samples = new ArrayList<>();

        for (int i=0;i<ranked_candidates.length;i++) {

            score_samples.add(ranked_candidates[i].getScore() + score_shift);


        }

        System.out.println(ranked_candidates[0]+" "+ranked_candidates[ranked_candidates.length-1]+" "+score_samples.get(0)+" "+score_samples.get(score_samples.size()-1));

      //  Collections.sort(score_samples);


        //sort so lowest element is 0 element
        if(score_samples.get(0)!=score_samples.get(score_samples.size()-1)){
            if(score_samples.get(0)>score_samples.get(score_samples.size()-1)){
                Collections.reverse(score_samples);

            }

        }



        ArrayList<Double> score_samples_last_mode;

        ArrayList<Integer> modes = find_modes(score_samples);
        Collections.sort(modes);

        if (modes.size() > 0) {


            int last_mode = modes.get(modes.size() - 1) + score_shift;

            int mode_index = -1;
            for (int i = 0; i < score_samples.size(); i++) {

                if (score_samples.get(i) > last_mode) {
                    mode_index = i;
                    break;
                }
            }

            score_samples_last_mode=   new ArrayList<>(score_samples.subList(Math.max(mode_index - (score_samples.size() - mode_index), 0), score_samples.size()));

        }else{
            score_samples_last_mode=score_samples;
        }

        LogNormalDistribution dist = estimate_lognormal_parameters(score_samples_last_mode);

        double p_value_lognormal = 1 - dist.cumulativeProbability(current_candidate.getScore() + score_shift);
        //TODO: This is a whacky fix
        if (p_value_lognormal == 0) p_value_lognormal = 0.00000000000001;

        double score = p_value_lognormal * ranked_candidates.length;//utils.condense_candidates_by_flag(ranked_candidates,flag).length;


        //sort list back to original state

        if(score_samples.get(0)!=score_samples.get(score_samples.size()-1)){
            if(score_samples.get(0)<score_samples.get(score_samples.size()-1)){
                Collections.reverse(score_samples);

            }

        }


        return score;


    }

    public List<Integer> mode(final List<Scored<FingerprintCandidate>> scores, String filename)  {


        ArrayList<Double> score_samples = new ArrayList<>();

        for (Scored<FingerprintCandidate> element : scores) {


                score_samples.add(element.getScore() + score_shift);

        }

        Collections.sort(score_samples);


try {
    BufferedWriter write = new BufferedWriter(new FileWriter(new File("/vol/clusterdata/fingerid_martin/exp2/pvalue_fit_scores/"+filename.split(">")[0]+"bin")));


    int[] numbers = new int[score_samples.size()];
    for (int i = 0; i < score_samples.size(); i++) {
        numbers[i] = score_samples.get(i).intValue();
    }


    final List<Integer> modes = new ArrayList<Integer>();
    final Map<Integer, Integer> countMap = new HashMap<Integer, Integer>();

    int max = -1;

    for (final int n : numbers) {
        int count = 0;

        if (countMap.containsKey(n)) {
            count = countMap.get(n) + 1;
        } else {
            count = 1;
        }

        countMap.put(n, count);

        if (count > max) {
            max = count;
        }
    }



    for (final Map.Entry<Integer, Integer> tuple : countMap.entrySet()) {
        if (tuple.getValue() == max) {
            modes.add(tuple.getKey());
        }
        write.write(tuple.getKey()+" "+tuple.getValue()+"\n");
    }
    write.close();

    return modes;
}catch (IOException e){
    e.printStackTrace();
}
return null;
    }


    public void gmmtest(ArrayList<Double> scores){

        GaussianMixture mixture = new GaussianMixture();

        String path = "data/mllib/gmm_data.txt";
        JavaSparkContext jsc = new JavaSparkContext();

        JavaRDD<String> data = jsc.textFile(path);


        JavaRDD<Vector> parsedData = data.map(s -> {
            String[] sarray = s.trim().split(" ");
            double[] values = new double[sarray.length];
            for (int i = 0; i < sarray.length; i++) {
                values[i] = Double.parseDouble(sarray[i]);
            }
            return Vectors.dense(values);
        });
        parsedData.cache();


// Cluster the data into two classes using GaussianMixture
        GaussianMixtureModel gmm = new GaussianMixture().setK(2).run(parsedData.rdd());


// Output the parameters of the mixture model
        for (int j = 0; j < gmm.k(); j++) {
            System.out.printf("weight=%f\nmu=%s\nsigma=\n%s\n",
                    gmm.weights()[j], gmm.gaussians()[j].mu(), gmm.gaussians()[j].sigma());
        }


    }

    public ArrayList<Integer> find_modes(ArrayList<Double> scores) {



        int[] bins = new int[score_shift];
        ArrayList<Integer> modes = new ArrayList<>();

        for (int i = 0; i < scores.size(); i++) {

            double curr = scores.get(i) - score_shift;

            bins[(int) -curr]++;
        }

        int curr_max = 5;
        boolean rising = false;

        for (int i = 0; i < bins.length; i++) {
            if (bins[i] > curr_max) {
                curr_max = bins[i];
                rising = true;
            } else if (rising) {

                curr_max = 5;
                rising = false;
                modes.add(-i);
            }

        }


        return modes;
    }

    public ParetoDistribution estimate_pareto_parameters(ArrayList<Double> scores){


            //input list is sorted

            double xmin = scores.get((int) (scores.size()/1.3));
            double sum = 0;
            double a;

            for (int i = (int) (scores.size()/1.3); i < scores.size(); i++) {
                sum += Math.log(scores.get(i)) - Math.log(xmin);

            }

            a = scores.size() / sum;

            return new ParetoDistribution(xmin, a);




    }



    public LogNormalDistribution estimate_lognormal_parameters(ArrayList<Double> scores){


        double score_sums=0;

        for(int i=0;i<scores.size();i++){
            score_sums+=Math.log(scores.get(i));
        }



        double mean = score_sums/scores.size();


        score_sums=0;

        for(int i=0;i<scores.size();i++){
            score_sums+=(Math.log(scores.get(i))-mean)*(Math.log(scores.get(i))-mean);
        }

        double sigma =Math.sqrt(score_sums/scores.size());


        LogNormalDistribution dist =  new LogNormalDistribution(mean,sigma);


        double median = Math.log(scores.get(scores.size()/2));
        double[] deviations_from_median=new double[scores.size()];
        ArrayList<Double> temp = new ArrayList();

        for(int i=0;i<scores.size();i++){
            deviations_from_median[i]=Math.abs(Math.log(scores.get(i))-median);
            temp.add(deviations_from_median[i]);
        }



        Collections.sort(temp);

        double mad = 1.4*temp.get(temp.size()/2);
        // System.out.println(mean+" "+median+" | "+mad+" "+sigma);


        //TODO: Median results are slightly worse, maybe check MAD coefficient?

        return dist;

    }

    public NormalDistribution estimate_normal_parameters(ArrayList<Double> scores) {

        double score_sums = 0;

        for (int i = 0; i < scores.size(); i++) {
            score_sums += scores.get(i);
        }

        double mean = score_sums / scores.size();

        score_sums = 0;

        for (int i = 0; i < scores.size(); i++) {
            score_sums += (scores.get(i) - mean) * (scores.get(i) - mean);
        }

        double sigma = Math.sqrt(score_sums / scores.size());

        NormalDistribution dist = new NormalDistribution(mean, sigma);
        return dist;

    }
}
