package de.unijena.bioinf.ms.gui.actions;

import de.unijena.bioinf.babelms.filefilter.SupportedSaveFileFilter;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.ms.frontend.core.SiriusProperties;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.gui.dialogs.ErrorReportDialog;
import de.unijena.bioinf.ms.gui.dialogs.FilePresentDialog;
import de.unijena.bioinf.ms.gui.utils.ReturnValue;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;

import static de.unijena.bioinf.ms.gui.mainframe.MainFrame.MF;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class SaveWorkspaceAction extends AbstractAction {

    public SaveWorkspaceAction() {
        super("Save Project");
        putValue(Action.LARGE_ICON_KEY, Icons.FOLDER_CLOSE_32);
        putValue(Action.SHORT_DESCRIPTION, "Save current Project to file");
        setEnabled(!MF.getPS().COMPOUNT_LIST.isEmpty());

        //add Workspace Listener for button activity
        MF.getPS().COMPOUNT_LIST.addListEventListener(listChanges -> setEnabled(!listChanges.getSourceList().isEmpty()));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        JFileChooser jfc = new JFileChooser();

        jfc.setCurrentDirectory(PropertyManager.getFile(SiriusProperties.DEFAULT_SAVE_FILE_PATH));
        jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        jfc.setAcceptAllFileFilterUsed(false);
        jfc.addChoosableFileFilter(new SupportedSaveFileFilter());

        File selectedFile = null;

        while (selectedFile == null) {
            int returnval = jfc.showSaveDialog(MF);
            if (returnval == JFileChooser.APPROVE_OPTION) {
                File selFile = jfc.getSelectedFile();

                {
                    final String path = selFile.getParentFile().getAbsolutePath();
                    Jobs.runInBackround(() ->
                            SiriusProperties.SIRIUS_PROPERTIES_FILE().
                                    setAndStoreProperty(SiriusProperties.DEFAULT_SAVE_FILE_PATH, path)
                    );
                }

                String name = selFile.getName();
                if (!selFile.getAbsolutePath().endsWith(".sirius")) {
                    selFile = new File(selFile.getAbsolutePath() + ".sirius");
                }

                if (selFile.exists()) {
                    FilePresentDialog fpd = new FilePresentDialog(MF, selFile.getName());
                    ReturnValue rv = fpd.getReturnValue();
                    if (rv == ReturnValue.Success) {
                        selectedFile = selFile;
                    }
                } else {
                    selectedFile = selFile;
                }
            } else {
                break;
            }
        }

        if (selectedFile != null) {
            try {
                MF.getPS().exportAsProjectSpace(selectedFile);
            } catch (Exception e2) {
                new ErrorReportDialog(MF, e2.getMessage());
            }
        }
    }
}
