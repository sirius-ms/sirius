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

package de.unijena.bioinf.sirius.gui.dialogs;

import de.unijena.bioinf.sirius.core.ApplicationCore;
import de.unijena.bioinf.sirius.gui.configs.Icons;
import org.slf4j.LoggerFactory;

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
        setTitle(ApplicationCore.VERSION_STRING);
        setLayout(new BorderLayout());

        // SIRIUS logo
        add(new JLabel(Icons.SIRIUS_SPLASH), BorderLayout.NORTH);

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


        final JPanel buttons = new JPanel(new GridLayout(0, 3));
        close = new JButton();
        bibtex = new JButton();
        clipboard = new JButton();
        buttons.add(close);
        buttons.add(bibtex);
        buttons.add(clipboard);
        add(buttons, BorderLayout.SOUTH);
        defineActions();

        setMinimumSize(new Dimension(Icons.SIRIUS_SPLASH.getIconWidth(), Icons.SIRIUS_SPLASH.getIconHeight() + buttons.getPreferredSize().height + 100));
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
    }
    //todo we should load that from property file
    private static String FINGERID_CITATION = "Kai Dührkop, Huibin Shen, Marvin Meusel, Juho Rousu and Sebastian Böcker\nSearching molecular structure databases with tandem mass spectra using CSI:FingerID. \nProc Natl Acad Sci U S A, 112(41):12580-12585, 2015." +
            "Huibin Shen, Kai Dührkop, Sebastian Böcker and Juho Rousu\nMetabolite Identification through Multiple Kernel Learning on Fragmentation Trees.\nBioinformatics, 30(12):i157-i164, 2014. Proc. of Intelligent Systems for Molecular Biology (ISMB 2014)." +
            "Markus Heinonen, Huibin Shen, Nicola Zamboni and Juho Rousu\nMetabolite identification and molecular fingerprint prediction through machine learning\nBioinformatics (2012) 28 (18): 2333-2341.Proc. of European Conference on Computational Biology (ECCB 2012).";

    private static String FINGERID_BIBTEX = "@Article{duehrkop15searching,\n" +
            "  Title                    = {Searching molecular structure databases with tandem mass spectra using {CSI:FingerID}},\n" +
            "  Author                   = {Kai D\\\"uhrkop and Huibin Shen and Marvin Meusel and Juho Rousu and Sebastian B\\\"ocker},\n" +
            "  Journal                  = PNAS,\n" +
            "  Year                     = {2015},\n" +
            "  Note                     = {Accepted for publication},\n" +
            "\n" +
            "  Owner                    = {Sebastian},\n" +
            "  Timestamp                = {2015.08.12}\n" +
            "}\n" +
            "@Article{shen14metabolite,\n" +
            "  Title                    = {Metabolite Identification through Multiple Kernel Learning on Fragmentation Trees},\n" +
            "  Author                   = {Huibin Shen and Kai D\\\"uhrkop and Sebastian B\\\"ocker and Juho Rousu},\n" +
            "  Journal                  = {Bioinformatics},\n" +
            "  Year                     = {2014},\n" +
            "  Note                     = {Proc.\\ of \\emph{Intelligent Systems for Molecular Biology} (ISMB 2014)},\n" +
            "  Number                   = {12},\n" +
            "  Pages                    = {i157-i164},\n" +
            "  Volume                   = {30},\n" +
            "\n" +
            "  Abstract                 = {Metabolite identification from tandem mass spectrometric data is a key task in metabolomics. Various computational methods has been proposed for the identification of metabolites from tandem mass spectra. Fragmentation tree methods explore the space of possible ways the metabolite can fragment, and base the metabolite identification on scoring of these fragmentation trees. Machine learning methods has been used to map mass spectra to molecular fingerprints; predicted fingerprints, in turn, can be used to score candidate molecular structures. Here, we combine fragmentation tree computations with kernel-based machine learning to predict molecular fingerprints and identify molecular structures. We introduce a family of kernels capturing the similarity of fragmentation trees, and combine these kernels using recently proposed multiple kernel learning approaches. Experiments on two large reference datasets show that the new methods significantly improve molecular fingerprint prediction accuracy. These improvements result in better metabolite identification, doubling the number of metabolites ranked at the top position of the candidates list.},\n" +
            "  Doi                      = {10.1093/bioinformatics/btu275},\n" +
            "  File                     = {ShenEtAl_MetaboliteIdentificationMultipleKernel_ISMB_2014.pdf:2014/ShenEtAl_MetaboliteIdentificationMultipleKernel_ISMB_2014.pdf:PDF},\n" +
            "  Keywords                 = {jena; IDUN; MS; tandem MS;},\n" +
            "  Owner                    = {fhufsky},\n" +
            "  Pmid                     = {24931979},\n" +
            "  Timestamp                = {2014.02.11},\n" +
            "  Url                      = {http://bioinformatics.oxfordjournals.org/content/30/12/i157.full}\n" +
            "}\n" +
            "@Article{shen14metabolite,\n" +
            "  Title                    = {Metabolite Identification through Multiple Kernel Learning on Fragmentation Trees},\n" +
            "  Author                   = {Huibin Shen and Kai D\\\"uhrkop and Sebastian B\\\"ocker and Juho Rousu},\n" +
            "  Journal                  = {Bioinformatics},\n" +
            "  Year                     = {2014},\n" +
            "  Note                     = {Proc.\\ of \\emph{Intelligent Systems for Molecular Biology} (ISMB 2014)},\n" +
            "  Number                   = {12},\n" +
            "  Pages                    = {i157-i164},\n" +
            "  Volume                   = {30},\n" +
            "\n" +
            "  Abstract                 = {Metabolite identification from tandem mass spectrometric data is a key task in metabolomics. Various computational methods has been proposed for the identification of metabolites from tandem mass spectra. Fragmentation tree methods explore the space of possible ways the metabolite can fragment, and base the metabolite identification on scoring of these fragmentation trees. Machine learning methods has been used to map mass spectra to molecular fingerprints; predicted fingerprints, in turn, can be used to score candidate molecular structures. Here, we combine fragmentation tree computations with kernel-based machine learning to predict molecular fingerprints and identify molecular structures. We introduce a family of kernels capturing the similarity of fragmentation trees, and combine these kernels using recently proposed multiple kernel learning approaches. Experiments on two large reference datasets show that the new methods significantly improve molecular fingerprint prediction accuracy. These improvements result in better metabolite identification, doubling the number of metabolites ranked at the top position of the candidates list.},\n" +
            "  Doi                      = {10.1093/bioinformatics/btu275},\n" +
            "  File                     = {ShenEtAl_MetaboliteIdentificationMultipleKernel_ISMB_2014.pdf:2014/ShenEtAl_MetaboliteIdentificationMultipleKernel_ISMB_2014.pdf:PDF},\n" +
            "  Keywords                 = {jena; IDUN; MS; tandem MS;},\n" +
            "  Owner                    = {fhufsky},\n" +
            "  Pmid                     = {24931979},\n" +
            "  Timestamp                = {2014.02.11},\n" +
            "  Url                      = {http://bioinformatics.oxfordjournals.org/content/30/12/i157.full}\n" +
            "}";

    private void copyBibTex() {
        copyPlainText(ApplicationCore.CITATION_BIBTEX + "\n" + FINGERID_BIBTEX);
    }

    private void copyPlainText() {
        copyPlainText(ApplicationCore.CITATION + "\n" + FINGERID_CITATION);
    }

    private void copyPlainText(String text) {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        final StringSelection sel = new StringSelection(text);
        clipboard.setContents(sel, null);
    }

}
