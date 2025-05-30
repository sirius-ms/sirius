package de.unijena.bioinf.sirius.elementdetection;

import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import de.unijena.bioinf.ChemistryBase.chem.FormulaFilter;
import lombok.Getter;

import java.util.List;

public class DetectedFormulaConstraints extends FormulaConstraints {
    @Getter
    /**
     * True if element detection has been performed. False if no data (MS1 isotope pattern) was available for detection.
     */
    final public boolean detectionPerformed;


    public DetectedFormulaConstraints(FormulaConstraints constraints, boolean detectionPerformed) {
        super(constraints);
        this.detectionPerformed = detectionPerformed;
    }

    @Override
    public FormulaConstraints getExtendedConstraints(FormulaConstraints otherConstraints) {
        return new DetectedFormulaConstraints(super.getExtendedConstraints(otherConstraints), detectionPerformed);
    }

    @Override
    public FormulaConstraints getExtendedConstraints(Element... elements) {
        return new DetectedFormulaConstraints(super.getExtendedConstraints(elements), detectionPerformed);
    }

    @Override
    public FormulaConstraints intersection(FormulaConstraints formulaConstraints) {
        return new DetectedFormulaConstraints(super.intersection(formulaConstraints), detectionPerformed);
    }

    @Override
    public FormulaConstraints intersection(Element... elements) {
        return new DetectedFormulaConstraints(super.intersection(elements), detectionPerformed);
    }

    @Override
    public DetectedFormulaConstraints withNewFilters(List<FormulaFilter> formulaFilters){
        final DetectedFormulaConstraints fc = this.clone();
        fc.filters.clear();
        fc.filters.addAll(formulaFilters);
        return fc;
    }

    @Override
    public DetectedFormulaConstraints clone() {
        return new DetectedFormulaConstraints(this, detectionPerformed);
    }

}
