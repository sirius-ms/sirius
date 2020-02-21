package de.unijena.bioinf.ms.middleware.compounds;

public class StructureResultSummary {

    protected String structureName;
    protected String smiles;

    protected Double csiScore;
    protected Double similarity;
    protected Double confidenceScore;

    protected Integer numOfPubMedIds;
    protected Double xlogP;
    protected String inchiKey;

    public String getStructureName() {
        return structureName;
    }

    public void setStructureName(String structureName) {
        this.structureName = structureName;
    }

    public String getSmiles() {
        return smiles;
    }

    public void setSmiles(String smiles) {
        this.smiles = smiles;
    }

    public Double getCsiScore() {
        return csiScore;
    }

    public void setCsiScore(Double csiScore) {
        this.csiScore = csiScore;
    }

    public Double getSimilarity() {
        return similarity;
    }

    public void setSimilarity(Double similarity) {
        this.similarity = similarity;
    }

    public Double getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(Double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public Integer getNumOfPubMedIds() {
        return numOfPubMedIds;
    }

    public void setNumOfPubMedIds(Integer numOfPubMedIds) {
        this.numOfPubMedIds = numOfPubMedIds;
    }

    public Double getXlogP() {
        return xlogP;
    }

    public void setXlogP(Double xlogP) {
        this.xlogP = xlogP;
    }

    public String getInchiKey() {
        return inchiKey;
    }

    public void setInchiKey(String inchiKey) {
        this.inchiKey = inchiKey;
    }
}
