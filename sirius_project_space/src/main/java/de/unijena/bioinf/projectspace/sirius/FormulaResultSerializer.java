package de.unijena.bioinf.projectspace.sirius;

import de.unijena.bioinf.projectspace.ContainerSerializer;
import de.unijena.bioinf.projectspace.FormulaResultId;
import de.unijena.bioinf.projectspace.ProjectReader;
import de.unijena.bioinf.projectspace.ProjectWriter;

import java.io.IOException;

public class FormulaResultSerializer implements ContainerSerializer<FormulaResultId, FormulaResult> {
    @Override
    public void writeToProjectSpace(ProjectWriter writer, ProjectWriter.ForContainer<FormulaResultId, FormulaResult> containerSerializer, FormulaResultId id, FormulaResult container) throws IOException {
        writer.inDirectory(id.getParentId().getDirectoryName(), ()->{
            containerSerializer.writeAllComponents(writer,container,container::get);
            return true;
        });
    }

    @Override
    public FormulaResult readFromProjectSpace(ProjectReader reader, ProjectReader.ForContainer<FormulaResultId, FormulaResult> containerSerializer, FormulaResultId id) throws IOException {
        return reader.inDirectory(id.getParentId().getDirectoryName(), ()->{
            final FormulaResult formulaResult = new FormulaResult(id);
            containerSerializer.readAllComponents(reader,formulaResult,formulaResult::set);
            return formulaResult;
        });
    }

    @Override
    public void deleteFromProjectSpace(ProjectWriter writer, ProjectWriter.DeleteContainer<FormulaResultId> containerSerializer, FormulaResultId id) throws IOException {
        writer.inDirectory(id.getParentId().getDirectoryName(), ()->{
            containerSerializer.deleteAllComponents(writer,id);
            return true;
        });
    }
}
