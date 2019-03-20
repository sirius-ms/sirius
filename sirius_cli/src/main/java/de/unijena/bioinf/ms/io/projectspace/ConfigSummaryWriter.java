package de.unijena.bioinf.ms.io.projectspace;

import de.unijena.bioinf.ms.properties.ParameterConfig;
import de.unijena.bioinf.sirius.ExperimentResult;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class ConfigSummaryWriter implements SummaryWriter {
    private final ParameterConfig config;

    public ConfigSummaryWriter(ParameterConfig config) {
        this.config = config;
    }

    @Override
    public void writeSummary(Iterable<ExperimentResult> experiments, DirectoryWriter writer) {
        try {
            writer.write(SiriusLocations.SIRIUS_CONFIG.fileName(), config::write);
        } catch (IOException e) {
            LoggerFactory.getLogger(getClass()).error("Error when writing Config Summary");
        }
    }
}
