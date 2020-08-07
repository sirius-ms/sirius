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
import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.gui.dialogs.WarningDialog;
import de.unijena.bioinf.ms.gui.io.filefilter.ProjectArchivedFilter;
import de.unijena.bioinf.ms.gui.io.filefilter.ProjectDirectoryFilter;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.projectspace.ProjectSpaceIO;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;

import static de.unijena.bioinf.ms.gui.mainframe.MainFrame.MF;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class ProjectOpenAction extends AbstractAction {

    public ProjectOpenAction() {
        super("Open");
        putValue(Action.LARGE_ICON_KEY, Icons.FOLDER_OPEN_32);
        putValue(Action.SHORT_DESCRIPTION, "Open previously saved project (directory or .sirius). This closes the current Project.");
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
            int state = jfc.showOpenDialog(MF);
            if (state == JFileChooser.CANCEL_OPTION || state == JFileChooser.ERROR_OPTION)
                break;

            final File selFile = jfc.getSelectedFile();
            if (ProjectSpaceIO.isZipProjectSpace(selFile.toPath()) || ProjectSpaceIO.isExistingProjectspaceDirectory(selFile.toPath())) {
                SiriusProperties.
                        setAndStoreInBackground(SiriusProperties.DEFAULT_LOAD_DIALOG_PATH, selFile.getParentFile().getAbsolutePath());

                MF.openNewProjectSpace(selFile.toPath());

                break;
            } else {
                new WarningDialog(MF, "'" + selFile.getAbsolutePath() + "' does not contain valid SIRIUS project.");
            }
        }
    }
}
