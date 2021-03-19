/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.gui.actions;

import de.unijena.bioinf.ms.frontend.core.SiriusProperties;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.gui.dialogs.ErrorReportDialog;
import de.unijena.bioinf.ms.gui.io.filefilter.ProjectArchivedFilter;
import de.unijena.bioinf.ms.gui.io.filefilter.ProjectDirectoryFilter;
import de.unijena.bioinf.ms.properties.PropertyManager;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Objects;

import static de.unijena.bioinf.ms.gui.mainframe.MainFrame.MF;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class ProjectCreateAction extends AbstractAction {

    public ProjectCreateAction() {
        super("New");
        putValue(Action.LARGE_ICON_KEY, Icons.ADD_DOC_32);
        putValue(Action.SHORT_DESCRIPTION, "Create a new empty project at the given location (.sirius or directory)");
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
            int returnval = jfc.showDialog(MF, "Create");
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
                MF.createNewProjectSpace(selectedFile.toPath());
            } catch (Exception e2) {
                new ErrorReportDialog(MF, e2.getMessage());
            }
        }
    }
}
