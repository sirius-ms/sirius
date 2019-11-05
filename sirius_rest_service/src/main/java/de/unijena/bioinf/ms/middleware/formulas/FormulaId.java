package de.unijena.bioinf.ms.middleware.formulas;

import de.unijena.bioinf.projectspace.FormulaResultId;

/**
 * The CompoundId contains the ID of a compound together with some read-only information that might be displayed in
 * some summary view.
 */
public class FormulaId {

    // identifier
    protected String id;

    // identifier source
    protected String molecularFormula;
    protected String ionType;

    // optional detail
    protected FormulaResultScores resultScores;

    public FormulaId(FormulaResultId id) {
        this(id.fileName(), id.getMolecularFormula().toString(), id.getIonType().toString());
    }

    public FormulaId(String id, String molecularFormula, String ionType) {
        this.id = id;
        this.molecularFormula = molecularFormula;
        this.ionType = ionType;
    }

    public String getId() {
        return id;
    }

    public String getIonType() {
        return ionType;
    }

    public String getMolecularFormula() {
        return molecularFormula;
    }

    public FormulaResultScores getResultScores() {
        return resultScores;
    }

    public void setResultScores(FormulaResultScores availableResults) {
        this.resultScores = availableResults;
    }
}

