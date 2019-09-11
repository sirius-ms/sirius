package de.unijena.bioinf.projectspace.sirius;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.properties.FinalConfig;
import de.unijena.bioinf.babelms.ms.JenaMsParser;
import de.unijena.bioinf.babelms.ms.JenaMsWriter;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.projectspace.*;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

public class MsExperimentSerializer implements ComponentSerializer<CompoundContainerId, CompoundContainer, Ms2Experiment> {

    @Override
    public Ms2Experiment read(ProjectReader reader, CompoundContainerId id, CompoundContainer container) throws IOException {
        final Ms2Experiment exp = reader.textFile(SiriusLocations.MS2_EXPERIMENT, (b) -> new JenaMsParser().parse(b, new File(id.getDirectoryName(), SiriusLocations.MS2_EXPERIMENT).toURI().toURL()));
        if (reader.exists(SiriusLocations.COMPOUND_CONFIG)) {
            reader.binaryFile(SiriusLocations.COMPOUND_CONFIG, s -> {
                try {
                    ParameterConfig c = PropertyManager.DEFAULTS.newIndependentInstance(s, "PROJECT_SPACE:" + id.getDirectoryName());
                    return Optional.of(new ProjectSpaceConfig(c));
                } catch (ConfigurationException e) {
                    LoggerFactory.getLogger(getClass()).error("Error when reading config for Compound with ID: " + id, e);
                    return Optional.empty();
                }
            }).ifPresent(config -> exp.setAnnotation(ProjectSpaceConfig.class, (ProjectSpaceConfig) config));
        }
        return exp;
    }

    @Override
    public void write(ProjectWriter writer, CompoundContainerId id, CompoundContainer container, Ms2Experiment experiment) throws IOException {
        writer.textFile(SiriusLocations.MS2_EXPERIMENT, (w) -> new JenaMsWriter().write(w, experiment));
        if (experiment.hasAnnotation(FinalConfig.class)) {
            //config after everything is merged
            writer.textFile(SiriusLocations.COMPOUND_CONFIG, experiment.getAnnotation(FinalConfig.class).config::write);
        } else if (experiment.hasAnnotation(ProjectSpaceConfig.class)) {
            //config annotated from another workspace when copying or merging project-spaces
            writer.textFile(SiriusLocations.COMPOUND_CONFIG, experiment.getAnnotation(ProjectSpaceConfig.class).config::write);
        }
    }

    @Override
    public void delete(ProjectWriter writer, CompoundContainerId id) throws IOException {
        writer.delete(id.getDirectoryName() + "/" + SiriusLocations.MS2_EXPERIMENT);
        writer.delete(id.getDirectoryName() + "/" + SiriusLocations.COMPOUND_CONFIG);
    }
}
