package de.unijena.bioinf.projectspace.sirius;

import de.unijena.bioinf.ms.annotations.Annotated;
import de.unijena.bioinf.ms.annotations.DataAnnotation;
import de.unijena.bioinf.projectspace.FormulaResultId;
import de.unijena.bioinf.projectspace.ProjectSpaceContainer;

public class FormulaResult extends ProjectSpaceContainer<FormulaResultId> implements Annotated<DataAnnotation> {

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
    protected <T extends DataAnnotation> T get(Class<T> klassname) {
        return getAnnotation(klassname);
    }

    @Override
    protected <T extends DataAnnotation> void set(Class<T> klassname, T value) {
        setAnnotation(klassname,value);
    }

    @Override
    public FormulaResultId getId() {
        return formulaResultId;
    }
}
