package de.unijena.bioinf.projectspace.sirius;

import de.unijena.bioinf.ms.annotations.Annotated;
import de.unijena.bioinf.ms.annotations.DataAnnotation;
import de.unijena.bioinf.projectspace.CompoundContainerId;
import de.unijena.bioinf.projectspace.FormulaResultId;
import de.unijena.bioinf.projectspace.ProjectSpaceContainer;

import java.util.ArrayList;
import java.util.List;

public class CompoundContainer extends ProjectSpaceContainer<CompoundContainerId> implements Annotated<DataAnnotation> {

    private final Annotations<DataAnnotation> annotations;

    private final List<FormulaResultId> results;
    private final CompoundContainerId id;

    public CompoundContainer(CompoundContainerId id) {
        this.annotations = new Annotations<>();
        this.results = new ArrayList<>();
        this.id = id;
    }

    public List<FormulaResultId> getResults() {
        return results;
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
    public CompoundContainerId getId() {
        return id;
    }

    @Override
    public Annotations<DataAnnotation> annotations() {
        return annotations;
    }
}
