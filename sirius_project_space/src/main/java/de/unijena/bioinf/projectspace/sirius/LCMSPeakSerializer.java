package de.unijena.bioinf.projectspace.sirius;

import de.unijena.bioinf.ChemistryBase.ms.lcms.CoelutingTraceSet;
import de.unijena.bioinf.ChemistryBase.ms.lcms.LCMSPeakInformation;
import de.unijena.bioinf.ChemistryBase.utils.IOFunctions;
import de.unijena.bioinf.babelms.binary.MassTraceIo;
import de.unijena.bioinf.projectspace.ComponentSerializer;
import de.unijena.bioinf.projectspace.CompoundContainerId;
import de.unijena.bioinf.projectspace.ProjectReader;
import de.unijena.bioinf.projectspace.ProjectWriter;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class LCMSPeakSerializer implements ComponentSerializer<CompoundContainerId, CompoundContainer, LCMSPeakInformation> {

    @Nullable
    @Override
    public LCMSPeakInformation read(ProjectReader reader, CompoundContainerId id, CompoundContainer container) throws IOException {
        if (!reader.exists(SiriusLocations.LCMS_TRACES))
            return null;
        return reader.binaryFile(SiriusLocations.LCMS_TRACES, new IOFunctions.IOFunction<BufferedInputStream, LCMSPeakInformation>() {
            @Override
            public LCMSPeakInformation apply(BufferedInputStream bufferedInputStream) throws IOException {
                try (final GZIPInputStream zipped = new GZIPInputStream(bufferedInputStream)) {
                    final CoelutingTraceSet[] traceSets = new MassTraceIo().readAll(zipped);
                    if (traceSets.length == 0) return null;
                    return new LCMSPeakInformation(traceSets);
                }
            }
        });
    }

    @Override
    public void write(ProjectWriter writer, CompoundContainerId container, CompoundContainer id, Optional<LCMSPeakInformation> component) throws IOException {
        // we always store chromatographic information in the directory as binary file
        writer.binaryFile(SiriusLocations.LCMS_TRACES, bufferedOutputStream -> {
            try (GZIPOutputStream zipped = new GZIPOutputStream(bufferedOutputStream)) {
                new MassTraceIo().writeAll(zipped, component.map(c -> c.traceSet).orElse(new CoelutingTraceSet[0]));
            }
        });
    }

    @Override
    public void delete(ProjectWriter writer, CompoundContainerId id) throws IOException {
        writer.delete(SiriusLocations.LCMS_TRACES);
    }
}
