package de.unijena.bioinf.ms.gui.dialogs;

import de.unijena.bioinf.ms.frontend.core.ApplicationCore;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.function.Supplier;

public class CitationDialog extends InfoDialog {
    public final String bibTexKey;

    public CitationDialog(Window owner, String bibtexKey, Supplier<String> messageSupplier) {
        this(owner, "de.unijena.bioinf.sirius.ui.cite." + bibtexKey, bibtexKey, messageSupplier);

    }

    public CitationDialog(Window owner, String propertyKey, String bibtexKey, Supplier<String> messageSupplier) {
        super(owner, "Please cite:", messageSupplier, propertyKey);
        this.bibTexKey = bibtexKey;
    }

    @Override
    protected void decorateButtonPanel(JPanel boxedButtonPanel) {
        final JButton cite = new JButton("Copy Citation");
        cite.setToolTipText("Copy citations text to clipboard.");
        cite.addActionListener(e -> {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            ApplicationCore.BIBTEX.getEntryAsPlainText(bibTexKey, false).ifPresent(s -> {
                final StringSelection sel = new StringSelection(s);
                clipboard.setContents(sel, null);
            });

        });

        final JButton bib = new JButton("Copy BibTeX");
        bib.setToolTipText("Copy citations bibtex entry to clipboard.");
        bib.addActionListener(e -> {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            ApplicationCore.BIBTEX.getEntryAsBibTex(bibTexKey).ifPresent(s -> {
                final StringSelection sel = new StringSelection(s);
                clipboard.setContents(sel, null);
            });
        });

        boxedButtonPanel.add(bib);
        boxedButtonPanel.add(cite);
        super.decorateButtonPanel(boxedButtonPanel);
    }
}
