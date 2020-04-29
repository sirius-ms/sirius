package de.unijena.bioinf.projectspace.sirius;

import de.unijena.bioinf.ChemistryBase.ms.DetectedAdducts;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.babelms.ms.JenaMsParser;
import de.unijena.bioinf.babelms.ms.JenaMsWriter;
import de.unijena.bioinf.projectspace.ComponentSerializer;
import de.unijena.bioinf.projectspace.CompoundContainerId;
import de.unijena.bioinf.projectspace.ProjectReader;
import de.unijena.bioinf.projectspace.ProjectWriter;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

public class MsExperimentSerializer implements ComponentSerializer<CompoundContainerId, CompoundContainer, Ms2Experiment> {

    @Override
    public Ms2Experiment read(ProjectReader reader, CompoundContainerId id, CompoundContainer container) throws IOException {
        if (!reader.exists(SiriusLocations.MS2_EXPERIMENT))
            return null;

        final Ms2Experiment exp = reader.textFile(SiriusLocations.MS2_EXPERIMENT, (b) -> new JenaMsParser().parse(b, Path.of(id.getDirectoryName(), SiriusLocations.MS2_EXPERIMENT).toUri().toURL()));
        if (exp != null)
            id.getDetectedAdducts().ifPresent(pa -> exp.setAnnotation(DetectedAdducts.class, pa));
        return exp;
    }

    @Override
    public void write(ProjectWriter writer, CompoundContainerId id, CompoundContainer container, Optional<Ms2Experiment> optEx) throws IOException {
        Ms2Experiment experiment = optEx.orElseThrow(() -> new RuntimeException("Could not find Experiment for FormulaResult with ID: " + id));
        writer.textFile(SiriusLocations.MS2_EXPERIMENT, (w) -> new JenaMsWriter().write(w, experiment));

        // actualize optional values in ID
        id.setIonMass(experiment.getIonMass());
        id.setIonType(experiment.getPrecursorIonType());
        id.setDetectedAdducts(experiment.getAnnotationOrNull(DetectedAdducts.class));

        writer.keyValues(SiriusLocations.COMPOUND_INFO, id.asKeyValuePairs());
    }

    @Override
    public void delete(ProjectWriter writer, CompoundContainerId id) throws IOException {
        writer.deleteIfExists(SiriusLocations.MS2_EXPERIMENT);
        writer.deleteIfExists(SiriusLocations.COMPOUND_INFO);
    }
}
