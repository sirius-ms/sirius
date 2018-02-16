package de.unijena.bioinf.sirius.gui.actions;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 29.01.17.
 */

import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import de.unijena.bioinf.sirius.gui.configs.Icons;
import de.unijena.bioinf.sirius.gui.dialogs.ErrorReportDialog;
import de.unijena.bioinf.sirius.gui.dialogs.FilePresentDialog;
import de.unijena.bioinf.sirius.gui.io.WorkspaceIO;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import de.unijena.bioinf.sirius.gui.structure.ReturnValue;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.AbstractList;

import static de.unijena.bioinf.fingerid.storage.ConfigStorage.CONFIG_STORAGE;
import static de.unijena.bioinf.sirius.gui.mainframe.MainFrame.MF;
import static de.unijena.bioinf.sirius.gui.mainframe.Workspace.COMPOUNT_LIST;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class SaveWorkspaceAction extends AbstractAction {

    public SaveWorkspaceAction() {
        super("Save Workspace");
        putValue(Action.LARGE_ICON_KEY, Icons.FOLDER_CLOSE_32);
        putValue(Action.SHORT_DESCRIPTION, "Save current Workspace to file");
        setEnabled(!COMPOUNT_LIST.isEmpty());

        //Workspace Listener
        COMPOUNT_LIST.addListEventListener(new ListEventListener<ExperimentContainer>() {
            @Override
            public void listChanged(ListEvent<ExperimentContainer> listChanges) {
                setEnabled(!listChanges.getSourceList().isEmpty());
            }
        });
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        JFileChooser jfc = new JFileChooser();
        jfc.setCurrentDirectory(CONFIG_STORAGE.getDefaultSaveFilePath());
        jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        jfc.setAcceptAllFileFilterUsed(false);
        jfc.addChoosableFileFilter(WorkspaceIO.SAVE_FILE_FILTER);

        File selectedFile = null;

        while (selectedFile == null) {
            int returnval = jfc.showSaveDialog(MF);
            if (returnval == JFileChooser.APPROVE_OPTION) {
                File selFile = jfc.getSelectedFile();
                CONFIG_STORAGE.setDefaultSaveFilePath(selFile.getParentFile());

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
                WorkspaceIO io = new WorkspaceIO();
                io.newStore(new AbstractList<ExperimentContainer>() {
                    @Override
                    public ExperimentContainer get(int index) {
                        return COMPOUNT_LIST.get(index);
                    }

                    @Override
                    public int size() {
                        return COMPOUNT_LIST.size();
                    }
                }, selectedFile);
            } catch (Exception e2) {
                new ErrorReportDialog(MF, e2.getMessage());
            }

        }
    }
}
