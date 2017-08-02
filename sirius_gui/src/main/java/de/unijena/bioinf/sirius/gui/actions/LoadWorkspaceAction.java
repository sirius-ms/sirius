package de.unijena.bioinf.sirius.gui.actions;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 29.01.17.
 */

import de.unijena.bioinf.sirius.gui.mainframe.Workspace;
import de.unijena.bioinf.sirius.gui.configs.Icons;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Arrays;

import static de.unijena.bioinf.sirius.gui.mainframe.MainFrame.MF;
import static de.unijena.bioinf.sirius.gui.mainframe.Workspace.CONFIG_STORAGE;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class LoadWorkspaceAction extends AbstractAction {

    public LoadWorkspaceAction() {
        super("Load Workspace");
        putValue(Action.LARGE_ICON_KEY, Icons.FOLDER_OPEN_32);
        putValue(Action.SHORT_DESCRIPTION,"Load all compounds and computed results from a previously saved workspace.");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        JFileChooser jfc = new JFileChooser();
        jfc.setCurrentDirectory(CONFIG_STORAGE.getDefaultSaveFilePath());
        jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        jfc.setAcceptAllFileFilterUsed(false);
        jfc.addChoosableFileFilter(new Workspace.SiriusSaveFileFilter());

        int returnVal = jfc.showOpenDialog(MF);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File selFile = jfc.getSelectedFile();
            CONFIG_STORAGE.setDefaultSaveFilePath(selFile.getParentFile());
            Workspace.importWorkspace(Arrays.asList(selFile));
        }
    }
}
