package de.unijena.bioinf.ms.middleware.compounds;
/**
 * Summary of the results of a Compound. Can be added to a CompoundId.
 * It is not null within a CompoundId if it was not requested und non null otherwise
 * The different summary fields within this summary are null if the corresponding
 * compound does not contain the represented results. The content of  non NULL
 * summary field id the result was computed but is empty.
 * */
public class CompoundSummary {
    //result previews
    protected FormulaResultSummary formulaResultSummary = null; // SIRIUS + ZODIAC
    protected StructureResultSummary structureResultSummary = null; // CSI:FingerID
    protected CategoryResultSummary categoryResultSummary = null; // CANOPUS


    public FormulaResultSummary getFormulaResultSummary() {
        return formulaResultSummary;
    }

    public void setFormulaResultSummary(FormulaResultSummary formulaResultSummary) {
        this.formulaResultSummary = formulaResultSummary;
    }

    public StructureResultSummary getStructureResultSummary() {
        return structureResultSummary;
    }

    public void setStructureResultSummary(StructureResultSummary structureResultSummary) {
        this.structureResultSummary = structureResultSummary;
    }

    public CategoryResultSummary getCategoryResultSummary() {
        return categoryResultSummary;
    }

    public void setCategoryResultSummary(CategoryResultSummary categoryResultSummary) {
        this.categoryResultSummary = categoryResultSummary;
    }
}
