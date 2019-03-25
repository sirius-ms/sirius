package de.unijena.bioinf.GibbsSampling.model;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;

import java.util.Set;

public class LibraryHitScorer implements NodeScorer<FragmentsCandidate> {
    private final double lambda;
    private final double lowestCosine;
    private final Set<MolecularFormula> expectedMFDifferences;
    private final MolecularFormula EmptyMF = MolecularFormula.emptyFormula();
    private double maxLogScore; //to normalize max to probability 1.0

    public LibraryHitScorer(double lambda, double lowestCosine, Set<MolecularFormula> expectedMFDifferences) {
        this.lambda = lambda;
        this.lowestCosine = lowestCosine;
        this.expectedMFDifferences = expectedMFDifferences;
        maxLogScore = logScore(1.0);
    }

    public void score(FragmentsCandidate[] candidates) {
        for(int j = 0; j < candidates.length; ++j) {
            FragmentsCandidate candidate = candidates[j];
            if(candidate.inEvaluationSet) {
//                candidate.addNodeProbabilityScore(1d); //probabilities should not be influenced at all
            } else if(candidate.hasLibraryHit()) {
                LibraryHit libraryHit = candidate.getLibraryHit();
                double cosine = libraryHit.getCosine();
                if (cosine>1.001) throw new RuntimeException(String.format("Cosine score for %s is greater than 1: %d", libraryHit.getQueryExperiment().getName(), cosine));
                if (cosine>1.0) cosine = 1.0;

                MolecularFormula diff = libraryHit.getMolecularFormula().subtract(candidate.getFormula());
                if(diff.equals(this.EmptyMF)) {
                    candidate.addNodeLogProbabilityScore(normalizedLogScore(cosine));
                } else {
                    if(diff.getMass() < 0.0D) {
                        diff = diff.negate();
                    }

                    if(this.expectedMFDifferences.contains(diff)) {
                        candidate.addNodeLogProbabilityScore(normalizedLogScore(cosine - 0.05D));
                    } else {
                        candidate.addNodeLogProbabilityScore(normalizedLogScore(0.0D));
                    }
                }
            } else {
//                candidate.addNodeProbabilityScore(1d); //probabilities should not be influenced at all
            }
        }

    }

    private double normalizedLogScore(double cosine) {
        return logScore(cosine)-maxLogScore;
    }


    private double logScore(double cosine) {
        return lambda * Math.max(cosine, lowestCosine) - 1.0D;
    }
//
//
//    private double normalizedScore(double cosine) {
//        return score(cosine)/maxScore;
//    }
//
//
//    private double score(double cosine) {
//        return Math.exp(lambda * Math.max(cosine, lowestCosine) - 1.0D);
//    }


}
