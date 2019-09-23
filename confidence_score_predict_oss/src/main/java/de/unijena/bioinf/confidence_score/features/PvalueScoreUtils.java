package de.unijena.bioinf.confidence_score.features;

import Tools.ExpectationMaximization1D;
import Tools.KMeans;
import de.unijena.bioinf.ChemistryBase.algorithm.Scored;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import jMEF.MixtureModel;
import jMEF.PVector;
import org.apache.commons.math3.distribution.LogNormalDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.ParetoDistribution;
import umontreal.ssj.probdist.EmpiricalDist;
import umontreal.ssj.randvar.KernelDensityGen;

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


    public double computePvalueScore(Scored<FingerprintCandidate>[] ranked_candidates, Scored<FingerprintCandidate>[] ranked_candidates_filtered, Scored<FingerprintCandidate> current_candidate){


       // double pvalue=compute_pvalue_with_KDE(ranked_candidates,current_candidate);

       // return(pvalue);

        ArrayList<Double> score_samples = new ArrayList<>();

        for (int i=0;i<ranked_candidates.length;i++) {

            score_samples.add(ranked_candidates[i].getScore() + score_shift);


        }


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

        double score = p_value_lognormal * ranked_candidates_filtered.length;


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

    public double compute_pvalue_with_KDE(Scored<FingerprintCandidate>[] candidates,Scored<FingerprintCandidate>[] candidates_filtered, Scored<FingerprintCandidate> current){

        double biosize= candidates_filtered.length;
        double pvalue=0;

        //remove best scoring hit from candidates (current)


        ArrayList<Double> tosortlist = new ArrayList<>();

        int dupe_counter=0;

        for(int i=0;i<candidates.length;i++){
            if(!(candidates[i].getScore()==current.getScore() && candidates[i].getCandidate().getInchiKey2D().equals(current.getCandidate().getInchiKey2D()))) {
                tosortlist.add(Math.log(candidates[i].getScore() + score_shift));
            }else {
                dupe_counter+=1;
            }
        }
        double[] scored_array= new double[tosortlist.size()];
        if (scored_array.length < 5)
            return 100;

        if(dupe_counter>=2)System.out.println("WARNING DUPLICATES");
        Collections.sort(tosortlist);
        for(int i=0;i<tosortlist.size();i++){
            scored_array[i]=tosortlist.get(i);
        }


        EmpiricalDist empdist= new EmpiricalDist(scored_array);

        double bandwidth= KernelDensityGen.getBaseBandwidth(empdist);

        for(int i=0;i<scored_array.length;i++){

            NormalDistribution dist = new NormalDistribution(scored_array[i],bandwidth);

            pvalue+=1-dist.cumulativeProbability(Math.log(current.getScore()+score_shift));



        }
       // System.out.println(pvalue+" ---"+scored_array.length+" --- "+(double)scored_array.length);
        pvalue=(double)pvalue/(double)scored_array.length;



        //System.out.println("pvalues: "+pvalue+" --- "+(double)biosize/candidates.length*pvalue+ " --- "+ (-Math.expm1(biosize* Math.log1p(-pvalue)))+" - "+biosize);



        return ((biosize/candidates.length)*pvalue);
        //return -Math.expm1(biosize* Math.log1p(-pvalue));
    }



    public double compute_pvalue_with_gmm(Scored<FingerprintCandidate>[] candidates, Scored<FingerprintCandidate> current){


        double pvalue=0;
        int component_nr=1;

        if(candidates.length>50) component_nr=2;









        double transformed_curr_score = Math.log(current.getScore()+score_shift);

        ArrayList<Double> score_samples = new ArrayList<>();

        for (int i=0;i<candidates.length;i++) {

            score_samples.add( Math.log(candidates[i].getScore() + score_shift));


        }

        PVector[]         vector   =  new PVector[score_samples.size()];

        for(int i=0;i<vector.length;i++){
            vector[i]= new PVector(1);
            vector[i].array[0]= score_samples.get(i);
        }


        Vector<PVector>[] clusters = KMeans.run(vector, component_nr);

        // Classical EM
        MixtureModel mmc;
        mmc = ExpectationMaximization1D.initialize(clusters);
        mmc = ExpectationMaximization1D.run(vector, mmc);


        for(int i=0;i<mmc.param.length;i++){
            PVector vec=(PVector)mmc.param[i];
            double mean= vec.array[0];
            double sigma= vec.array[1];
            double weight= mmc.weight[i];

            NormalDistribution norm= new NormalDistribution(mean,Math.sqrt(sigma));

            double partial = weight*(1-norm.cumulativeProbability(transformed_curr_score))*(weight*candidates.length);
            pvalue+=partial;


        }
        return pvalue;
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
