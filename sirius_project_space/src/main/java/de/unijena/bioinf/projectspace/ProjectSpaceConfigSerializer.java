package de.unijena.bioinf.projectspace;

import de.unijena.bioinf.ms.properties.ParameterConfig;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.projectspace.sirius.CompoundContainer;
import de.unijena.bioinf.projectspace.sirius.SiriusLocations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;

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
    public void write(ProjectWriter writer, CompoundContainerId id, CompoundContainer container, Optional<ProjectSpaceConfig> optConf) throws IOException {
        if (optConf.isPresent())
            writer.textFile(SiriusLocations.COMPOUND_CONFIG, optConf.get().config::write);
        else
            LoggerFactory.getLogger("Could not find config/parameter info for this Compound: '" + id + "'. Project-Space will not contain parameter information");
    }

    @Override
    public void delete(ProjectWriter writer, CompoundContainerId id) throws IOException {
        writer.delete(id.getDirectoryName() + "/" + SiriusLocations.COMPOUND_CONFIG);
    }
}
