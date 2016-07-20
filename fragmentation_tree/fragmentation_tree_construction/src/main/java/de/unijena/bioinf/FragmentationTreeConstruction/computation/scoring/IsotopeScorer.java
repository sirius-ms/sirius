/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2015 Kai Dührkop
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.utils.ChargedSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedPeak;
import de.unijena.bioinf.IsotopePatternAnalysis.IsotopePattern;
import de.unijena.bioinf.IsotopePatternAnalysis.IsotopePatternAnalysis;
import de.unijena.bioinf.MassDecomposer.Chemistry.MassToFormulaDecomposer;
import gurobi.*;

import java.util.*;


/**
 * //todo UNDER CONSTRUCTION! NOT completely TESTED BY NOW!
 * Scorers the isotope patterns.
 * finds the most likely segmentation of isotope patterns using Maximum-Weight Independent Set.
 * By Now only this explanation is used. Later suboptimal patterns will be added greedy.
 * But therefore you need to color contradictory explanations equally and put both in the FragmentationGraph.
 */
public class IsotopeScorer implements DecompositionScorer<boolean[]>{
    private static final double NEUTRON_MASS = 1.00866491;
    private IsotopePatternAnalysis patternScorer;
    private int maxLength;
    private ProcessedPeak[] peaks;
    private Deviation deviation;
    private Ionization ionization;
    private MassToFormulaDecomposer decomposer;
    private Pattern[] patterns;
    private Map<ProcessedPeak, Pattern> peakToPatternMap;
    private GRBEnv env;
    private double penalty;
    private boolean isPrepared;
    private double indecomposablePenalty = -1000; //maybe noise or an isotope
    private static final boolean VERBOSE = false;
    private static final double DEFAULT_PENALTY = -5;
    //todo irgendwas mit Intensitäten und relativen Intensitäten beachten?


    //todo implement as PatternExtractor?
    //todo: look at different deviations needed for IsotopePatternScorer. intensityDev, MassDevScorer uses different dev for monoIso!!


    //todo use for different charges in same spectrum?

    //todo add setter to set decomposer later in FragmentationPatternAnalysis?
    //todo irgendeine rückgabe der pattern+score, falls man nciht nur optimale will
    public IsotopeScorer(GRBEnv env, IsotopePatternAnalysis isotopePatternScorer, MassToFormulaDecomposer decomposer, int maxPatternLength, double penalty){
        this.patternScorer = isotopePatternScorer;
        this.maxLength = maxPatternLength;
        this.decomposer = decomposer;
        this.env = env;
        this.penalty = penalty;
        this.isPrepared = false;
    }

    public IsotopeScorer(IsotopePatternAnalysis isotopePatternScorer, MassToFormulaDecomposer decomposer, int maxPatternLength, double penalty){
        this(getDefaultEnv(), isotopePatternScorer, decomposer, maxPatternLength, penalty);
    }

    public IsotopeScorer(IsotopePatternAnalysis isotopePatternScorer, MassToFormulaDecomposer decomposer, int maxPatternLength){
        this(getDefaultEnv(), isotopePatternScorer, decomposer, maxPatternLength, DEFAULT_PENALTY);
    }

    @Override
    public boolean[] prepare(ProcessedInput input) {
        peakToPatternMap = new HashMap<ProcessedPeak, Pattern>();
        //todo parent nicht wegschmeißen !!!!

         //todo peaks sorted by mass?

        this.deviation = input.getMeasurementProfile().getAllowedMassDeviation(); //todo what about intensities in deviation?
        this.ionization = input.getExperimentInformation().getPrecursorIonType().getIonization();
        final FormulaConstraints constraints = input.getMeasurementProfile().getFormulaConstraints();
        //todo group pattern with same monosisotopic -> only have to decompose once

        //Estimate all possible pattern
        findAndScorePatterns(input.getMergedPeaks().toArray(new ProcessedPeak[0]), constraints, input.getExperimentInformation());


        //solve independent set
        boolean[] usedPatterns;
        try {
            IndependentSetSolver solver = new IndependentSetSolver(env, patterns, peaks);
            usedPatterns = solver.solve();
            double score = 0;
            for (int i = 0; i < usedPatterns.length; i++) {
                if (usedPatterns[i]==true){
                    score += patterns[i].getScore();
                    peakToPatternMap.put(patterns[i].getMonoIsotopicPeak(), patterns[i]);
                }

            }
            System.out.println("score: "+score);
        } catch (GRBException e) {
            throw new RuntimeException(e);
        }

        isPrepared = true;
        return usedPatterns;   //todo store all information in IsotopeScorer object?
    }




