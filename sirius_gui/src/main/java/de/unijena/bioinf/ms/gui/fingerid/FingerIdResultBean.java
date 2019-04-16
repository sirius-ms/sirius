/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2015 Kai Dührkop
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.unijena.bioinf.ms.gui.fingerid;

import de.unijena.bioinf.ChemistryBase.algorithm.Scored;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.ChemistryBase.fp.Tanimoto;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.fingerid.CanopusResult;
import de.unijena.bioinf.fingerid.FingerIdResult;
import de.unijena.bioinf.ms.frontend.core.AbstractEDTBean;

/**
 * This is the wrapper for the FingerIdResult class to interact with the gui
 * elements. It uses a special property change support that executes the events
 * in the EDT. So you can change all fields from any thread, the gui will still
 * be updated in the EDT. Some operations may NOT be Thread save, so you may have
 * to care about Synchronization.
 */
public class FingerIdResultBean extends AbstractEDTBean {

    protected final FingerIdResult result;

    protected final double[] tanimotoScores;
    private double topScore;


    public FingerIdResultBean(FingerIdResult result) {
        this.result = result;
        this.topScore = result.getCandidates().get(0).getScore();
        this.tanimotoScores = result.getCandidates().stream().mapToDouble(c ->
                Tanimoto.probabilisticTanimoto(result.getPredictedFingerprint(), c.getCandidate().getFingerprint()).expectationValue()).toArray();
    }

    public ProbabilityFingerprint getCanopusFingerprint() {
        if (!result.hasAnnotation(CanopusResult.class))
            return null;
        return result.getAnnotation(CanopusResult.class).getCanopusFingerprint();
    }

    public ProbabilityFingerprint getFingerIdFingerprint() {
        return result.getPredictedFingerprint();
    }

    public double getConfidence() {
        return result.getConfidence();
    }

    public void setConfidence(double confidence) {
        double old = getConfidence();
        result.setConfidence(confidence);
        firePropertyChange("confidence", old, getConfidence());
    }

    public double getTopScore() {
        return topScore;
    }

    public void setTopScore(double topScore) {
        double old = this.topScore;
        this.topScore = topScore;
        firePropertyChange("topScore", old, this.topScore);
    }

    public FingerprintCandidate[] getCompounds() {
        return result.getCandidates().stream().map(Scored::getCandidate).toArray(FingerprintCandidate[]::new);
    }

    public double[] getTanimotoScores() {
        return tanimotoScores;
    }

    public double getTanimoto(int index) {
        return tanimotoScores[index];
    }

    public double getScore(int index) {
        return result.getCandidates().get(index).getScore();
    }

    public FingerprintCandidate getCandidate(int index) {
        return result.getCandidates().get(index).getCandidate();
    }

    public FingerIdResult getFingerIdResult() {
        return result;
    }
}
