package de.unijena.bioinf.projectspace.sirius;

import de.unijena.bioinf.ms.annotations.DataAnnotation;
import de.unijena.bioinf.projectspace.FormulaResultId;
import de.unijena.bioinf.projectspace.ProjectSpaceContainer;

public final class FormulaResult extends ProjectSpaceContainer<FormulaResultId> {

    private final Annotations<DataAnnotation> annotations;
    private final FormulaResultId formulaResultId;

    public FormulaResult(FormulaResultId id) {
        this.annotations = new Annotations<>();
        this.formulaResultId = id;
    }

    @Override
    public Annotations<DataAnnotation> annotations() {
        return annotations;
    }

    @Override
    public FormulaResultId getId() {
        return formulaResultId;
    }
}
