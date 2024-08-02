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
