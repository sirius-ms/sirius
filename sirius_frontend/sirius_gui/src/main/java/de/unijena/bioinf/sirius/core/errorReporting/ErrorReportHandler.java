package de.unijena.bioinf.sirius.core.errorReporting;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 03.10.16.
 */

import de.unijena.bioinf.sirius.gui.io.ByteRingBufferOutputStream;

import java.util.logging.StreamHandler;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class ErrorReportHandler extends StreamHandler {
    protected ByteRingBufferOutputStream s;

    public ErrorReportHandler(ByteRingBufferOutputStream out) {
        super();
        setOutputStream(out);
    }

    public ErrorReportHandler() {
        super();
        setOutputStream(new ByteRingBufferOutputStream(10485760));
    }

    protected synchronized void setOutputStream(ByteRingBufferOutputStream out) throws SecurityException {
        super.setOutputStream(out);
        s = out;
    }

    public byte[] flushToByteArray() {
        flush();
        return s.toByteArray();
    }
}
