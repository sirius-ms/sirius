package de.unijena.bioinf.fingerid.blast;

import de.unijena.bioinf.ChemistryBase.fp.Fingerprint;
import de.unijena.bioinf.ChemistryBase.fp.FingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.PredictionPerformance;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.io.IOException;
import java.nio.file.Path;

import static de.unijena.bioinf.fingerid.blast.BayesnetScoring.AbstractCorrelationTreeNode;

public class BayesnetScoringCorrelationFormulaSpecificBuilder extends BayesnetScoringFormulaSpecificBuilder {

    public static BayesnetScoring createScoringMethod(PredictionPerformance[] performances, ProbabilityFingerprint[] predictedSpecific, Fingerprint[] correctSpecific, ProbabilityFingerprint[] predictedAll, Fingerprint[] correctAll, Path dotFilePath) throws IOException {
        int[][] covTreeEdges = parseTreeFromDotFile(dotFilePath);
        return new BayesnetScoringCorrelationFormulaSpecificBuilder(performances, predictedSpecific, correctSpecific, predictedAll, correctAll, covTreeEdges, false, 100).buildScoring();
    }

    public static BayesnetScoring createScoringMethod(PredictionPerformance[] performances, ProbabilityFingerprint[] predictedSpecific, Fingerprint[] correctSpecific, ProbabilityFingerprint[] predictedAll, Fingerprint[] correctAll, Path dotFilePath, double generalDataWeight) throws IOException {
        int[][] covTreeEdges = parseTreeFromDotFile(dotFilePath);
        return new BayesnetScoringCorrelationFormulaSpecificBuilder(performances, predictedSpecific, correctSpecific, predictedAll, correctAll, covTreeEdges, false, generalDataWeight).buildScoring();
    }

    public static BayesnetScoring createScoringMethod(PredictionPerformance[] performances, ProbabilityFingerprint[] predictedSpecific, Fingerprint[] correctSpecific, ProbabilityFingerprint[] predictedAll, Fingerprint[] correctAll, int[][] covTreeEdges, boolean allowOnlyNegativeScores, double generalDataWeight) {
        return new BayesnetScoringCorrelationFormulaSpecificBuilder(performances, predictedSpecific, correctSpecific, predictedAll, correctAll, covTreeEdges, false, generalDataWeight).buildScoring();
    }


    public BayesnetScoringCorrelationFormulaSpecificBuilder(PredictionPerformance[] performances, ProbabilityFingerprint[] predictedSpecific, Fingerprint[] correctSpecific, ProbabilityFingerprint[] predictedAll, Fingerprint[] correctAll, int[][] covTreeEdges, boolean allowOnlyNegativeScores, double generalDataWeight) {
        super(performances, predictedSpecific, correctSpecific, predictedAll, correctAll, covTreeEdges, allowOnlyNegativeScores, generalDataWeight);

    }


    @Override
    protected AbstractCorrelationTreeNode createTreeNode(int fingerprintIndex, AbstractCorrelationTreeNode... parentNodes){
        if (parentNodes.length==0){
            return new BayesnetScoringCorrelation.CorrelationTreeNodeCorrelation(fingerprintIndex);
        } else if (parentNodes.length==1){
            return new BayesnetScoringCorrelation.CorrelationTreeNodeCorrelation(fingerprintIndex, parentNodes[0]);
        } else {
            throw new RuntimeException("don't support nodes with no or more than 1 parents");
        }
    }


    @Override
    protected BayesnetScoring getNewInstance(TIntObjectHashMap<AbstractCorrelationTreeNode> nodes, AbstractCorrelationTreeNode[] nodeList, AbstractCorrelationTreeNode[] forests, double alpha, FingerprintVersion fpVersion, PredictionPerformance[] performances, boolean allowOnlyNegativeScores){
        return new BayesnetScoringCorrelation(nodes, nodeList, forests, alpha, fpVersion, performances, allowOnlyNegativeScores);
    }

}
