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

package de.unijena.bioinf.sirius.gui.mainframe;

import de.unijena.bioinf.sirius.Sirius;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class AboutDialog extends JDialog{

    private final JButton close, bibtex, clipboard;

    public AboutDialog(Frame owner) {
        super(owner, true);
        setTitle("SIRIUS 3");
        this.setLayout(new BorderLayout());
        setPreferredSize(new Dimension(500, 800));
        // SIRIUS logo
        final ImageIcon image = new ImageIcon(AboutDialog.class.getResource("/icons/sirius.jpg"));
        final JLabel imageLabel = new JLabel(image);
        add(imageLabel, BorderLayout.NORTH);

        try {
            final String htmlText;
            {
                final BufferedReader br = new BufferedReader(new InputStreamReader(AboutDialog.class.getResourceAsStream("/sirius/about.html")));
                final StringBuilder buf = new StringBuilder();
                String line;
                while ((line=br.readLine())!=null) buf.append(line).append('\n');
                br.close();
                htmlText = buf.toString();
            }
            final JTextPane textPane = new JTextPane();
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
                            error.printStackTrace();
                        }
                    }
                }
            });
            add(textPane, BorderLayout.CENTER);
        } catch (IOException e) {
            e.printStackTrace();
        }


        final JPanel buttons = new JPanel(new GridLayout(0, 3));
        close = new JButton();
        bibtex = new JButton();
        clipboard = new JButton();
        buttons.add(close);
        buttons.add(bibtex);
        buttons.add(clipboard);
        add(buttons, BorderLayout.SOUTH);
        defineActions();
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
    }

    private void copyBibTex() {
        copyPlainText(Sirius.CITATION_BIBTEX);
    }

    private void copyPlainText() {
        copyPlainText(Sirius.CITATION);
    }

    private void copyPlainText(String text) {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        final StringSelection sel = new StringSelection(text);
        clipboard.setContents(sel, null);
    }

}
