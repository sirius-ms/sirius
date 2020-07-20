package de.unijena.bioinf.ms.gui.actions;

import de.unijena.bioinf.ms.frontend.core.SiriusProperties;
import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.gui.dialogs.WarningDialog;
import de.unijena.bioinf.ms.gui.io.filefilter.ProjectArchivedFilter;
import de.unijena.bioinf.ms.gui.io.filefilter.ProjectDirectoryFilter;
import de.unijena.bioinf.ms.properties.PropertyManager;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Objects;

import static de.unijena.bioinf.ms.gui.mainframe.MainFrame.MF;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class ProjectSaveAction extends AbstractAction {

    public ProjectSaveAction() {
        super("SaveAs");
        putValue(Action.LARGE_ICON_KEY, Icons.FOLDER_CLOSE_32);
        putValue(Action.SHORT_DESCRIPTION, "Save (copy) the current project to a new location.");
        setEnabled(true);

        //add action list Listener for button activity
//        MF.ps().COMPOUNT_LIST.addListEventListener(listChanges -> setEnabled(!listChanges.getSourceList().isEmpty()));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        JFileChooser jfc = new JFileChooser();
        jfc.setCurrentDirectory(PropertyManager.getFile(SiriusProperties.DEFAULT_SAVE_DIR_PATH));
        jfc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        jfc.setAcceptAllFileFilterUsed(false);
        jfc.addChoosableFileFilter(new ProjectDirectoryFilter());
        jfc.addChoosableFileFilter(new ProjectArchivedFilter());


        while (true) {
            final int state = jfc.showSaveDialog(MF);
            if (state == JFileChooser.CANCEL_OPTION || state == JFileChooser.ERROR_OPTION)
                break;
            File selFile = jfc.getSelectedFile();

            if (jfc.getFileFilter() instanceof ProjectArchivedFilter)
                if (!selFile.getName().endsWith(".sirius"))
                    selFile = new File(selFile.getParentFile(),selFile.getName() + ".sirius");



            if (!selFile.exists() || selFile.isDirectory() && Objects.requireNonNull(selFile.list()).length == 0) {
                SiriusProperties.
                        setAndStoreInBackground(SiriusProperties.DEFAULT_SAVE_DIR_PATH, selFile.getParentFile().getAbsolutePath());

                MF.ps().saveAs(selFile.toPath());
                break;
            } else {
                new WarningDialog(MF, "'" + selFile.getAbsolutePath() + "' does not contain valid SIRIUS project.");
            }
        }
    }
}