    /*
    peaks[]  sorted by mass!
     */
    private void findAndScorePatterns(ProcessedPeak[] peaks, FormulaConstraints constraints, Ms2Experiment experiment){
        for (int i = 0; i < peaks.length; i++) {
            System.out.print(peaks[i].getMass()+" ");

        }
        System.out.println();
        this.peaks = peaks;
        List<Pattern> possiblePatterns = new ArrayList<Pattern>();
        for (int start = 0; start < peaks.length; start++) {
             //find all possible longest patterns starting at peaks[start]
            List<MolecularFormula> decompositions = decomposer.decomposeToFormulas(peaks[start].getUnmodifiedMass(), deviation, constraints);
            if (VERBOSE) System.out.println("decomps for "+peaks[start].getUnmodifiedMass()+": "+Arrays.toString(decompositions.toArray(new MolecularFormula[0])));
            if (decompositions.size()>0){
                List<List<ProcessedPeak>> patternList = new ArrayList<List<ProcessedPeak>>();
                List<ProcessedPeak> singleList = new ArrayList<ProcessedPeak>();
                singleList.add(peaks[start]);
                patternList.add(singleList);
                List<List<ProcessedPeak>> newPatternList;
                int pos = start+1;
                int patternPos = 0;
                boolean goOn = true;
                while (goOn){
                    patternPos++;
                    if (patternPos>=maxLength) break; //pattern not longer than maxLength
                    goOn = false;
                    newPatternList = new ArrayList<List<ProcessedPeak>>();
                    for (int i = pos; i < peaks.length; i++) {
                        if (deviation.inErrorWindow(patternPos+peaks[start].getMass(), peaks[i].getMass())){ //todo better use difference?
                            goOn = true;
                            for (List<ProcessedPeak> peakList : patternList) {
                                final List<ProcessedPeak> newPeakList = new ArrayList<ProcessedPeak>(peakList);
                                newPeakList.add(peaks[i]);
                                newPatternList.add(newPeakList);
                            }
                        } else if (peaks[i].getMass() > (patternPos+peaks[start].getMass())) {
                            if (i <=pos) break;
                            pos = i;
                            break;
                        }
                    }
                    if (newPatternList.size() > 0) patternList = newPatternList;
                    //else break; //todo passt das?
                }
                //..............................................
                System.out.println("foundPatternList: "+(patternList.size()==1 ? "0" : "")+patternList.size());
                //insert all possible pattern lengths...
                for (List<ProcessedPeak> peakList : patternList) {
                    System.out.println("size: "+peakList.size());
                    for (int i = 0; i < peakList.size(); i++) {
                        final Pattern pattern = new Pattern(peakList.subList(0, i+1));

                        final List<IsotopePattern> scorePatts = patternScorer.scoreFormulas(new SimpleSpectrum(pattern.createChargedSpectrum()), decompositions, experiment, patternScorer.getDefaultProfile());
                        final double[] scores = new double[scorePatts.size()];
                        for (int k=0; k < scorePatts.size(); ++k) scores[k] = scorePatts.get(k).getScore();
                        if (VERBOSE) System.out.println("scores: "+Arrays.toString(scores));
                        final int scorePos = bestScorePos(scores);
                        if (scorePos >= 0){
                            pattern.setScore(scores[scorePos]);
                            pattern.setBestDecomposition(decompositions.get(scorePos));
                            //changed
                            possiblePatterns.add(pattern);
                        } else {
                            //all scores -Infinity
                            //todo obviously stupid pattern
                            //todo options -> 1. just delete it ( -> problem: could it be, that there doesn't exist at least one pattern for each peak?)
                            //todo 2. -> give it indecomposablePenalty : not that good because algorithm will take stupid choises (won't pick single peaks out of this pattern because scores are always negative)
                            //todo 3. -> give it really low score below indecomposablePenalty
                            //by now just remove it
                            /*
                            pattern.setScore(Double.NEGATIVE_INFINITY);
                            pattern.setBestDecomposition(null);
                            assert scores.length>0;
                            System.out.println("scores: "+Arrays.toString(scores));
                            System.out.println("decomps for "+peaks[start].getUnmodifiedMass()+": "+Arrays.toString(decompositions.toArray(new MolecularFormula[0])));
                            for (ProcessedPeak peak : pattern.peaks) {
                                System.out.println("mz: "+peak.getMass()+" | int: "+peak.getIntensity()+" | relInt: "+peak.getRelativeIntensity());
                            }
                            throw new RuntimeException("mono peak was decomposable, but isotope pattern score was -Infinity");
                            */
                        }

                        //possiblePatterns.add(pattern);
                    }
                }

            } else {
                //noise or isotope peak
                final Pattern pattern = new Pattern(Collections.singletonList(peaks[start]));
                pattern.setBestDecomposition(null);
                pattern.setScore(indecomposablePenalty);
                possiblePatterns.add(pattern);
            }




        }

        patterns = possiblePatterns.toArray(new Pattern[0]);
    }



