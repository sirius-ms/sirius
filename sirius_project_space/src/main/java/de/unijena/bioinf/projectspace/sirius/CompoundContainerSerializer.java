package de.unijena.bioinf.projectspace.sirius;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.Score;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.projectspace.*;
import de.unijena.bioinf.sirius.scores.FormulaScore;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CompoundContainerSerializer implements ContainerSerializer<CompoundContainerId, CompoundContainer> {

    @Override
    public void writeToProjectSpace(ProjectWriter writer, ProjectWriter.ForContainer containerSerializer, CompoundContainerId id, CompoundContainer container) throws IOException {
        // nothing to do
    }

    private final static Pattern resultPattern = Pattern.compile("(\\d+)_([^_]+)_(.+)\\.json");

    @Override
    public CompoundContainer readFromProjectSpace(ProjectReader reader, ProjectReader.ForContainer<CompoundContainerId, CompoundContainer> containerSerializer, CompoundContainerId id) throws IOException {
        return reader.inDirectory(id.getDirectoryName(), ()->{
            Map<String, String> info = reader.keyValues(SiriusLocations.COMPOUND_INFO);
            final CompoundContainer container = new CompoundContainer(id, (Class<? extends FormulaScore>) Score.resolve(info.get("rankingScore")));
            for (String file : reader.glob("trees/*.json")) {
                Matcher matcher = resultPattern.matcher(new File(file).getName());
                container.getResults().add(new FormulaResultId(id, MolecularFormula.parseOrThrow(matcher.group(2)), PrecursorIonType.fromString(matcher.group(3)), Integer.parseInt(matcher.group(1)), container.getRankingScore()));
            }
            container.getResults().sort(Comparator.comparingInt(FormulaResultId::getRank));
            containerSerializer.readAllComponents(reader, container, container::get);
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
