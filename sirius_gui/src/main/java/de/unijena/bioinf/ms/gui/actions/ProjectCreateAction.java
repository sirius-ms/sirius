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
import de.unijena.bioinf.ms.gui.dialogs.StacktraceDialog;
import de.unijena.bioinf.ms.gui.io.filefilter.ProjectArchivedFilter;
import de.unijena.bioinf.ms.gui.io.filefilter.ProjectDirectoryFilter;
import de.unijena.bioinf.ms.properties.PropertyManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.ExecutionException;


/**
 * @author Markus Fleischauer
 */
public class ProjectCreateAction extends ProjectOpenAction {

    public static final String DONT_ASK_NEW_WINDOW_CREATE_KEY = "de.unijena.bioinf.sirius.dragdrop.newWindowCreate.dontAskAgain";


    public ProjectCreateAction(SiriusGui gui) {
        super("New", gui);
        putValue(Action.LARGE_ICON_KEY, Icons.ADD_DOC_32);
        putValue(Action.SHORT_DESCRIPTION, "Create a new empty project at the given location.");
        setEnabled(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        JFileChooser jfc = new JFileChooser();

        jfc.setCurrentDirectory(PropertyManager.getFile(SiriusProperties.DEFAULT_SAVE_DIR_PATH));
        jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        jfc.setAcceptAllFileFilterUsed(false);
        jfc.addChoosableFileFilter(new ProjectDirectoryFilter());
        jfc.addChoosableFileFilter(new ProjectArchivedFilter());

        File selectedFile = null;

        while (selectedFile == null) {
            int returnval = jfc.showDialog(mainFrame, "Create");
            if (returnval == JFileChooser.APPROVE_OPTION) {
                File selFile = jfc.getSelectedFile();
                {
                    final String path = selFile.getParentFile().getAbsolutePath();
                    Jobs.runInBackground(() ->
                            SiriusProperties.SIRIUS_PROPERTIES_FILE().
                                    setAndStoreProperty(SiriusProperties.DEFAULT_SAVE_DIR_PATH, path)
                    );
                }

                if (jfc.getFileFilter() instanceof ProjectArchivedFilter)
                    if (!selFile.getAbsolutePath().endsWith(".sirius"))
                        selFile = new File(selFile.getAbsolutePath() + ".sirius");

                if (!selFile.exists() || selFile.isDirectory() && Objects.requireNonNull(selFile.list()).length == 0)
                    selectedFile = selFile;

                break;
            } else {
                break;
            }
        }

        if (selectedFile != null) {
            try {
                createProject(selectedFile.toPath());
            } catch (Exception e2) {
                new StacktraceDialog(mainFrame, e2.getMessage(), e2);
            }
        }
    }

    public void createProject(@NotNull Path projectPath) {
        createProject(projectPath, null);
    }

    public void createProject(@NotNull Path projectPath, @Nullable Boolean closeCurrent) {
        try {
            String pid = Jobs.runInBackgroundAndLoad(gui.getMainFrame(), "Creating Project...", () ->
                    gui.getSiriusClient().projects()
                            .createProjectSpace(projectPath.getFileName().toString(), projectPath.toAbsolutePath().toString(), null,true)
                            .getProjectId()

            ).awaitResult();

            openProject(pid, closeCurrent);
        } catch (ExecutionException e) {
            LoggerFactory.getLogger(getClass()).error("Error when creating new project!", e);
            Jobs.runEDTLater(() -> new StacktraceDialog(gui.getMainFrame(), "Error when creating new project!", e));
        }
    }

    @Override
    protected String dontAskKey() {
        return DONT_ASK_NEW_WINDOW_CREATE_KEY;
    }
}
