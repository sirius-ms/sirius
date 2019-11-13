package de.unijena.bioinf.ms.gui.actions;

import de.unijena.bioinf.ms.frontend.core.SiriusProperties;
import de.unijena.bioinf.ms.frontend.io.projectspace.InstanceBeanFactory;
import de.unijena.bioinf.ms.frontend.io.projectspace.ProjectSpaceManager;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.projectspace.ProjectSpaceIO;
import de.unijena.bioinf.projectspace.SiriusProjectSpace;

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
            jfc.showOpenDialog(MF);
            final File selFile = jfc.getSelectedFile();
            if (selFile.isDirectory() && ProjectSpaceIO.isExistingProjectspaceDirectory(selFile)) {
                Jobs.runInBackground(() ->
                        SiriusProperties.SIRIUS_PROPERTIES_FILE().
                                setAndStoreProperty(SiriusProperties.DEFAULT_SAVE_DIR_PATH, selFile.getParentFile().getAbsolutePath())
                );

                final ProjectSpaceManager psm = Jobs.runInBackgroundAndLoad(MF, "Opening new Project...", () -> {
                    SiriusProjectSpace ps = new ProjectSpaceIO(ProjectSpaceManager.newDefaultConfig()).openExistingProjectSpace(selFile);
                    return new ProjectSpaceManager(ps, new InstanceBeanFactory(), null, null);
                }).getResult();

                Jobs.runInBackgroundAndLoad(MF, "Importing new Project...", () -> {
                    //todo we need to cancel all running computations here.
                    System.out.println("todo we need to cancel all running computations here!");
                    MF.getPS().changeProjectSpace(psm);
                });

                break;
            }
        }
    }
}
