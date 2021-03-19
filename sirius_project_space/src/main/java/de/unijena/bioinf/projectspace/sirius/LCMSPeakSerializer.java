package de.unijena.bioinf.projectspace.sirius;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.unijena.bioinf.ChemistryBase.ms.lcms.CoelutingTraceSet;
import de.unijena.bioinf.ChemistryBase.ms.lcms.LCMSPeakInformation;
import de.unijena.bioinf.projectspace.ComponentSerializer;
import de.unijena.bioinf.projectspace.CompoundContainerId;
import de.unijena.bioinf.projectspace.ProjectReader;
import de.unijena.bioinf.projectspace.ProjectWriter;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Optional;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class LCMSPeakSerializer implements ComponentSerializer<CompoundContainerId, CompoundContainer, LCMSPeakInformation> {

    @Nullable
    @Override
    public LCMSPeakInformation read(ProjectReader reader, CompoundContainerId id, CompoundContainer container) throws IOException {
        if (reader.exists(SiriusLocations.LCMS_JSON)) {
            return reader.binaryFile(SiriusLocations.LCMS_JSON, (io)->{
                try (GZIPInputStream zipped = new GZIPInputStream(io)) {
                    ObjectMapper objectMapper = new ObjectMapper();
                    return objectMapper.readValue(zipped, LCMSPeakInformation.class);
                }
            });
        } else {
            return LCMSPeakInformation.empty();
        }
    }

    @Override
    public void write(ProjectWriter writer, CompoundContainerId container, CompoundContainer id, Optional<LCMSPeakInformation> component) throws IOException {
        writer.binaryFile(SiriusLocations.LCMS_JSON, bufferedOutputStream -> {
            try (GZIPOutputStream zipped = new GZIPOutputStream(bufferedOutputStream)) {
                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.writeValue(zipped, component.orElseGet(()->new LCMSPeakInformation(new CoelutingTraceSet[0])));
            }
        });
    }

    @Override
    public void delete(ProjectWriter writer, CompoundContainerId id) throws IOException {
        writer.delete(SiriusLocations.LCMS_JSON);
    }
}
