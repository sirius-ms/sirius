package de.unijena.bioinf.projectspace.sirius;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.projectspace.*;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CompoundContainerSerializer implements ContainerSerializer<CompoundContainerId, CompoundContainer> {

    @Override
    public void writeToProjectSpace(ProjectWriter writer, ProjectWriter.ForContainer containerSerializer, CompoundContainerId id, CompoundContainer container) throws IOException {
        // ensure that we are in the right directory
        writer.inDirectory(id.getDirectoryName(), ()->{
            containerSerializer.writeAllComponents(writer, container, container::getAnnotation);
            return true;
        });
    }

    private final static Pattern resultPattern = Pattern.compile("(\\d+)_([^_]+)_(.+)\\.json");

    @Override
    public CompoundContainer readFromProjectSpace(ProjectReader reader, ProjectReader.ForContainer<CompoundContainerId, CompoundContainer> containerSerializer, CompoundContainerId id) throws IOException {
        return reader.inDirectory(id.getDirectoryName(), ()->{
//            Map<String, String> info = reader.keyValues(SiriusLocations.COMPOUND_INFO);
            final CompoundContainer container = new CompoundContainer(id/*, (Class<? extends FormulaScore>) Score.resolve(info.get("rankingScore"))*/);
            reader.inDirectory("trees", ()->{
                for (String file : reader.list("*.json")) {
                    final String name = file.substring(0, file.length()-".json".length());
                    String[] pt = name.split("_");
                    container.results.add(new FormulaResultId(id, MolecularFormula.parseOrThrow(pt[0]), PrecursorIonType.fromString(pt[1])));
                }
                return true;
            });

            containerSerializer.readAllComponents(reader, container, container::setAnnotation);
            return container;
        });
    }

    @Override
    public void deleteFromProjectSpace(ProjectWriter writer, ProjectWriter.DeleteContainer<CompoundContainerId> containerSerializer, CompoundContainerId id) throws IOException {
        writer.inDirectory(id.getDirectoryName(), ()->{
            containerSerializer.deleteAllComponents(writer, id);
            return null;
        });
        writer.delete(id.getDirectoryName());
    }
}
