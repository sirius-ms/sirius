/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.middleware.compounds;

import de.unijena.bioinf.ChemistryBase.ms.Deviation;

public class FormulaResultSummary {

    protected Double siriusScore;
    protected Double isotopeScore;
    protected Double treeScore;
    protected Double zodiacScore;

    protected String molecularFormula;
    protected String adduct;

    protected Integer numOfexplainedPeaks;
    protected Integer numOfexplainablePeaks;

    protected Double totalExplainedIntensity;
    protected Deviation medianMassDeviation;

    public Double getSiriusScore() {
        return siriusScore;
    }

    public void setSiriusScore(Double siriusScore) {
        this.siriusScore = siriusScore;
    }

    public Double getIsotopeScore() {
        return isotopeScore;
    }

    public void setIsotopeScore(Double isotopeScore) {
        this.isotopeScore = isotopeScore;
    }

    public Double getTreeScore() {
        return treeScore;
    }

    public void setTreeScore(Double treeScore) {
        this.treeScore = treeScore;
    }

    public Double getZodiacScore() {
        return zodiacScore;
    }

    public void setZodiacScore(Double zodiacScore) {
        this.zodiacScore = zodiacScore;
    }

    public String getMolecularFormula() {
        return molecularFormula;
    }

    public void setMolecularFormula(String molecularFormula) {
        this.molecularFormula = molecularFormula;
    }

    public String getAdduct() {
        return adduct;
    }

    public void setAdduct(String adduct) {
        this.adduct = adduct;
    }

    public Integer getNumOfexplainedPeaks() {
        return numOfexplainedPeaks;
    }

    public void setNumOfexplainedPeaks(Integer numOfexplainedPeaks) {
        this.numOfexplainedPeaks = numOfexplainedPeaks;
    }

    public Integer getNumOfexplainablePeaks() {
        return numOfexplainablePeaks;
    }

    public void setNumOfexplainablePeaks(Integer numOfexplainablePeaks) {
        this.numOfexplainablePeaks = numOfexplainablePeaks;
    }

    public Double getTotalExplainedIntensity() {
        return totalExplainedIntensity;
    }

    public void setTotalExplainedIntensity(Double totalExplainedIntensity) {
        this.totalExplainedIntensity = totalExplainedIntensity;
    }

    public Deviation getMedianMassDeviation() {
        return medianMassDeviation;
    }

    public void setMedianMassDeviation(Deviation medianMassDeviation) {
        this.medianMassDeviation = medianMassDeviation;
    }
}
