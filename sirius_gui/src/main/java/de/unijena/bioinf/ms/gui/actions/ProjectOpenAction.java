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

import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.ms.frontend.core.SiriusProperties;
import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.gui.dialogs.QuestionDialog;
import de.unijena.bioinf.ms.gui.dialogs.StacktraceDialog;
import de.unijena.bioinf.ms.gui.dialogs.WarningDialog;
import de.unijena.bioinf.ms.gui.io.filefilter.NoSQLProjectFileFilter;
import io.sirius.ms.sdk.model.ProjectInfo;
import io.sirius.ms.sdk.model.ProjectInfoOptField;
import de.unijena.bioinf.ms.properties.PropertyManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import static de.unijena.bioinf.ms.persistence.storage.SiriusProjectDocumentDatabase.SIRIUS_PROJECT_SUFFIX;

/**
 * @author Markus Fleischauer
 */
public class ProjectOpenAction extends AbstractGuiAction {
    public static final String DONT_ASK_NEW_WINDOW_OPEN_KEY = "de.unijena.bioinf.sirius.dragdrop.newWindowOpen.dontAskAgain";

    //todo: should be a singelton action


    public ProjectOpenAction(SiriusGui gui) {
        this("Open", gui);
        putValue(Action.LARGE_ICON_KEY, Icons.FOLDER_OPEN.derive(32,32));
        putValue(Action.SHORT_DESCRIPTION, "Open a previously saved project.");
    }

    protected ProjectOpenAction(String name, SiriusGui gui) {
        super(name, gui);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        JFileChooser jfc = new JFileChooser();
        jfc.setCurrentDirectory(PropertyManager.getFile(SiriusProperties.DEFAULT_LOAD_DIALOG_PATH));
        jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        jfc.setAcceptAllFileFilterUsed(false);
        jfc.addChoosableFileFilter(new NoSQLProjectFileFilter());

        int state = jfc.showOpenDialog(mainFrame);

        if (state == JFileChooser.APPROVE_OPTION) {
            final Path selFile = jfc.getSelectedFile().toPath();
            if (Files.exists(selFile) && Files.isRegularFile(selFile)) {
                String projectID = selFile.getFileName().toString();
                if (!projectID.endsWith(SIRIUS_PROJECT_SUFFIX)) {
                    new WarningDialog(mainFrame, "'" + selFile.toAbsolutePath() + "' has no valid file suffix: \".sirius\" !");
                } else {
                    projectID = projectID.substring(0, projectID.length() - SIRIUS_PROJECT_SUFFIX.length());
                    Path parentDir = selFile.getParent();
                    if (Files.exists(parentDir) && Files.isDirectory(parentDir)) {
                        Jobs.runInBackground(() ->
                                SiriusProperties.SIRIUS_PROPERTIES_FILE().setAndStoreProperty(SiriusProperties.DEFAULT_LOAD_DIALOG_PATH, parentDir.toAbsolutePath().toString())
                        );
                    }
                    openProject(projectID, selFile);
                }
            } else {
                new WarningDialog(mainFrame, "'" + selFile.toAbsolutePath() + "' is no valid SIRIUS project.");
            }
        }
    }

    public synchronized void openProject(@NotNull Path projectPath, @Nullable Boolean closeCurrent) {
        String projectID = projectPath.getFileName().toString();
        if (projectID.endsWith(SIRIUS_PROJECT_SUFFIX))
            projectID = projectID.substring(0, projectID.length() - SIRIUS_PROJECT_SUFFIX.length());
        openProject(projectID, projectPath, closeCurrent);
    }

    public synchronized void openProject(@NotNull String projectID, @NotNull Path projectPath) {
        openProject(projectID, projectPath, null);
    }

    public synchronized void openProject(@NotNull String projectId, @NotNull Path projectPath, @Nullable Boolean closeCurrent) {
        try {
            String pidInput = FileUtils.sanitizeFilename(projectId);
            if (!pidInput.equals(projectId))
                LoggerFactory.getLogger(getClass()).warn("Changed pid from '{}' to '{};, to respect name restrictions", projectId, pidInput);
            String pid = Jobs.runInBackgroundAndLoad(gui.getMainFrame(), "Opening Project...", () -> {
                        ProjectInfo project = gui.getSiriusClient().projects().getProjectSpaces().stream()
                                .filter(p -> p.getLocation() != null && projectPath.equals(Path.of(p.getLocation()))).findFirst().orElse(null);

                        if (project == null)
                            project = gui.getSiriusClient().projects().openProjectSpace(pidInput, projectPath.toAbsolutePath().toString(), List.of(ProjectInfoOptField.NONE));

                        return project.getProjectId();
                    }

            ).awaitResult();

            openProjectByID(pid, closeCurrent);
        } catch (ExecutionException e) {
            LoggerFactory.getLogger(getClass()).error("Error when opening project!", e);
            Jobs.runEDTLater(() -> new StacktraceDialog(gui.getMainFrame(), "Error when opening project!", e));
        }


    }

    public synchronized void openProjectByID(String projectId) {
        openProjectByID(projectId, null);
    }

    public synchronized void openProjectByID(@NotNull String projectId, @Nullable Boolean closeCurrent) {
        final boolean close =
                Objects.requireNonNullElseGet(closeCurrent, () -> new QuestionDialog(
                        gui.getMainFrame(), "Open Project", openNewWindowQuestion(), dontAskKey()).isCancel());

        Jobs.runInBackgroundAndLoad(gui.getMainFrame(), "Loading Project...", () -> {
            gui.getSiriusClient().gui().openGui(projectId);
            if (close)
                gui.close();


        }).getResult();
        if (!close) // it would be better to bring the new gui to front via the API
            Jobs.runEDTLater(() -> gui.getMainFrame().toBack());
    }

    protected String dontAskKey() {
        return DONT_ASK_NEW_WINDOW_OPEN_KEY;
    }

    protected String openNewWindowQuestion() {
        return "<html><body>Do you wish to open the Project in an additional Window? <br> Otherwise, the current one will be replaced. </body></html>";
    }


}
