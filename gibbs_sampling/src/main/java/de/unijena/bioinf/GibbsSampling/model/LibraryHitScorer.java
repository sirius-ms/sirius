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

package de.unijena.bioinf.GibbsSampling.model;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;

import java.util.Set;
import java.util.StringJoiner;

public class LibraryHitScorer implements NodeScorer<FragmentsCandidate> {
    private final double lambda;
    private final double lowestCosine;
    private final Set<MolecularFormula> expectedMFDifferences;
    private final MolecularFormula EmptyMF = MolecularFormula.emptyFormula();
//    private double maxLogScore; //to normalize max to probability 1.0

    private static final boolean DEBUG = false;

    public LibraryHitScorer(double lambda, double lowestCosine, Set<MolecularFormula> expectedMFDifferences) {
        this.lambda = lambda;
        this.lowestCosine = lowestCosine;
        this.expectedMFDifferences = expectedMFDifferences;
//        maxLogScore = logScore(1.0); //todo always 0!?
    }

    public void score(FragmentsCandidate[] candidates) {
        StringJoiner debugOutJoiner, debugCorrectJoiner, debugIncorrectJoiner;
        if (DEBUG){
            debugOutJoiner = new StringJoiner(", ");
            debugCorrectJoiner = new StringJoiner(", ");
            debugIncorrectJoiner = new StringJoiner(", ");
            if (candidates.length>0) {
                debugOutJoiner.add(candidates[0].experiment.getName());
                if (candidates[0].hasLibraryHit()){
                    if (!candidates[0].experiment.getName().equals(candidates[0].getLibraryHit().getQueryExperiment().getName())) throw new RuntimeException("ids differ");
                    debugOutJoiner.add("cosine: "+candidates[0].getLibraryHit().getCosine());
                }
            }

        }
        for(int j = 0; j < candidates.length; ++j) {
            FragmentsCandidate candidate = candidates[j];
            if(candidate.inEvaluationSet) {
                if (DEBUG) System.out.println("in evaluation set");
//                candidate.addNodeProbabilityScore(1d); //probabilities should not be influenced at all
            } else if(candidate.hasLibraryHit()) {
                LibraryHit libraryHit = candidate.getLibraryHit();
                double cosine = libraryHit.getCosine();
                if (cosine>1.01) throw new RuntimeException(String.format("Cosine score for %s is greater than 1: %f", libraryHit.getQueryExperiment().getName(), cosine));
                if (cosine>1.0) cosine = 1.0;

                MolecularFormula diff = libraryHit.getMolecularFormula().subtract(candidate.getFormula());
                if(diff.equals(this.EmptyMF)) {
                    double libMz = libraryHit.getPrecursorMz();
                    if (Math.abs(libraryHit.getQueryExperiment().getIonMass()-libMz)<=0.1){
                        //this is without biotransformation
                        if (DEBUG) {
                            debugCorrectJoiner.add("(w/o biotransf) "+candidate.getFormula()+": "+normalizedLogScore(cosine));

                        }
                        candidate.addNodeLogProbabilityScore(normalizedLogScore(cosine));
                    } else {
                        //this is with biotransformation but the MF of the compound (which is different to the library MF) was already suggested
                        if (DEBUG) {
                            debugCorrectJoiner.add("(w biotransf) "+candidate.getFormula()+": "+normalizedLogScore(cosine - 0.1D));

                        }
                        candidate.addNodeLogProbabilityScore(normalizedLogScore(cosine - 0.1D));
                    }

                } else {
                    if(diff.getMass() < 0.0D) {
                        diff = diff.negate();
                    }

                    if(this.expectedMFDifferences.contains(diff)) {
                        if (DEBUG){
                            debugCorrectJoiner.add("(w biotransf) "+candidate.getFormula()+": "+normalizedLogScore(cosine - 0.1D));

                        }
                        candidate.addNodeLogProbabilityScore(normalizedLogScore(cosine - 0.1D));

                    } else {
                        if (DEBUG){
                            debugIncorrectJoiner.add(candidate.getFormula()+": "+normalizedLogScore(0));

                        }
                        candidate.addNodeLogProbabilityScore(normalizedLogScore(0.0D));

                    }
                }
            } else {
//                candidate.addNodeProbabilityScore(1d); //probabilities should not be influenced at all
            }
        }
        if (DEBUG) System.out.println(debugOutJoiner.toString()+" | "+debugCorrectJoiner.toString()+" | "+debugIncorrectJoiner.toString());

    }


    private double normalizedLogScore(double cosine) {
        return logScore(cosine);
    }


    private double logScore(double cosine) {
        double transformedCos = Math.max((cosine - lowestCosine) / (1d - lowestCosine), 0);
        return lambda * transformedCos / (1.0D - transformedCos);
    }

}
