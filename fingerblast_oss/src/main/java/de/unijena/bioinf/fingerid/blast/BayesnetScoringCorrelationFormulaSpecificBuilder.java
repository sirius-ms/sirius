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
