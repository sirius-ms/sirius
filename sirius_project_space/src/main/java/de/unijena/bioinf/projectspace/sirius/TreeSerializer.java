package de.unijena.bioinf.projectspace.sirius;

import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.babelms.json.FTJsonReader;
import de.unijena.bioinf.projectspace.ComponentSerializer;
import de.unijena.bioinf.projectspace.ProjectReader;
import de.unijena.bioinf.projectspace.ProjectWriter;

import java.io.File;
import java.io.IOException;

public class TreeSerializer implements ComponentSerializer<FormulaResultId, FormulaResult, FTree> {
    @Override
    public FTree read(ProjectReader reader, FormulaResultId id, FormulaResult container) {
        final File path = new File(getPathName(id));
        reader.inDirectory(path.getParent(), (r)->{
            reader.textFile(path.getName(),(r)-> {
                try {
                    return new FTJsonReader().parse(r,path.toURI().toURL());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        });
    }

    @Override
    public void write(ProjectWriter writer, FormulaResultId id, FormulaResult container, FTree component) {

    }

    @Override
    public void delete(ProjectWriter writer, FormulaResultId id, FormulaResult container, FTree component) {

    }

    private String getPathName(FormulaResultId id) {
        return id.getParentId().getDirectoryName() + "/trees/" + id.fileName(".json");
    }
}
