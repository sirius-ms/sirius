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
