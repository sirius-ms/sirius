package de.unijena.bioinf.sirius.gui.actions;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 29.01.17.
 */

import de.unijena.bioinf.sirius.gui.dialogs.AboutDialog;
import de.unijena.bioinf.sirius.gui.configs.Icons;

import javax.swing.*;
import java.awt.event.ActionEvent;

import static de.unijena.bioinf.sirius.gui.mainframe.MainFrame.MF;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class ShowAboutDialogAction extends AbstractAction {
    public ShowAboutDialogAction() {
        super("About");
        putValue(Action.LARGE_ICON_KEY, Icons.INFO_32);
        putValue(Action.SHORT_DESCRIPTION,"Information about this Software and how to cite it");
    }
    @Override
    public void actionPerformed(ActionEvent e) {
        new AboutDialog(MF);
    }
}
