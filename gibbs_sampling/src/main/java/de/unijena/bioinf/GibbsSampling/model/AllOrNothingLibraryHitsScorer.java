package de.unijena.bioinf.GibbsSampling.model;

public class AllOrNothingLibraryHitsScorer implements NodeScorer<FragmentsCandidate>{
    @Override
    public void score(FragmentsCandidate[] candidates) {
        for(int j = 0; j < candidates.length; ++j) {
            FragmentsCandidate candidate = candidates[j];
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
