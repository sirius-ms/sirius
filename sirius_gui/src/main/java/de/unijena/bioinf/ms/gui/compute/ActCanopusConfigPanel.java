package de.unijena.bioinf.ms.gui.compute;

import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.subtools.canopus.CanopusOptions;
import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.gui.dialogs.ExceptionDialog;
import de.unijena.bioinf.ms.gui.dialogs.WarningDialog;
import de.unijena.bioinf.ms.gui.mainframe.MainFrame;
import de.unijena.bioinf.ms.properties.PropertyManager;
import org.jbibtex.BibTeXEntry;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */

public class ActCanopusConfigPanel extends ActivatableConfigPanel<SubToolConfigPanel<CanopusOptions>> {
    public ActCanopusConfigPanel() {
        super("CANOPUS", Icons.BUG_32, true, () -> {
            SubToolConfigPanel<CanopusOptions> p = new SubToolConfigPanel<>(CanopusOptions.class) {
            };
            p.add(new JLabel("Parameter-Free! Nothing to set up here. =)"));
//            l.setBorder(BorderFactory.createEmptyBorder(0, GuiUtils.LARGE_GAP, 0, 0));
            return p;
        });
    }

    @Override
    protected void setComponentsEnabled(boolean enabled) {
        super.setComponentsEnabled(enabled);
        if (enabled && !PropertyManager.getBoolean("de.unijena.bioinf.sirius.ui.cite.classyfire", false)) {
            new CitationDialog(MainFrame.MF);
        }
    }

    protected static class CitationDialog extends WarningDialog {
        public static final String KEY = "djoumbou-feunang16classyfire";
        public static String MESSAGE = "<html><h3> CANOPUS would not have been possible without the awesome work of the ClassyFire people.</h3> " +
                "So please also cite the ClassyFire publication when using CANOPUS:<br><br><p>"
                + ApplicationCore.BIBTEX.getEntryAsHTML(KEY, false, false).map(s -> s.replace("beck, ", "beck,<br>")).orElse(null)
                + "</p></html>";

        public CitationDialog(Window owner) {
            super(owner, MESSAGE, "de.unijena.bioinf.sirius.ui.cite.classyfire");
            /*textLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
            textLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    try {
                        Desktop.getDesktop().browse(new URI("https://doi.org/"+ ApplicationCore.BIBTEX.getEntry(KEY).map(en -> en.getField(BibTeXEntry.KEY_DOI).toUserString()).orElse("")));
                    } catch (URISyntaxException | IOException ex) {
                        LoggerFactory.getLogger(getClass()).error("Could not open link in Browser!",e);
                        new ExceptionDialog(MainFrame.MF, "Could not open link in Browser!");
                    }
                }
            });*/

        }


        @Override
        protected void decorateButtonPanel(JPanel boxedButtonPanel) {
            final JButton cite = new JButton("Copy Citation");
            cite.setToolTipText("Copy citations text to clipboard.");
            cite.addActionListener(e -> {
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                ApplicationCore.BIBTEX.getEntryAsPlainText(KEY, false).ifPresent(s -> {
                    final StringSelection sel = new StringSelection(s);
                    clipboard.setContents(sel, null);
                });

            });

            final JButton bib = new JButton("Copy BibTeX");
            bib.setToolTipText("Copy citations bibtex entry to clipboard.");
            bib.addActionListener(e -> {
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                ApplicationCore.BIBTEX.getEntryAsBibTex(KEY).ifPresent(s -> {
                    final StringSelection sel = new StringSelection(s);
                    clipboard.setContents(sel, null);
                });
            });

            boxedButtonPanel.add(bib);
            boxedButtonPanel.add(cite);
            super.decorateButtonPanel(boxedButtonPanel);
        }
    }

}
