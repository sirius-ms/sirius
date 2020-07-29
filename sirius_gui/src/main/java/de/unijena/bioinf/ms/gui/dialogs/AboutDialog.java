/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2015 Kai Dührkop
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.unijena.bioinf.ms.gui.dialogs;

import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.core.SiriusProperties;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.properties.PropertyManager;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class AboutDialog extends JDialog {
    public static final String PROPERTY_KEY = "de.unijena.bioinf.sirius.ui.cite";

    private final JButton close, bibtex, clipboard;
    private final boolean doNotShowAgain;
    private JCheckBox dontAsk;

    public AboutDialog(Frame owner, boolean doNotShowAgain) {
        super(owner, true);
        this.doNotShowAgain = doNotShowAgain;
        setTitle(ApplicationCore.VERSION_STRING());
        setLayout(new BorderLayout());

        // SIRIUS logo
        add(new JLabel(Icons.SIRIUS_SPLASH), BorderLayout.NORTH);

        try {
            final String htmlText;
            {
                final StringBuilder buf = new StringBuilder();
                try (final BufferedReader br = FileUtils.ensureBuffering(new InputStreamReader(AboutDialog.class.getResourceAsStream("/sirius/about.html")))) {
                    String line;
                    while ((line = br.readLine()) != null) buf.append(line).append('\n');
                }
                buf.append(ApplicationCore.BIBTEX.getCitationsHTML());
                htmlText = buf.toString();
            }

            final JTextPane textPane = new JTextPane();
            final JScrollPane scroll = new JScrollPane(textPane, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            textPane.setContentType("text/html");
            textPane.setText(htmlText);
            textPane.setEditable(false);
            textPane.addHyperlinkListener(new HyperlinkListener() {
                @Override
                public void hyperlinkUpdate(HyperlinkEvent e) {
                    if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                        try {
                            Desktop.getDesktop().browse(e.getURL().toURI());
                        } catch (Exception error) {
                            LoggerFactory.getLogger(this.getClass()).error(error.getMessage(),error);
                        }
                    }
                }
            });
            textPane.setCaretPosition(0);
            add(scroll, BorderLayout.CENTER);
        } catch (IOException e) {
            e.printStackTrace();
        }


//        final JPanel buttons = new JPanel(new GridLayout(0, 3));
        JPanel south = new JPanel();
        south.setLayout(new BoxLayout(south, BoxLayout.X_AXIS));
        south.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        if (doNotShowAgain) {
            dontAsk = new JCheckBox();
            dontAsk.setText("Do not show dialog again.");
            dontAsk.setSelected(PropertyManager.getBoolean(PROPERTY_KEY, false));
            south.add(dontAsk);
        }
        south.add(Box.createHorizontalGlue());

        close = new JButton();
        bibtex = new JButton();
        clipboard = new JButton();
        south.add(bibtex);
        south.add(clipboard);
        south.add(Box.createHorizontalGlue());
        south.add(close);


        this.add(south, BorderLayout.SOUTH);

        defineActions();

        setMinimumSize(new Dimension(Icons.SIRIUS_SPLASH.getIconWidth(), Icons.SIRIUS_SPLASH.getIconHeight() + south.getPreferredSize().height + 100));
        setPreferredSize(new Dimension(Icons.SIRIUS_SPLASH.getIconWidth(), getPreferredSize().height));

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        setLocationRelativeTo(getParent());
        setVisible(true);

    }

    private void defineActions() {
        final Action closeA = new AbstractAction("close") {
            @Override
            public void actionPerformed(ActionEvent e) {
                AboutDialog.this.dispose();
            }
        };
        final Action copyBibTexA = new AbstractAction("copy BibTex") {
            @Override
            public void actionPerformed(ActionEvent e) {
                copyBibTex();
            }
        };
        final Action copyPlainA = new AbstractAction("copy plain text") {
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

        addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent e) {
            }

            public void windowClosing(WindowEvent e) {
                if (dontAsk != null)
                    Jobs.runInBackground(() -> SiriusProperties.SIRIUS_PROPERTIES_FILE().setAndStoreProperty(PROPERTY_KEY, String.valueOf(dontAsk.isSelected())));
            }
        });

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
