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

import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.gui.dialogs.QuestionDialog;
import de.unijena.bioinf.ms.gui.dialogs.StacktraceDialog;
import io.sirius.ms.sdk.model.ProjectInfoOptField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
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
        putValue(Action.LARGE_ICON_KEY, Icons.FOLDER_CLOSE.derive(32,32));
        putValue(Action.SHORT_DESCRIPTION, "Save (copy) the current project to a new location.");
        setEnabled(true);

        //add action list Listener for button activity
        gui.getProjectManager().INSTANCE_LIST.addListEventListener(listChanges -> setEnabled(!listChanges.getSourceList().isEmpty()));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        ProjectCreateAction.openDialog(mainFrame, "Save", this::copyProject);
    }

    public void copyProject(@NotNull String projectId, @NotNull Path projectPath) {
        copyProject(projectId, projectPath, null);
    }

    public void copyProject(@NotNull String projectId, @NotNull Path projectPath, @Nullable Boolean closeCurrent) {
        try {
            String nuPid = Jobs.runInBackgroundAndLoad(gui.getMainFrame(), "Copying Project...", () ->
                    gui.applySiriusClient((c, pid) -> c.projects().copyProject(
                            pid, projectPath.toAbsolutePath().toString(), projectId,
                            List.of(ProjectInfoOptField.NONE)
                    ).getProjectId())
            ).awaitResult();

            openProjectByID(nuPid, closeCurrent);
        } catch (ExecutionException e) {
            LoggerFactory.getLogger(getClass()).error("Error when creating new project!", e);
            Jobs.runEDTLater(() -> new StacktraceDialog(gui.getMainFrame(), "Error when creating new project!", e));
        }
    }

    public void openProjectByID(@NotNull String projectId, @Nullable Boolean closeCurrent) {
        final boolean close =
                Objects.requireNonNullElseGet(closeCurrent, () -> new OpenInNewProjectDialog(
                        gui.getMainFrame(), "Open Project", openNewWindowQuestion(), dontAskKey()).isSuccess());

            Jobs.runInBackgroundAndLoad(gui.getMainFrame(), "Loading new Project Window...", () -> {
                if (close){
                    gui.getSiriusClient().gui().openGui(projectId);
                    gui.close();
                } else {
                    gui.getSiriusClient().projects().closeProject(projectId);
                }
            });
    }

    @Override
    protected String openNewWindowQuestion() {
        return "<html><body>Would you like to open the newly saved project, or continue working on the current one? </body></html>";

    }

    @Override
    protected String dontAskKey() {
        return DONT_ASK_NEW_WINDOW_COPY_KEY;
    }

    protected class OpenInNewProjectDialog extends QuestionDialog {

        public OpenInNewProjectDialog(Window owner, String title, String question, String propertyKey) {
            super(owner, title, question, propertyKey);
        }

        @Override
        protected void decorateButtonPanel(JPanel boxedButtonPanel) {
            super.decorateButtonPanel(boxedButtonPanel);
            ok.setText("New Project");
            cancel.setText("Current Project");
        }
    }
}
