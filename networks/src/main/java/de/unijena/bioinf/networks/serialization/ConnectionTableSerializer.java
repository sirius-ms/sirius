package de.unijena.bioinf.networks.serialization;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.unijena.bioinf.projectspace.ComponentSerializer;
import de.unijena.bioinf.projectspace.CompoundContainerId;
import de.unijena.bioinf.projectspace.ProjectReader;
import de.unijena.bioinf.projectspace.ProjectWriter;
import de.unijena.bioinf.projectspace.CompoundContainer;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Optional;

public class ConnectionTableSerializer implements ComponentSerializer<CompoundContainerId, CompoundContainer, ConnectionTable> {

    public static String filename = "connections.json";

    private final ObjectMapper mapper;

    public ConnectionTableSerializer() {
        mapper = new ObjectMapper();
    }


    @Override
    public @Nullable ConnectionTable read(ProjectReader reader, CompoundContainerId id, CompoundContainer container) throws IOException {
        if (!reader.exists(filename)) return null;
        return reader.textFile(filename, (r)-> mapper.readValue(r, ConnectionTable.class));
    }

    @Override
    public void write(ProjectWriter writer, CompoundContainerId id, CompoundContainer container, Optional<ConnectionTable> component) throws IOException {
        if (component.isEmpty()) {
            delete(writer,id);
        } else {
            writer.textFile(filename, (w)->{
                mapper.writeValue(w, component.get());
            });
        }
    }

    @Override
    public void delete(ProjectWriter writer, CompoundContainerId id) throws IOException {
        writer.deleteIfExists(filename);
    }
}
