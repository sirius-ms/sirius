package de.unijena.bioinf.ms.io.projectspace;

import de.unijena.bioinf.sirius.ExperimentResult;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Set;

public interface MetaDataSerializer {
    void read(@NotNull final ExperimentResult input, @NotNull final DirectoryReader reader, @NotNull final Set<String> names) throws IOException;
    void write(@NotNull final ExperimentResult input, @NotNull final DirectoryWriter writer) throws IOException;

}
