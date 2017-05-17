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

package de.unijena.bioinf.sirius.gui.fingerid;

import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.chemdb.DatasourceService;
import de.unijena.bioinf.sirius.gui.db.SearchableDatabase;
import org.jdesktop.beans.AbstractBean;

import java.util.EnumSet;

public class FingerIdData extends AbstractBean {

    protected final ProbabilityFingerprint platts;
    protected final Compound[] compounds;
    protected final double[] scores;
    protected final double[] tanimotoScores;
    private double confidence;
    private double topScore;
    public final SearchableDatabase db;
    protected double minLogPFilter = Double.NEGATIVE_INFINITY, maxLogPFilter = Double.POSITIVE_INFINITY;
    public EnumSet<DatasourceService.Sources> dbSelection;

    public FingerIdData(SearchableDatabase db, Compound[] compounds, double[] scores, double[] tanimotoScores, ProbabilityFingerprint platts) {
        this.db = db;
        this.compounds = compounds;
        this.scores = scores;
        this.tanimotoScores = tanimotoScores;
        this.platts = platts;
        this.topScore = scores.length == 0 ? Double.NEGATIVE_INFINITY : scores[0];
        this.confidence = Double.NaN;
        this.dbSelection = EnumSet.of(DatasourceService.Sources.PUBCHEM);
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        double old = this.confidence;
        this.confidence = confidence;
        firePropertyChange("confidence",old,this.confidence);
    }

    public double getTopScore() {
        return topScore;
    }

    public void setTopScore(double topScore) {
        double old = this.topScore;
        this.topScore = topScore;
        firePropertyChange("topScore",old,this.topScore);
    }

    public double getMinLogPFilter() {
        return minLogPFilter;
    }

    public void setMinLogPFilter(double minLogPFilter) {
        double old = this.minLogPFilter;
        this.minLogPFilter = minLogPFilter;
        firePropertyChange("minLogPFilter",old,this.minLogPFilter);
    }

    public double getMaxLogPFilter() {
        return maxLogPFilter;
    }

    public void setMaxLogPFilter(double maxLogPFilter) {
        double old = this.maxLogPFilter;
        this.maxLogPFilter = maxLogPFilter;
        firePropertyChange("maxLogPFilter",old,this.maxLogPFilter);
    }
}
