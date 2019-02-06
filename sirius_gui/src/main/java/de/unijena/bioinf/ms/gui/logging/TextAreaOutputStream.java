package de.unijena.bioinf.ms.gui.logging;

import javax.swing.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class TextAreaOutputStream extends OutputStream {
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    private final JTextArea textArea;

    public TextAreaOutputStream(JTextArea textArea) {
        super();
        this.textArea = textArea;
    }

    @Override
    public void flush() throws IOException {
        if (textArea != null)
            textArea.append(buffer.toString("UTF-8"));
        buffer.reset();
    }

    @Override
    public void write(int b) throws IOException {
        buffer.write(b);
    }
}
