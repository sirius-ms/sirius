

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

import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.core.SiriusProperties;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.configs.Colors;
import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.gui.webView.WebViewJPanel;
import de.unijena.bioinf.ms.properties.PropertyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class AboutDialog extends JDialog {
    public static final String PROPERTY_KEY = "de.unijena.bioinf.sirius.ui.cite";
    private static final Logger log = LoggerFactory.getLogger(AboutDialog.class);

    private final JButton close, bibtex, clipboard;
    private JCheckBox dontAsk;

    public AboutDialog(Frame owner, boolean doNotShowAgain) {
        super(owner, true);
        setTitle(ApplicationCore.VERSION_STRING());
        setLayout(new BorderLayout());

        // SIRIUS logo
        add(new JLabel(Icons.SIRIUS_SPLASH), BorderLayout.NORTH);
        String htmlText =  "<html><head></head><body>Data missing!</html>\n";
        WebViewJPanel htmlPanel =  new WebViewJPanel();
        try {
            final StringBuilder buf = new StringBuilder();
            try (final BufferedReader br = FileUtils.ensureBuffering(new InputStreamReader(AboutDialog.class.getResourceAsStream("/sirius/about.html")))) {
                String line;
                while ((line = br.readLine()) != null) buf.append(line).append('\n');
            }
            buf.append(ApplicationCore.BIBTEX.getCitationsHTML(true));
            htmlText = buf.append("</div></body></html>").toString();

            add(htmlPanel, BorderLayout.CENTER);
            htmlPanel.load(htmlText);
        } catch (IOException e) {
            log.error("Error when loading about html.", e);
        }

        JPanel south = new JPanel();
        south.setLayout(new BoxLayout(south, BoxLayout.X_AXIS));
        south.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        if (doNotShowAgain) {
            dontAsk = new JCheckBox();
            dontAsk.setText("Do not show dialog again.");
            dontAsk.setSelected(PropertyManager.getBoolean(PROPERTY_KEY, false));
            south.add(dontAsk);
            south.add(Box.createHorizontalGlue());
        }

        close = new JButton();
        bibtex = new JButton();
        clipboard = new JButton();
        south.add(bibtex);
        south.add(clipboard);
        south.add(Box.createHorizontalGlue());
        south.add(close);


        this.add(south, BorderLayout.SOUTH);

        defineActions();

        setMinimumSize(new Dimension(Icons.SIRIUS_SPLASH.getIconWidth(), Icons.SIRIUS_SPLASH.getIconHeight() + 550));
        setMaximumSize(new Dimension(Icons.SIRIUS_SPLASH.getIconWidth(), getMaximumSize().height));
        setResizable(false);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        setLocationRelativeTo(getOwner());
        setVisible(true);
    }

    private void defineActions() {
        final Action closeA = new AbstractAction("Close") {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (dontAsk != null)
                    Jobs.runInBackground(() -> SiriusProperties.SIRIUS_PROPERTIES_FILE().setAndStoreProperty(PROPERTY_KEY, String.valueOf(dontAsk.isSelected())));
                AboutDialog.this.dispose();
            }
        };
        final Action copyBibTexA = new AbstractAction("Copy as BibTex") {
            @Override
            public void actionPerformed(ActionEvent e) {
                copyBibTex();
            }
        };
        final Action copyPlainA = new AbstractAction("Copy as text") {
            @Override
            public void actionPerformed(ActionEvent e) {
                copyPlainText();
            }
        };

        final KeyStroke enterKey = KeyStroke.getKeyStroke("ENTER");
        final KeyStroke escKey = KeyStroke.getKeyStroke("ESCAPE");
        final InputMap inputMap =getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        inputMap.put(enterKey, "close");
        inputMap.put(escKey, "close");
        final ActionMap actionMap = getRootPane().getActionMap();
        actionMap.put("close", closeA);
        actionMap.put("copy BibTex", copyBibTexA);
        actionMap.put("copy plain text", copyPlainA);

        close.setAction(closeA);
        bibtex.setAction(copyBibTexA);
        clipboard.setAction(copyPlainA);
    }

    private void copyBibTex() {


        copyPlainText(ApplicationCore.BIBTEX.getCitationsBibTex());
    }

    private void copyPlainText() {
        copyPlainText(ApplicationCore.BIBTEX.getCitationsText());
    }

    private void copyPlainText(String text) {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        final StringSelection sel = new StringSelection(text);
        clipboard.setContents(sel, null);
    }

}
