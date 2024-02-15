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
import de.unijena.bioinf.ms.nightsky.sdk.model.PageProjectInfo;
import de.unijena.bioinf.ms.nightsky.sdk.model.ProjectInfo;
import de.unijena.bioinf.ms.nightsky.sdk.model.ProjectInfoOptField;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.projectspace.ProjectSpaceIO;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * @author Markus Fleischauer
 */
public class ProjectOpenAction extends AbstractGuiAction {
    public static final String DONT_ASK_NEW_WINDOW_OPEN_KEY = "de.unijena.bioinf.sirius.dragdrop.newWindowOpen.dontAskAgain";




    public ProjectOpenAction(SiriusGui gui) {
        this("Open", gui);
        putValue(Action.LARGE_ICON_KEY, Icons.FOLDER_OPEN_32);
        putValue(Action.SHORT_DESCRIPTION, "Open previously saved project (directory or .sirius). This closes the current Project.");
    }

    protected ProjectOpenAction(String name, SiriusGui gui) {
        super(name, gui);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        JFileChooser jfc = new JFileChooser();
        jfc.setCurrentDirectory(PropertyManager.getFile(SiriusProperties.DEFAULT_LOAD_DIALOG_PATH));
        jfc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        jfc.setAcceptAllFileFilterUsed(false);
        jfc.addChoosableFileFilter(new ProjectArchivedFilter());
        jfc.addChoosableFileFilter(new ProjectDirectoryFilter());

        while (true) {
            int state = jfc.showOpenDialog(mainFrame);
            if (state == JFileChooser.CANCEL_OPTION || state == JFileChooser.ERROR_OPTION)
                break;

            final File selFile = jfc.getSelectedFile();
            if (ProjectSpaceIO.isZipProjectSpace(selFile.toPath()) || ProjectSpaceIO.isExistingProjectspaceDirectory(selFile.toPath())) {
                openProject(selFile.toPath());
                break;
            } else {
                new WarningDialog(mainFrame, "'" + selFile.getAbsolutePath() + "' does not contain valid SIRIUS project.");
            }
        }
    }

    public void openProject(@NotNull Path projectPath) {
        openProject(projectPath, null);
    }

    public void openProject(@NotNull Path projectPath, @Nullable Boolean closeCurrent) {
        try {
            String pid = Jobs.runInBackgroundAndLoad(gui.getMainFrame(), "Opening Project...", () -> {
                        PageProjectInfo projects = gui.getSiriusClient().projects()
                                .getProjectSpaces(0, Integer.MAX_VALUE, null, null, null);

                        ProjectInfo project = Optional.ofNullable(projects).map(PageProjectInfo::getContent).stream().flatMap(List::stream)
                                .filter(p -> projectPath.equals(Path.of(p.getLocation()))).findFirst().orElse(null);

                        if (project == null)
                            project = gui.getSiriusClient().projects().openProjectSpace(projectPath.getFileName()
                                    .toString(), projectPath.toAbsolutePath().toString(), List.of(ProjectInfoOptField.NONE));
                        return project.getProjectId();
                    }

            ).awaitResult();

            openProject(pid, closeCurrent);
        } catch (ExecutionException e) {
            LoggerFactory.getLogger(getClass()).error("Error when opening project!", e);
            Jobs.runEDTLater(() -> new StacktraceDialog(gui.getMainFrame(), "Error when opening project!", e));
        }


    }

    public void openProject(String projectId) {
        openProject(projectId, null);
    }

    public void openProject(@NotNull String projectId, @Nullable Boolean closeCurrent) {
        final boolean close =
                Objects.requireNonNullElseGet(closeCurrent, () -> new QuestionDialog(
                        gui.getMainFrame(), "Open Project", openNewWindowQuestion(), dontAskKey()).isAbort());

        Jobs.runInBackgroundAndLoad(gui.getMainFrame(), "Loading Project Window...", () -> {
            gui.getSiriusClient().gui().openGui(projectId, false, null);
            if (close)
                gui.close();
        });
    }

    protected String dontAskKey(){
        return DONT_ASK_NEW_WINDOW_OPEN_KEY;
    }

    protected String openNewWindowQuestion(){
        return "<html><body>Do you wish to open the Project in an additional Window? <br> Otherwise, the current one will be replaced. </body></html>";
    }


}
