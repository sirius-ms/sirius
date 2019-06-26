package de.unijena.bioinf.ms.gui.actions;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 29.01.17.
 */

import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.ms.frontend.core.SiriusProperties;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.gui.dialogs.ImportWorkspaceDialog;
import de.unijena.bioinf.babelms.GuiProjectSpaceIO;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;

import static de.unijena.bioinf.ms.gui.mainframe.MainFrame.MF;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class LoadWorkspaceAction extends AbstractAction {

    public LoadWorkspaceAction() {
        super("Load Workspace");
        putValue(Action.LARGE_ICON_KEY, Icons.FOLDER_OPEN_32);
        putValue(Action.SHORT_DESCRIPTION, "Load all compounds and computed results from a previously saved workspace.");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        JFileChooser jfc = new JFileChooser();
        jfc.setCurrentDirectory(PropertyManager.getFile(SiriusProperties.DEFAULT_SAVE_FILE_PATH));
        jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        jfc.setAcceptAllFileFilterUsed(false);
        jfc.addChoosableFileFilter(GuiProjectSpaceIO.SAVE_FILE_FILTER);

        int returnVal = jfc.showOpenDialog(MF);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            final File selFile = jfc.getSelectedFile();
            Jobs.runInBackround(() ->
                    SiriusProperties.SIRIUS_PROPERTIES_FILE().
                            setAndStoreProperty(SiriusProperties.DEFAULT_SAVE_FILE_PATH, selFile.getParentFile().getAbsolutePath())
            );

            ImportWorkspaceDialog workspaceDialog = new ImportWorkspaceDialog(MF);
            workspaceDialog.start();

            if (workspaceDialog.hasImportMode())
                GuiProjectSpaceIO.importFromProjectSpace(workspaceDialog.getImportMode(), selFile);
        }
    }
}
