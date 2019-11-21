package de.unijena.bioinf.ms.gui.actions;

import de.unijena.bioinf.ms.gui.io.LoadController;
import de.unijena.bioinf.ms.gui.configs.Icons;

import javax.swing.*;
import java.awt.event.ActionEvent;

import static de.unijena.bioinf.ms.gui.mainframe.MainFrame.MF;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class ImportCompoundAction extends AbstractAction {
    public ImportCompoundAction() {
        super("Import Compound");
        putValue(Action.LARGE_ICON_KEY, Icons.DOC_32);
        putValue(Action.SMALL_ICON, Icons.ADD_DOC_16);
        putValue(Action.SHORT_DESCRIPTION, "Import measurements of a single compound and define parameters.");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        LoadController lc = new LoadController(MF);
        lc.showDialog();
    }
}