    private int bestScorePos(double[] scores){
        double max = Double.NEGATIVE_INFINITY;
        int pos = -1;
        for (int i = 0; i < scores.length; i++) {
            if (scores[i]>max){
                max = scores[i];
                pos = i;
            }
        }
        return pos;
    }

    private static GRBEnv getDefaultEnv() {
        try {
            final GRBEnv env = new GRBEnv();
            env.set(GRB.IntParam.OutputFlag, 0);
            return env;
        } catch (GRBException e) {
            throw new RuntimeException(e);
        }
    }


    public Pattern[] getPatterns() {
        return patterns;
    }

    public ProcessedPeak[] getPeaks() {
        return peaks;
    }

    public double getIndecomposablePenalty() {
        return indecomposablePenalty;
    }

    public void setIndecomposablePenalty(double indecomposablePenalty) {
        this.indecomposablePenalty = indecomposablePenalty;
    }

    public List<Pattern> getSubOptomal(int count){
        //todo implement
        if (!isPrepared) return null;
        return null;
    }

    @Override
    public double score(MolecularFormula formula, ProcessedPeak peak, ProcessedInput input, boolean[] precomputed) {
        final Pattern pattern = peakToPatternMap.get(peak);
        if (pattern != null){
            if (pattern.getBestDecomposition().equals(formula)) return pattern.getScore();
            //todo how to score formulas which had not the best score to the isotope pattern?
            //todo recalculate vs. storing all?
            return Math.max(patternScorer.scoreFormulas(new SimpleSpectrum(pattern.createChargedSpectrum()), Collections.singletonList(formula), input.getExperimentInformation(), patternScorer.getDefaultProfile()).get(0).getScore(), penalty); //max to avoid -Infinity score
        }

        return penalty; //todo how to score peaks, which weren't rated as mono isotopic according to indepentent set solver
        //todo how to integrate suboptimal?

    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        //To change body of implemented methods use File | Settings | File Templates.
    }


    public class Pattern implements Comparable<Pattern>{
        private double score;
        private MolecularFormula bestDecomposition;
        final private ProcessedPeak monoIsotopic;
        final private List<ProcessedPeak> peaks;


