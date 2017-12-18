package de.unijena.bioinf.GibbsSampling.model;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;

import java.util.Set;

public class LibraryHitScorer implements NodeScorer<FragmentsCandidate> {
    private final double lambda;
    private final double lowestCosine;
    private final Set<MolecularFormula> expectedMFDifferences;
    private final MolecularFormula EmptyMF = MolecularFormula.emptyFormula();

    public LibraryHitScorer(double lambda, double lowestCosine, Set<MolecularFormula> expectedMFDifferences) {
        this.lambda = lambda;
        this.lowestCosine = lowestCosine;
        this.expectedMFDifferences = expectedMFDifferences;
    }

    public void score(FragmentsCandidate[] candidates) {
        for(int j = 0; j < candidates.length; ++j) {
            FragmentsCandidate candidate = candidates[j];
            if(candidate.inEvaluationSet) {
                candidate.addNodeProbabilityScore(this.score(0.0D));
            } else if(candidate.hasLibraryHit()) {
                LibraryHit libraryHit = candidate.getLibraryHit();
                double cosine = libraryHit.getCosine();
                MolecularFormula diff = libraryHit.getMolecularFormula().subtract(candidate.getFormula());
                if(diff.equals(this.EmptyMF)) {
                    candidate.addNodeProbabilityScore(this.score(cosine));
                } else {
                    if(diff.getMass() < 0.0D) {
                        diff = diff.negate();
                    }

                    if(this.expectedMFDifferences.contains(diff)) {
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

    private double score(double cosine) {
        return Math.exp(lambda * Math.max(cosine, lowestCosine) - 1.0D);
    }
}
