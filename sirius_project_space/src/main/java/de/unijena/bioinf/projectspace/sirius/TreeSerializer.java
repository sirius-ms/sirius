package de.unijena.bioinf.projectspace.sirius;

import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.babelms.json.FTJsonReader;
import de.unijena.bioinf.babelms.json.FTJsonWriter;
import de.unijena.bioinf.projectspace.ComponentSerializer;
import de.unijena.bioinf.projectspace.FormulaResultId;
import de.unijena.bioinf.projectspace.ProjectReader;
import de.unijena.bioinf.projectspace.ProjectWriter;

import java.io.IOException;
import java.util.Optional;

import static de.unijena.bioinf.projectspace.sirius.SiriusLocations.TREES;

public class TreeSerializer implements ComponentSerializer<FormulaResultId, FormulaResult, FTree> {
    @Override
    public FTree read(ProjectReader reader, FormulaResultId id, FormulaResult container) throws IOException {
        return reader.inDirectory(TREES.relDir(), () -> {
            final String relativePath = TREES.fileName(id);
            return reader.textFile(relativePath, (r) -> new FTJsonReader().parse(r, reader.asURL(relativePath)));
        });
    }

    @Override
    public void write(ProjectWriter writer, FormulaResultId id, FormulaResult container, Optional<FTree> optTree) throws IOException {
        final FTree tree = optTree.orElseThrow(() -> new RuntimeException("Could not find tree for FormulaResult with ID: " + id));
        writer.inDirectory(TREES.relDir(), () -> {
            writer.textFile(TREES.fileName(id), (w) -> new FTJsonWriter().writeTree(w, tree));
            return true;
        });
    }

    @Override
    public void delete(ProjectWriter writer, FormulaResultId id) throws IOException {
        writer.inDirectory(TREES.relDir(), () -> {
            writer.delete(TREES.fileName(id));
            return true;
        });
    }
}
