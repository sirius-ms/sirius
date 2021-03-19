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
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 29.01.17.
 */

import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.configs.Icons;

import javax.swing.*;
import java.awt.event.ActionEvent;

import static de.unijena.bioinf.ms.gui.mainframe.MainFrame.MF;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class ShowJobsDialogAction extends AbstractAction {

    public ShowJobsDialogAction() {
        super("Jobs");
        putValue(Action.LARGE_ICON_KEY, Icons.FB_LOADER_STOP_32);
        putValue(Action.SHORT_DESCRIPTION, "Show background jobs and their status");
    }


    public void setComputing(boolean compute) {
        if (compute) {
            if (getValue(Action.LARGE_ICON_KEY).equals(Icons.FB_LOADER_STOP_32))
                Jobs.runEDTLater(() -> putValue(Action.LARGE_ICON_KEY, Icons.FB_LOADER_RUN_32));
        } else {
            if (getValue(Action.LARGE_ICON_KEY).equals(Icons.FB_LOADER_RUN_32))
                Jobs.runEDTLater(() -> putValue(Action.LARGE_ICON_KEY, Icons.FB_LOADER_STOP_32));
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        MF.getJobDialog().setVisible(true);
    }
}