        Pattern(List<ProcessedPeak> peaks){
            if (peaks.size()>6) throw new RuntimeException("too long pattern");
            this.monoIsotopic = peaks.get(0);
            this.peaks = Collections.unmodifiableList(peaks);
        }

        public boolean overlap(Pattern pattern){
            int i = 0;
            int j = 0;
            while (i<peaks.size() && j<pattern.getPeaks().size()){
                if (peaks.get(i).getMass()<pattern.getPeaks().get(j).getMass()){
                    i++;
                } else if (peaks.get(i).getMass()>pattern.getPeaks().get(j).getMass()){
                    j++;
                } else {
                    return true;
                }
            }
            return false;
        }

        public boolean sameMonoIsotopic(Pattern pattern){
            if (peaks.equals(pattern.getPeaks().get(0))) return true;
            return false;
        }

        public double getScore() {
            return score;
        }

        private void setScore(double score) {
            this.score = score;
        }

        public MolecularFormula getBestDecomposition() {
            return bestDecomposition;
        }

        private void setBestDecomposition(MolecularFormula bestDecomposition) {
            this.bestDecomposition = bestDecomposition;
        }

        public ProcessedPeak getMonoIsotopicPeak() {
            return monoIsotopic;
        }

        public int size(){
            return peaks.size();
        }

        public ChargedSpectrum createChargedSpectrum() {
            double[] masses = new double[peaks.size()];
            double[] intensities = new double[peaks.size()];
            int i = 0;
            for (ProcessedPeak peak : peaks) {
                masses[i] = peak.getMass();
                //if (peaks.size()==4) System.out.println("rel: "+peak.getRelativeIntensity()+" m:"+peak.getMass());
                intensities[i] = peak.getRelativeIntensity(); //todo relative oder intensity????? !!!!
                i++;
            }
            return new ChargedSpectrum(masses, intensities, getMonoIsotopicPeak().getIon());
        }

        public List<ProcessedPeak> getPeaks(){
            return  peaks;
        }

        @Override
        public int compareTo(Pattern o) {
            List<ProcessedPeak> oPeaks = o.getPeaks();
            //final int compareLength = -Integer.compare(oPeaks.size(), peaks.size());
            //if (compareLength!=0) return compareLength;
            for (int i = 0; i < Math.min(oPeaks.size(), peaks.size()); i++) {
                final int compare = -Double.compare(oPeaks.get(i).getMass(), peaks.get(i).getMass());
                if (compare!=0) return compare;
            }
            return -Integer.compare(oPeaks.size(), peaks.size());
        }
    }


    private class IndependentSetSolver{
        GRBEnv env;
        final GRBModel model;
        final GRBVar[] variables;
        Pattern[] patterns;
        ProcessedPeak[] peaks;
        boolean built;

        IndependentSetSolver(GRBEnv env, Pattern[] patterns, ProcessedPeak[] peaks) throws GRBException {
            this.env = env;
            this.patterns = patterns;

            //Arrays.sort(this.patterns);
            System.out.println("patterns solver: ");
            for (int i = 0; i < patterns.length; i++) {
                System.out.println(this.patterns[i].getMonoIsotopicPeak().getMass()+" | "+this.patterns[i].getPeaks().size());

            }
            this.peaks = peaks;
            this.model = new GRBModel(env);
            this.variables = new GRBVar[patterns.length];
            //this.variables = new GRBVar[patterns.length];
            this.built = false;
        }


        //todo set good start values?
        private void build() {
            try {
                defineVariables();
                setConstraints();
                model.update();
                built = true;
            } catch (GRBException e) {
                throw new RuntimeException(String.valueOf(e.getErrorCode()), e);
            }
        }



