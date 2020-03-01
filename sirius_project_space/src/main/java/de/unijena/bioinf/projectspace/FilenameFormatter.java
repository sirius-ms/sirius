package de.unijena.bioinf.projectspace;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

public interface FilenameFormatter extends Function<Ms2Experiment, String> {
    String getFormatExpression();

    class PSProperty implements ProjectSpaceProperty {
        public final String formatExpression;

        public PSProperty(FilenameFormatter formatter) {
            formatExpression = formatter.getFormatExpression();
        }

        public PSProperty(String formatExpression) {
            this.formatExpression = formatExpression;
        }
    }

    class PSPropertySerializer implements ComponentSerializer<ProjectSpaceContainerId, ProjectSpaceContainer<ProjectSpaceContainerId>, PSProperty> {
        public static final String FILENAME = ".format";

        @Override
        public PSProperty read(ProjectReader reader, ProjectSpaceContainerId id, ProjectSpaceContainer<ProjectSpaceContainerId> container) throws IOException {
            if (reader.exists(FILENAME))
                try (Stream<String> lines = Files.lines(reader.asPath(FILENAME))) {
                    return lines.findFirst().map(PSProperty::new).orElse(null);
                }
            return null;
        }

        @Override
        public void write(ProjectWriter writer, ProjectSpaceContainerId id, ProjectSpaceContainer<ProjectSpaceContainerId> container, Optional<PSProperty> optProp) throws IOException {
            if (optProp.isPresent()) {
                writer.deleteIfExists(FILENAME);
                writer.textFile(FILENAME, bf -> bf.write(optProp.get().formatExpression));
            } else {
                LoggerFactory.getLogger(getClass()).warn("Could not find Project Space formatting information!");
            }

        }

        @Override
        public void delete(ProjectWriter writer, ProjectSpaceContainerId id) throws IOException {
            writer.deleteIfExists(FILENAME);
        }
    }
}
