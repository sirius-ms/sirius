package de.unijena.bioinf.ms.projectspace;

import de.unijena.bioinf.sirius.ExperimentResult;

import java.io.IOException;
import java.io.Writer;

public class VersionInfoWriter implements SummaryWriter {
    @Override
    public void writeSummary(Iterable<ExperimentResult> experiments, DirectoryWriter writer) {
        writer.write(SiriusLocations.SIRIUS_VERSION_FILE.fileName(), this::addVersionStrings);
        writer.write(SiriusLocations.SIRIUS_CITATION_FILE.fileName(), this::addVersionStrings);
        writer.write(SiriusLocations.SIRIUS_FORMATTER_FILE.fileName(), this::addVersionStrings);
    }


    protected void writeVersionInfo(Writer w) {
        try {
            w.write(versionString);
            w.write("\n");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void writeCitationInfo(Writer w) {
        try {
            w.write(versionString);
            w.write("\n");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void writeFileNameFormatter(Writer w) {
        try {
            w.write(versionString);
            w.write("\n");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