        /**
         *
         * @return boolean array indicating which patterns are used
         */
        private boolean[] solve(){
            try {
                if (!built) build();
                model.optimize();
                if (model.get(GRB.IntAttr.Status) != GRB.OPTIMAL){           //todo can there occur problems?
                    if (model.get(GRB.IntAttr.Status) == GRB.INFEASIBLE) {
                        //todo maybe one weight was -Infinity -> check while adding
                        throw new RuntimeException("Can't find a feasible solution: Solution is buggy"); //shouldn't occur?
                    } else {
                        throw new RuntimeException("Can't find optimal solution");
                    }
                }
                System.out.println("alles schön");
                final double score = -model.get(GRB.DoubleAttr.ObjVal);
                System.out.println("score1: "+score);
                final boolean[] usedPatterns = getVariableAssignment();
                System.out.println(Arrays.toString(usedPatterns));
                model.dispose();
                return usedPatterns;
            } catch (GRBException e) {
                throw new RuntimeException(String.valueOf(e.getErrorCode()), e);
            }
        }

        private void defineVariables() throws GRBException {
            //todo patterns ohne decomposition ausschließen
            for (int i = 0; i < patterns.length; i++) {
                variables[i] = model.addVar(0.0, 1.0, -patterns[i].getScore(), GRB.BINARY, null); //todo richtig so?
            }
            //model.set(GRB.IntAttr.ModelSense, 1);

//            for (int i = 0; i < peaks.length; i++) {
//                variables[i+patterns.length] = model.addVar(1.0 , 1.0, -1.0, GRB.BINARY, null);
//
//            }
            model.update();
        }

        private void setConstraints() throws GRBException {
//            for (int i = 0; i < patterns.length; i++) {
//                for (int j = i+1; j < patterns.length; j++) {
//                    if (patterns[i].overlap(patterns[j])){
//                        //if patterns overlap at most one can be chosen
//                        final GRBLinExpr expression = new GRBLinExpr();
//                        expression.addTerm(1d, variables[i]);
//                        expression.addTerm(1d, variables[j]);
//                        model.addConstr(expression, GRB.LESS_EQUAL, 1, null);
//                    } else {
//                        System.out.println("no overlap");
//                    }
//                }
//            }

            //pick all patterns which contain specific peak: add constraint: use exactly 1
            int mostLeftPatternPos = 0;
            for (int i = 0; i < peaks.length; i++) {
                ProcessedPeak peak = peaks[i];
                final GRBLinExpr expression = new GRBLinExpr();
                final double currentMass = peak.getMass();
                for (int j = mostLeftPatternPos; j < patterns.length; j++) {
                    for (ProcessedPeak patternPeak : patterns[j].getPeaks()) {
                        if (currentMass==patternPeak.getMass()){
                            expression.addTerm(1d, variables[j]);
                            break;
                        }
                    }
                }
                //todo optimize
                model.addConstr(expression, GRB.EQUAL, 1, null);

            }
        }


        private boolean[] getVariableAssignment() throws GRBException {
            final double[] usedPatterns = model.get(GRB.DoubleAttr.X, variables);
            System.out.println(Arrays.toString(usedPatterns));
            final boolean[] assignments = new boolean[variables.length];
            //final double tolerance = model.get(GRB.DoubleAttr.IntVio);
            for (int i=0; i < assignments.length; ++i) {
                assert usedPatterns[i] > -0.5 : "LP_LOWERBOUND violation for var " + i + " with value " + usedPatterns[i];
                assert usedPatterns[i] < 1.5 : "LP_LOWERBOUND violation for var " + i + " with value " + usedPatterns[i];;
                assignments[i] = (Math.round(usedPatterns[i]) == 1);
            }
            return assignments;
        }

        private Pattern[] getPatterns() {
            return patterns;
        }

        //todo usefull?
//        public void setNumberOfCPUs(int numberOfCPUs) {
//            if (numberOfCPUs != this.numberOfCPUs) {
//                try {
//                    env.set(GRB.IntParam.Threads, numberOfCPUs);
//                } catch (GRBException e) {
//                    throw new RuntimeException(e);
//                }
//            }
//            this.numberOfCPUs = numberOfCPUs;
//
//        }




    }
}
