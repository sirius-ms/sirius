package de.isas.mztab2.io;

import de.isas.mztab2.model.MzTab;

import java.io.IOException;
import java.io.Writer;
import java.util.Optional;

public class SiriusWorkspaceMzTabNonValidatingWriter extends MzTabNonValidatingWriter {
    public SiriusWorkspaceMzTabNonValidatingWriter() {
        super();
    }

    public SiriusWorkspaceMzTabNonValidatingWriter(MzTabWriterDefaults writerDefaults) {
        super(writerDefaults);
    }

    /**
     * <p>
     * Write the mzTab object to the provided output stream writer.</p>
     * <p>
     * This method does not close the output stream but will issue a
     * <code>flush</code> on the provided output stream writer!
     *
     * @param writer a {@link java.io.OutputStreamWriter} object.
     * @param mzTab  a {@link de.isas.mztab2.model.MzTab} object.
     * @throws java.io.IOException if any.
     */
    public Optional<Void> write(Writer writer, MzTab mzTab) throws IOException {
        writeMzTab(mzTab, writer);
        return Optional.empty();
    }
}
