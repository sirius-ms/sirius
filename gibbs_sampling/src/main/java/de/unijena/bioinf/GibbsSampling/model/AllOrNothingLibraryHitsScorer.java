package de.unijena.bioinf.GibbsSampling.model;

public class AllOrNothingLibraryHitsScorer implements NodeScorer<FragmentsCandidate>{
    @Override
    public void score(FragmentsCandidate[][] candidates) {
        for(int i = 0; i < candidates.length; ++i) {
            FragmentsCandidate[] mfCandidates = candidates[i];

            for(int j = 0; j < mfCandidates.length; ++j) {
                FragmentsCandidate candidate = mfCandidates[j];
                if(candidate.inEvaluationSet) {
                    System.out.println("in Evaluation set: " + candidate.experiment.getName() + "\t" + candidate.getFormula());
                    candidate.addNodeProbabilityScore(1d);//all get this score
                } else if(candidate.hasLibraryHit()) {
                    if (candidate.isCorrect()){
                        candidate.addNodeProbabilityScore(1d);
                        System.out.println("score correct library hit "+candidate.getExperiment().getName());
                    }
                    else candidate.addNodeProbabilityScore(0d);

                } else {
                    candidate.addNodeProbabilityScore(1d); //nothing changes
                }
            }
        }
    }
}
