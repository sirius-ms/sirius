package de.unijena.bioinf.projectspace.sirius;

import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.babelms.json.FTJsonReader;
import de.unijena.bioinf.babelms.json.FTJsonWriter;
import de.unijena.bioinf.projectspace.ComponentSerializer;
import de.unijena.bioinf.projectspace.FormulaResultId;
import de.unijena.bioinf.projectspace.ProjectReader;
import de.unijena.bioinf.projectspace.ProjectWriter;

import java.io.IOException;

public class TreeSerializer implements ComponentSerializer<FormulaResultId, FormulaResult, FTree> {
    @Override
    public FTree read(ProjectReader reader, FormulaResultId id, FormulaResult container) throws IOException {
        return reader.inDirectory("trees", ()->{
            String relativePath = id.fileName("json");
            return reader.textFile(relativePath,(r)-> new FTJsonReader().parse(r,reader.asURL(relativePath)));
        });
    }

    @Override
    public void write(ProjectWriter writer, FormulaResultId id, FormulaResult container, FTree tree) throws IOException {
        writer.inDirectory("trees", ()->{
            writer.textFile(id.fileName("json"), (w)-> new FTJsonWriter().writeTree(w,tree));
            return true;
        });
    }

    @Override
    public void delete(ProjectWriter writer, FormulaResultId id, FormulaResult container, FTree component) throws IOException {
        writer.inDirectory("trees", ()->{
            writer.delete(id.fileName("json"));
            return true;
        });
    }
}
