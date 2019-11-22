package de.unijena.bioinf.ms.gui.actions;

import de.unijena.bioinf.ms.frontend.core.SiriusProperties;
import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.gui.dialogs.WarningDialog;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.projectspace.ProjectSpaceIO;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;

import static de.unijena.bioinf.ms.gui.mainframe.MainFrame.MF;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class ProjectOpenAction extends AbstractAction {

    public ProjectOpenAction() {
        super("Open Project");
        putValue(Action.LARGE_ICON_KEY, Icons.FOLDER_OPEN_32);
        putValue(Action.SHORT_DESCRIPTION, "Open previously saved project (directory). This closes the current Project.");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        JFileChooser jfc = new JFileChooser();
        jfc.setCurrentDirectory(PropertyManager.getFile(SiriusProperties.DEFAULT_SAVE_DIR_PATH));
        jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        jfc.setAcceptAllFileFilterUsed(false);

        while (true) {
            int state = jfc.showOpenDialog(MF);
            if (state == JFileChooser.CANCEL_OPTION || state == JFileChooser.ERROR_OPTION)
                break;

            final File selFile = jfc.getSelectedFile();
            if (selFile.isDirectory() && ProjectSpaceIO.isExistingProjectspaceDirectory(selFile.toPath())) {
                SiriusProperties.
                        setAndStoreInBackground(SiriusProperties.DEFAULT_LOAD_DIALOG_PATH, selFile.getParentFile().getAbsolutePath());

                MF.getPS().openProjectSpace(selFile.toPath());

                break;
            } else {
                new WarningDialog(MF, "'" + selFile.getAbsolutePath() + "' does not contain valid SIRIUS project.");
            }
        }
    }
}
