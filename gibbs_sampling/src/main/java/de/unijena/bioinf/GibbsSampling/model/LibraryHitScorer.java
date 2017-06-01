package de.unijena.bioinf.GibbsSampling.model;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;

import java.util.Set;

public class LibraryHitScorer implements NodeScorer<FragmentsCandidate> {
    private final double lambda;
    private final double lowestCosine;
//    private final double lowestScore = 0.5D;
//    private final double expLowestCosine;
//    private final double normalization;
    private final Set<MolecularFormula> expectedMFDifferences;
    private final MolecularFormula EmptyMF = MolecularFormula.emptyFormula();

    public LibraryHitScorer(double lambda, double lowestCosine, Set<MolecularFormula> expectedMFDifferences) {
        this.lambda = lambda;
        this.lowestCosine = lowestCosine;
//        this.expLowestCosine = Math.exp(-1.0D);
//        this.normalization = topScore / (Math.exp(0.0D) - this.expLowestCosine);
        this.expectedMFDifferences = expectedMFDifferences;
    }

    public void score(FragmentsCandidate[][] candidates) {
        for(int i = 0; i < candidates.length; ++i) {
            FragmentsCandidate[] mfCandidates = candidates[i];

            for(int j = 0; j < mfCandidates.length; ++j) {
                FragmentsCandidate candidate = mfCandidates[j];
                if(candidate.inEvaluationSet) {
                    System.out.println("in Evaluation set: " + candidate.experiment.getName() + "\t" + candidate.getFormula());
                    candidate.addNodeProbabilityScore(this.score(0.0D));
                } else if(candidate.hasLibraryHit()) {
                    LibraryHit libraryHit = candidate.getLibraryHit();
                    double cosine = libraryHit.getCosine();
                    MolecularFormula diff = libraryHit.getMolecularFormula().subtract(candidate.getFormula());
                    if(diff.equals(this.EmptyMF)) {
                        candidate.addNodeProbabilityScore(this.score(cosine));
//                        System.out.println("library match " + candidate.experiment.getName() + "\t" + candidate.getFormula() + " vs " + libraryHit.getMolecularFormula());
                    } else {
                        if(diff.getMass() < 0.0D) {
                            diff = diff.negate();
                        }

                        if(this.expectedMFDifferences.contains(diff)) {
//                            System.out.println("library diff " + candidate.experiment.getName() + "\t" + candidate.getFormula() + " vs " + libraryHit.getMolecularFormula());
                            candidate.addNodeProbabilityScore(this.score(cosine - 0.05D));
                        } else {
                            candidate.addNodeProbabilityScore(this.score(0.0D));
                        }
                    }
                } else {
                    candidate.addNodeProbabilityScore(this.score(0.0D));
                }
            }
        }

    }

    private double score(double cosine) {
//        return Math.max(0.0D, this.normalization * (Math.exp(Math.max(cosine, lowestCosine) - 1.0D) - this.expLowestCosine));
        return Math.exp(lambda * Math.max(cosine, lowestCosine) - 1.0D);
    }
}
