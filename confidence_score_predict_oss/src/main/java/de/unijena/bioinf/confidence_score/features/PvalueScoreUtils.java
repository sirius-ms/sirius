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

package de.unijena.bioinf.confidence_score.features;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.Scored;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import org.apache.commons.math3.distribution.LogNormalDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.ParetoDistribution;
import org.openscience.cdk.tools.LoggingToolFactory;
import org.slf4j.LoggerFactory;
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
        HashMap<String,String> dupl_map =new HashMap<>();

        for (int i=0;i<ranked_candidates.length;i++) {
            if(!(ranked_candidates[i].getCandidate().getFingerprint().toOneZeroString().equals(current_candidate.getCandidate().getFingerprint().toOneZeroString())) && !dupl_map.containsKey(ranked_candidates[i].getCandidate().getFingerprint().toOneZeroString()))

                score_samples.add(ranked_candidates[i].getScore() + score_shift);
            dupl_map.put(ranked_candidates[i].getCandidate().getFingerprint().toOneZeroString(),"true");


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

        if (score_samples_last_mode.size()<5){
            score_samples_last_mode=score_samples; //TODO: This should prevent failed lognormal parameter estimations
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

        for(int i=1;i<candidates.length;i++){
            tosortlist.add(Math.log(candidates[i].getScore() + score_shift));

        }
        double[] scored_array= new double[tosortlist.size()];

//        if(dupe_counter>=2)
//            System.out.println("WARNING DUPLICATES");
        Collections.sort(tosortlist);
        for(int i=0;i<tosortlist.size();i++){
            scored_array[i]=tosortlist.get(i);
        }


        EmpiricalDist empdist= new EmpiricalDist(scored_array);

        double bandwidth= 0.7764*KernelDensityGen.getBaseBandwidth(empdist);

        if (bandwidth==0){
            LoggerFactory.getLogger(PvalueScoreUtils.class).debug(String.format(Locale.US,"Bandwidth estimation error"));
            return 100;
        }

        for(int i=0;i<scored_array.length;i++){

            NormalDistribution dist = new NormalDistribution(scored_array[i],bandwidth);

            double cp = (dist.cumulativeProbability(Math.log(current.getScore()+score_shift)));
            pvalue += (1-cp);
          //  pvalue+=1-cp;
        }


        // System.out.println(pvalue+" ---"+scored_array.length+" --- "+(double)scored_array.length);

        pvalue=(double)pvalue/(double)scored_array.length;

        if(pvalue==0){
            pvalue=Double.MIN_VALUE;
        }


        //System.out.println("pvalues: "+pvalue+" --- "+(double)biosize/candidates.length*pvalue+ " --- "+ (-Math.expm1(biosize* Math.log1p(-pvalue)))+" - "+biosize);

        double evalue= (biosize/candidates.length)*pvalue;

        return evalue>0 ? evalue : Double.MIN_VALUE;
        //return evalue;
        //return -Math.expm1(biosize* Math.log1p(-pvalue));
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
