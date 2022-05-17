/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.gui.dialogs;

import de.unijena.bioinf.ms.frontend.core.ApplicationCore;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class CitationDialog extends InfoDialog {
    public final List<String> bibtexKeys;

    public CitationDialog(Window owner, String bibtexKey, Supplier<String> messageSupplier) {
        this(owner, "de.unijena.bioinf.sirius.ui.cite." + bibtexKey, List.of(bibtexKey), messageSupplier);
    }

    public CitationDialog(Window owner, String propertyKey, Collection<String> bibtexKeys, Supplier<String> messageSupplier) {
        super(owner, "Please cite:", messageSupplier, propertyKey, false);
        this.bibtexKeys = new ArrayList<>(bibtexKeys);
        setVisible(true);
    }

    @Override
    protected void decorateButtonPanel(JPanel boxedButtonPanel) {
        final JButton cite = new JButton("Copy Citation(s)");
        cite.setToolTipText("Copy citations text to clipboard.");
        cite.addActionListener(e -> {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            String s = bibtexKeys.stream()
                    .map(bibtexKey -> ApplicationCore.BIBTEX.getEntryAsPlainText(bibtexKey, false))
                    .flatMap(Optional::stream).collect(Collectors.joining(System.lineSeparator()));
            final StringSelection sel = new StringSelection(s);
            clipboard.setContents(sel, null);

        });

        final JButton bib = new JButton("Copy BibTeX");
        bib.setToolTipText("Copy citations bibtex entry to clipboard.");
        bib.addActionListener(e -> {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            String s = bibtexKeys.stream()
                    .map(ApplicationCore.BIBTEX::getEntryAsBibTex)
                    .flatMap(Optional::stream).collect(Collectors.joining(System.lineSeparator()));
            final StringSelection sel = new StringSelection(s);
            clipboard.setContents(sel, null);
        });

        boxedButtonPanel.add(bib);
        boxedButtonPanel.add(cite);
        super.decorateButtonPanel(boxedButtonPanel);
    }
}
