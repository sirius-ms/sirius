package de.unijena.bioinf.projectspace.fingerid;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.unijena.bioinf.fingerid.StructureSearchResult;
import de.unijena.bioinf.projectspace.*;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Optional;

import static de.unijena.bioinf.projectspace.fingerid.FingerIdLocations.SEARCH;


public class StructureSearchResultSerializer implements ComponentSerializer<FormulaResultId, FormulaResult, StructureSearchResult> {

    @Override
    public @Nullable StructureSearchResult read(ProjectReader reader, FormulaResultId id, FormulaResult container) throws IOException {
        final String loc = SEARCH.relFilePath(id);
        if (!reader.exists(loc)) return null;
        ObjectMapper m = new ObjectMapper();
        return reader.textFile(loc, br -> m.readValue(br, StructureSearchResult.class));
    }

    @Override
    public void write(ProjectWriter writer, FormulaResultId id, FormulaResult container, Optional<StructureSearchResult> component) throws IOException {
        final String loc = SEARCH.relFilePath(id);
        if (component.isPresent()){
            ObjectMapper m = new ObjectMapper();
            writer.textFile(loc, bw -> m.writeValue(bw, component.get()));
        }
    }

    @Override
    public void delete(ProjectWriter writer, FormulaResultId id) throws IOException {
        writer.deleteIfExists(SEARCH.relFilePath(id));
    }

    @Override
    public void deleteAll(ProjectWriter writer) throws IOException {
        writer.deleteIfExists(SEARCH.relDir());
    }
}
