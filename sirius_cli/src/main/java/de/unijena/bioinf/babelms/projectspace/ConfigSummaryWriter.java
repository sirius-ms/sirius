package de.unijena.bioinf.babelms.projectspace;

import de.unijena.bioinf.ms.properties.ParameterConfig;

import org.slf4j.LoggerFactory;

import java.io.IOException;

//currently not used because we do the config stuff on the compound level
@Deprecated
public class ConfigSummaryWriter implements SummaryWriter {
    private final ParameterConfig config;

    public ConfigSummaryWriter(ParameterConfig config) {
        this.config = config;
    }

    @Override
    public void writeSummary(Iterable<Instance> experiments, DirectoryWriter writer) {
        try {
            writer.write(SiriusLocations.SIRIUS_WORKSPACE_CONFIG.fileName(), config::write);
        } catch (IOException e) {
            LoggerFactory.getLogger(getClass()).error("Error when writing Config Summary");
        }
    }
}
