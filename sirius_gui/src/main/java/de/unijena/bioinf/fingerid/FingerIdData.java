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

package de.unijena.bioinf.fingerid;

import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.fingerid.db.SearchableDatabase;
import de.unijena.bioinf.sirius.gui.structure.AbstractEDTBean;

public class FingerIdData extends AbstractEDTBean {

    protected final ProbabilityFingerprint platts;

    protected ProbabilityFingerprint canopusFingerprint;

    protected final Compound[] compounds;
    protected final double[] scores;
    protected final double[] tanimotoScores;
    public final SearchableDatabase db;

    //todo is the confidence stuff still needed or dead?
    private double confidence;
    private double topScore;


    public FingerIdData(SearchableDatabase db, Compound[] compounds, double[] scores, double[] tanimotoScores, ProbabilityFingerprint platts) {
        this.db = db;
        this.compounds = compounds;
        this.scores = scores;
        this.tanimotoScores = tanimotoScores;
        this.platts = platts;
        this.topScore = scores.length == 0 ? Double.NEGATIVE_INFINITY : scores[0];
        this.confidence = Double.NaN;
    }

    public ProbabilityFingerprint getCanopusFingerprint() {
        return canopusFingerprint;
    }

    public void setCanopusFingerprint(ProbabilityFingerprint canopusFingerprint) {
        this.canopusFingerprint = canopusFingerprint;
    }

    public ProbabilityFingerprint getPlatts() {
        return platts;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        double old = this.confidence;
        this.confidence = confidence;
        firePropertyChange("confidence", old, this.confidence);
    }

    public double getTopScore() {
        return topScore;
    }

    public void setTopScore(double topScore) {
        double old = this.topScore;
        this.topScore = topScore;
        firePropertyChange("topScore", old, this.topScore);
    }

    public Compound[] getCompounds() {
        return compounds;
    }

    public double[] getScores() {
        return scores;
    }
}
