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
import de.unijena.bioinf.ms.gui.dialogs.ErrorWithDetailsDialog;
import de.unijena.bioinf.ms.gui.dialogs.WarningDialog;
import de.unijena.bioinf.ms.gui.io.filefilter.NoSQLProjectFileFilter;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.jjobs.JobProgressEvent;
import de.unijena.bioinf.jjobs.TinyBackgroundJJob;
import io.sirius.ms.sdk.model.Job;
import io.sirius.ms.sdk.model.JobOptField;
import io.sirius.ms.sdk.model.JobProgress;
import io.sirius.ms.sdk.model.ProjectInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
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

    /** How often the server is asked how far the opening has come. Often enough to look live, rarely enough to be free. */
    private static final long POLL_MILLIS = 300;
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
            String pid = Jobs.runInBackgroundAndLoad(gui.getMainFrame(), "Opening '" + projectPath.getFileName() + "'...",
                    new TinyBackgroundJJob<String>() {
                        @Override
                        protected String compute() throws Exception {
                            ProjectInfo project = gui.getSiriusClient().projects().getProjects().stream()
                                    .filter(p -> p.getLocation() != null && projectPath.equals(Path.of(p.getLocation())))
                                    .findFirst().orElse(null);
                            if (project != null)
                                return project.getProjectId();

                            return openAndFollow(pidInput, projectPath);
                        }

                        /**
                         * Opens the project as a background job on the server and follows it until it is done.
                         * <p>
                         * A project written by an older SIRIUS is converted while it opens, which on a large one
                         * takes minutes. Asking for it synchronously would leave this dialog saying "Opening
                         * Project..." for all of it, with no way to tell a slow conversion from a hang - so the
                         * server's own progress is shown instead, message and all.
                         */
                        private String openAndFollow(String projectId, Path path) throws InterruptedException {
                            Job opening = gui.getSiriusClient().projects().openProjectAsJob(projectId,
                                    path.toAbsolutePath().toString(), List.of(JobOptField.PROGRESS));
                            String openedId = opening.getId();

                            while (true) {
                                checkForInterruption();
                                JobProgress progress = opening.getProgress();
                                if (progress != null) {
                                    String message = Objects.requireNonNullElse(progress.getMessage(),
                                            "Opening project ...");
                                    if (Boolean.TRUE.equals(progress.isIndeterminate())) {
                                        // A step that cannot count (e.g. the index build) must not be drawn
                                        // as a bar stuck at zero.
                                        updateProgress(new JobProgressEvent(this, message));
                                    } else {
                                        // The bar shows (progress - min) / (max - min), so min has to stay at
                                        // the bottom of the range rather than follow the progress - the server
                                        // counts from zero. Reported whether or not the phase named itself,
                                        // since a bar that only moves when there is a message looks stuck.
                                        long done = orZero(progress.getCurrentProgress());
                                        long total = Math.max(1, orZero(progress.getMaxProgress()));
                                        updateProgress(0, total, Math.min(done, total), message);
                                    }
                                    if (progress.getState() == io.sirius.ms.sdk.model.JobState.DONE)
                                        return openedId;
                                    if (progress.getState() == io.sirius.ms.sdk.model.JobState.FAILED
                                            || progress.getState() == io.sirius.ms.sdk.model.JobState.CANCELED)
                                        throw new IllegalStateException("Could not open project '" + openedId
                                                + "': " + Objects.requireNonNullElse(progress.getErrorMessage(),
                                                String.valueOf(progress.getState())));
                                }
                                Thread.sleep(POLL_MILLIS);
                                opening = gui.getSiriusClient().projects()
                                        .getOpenJob(openedId, List.of(JobOptField.PROGRESS));
                            }
                        }

                        private long orZero(Long value) {
                            return value == null ? 0L : value;
                        }
                    }
            ).awaitResult();

            openProjectByID(pid, closeCurrent);
        } catch (ExecutionException e) {
            LoggerFactory.getLogger(getClass()).error("Error when opening project!", e);
            Jobs.runEDTLater(() -> new ErrorWithDetailsDialog(gui.getMainFrame(), gui.getSiriusClient().unwrapErrorMessage(e), e));
        }
    }

    public synchronized void openProjectByID(String projectId) {
        openProjectByID(projectId, null);
    }

    public synchronized void openProjectByID(@NotNull String projectId, @Nullable Boolean closeCurrent) {
        final boolean close =
                Objects.requireNonNullElseGet(closeCurrent, () -> new OpenInNewWindowDialog(
                        gui.getMainFrame(), "Open Project", openNewWindowQuestion(), dontAskKey()).isCancel());

        Jobs.runInBackgroundAndLoad(gui.getMainFrame(), "Loading Project...", () -> {
            gui.getSiriusClient().gui().openGui(projectId);
            if (close)
                gui.close();


        }).getResult();
    }

    protected String dontAskKey() {
        return DONT_ASK_NEW_WINDOW_OPEN_KEY;
    }

    protected String openNewWindowQuestion() {
        return "<html><body>Would you like to open this project in a <b>new window</b> or in the <b>current window</b>? </body></html>";
    }

    private class OpenInNewWindowDialog extends QuestionDialog {

        public OpenInNewWindowDialog(Window owner, String title, String question, String propertyKey) {
            super(owner, title, question, propertyKey);
        }

        @Override
        protected void decorateButtonPanel(JPanel boxedButtonPanel) {
            super.decorateButtonPanel(boxedButtonPanel);
            ok.setText("New Window");
            cancel.setText("This Window");
        }
    }
}
