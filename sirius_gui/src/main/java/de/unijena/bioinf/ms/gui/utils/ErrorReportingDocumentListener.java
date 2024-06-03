package de.unijena.bioinf.ms.gui.utils;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;

public abstract class ErrorReportingDocumentListener extends ErrorReportingInputVerifier implements DocumentListener {

    private final JTextComponent source;

    public ErrorReportingDocumentListener(JTextComponent source) {
        this.source = source;
        this.source.getDocument().addDocumentListener(this);
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
        modifyIfError(source);
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        modifyIfError(source);
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        modifyIfError(source);
    }
}
