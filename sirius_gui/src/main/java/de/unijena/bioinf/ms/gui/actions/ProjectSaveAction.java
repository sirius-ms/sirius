/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.gui.actions;

import de.unijena.bioinf.ms.frontend.core.SiriusProperties;
import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.gui.dialogs.QuestionDialog;
import de.unijena.bioinf.ms.gui.dialogs.StacktraceDialog;
import de.unijena.bioinf.ms.gui.dialogs.WarningDialog;
import de.unijena.bioinf.ms.gui.io.filefilter.ProjectArchivedFilter;
import de.unijena.bioinf.ms.gui.io.filefilter.ProjectDirectoryFilter;
import de.unijena.bioinf.ms.nightsky.sdk.model.ProjectInfoOptField;
import de.unijena.bioinf.ms.properties.PropertyManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;


/**
 * @author Markus Fleischauer
 */
public class ProjectSaveAction extends ProjectOpenAction {
    public static final String DONT_ASK_NEW_WINDOW_COPY_KEY = "de.unijena.bioinf.sirius.dragdrop.newWindowCopy.dontAskAgain";


    public ProjectSaveAction(SiriusGui gui) {
        super("Save as", gui);
        putValue(Action.LARGE_ICON_KEY, Icons.FOLDER_CLOSE_32);
        putValue(Action.SHORT_DESCRIPTION, "Save (copy) the current project to a new location.");
        setEnabled(true);

        //add action list Listener for button activity
        gui.getProjectManager().INSTANCE_LIST.addListEventListener(listChanges -> setEnabled(!listChanges.getSourceList().isEmpty()));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        saveAs();
    }

    public void saveAs() {
        JFileChooser jfc = new JFileChooser();
        jfc.setCurrentDirectory(PropertyManager.getFile(SiriusProperties.DEFAULT_SAVE_DIR_PATH));
        jfc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        jfc.setAcceptAllFileFilterUsed(false);
        jfc.addChoosableFileFilter(new ProjectDirectoryFilter());

        while (true) {
            final int state = jfc.showDialog(mainFrame, "Save");
            if (state == JFileChooser.CANCEL_OPTION || state == JFileChooser.ERROR_OPTION)
                break;
            File selFile = jfc.getSelectedFile();

            if (jfc.getFileFilter() instanceof ProjectArchivedFilter)
                if (!selFile.getName().endsWith(".sirius"))
                    selFile = new File(selFile.getParentFile(), selFile.getName() + ".sirius");

            if (!selFile.exists() || selFile.isDirectory() && Objects.requireNonNull(selFile.list()).length == 0) {
                SiriusProperties.
                        setAndStoreInBackground(SiriusProperties.DEFAULT_SAVE_DIR_PATH, selFile.getParentFile().getAbsolutePath());
                try {
                    copyProject(selFile.toPath());
                } catch (Exception e2) {
                    new StacktraceDialog(mainFrame, e2.getMessage(), e2);
                }
                break;
            } else {
                new WarningDialog(mainFrame, "'" + selFile.getAbsolutePath() + "' does not contain valid SIRIUS project.");
            }
        }


    }

    public void copyProject(@NotNull Path projectPath) {
        copyProject(projectPath, null);
    }

    public void copyProject(@NotNull Path projectPath, @Nullable Boolean closeCurrent) {
        try {
            String nuPid = Jobs.runInBackgroundAndLoad(gui.getMainFrame(), "Copying Project...", () ->
                    gui.applySiriusClient((c, pid) -> c.projects().copyProjectSpace(
                            pid, projectPath.toAbsolutePath().toString(), projectPath.getFileName().toString(),
                            List.of(ProjectInfoOptField.NONE)
                    ).getProjectId())
            ).awaitResult();

            openProject(nuPid, closeCurrent);
        } catch (ExecutionException e) {
            LoggerFactory.getLogger(getClass()).error("Error when creating new project!", e);
            Jobs.runEDTLater(() -> new StacktraceDialog(gui.getMainFrame(), "Error when creating new project!", e));
        }
    }

    public void openProject(@NotNull String projectId, @Nullable Boolean closeCurrent) {
        final boolean close =
                Objects.requireNonNullElseGet(closeCurrent, () -> new QuestionDialog(
                        gui.getMainFrame(), "Open Project", openNewWindowQuestion(), dontAskKey()).isSuccess());

            Jobs.runInBackgroundAndLoad(gui.getMainFrame(), "Loading new Project Window...", () -> {
                if (close){
                    gui.getSiriusClient().gui().openGui(projectId);
                    gui.close();
                } else {
                    gui.getSiriusClient().projects().closeProjectSpace(projectId);
                }
            });
    }

    @Override
    protected String openNewWindowQuestion() {
        return "<html><body>Do you wish to open the newly saved project (new location)? <br> Otherwise, the current one will kept open. </body></html>";

    }

    @Override
    protected String dontAskKey() {
        return DONT_ASK_NEW_WINDOW_COPY_KEY;
    }
}
