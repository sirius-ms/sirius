package de.unijena.bioinf.projectspace;

import de.unijena.bioinf.ms.properties.ParameterConfig;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.projectspace.sirius.CompoundContainer;
import de.unijena.bioinf.projectspace.sirius.SiriusLocations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class ProjectSpaceConfigSerializer implements ComponentSerializer<CompoundContainerId, CompoundContainer, ProjectSpaceConfig> {
    @Override
    public ProjectSpaceConfig read(ProjectReader reader, CompoundContainerId id, CompoundContainer container) throws IOException {
        if (reader.exists(SiriusLocations.COMPOUND_CONFIG)) {
            return reader.binaryFile(SiriusLocations.COMPOUND_CONFIG, s -> {
                try {
                    ParameterConfig c = PropertyManager.DEFAULTS.newIndependentInstance(s, "PROJECT_SPACE:" + id.getDirectoryName());
                    return new ProjectSpaceConfig(c);
                } catch (ConfigurationException e) {
                    LoggerFactory.getLogger(getClass()).error("Error when reading config for Compound with ID: " + id, e);
                    return null;
                }
            });
        }
        return null;
    }

    @Override
    public void write(ProjectWriter writer, CompoundContainerId id, CompoundContainer container, ProjectSpaceConfig component) throws IOException {
        writer.textFile(SiriusLocations.COMPOUND_CONFIG, component.config::write);
    }

    @Override
    public void delete(ProjectWriter writer, CompoundContainerId id) throws IOException {
        writer.delete(id.getDirectoryName() + "/" + SiriusLocations.COMPOUND_CONFIG);
    }
}
