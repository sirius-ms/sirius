package de.unijena.bioinf.ms.gui.compute;

import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.subtools.canopus.CanopusOptions;
import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.gui.dialogs.CitationDialog;
import de.unijena.bioinf.ms.gui.mainframe.MainFrame;
import de.unijena.bioinf.ms.properties.PropertyManager;

import javax.swing.*;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */

public class ActCanopusConfigPanel extends ActivatableConfigPanel<SubToolConfigPanel<CanopusOptions>> {
    public static final String BIBTEX_KEY = "djoumbou-feunang16classyfire";

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
            new CitationDialog(MainFrame.MF, BIBTEX_KEY, () ->
                    "<html><h3> CANOPUS would not have been possible without the awesome work of the ClassyFire people.</h3> "
                            + "So please also cite the ClassyFire publication when using CANOPUS:<br><br><p>"
                            + ApplicationCore.BIBTEX.getEntryAsHTML(BIBTEX_KEY, false, true).map(s -> s.replace("beck, ", "beck,<br>")).orElse(null)
                            + "</p></html>");
        }
    }

}
