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

import com.formdev.flatlaf.extras.FlatSVGIcon;
import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.compute.JobDialog;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.configs.Icons;
import io.sirius.ms.sdk.model.BackgroundComputationsStateEvent;
import io.sirius.ms.sse.DataEventType;
import io.sirius.ms.sse.DataObjectEvent;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class ShowJobsDialogAction extends AbstractGuiAction {

    private final static FlatSVGIcon FB_LOADER_STOP = Icons.FB_LOADER_STOP_64.derive(.5f);
    public ShowJobsDialogAction(SiriusGui gui) {
        super("Jobs", gui);
        putValue(Action.LARGE_ICON_KEY, FB_LOADER_STOP);
        putValue(Action.SHORT_DESCRIPTION, "Show background jobs and their status");

        gui.acceptSiriusClient((client, pid) ->
                client.addEventListener(evt -> setComputing(
                ((DataObjectEvent<BackgroundComputationsStateEvent>) evt.getNewValue())
                        .getData().getNumberOfRunningJobs() > 0
        ), pid, DataEventType.BACKGROUND_COMPUTATIONS_STATE));

        gui.acceptSiriusClient((client, pid) -> client.jobs().hasJobs(pid, false));
    }


    public void setComputing(boolean compute) {
        if (compute) {
            if (getValue(Action.LARGE_ICON_KEY).equals(FB_LOADER_STOP))
                Jobs.runEDTLater(() -> putValue(Action.LARGE_ICON_KEY, Icons.FB_LOADER_RUN_32));
        } else {
            if (getValue(Action.LARGE_ICON_KEY).equals(Icons.FB_LOADER_RUN_32))
                Jobs.runEDTLater(() -> putValue(Action.LARGE_ICON_KEY, FB_LOADER_STOP));
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() instanceof JComponent c)
            JobDialog.INSTANCE().setLocationRelativeTo(c.getRootPane());
        JobDialog.INSTANCE().setVisible(true);
    }
}
