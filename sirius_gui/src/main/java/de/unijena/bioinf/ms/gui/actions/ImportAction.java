package de.unijena.bioinf.ms.gui.actions;

import de.unijena.bioinf.ms.gui.io.filefilter.SupportedArchivedProjectFilter;
import de.unijena.bioinf.ms.gui.io.filefilter.SupportedBatchDataFormatFilter;
import de.unijena.bioinf.ms.frontend.core.SiriusProperties;
import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.properties.PropertyManager;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;

import static de.unijena.bioinf.ms.gui.mainframe.MainFrame.MF;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class ImportAction extends AbstractAction {

    public ImportAction() {
        super("Import");
        putValue(Action.LARGE_ICON_KEY, Icons.DOCS_32);
        putValue(Action.SMALL_ICON, Icons.BATCH_DOC_16);
        putValue(Action.SHORT_DESCRIPTION, "<html>" +
                "<p>Import measurements of:</p>" +
                        "<ul style=\"list-style-type:none;\">" +
                        "  <li>- Multiple compounds (.ms, .mgf)</li>" +
                        "  <li>- LC-MS/MS runs (.mzML, .mzXml)</li>" +
                        "  <li>- Projects (.sirius, directory)</li>" +
                        "</ul>" +
                "<p>into the current project-space. (Same as drag and drop)</p>" +
                "</html>");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        JFileChooser chooser = new JFileChooser(PropertyManager.getFile(SiriusProperties.DEFAULT_LOAD_DIALOG_PATH));
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        chooser.setMultiSelectionEnabled(true);
        chooser.addChoosableFileFilter(new SupportedBatchDataFormatFilter());
        chooser.addChoosableFileFilter(new SupportedArchivedProjectFilter());
        chooser.setAcceptAllFileFilterUsed(false);
        int returnVal = chooser.showOpenDialog(MF);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File[] files = chooser.getSelectedFiles();
            if (files.length > 0) {
                SiriusProperties.
                        setAndStoreInBackground(SiriusProperties.DEFAULT_LOAD_DIALOG_PATH, files[0].getParentFile().getAbsolutePath());
                MF.getPS().importOneExperimentPerLocation(files);
            }

        }
    }
}
