package de.unijena.bioinf.confidence_score.features;

import de.unijena.bioinf.ChemistryBase.algorithm.Scored;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.confidence_score.Utils;
import de.unijena.bioinf.sirius.IdentificationResult;
import org.apache.commons.math3.distribution.LogNormalDistribution;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by martin on 25.06.18.
 */
public class PvalueScoreUtils {

    int score_shift=10000;



    public PvalueScoreUtils(){

    }


    public double computePvalueScore(Scored<FingerprintCandidate>[] ranked_candidates, Scored<FingerprintCandidate> current_candidate){

        Utils utils =  new Utils();

        ArrayList<Double> score_samples = new ArrayList<>();

        for (Scored<FingerprintCandidate> element : ranked_candidates) {

            score_samples.add(element.getScore() + score_shift);


        }
        Collections.sort(score_samples);


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


            score_samples_last_mode = new ArrayList<>(score_samples.subList(Math.max(mode_index - (score_samples.size() - mode_index), 0), score_samples.size()));
        }else{
            score_samples_last_mode=score_samples;
        }

        LogNormalDistribution dist = estimate_lognormal_parameters(score_samples_last_mode);

        double p_value_lognormal = 1 - dist.cumulativeProbability(current_candidate.getScore() + score_shift);
        //TODO: This is a whacky fix
        if (p_value_lognormal == 0) p_value_lognormal = 0.00000000000001;

        double score = p_value_lognormal * utils.condense_candidates_by_flag(ranked_candidates,current_candidate.getCandidate().getBitset()).length;

        return score;


    }

    public ArrayList<Integer> find_modes(ArrayList<Double> scores){
        int[] bins = new int [score_shift];
        ArrayList<Integer> modes =  new ArrayList<>();

        for(int i=0;i< scores.size();i++){

            double curr = scores.get(i)-score_shift;

            bins[(int) -curr]++;
        }

        int curr_max=5;
        boolean rising=false;

        for(int i=0;i<bins.length;i++){
            if (bins[i]>curr_max){
                curr_max=bins[i];
                rising=true;
            }else if(rising){

                curr_max=5;
                rising=false;
                modes.add(-i);
            }

        }

        return modes;



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
}
