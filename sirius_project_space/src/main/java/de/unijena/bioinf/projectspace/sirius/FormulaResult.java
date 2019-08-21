package de.unijena.bioinf.projectspace.sirius;

import de.unijena.bioinf.ms.annotations.Annotated;
import de.unijena.bioinf.ms.annotations.DataAnnotation;
import de.unijena.bioinf.projectspace.ProjectReader;
import de.unijena.bioinf.projectspace.ProjectSpaceContainer;
import de.unijena.bioinf.projectspace.ProjectWriter;

import java.util.ArrayList;

public class FormulaResult extends ProjectSpaceContainer<FormulaResultId> implements Annotated<DataAnnotation> {

    private final Annotations<DataAnnotation> annotations;

    public FormulaResult() {
        this.annotations = new Annotations<>();
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
}
