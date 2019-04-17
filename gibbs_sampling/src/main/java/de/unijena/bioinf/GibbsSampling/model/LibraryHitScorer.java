package de.unijena.bioinf.GibbsSampling.model;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;

import java.util.HashSet;
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
                if (cosine>1.001) throw new RuntimeException(String.format("Cosine score for %s is greater than 1: %d", libraryHit.getQueryExperiment().getName(), cosine));
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

    public static void main(String... args) {

        double[] lambdas = new double[]{1,10,100};

        for (double lambda : lambdas) {
            System.out.println("lambda "+lambda);

            double lowestCosine = 0.5;
            LibraryHitScorer scorer = new LibraryHitScorer(lambda, lowestCosine, new HashSet<>());

            double[] arr = new double[]{0.0,0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.8,0.9,0.95,0.99, 1.0};


            for (double d : arr) {
//                System.out.println(d+"\t"+scorer.normalizedLogScore(d)+"\t"+Math.exp(scorer.normalizedLogScore(d)));
                System.out.println(d+"\t"+scorer.normalizedLogScore(d)+"\t"+Math.exp(scorer.normalizedLogScore(d))+"\t"+Math.log(scorer.normalizedLogScore(d)));
            }

        }


    }
}
