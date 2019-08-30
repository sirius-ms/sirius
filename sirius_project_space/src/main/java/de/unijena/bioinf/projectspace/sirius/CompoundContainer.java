package de.unijena.bioinf.projectspace.sirius;

import de.unijena.bioinf.ms.annotations.Annotated;
import de.unijena.bioinf.ms.annotations.DataAnnotation;
import de.unijena.bioinf.projectspace.CompoundContainerId;
import de.unijena.bioinf.projectspace.FormulaResultId;
import de.unijena.bioinf.projectspace.FormulaScore;
import de.unijena.bioinf.projectspace.ProjectSpaceContainer;

import java.util.ArrayList;
import java.util.List;

public class CompoundContainer extends ProjectSpaceContainer<CompoundContainerId> implements Annotated<DataAnnotation> {

    private final Annotations<DataAnnotation> annotations;

    private final List<FormulaResultId> results;
    private final CompoundContainerId id;

    // necessary information
    private Class<? extends FormulaScore> rankingScore;

    public CompoundContainer(CompoundContainerId id, Class<? extends FormulaScore> resultScore) {
        this.annotations = new Annotations<>();
        this.results = new ArrayList<>();
        this.id = id;
        this.rankingScore = resultScore;
    }

    public List<FormulaResultId> getResults() {
        return results;
    }

    public Class<? extends FormulaScore> getRankingScore() {
        return rankingScore;
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
