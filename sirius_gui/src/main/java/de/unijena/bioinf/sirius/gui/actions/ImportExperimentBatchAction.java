package de.unijena.bioinf.sirius.gui.actions;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 29.01.17.
 */

import de.unijena.bioinf.sirius.gui.filefilter.SupportedBatchDataFormatFilter;
import de.unijena.bioinf.sirius.gui.mainframe.Workspace;
import de.unijena.bioinf.sirius.gui.configs.Icons;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;

import static de.unijena.bioinf.sirius.gui.mainframe.MainFrame.MF;
import static de.unijena.bioinf.sirius.gui.mainframe.Workspace.CONFIG_STORAGE;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class ImportExperimentBatchAction extends AbstractAction {

    public ImportExperimentBatchAction() {
        super("Batch Import");
        putValue(Action.LARGE_ICON_KEY, Icons.DOCS_32);
        putValue(Action.SMALL_ICON, Icons.BATCH_DOC_16);
        putValue(Action.SHORT_DESCRIPTION,"Import measurements of several compounds");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        JFileChooser chooser = new JFileChooser(CONFIG_STORAGE.getDefaultLoadDialogPath());
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        chooser.setMultiSelectionEnabled(true);
        chooser.addChoosableFileFilter(new SupportedBatchDataFormatFilter());
        chooser.setAcceptAllFileFilterUsed(false);
        int returnVal = chooser.showOpenDialog(MF);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File[] files = chooser.getSelectedFiles();
            CONFIG_STORAGE.setDefaultLoadDialogPath(files[0].getParentFile());
            Workspace.importOneExperimentPerFile(files);
        }
    }
}
