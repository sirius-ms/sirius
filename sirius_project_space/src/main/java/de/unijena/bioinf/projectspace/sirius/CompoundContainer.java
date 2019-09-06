package de.unijena.bioinf.projectspace.sirius;

import de.unijena.bioinf.ms.annotations.DataAnnotation;
import de.unijena.bioinf.projectspace.CompoundContainerId;
import de.unijena.bioinf.projectspace.FormulaResultId;
import de.unijena.bioinf.projectspace.ProjectSpaceContainer;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class CompoundContainer extends ProjectSpaceContainer<CompoundContainerId> {

    private final Annotations<DataAnnotation> annotations;

    protected final Set<FormulaResultId> results;
    private final CompoundContainerId id;

    // necessary information
//    private Class<? extends FormulaScore> rankingScore;

    public CompoundContainer(CompoundContainerId id/*, Class<? extends FormulaScore> resultScore*/) {
        this.annotations = new Annotations<>();
        this.results = Collections.newSetFromMap(new ConcurrentHashMap<>());
        this.id = id;
//        this.rankingScore = resultScore;
    }

    public Set<FormulaResultId> getResults() {
        return results;
    }

//    public Class<? extends FormulaScore> getRankingScore() {
//        return rankingScore;
//    }

    @Override
    public CompoundContainerId getId() {
        return id;
    }

    @Override
    public Annotations<DataAnnotation> annotations() {
        return annotations;
    }

    public boolean contains(FormulaResultId fid) {
        if (!fid.getParentId().getDirectoryName().equals(getId().getDirectoryName()))
            return false;
        return false;
    }
}
