package de.unijena.bioinf.projectspace.sirius;

import de.unijena.bioinf.ms.annotations.DataAnnotation;
import de.unijena.bioinf.projectspace.CompoundContainerId;
import de.unijena.bioinf.projectspace.FormulaResultId;
import de.unijena.bioinf.projectspace.ProjectSpaceContainer;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class CompoundContainer extends ProjectSpaceContainer<CompoundContainerId> {

    private final Annotations<DataAnnotation> annotations;

    protected final Map<String, FormulaResultId> results;
    private final CompoundContainerId id;

    public CompoundContainer(CompoundContainerId id/*, Class<? extends FormulaScore> resultScore*/) {
        this.annotations = new Annotations<>();
        this.results = new ConcurrentHashMap<>();
        this.id = id;
    }

    public Map<String, FormulaResultId> getResults() {
        return results;
    }

    public boolean containsResult(FormulaResultId id) {
        return id != null && id == (results.get(id.fileName()));
    }

    public Optional<FormulaResultId> findResult(String id) {
        return Optional.ofNullable(results.get(id));
    }

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
