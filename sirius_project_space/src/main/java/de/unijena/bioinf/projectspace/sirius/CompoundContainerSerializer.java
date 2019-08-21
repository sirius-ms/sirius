package de.unijena.bioinf.projectspace.sirius;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.projectspace.ContainerSerializer;
import de.unijena.bioinf.projectspace.ProjectReader;
import de.unijena.bioinf.projectspace.ProjectWriter;

import java.io.File;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CompoundContainerSerializer implements ContainerSerializer<CompoundContainerId, CompoundContainer> {

    @Override
    public void writeToProjectSpace(ProjectWriter.ForContainer writer, CompoundContainerId id, CompoundContainer container) {
        writer.inDirectory(id.getDirectoryName(), (w)->{
            writer.writeAllComponents(container::getAnnotation);
        });
    }

    private final static Pattern resultPattern = Pattern.compile("(\\d+)_([^_]+)_(.+)\\.json");

    @Override
    public CompoundContainer readFromProjectSpace(ProjectReader.ForContainer reader, CompoundContainerId id) {
        final CompoundContainer container = new CompoundContainer(id);
        reader.inDirectory(id.getDirectoryName(), (r)->{
            final ArrayList<FormulaResultId> results = new ArrayList<>();
            for (String file : reader.glob("trees/*.json")) {
                Matcher matcher = resultPattern.matcher(new File(file).getName());
                results.add(new FormulaResultId(id, MolecularFormula.parseOrThrow(matcher.group(2)), PrecursorIonType.fromString(matcher.group(3)), Integer.parseInt(matcher.group(1))));
            }
            container.getResults().addAll(results);
            reader.readAllComponents(container::setAnnotation);
        });
        return container;
    }

    @Override
    public void deleteFromProjectSpace(ProjectWriter.ForContainer writer, CompoundContainerId id, CompoundContainer container) {
        writer.inDirectory(id.getDirectoryName(), (w)->{
            writer.deleteAllComponents(container::getAnnotation);
        });
        writer.delete(id.getDirectoryName());
    }
}
