package de.unijena.bioinf.projectspace.sirius;

import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.babelms.json.FTJsonReader;
import de.unijena.bioinf.babelms.json.FTJsonWriter;
import de.unijena.bioinf.babelms.ms.AnnotatedSpectrumWriter;
import de.unijena.bioinf.projectspace.ComponentSerializer;
import de.unijena.bioinf.projectspace.FormulaResultId;
import de.unijena.bioinf.projectspace.ProjectReader;
import de.unijena.bioinf.projectspace.ProjectWriter;

import java.io.IOException;
import java.util.Optional;

import static de.unijena.bioinf.projectspace.sirius.SiriusLocations.SPECTRA;
import static de.unijena.bioinf.projectspace.sirius.SiriusLocations.TREES;

public class TreeSerializer implements ComponentSerializer<FormulaResultId, FormulaResult, FTree> {
    @Override
    public FTree read(ProjectReader reader, FormulaResultId id, FormulaResult container) throws IOException {
        return reader.inDirectory(TREES.relDir(), () -> {
            final String relativePath = TREES.fileName(id);
            return reader.textFile(relativePath, (r) -> new FTJsonReader().parse(r, reader.asURL(relativePath)));
        });

        //NOTE: we do not need to read annotated spectra because the information is already in the trees.
        // The annotated spectra are just for the users spectra
    }

    @Override
    public void write(ProjectWriter writer, FormulaResultId id, FormulaResult container, Optional<FTree> optTree) throws IOException {
        final FTree tree = optTree.orElseThrow(() -> new RuntimeException("Could not find tree for FormulaResult with ID: " + id));
        // write tree json
        writer.inDirectory(TREES.relDir(), () -> {
            writer.textFile(TREES.fileName(id), (w) -> new FTJsonWriter().writeTree(w, tree));
            return true;
        });

        //write annotated spectra
        writer.inDirectory(SPECTRA.relDir(), () -> {
            writer.textFile(SPECTRA.fileName(id), (w) -> new AnnotatedSpectrumWriter().write(w, tree));
            return true;
        });
    }

    @Override
    public void delete(ProjectWriter writer, FormulaResultId id) throws IOException {
        //delete trees
        writer.inDirectory(TREES.relDir(), () -> {
            writer.deleteIfExists(TREES.fileName(id));
            return true;
        });

        //delete annotated spectra
        writer.inDirectory(SPECTRA.relDir(), () -> {
            writer.deleteIfExists(SPECTRA.fileName(id));
            return true;
        });
    }
}
