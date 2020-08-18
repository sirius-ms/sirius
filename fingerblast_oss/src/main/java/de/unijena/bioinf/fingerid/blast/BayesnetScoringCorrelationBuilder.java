package de.unijena.bioinf.fingerid.blast;

import de.unijena.bioinf.ChemistryBase.fp.Fingerprint;
import de.unijena.bioinf.ChemistryBase.fp.FingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.PredictionPerformance;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.io.IOException;
import java.nio.file.Path;

public class BayesnetScoringCorrelationBuilder extends BayesnetScoringBuilder {

    public BayesnetScoringCorrelationBuilder(PredictionPerformance[] performances, ProbabilityFingerprint[] predicted, Fingerprint[] correct, int[][] covTreeEdges, boolean allowOnlyNegativeScores) {
        super(performances, predicted, correct, covTreeEdges, allowOnlyNegativeScores);
    }

    public static BayesnetScoring createScoringMethod(PredictionPerformance[] performances, ProbabilityFingerprint[] predicted, Fingerprint[] correct, Path dotFilePath) throws IOException {
        int[][] covTreeEdges = parseTreeFromDotFile(dotFilePath);
        return new BayesnetScoringCorrelationBuilder(performances, predicted, correct, covTreeEdges, false).buildScoring();
    }

    public static BayesnetScoring createScoringMethod(PredictionPerformance[] performances, ProbabilityFingerprint[] predicted, Fingerprint[] correct, int[][] covTreeEdges){
        return new BayesnetScoringCorrelationBuilder(performances, predicted, correct, covTreeEdges, false).buildScoring();
    }

    public static BayesnetScoring createScoringMethod(PredictionPerformance[] performances, ProbabilityFingerprint[] predicted, Fingerprint[] correct, Path dotFilePath, boolean allowOnlyNegativeScores) throws IOException {
        int[][] covTreeEdges = parseTreeFromDotFile(dotFilePath);
        return new BayesnetScoringCorrelationBuilder(performances, predicted, correct, covTreeEdges, allowOnlyNegativeScores).buildScoring();
    }

    public static BayesnetScoring createScoringMethod(PredictionPerformance[] performances, ProbabilityFingerprint[] predicted, Fingerprint[] correct, int[][] covTreeEdges, boolean allowOnlyNegativeScores){
        return new BayesnetScoringCorrelationBuilder(performances, predicted, correct, covTreeEdges, allowOnlyNegativeScores).buildScoring();
    }

    @Override
    protected BayesnetScoring.AbstractCorrelationTreeNode createTreeNode(int fingerprintIndex, BayesnetScoring.AbstractCorrelationTreeNode... parentNodes){
        if (parentNodes.length==0){
            return new BayesnetScoringCorrelation.CorrelationTreeNodeCorrelation(fingerprintIndex);
        } else if (parentNodes.length==1){
            return new BayesnetScoringCorrelation.CorrelationTreeNodeCorrelation(fingerprintIndex, parentNodes[0]);
        } else {
            throw new RuntimeException("don't support nodes with no or more than 1 parents");
        }
    }


    @Override
    protected BayesnetScoring getNewInstance(TIntObjectHashMap<BayesnetScoring.AbstractCorrelationTreeNode> nodes, BayesnetScoring.AbstractCorrelationTreeNode[] nodeList, BayesnetScoring.AbstractCorrelationTreeNode[] forests, double alpha, FingerprintVersion fpVersion, PredictionPerformance[] performances, boolean allowOnlyNegativeScores){
        return new BayesnetScoringCorrelation(nodes, nodeList, forests, alpha, fpVersion, performances, allowOnlyNegativeScores);
    }


}
