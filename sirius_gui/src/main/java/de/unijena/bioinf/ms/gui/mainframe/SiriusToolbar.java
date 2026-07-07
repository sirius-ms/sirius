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

package de.unijena.bioinf.ms.gui.mainframe;

import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.actions.SiriusActions;
import de.unijena.bioinf.ms.gui.configs.Colors;
import de.unijena.bioinf.ms.gui.utils.ToolbarButton;
import de.unijena.bioinf.ms.gui.utils.softwaretour.SoftwareTourInfoStore;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;

/**
 * @author Markus Fleischauer
 */
@Getter
public class SiriusToolbar extends JToolBar {
    private ToolbarButton logsB, createB, openB, saveB, summB, fbmnB, importB, computeAllB, sample, jobs, db, connect, settings, account, /*bug,*/
            help, about;


    SiriusToolbar(SiriusGui gui) {
        setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, Colors.Menu.ICON_BLUE));
        //create/open/save project
        createB = new ToolbarButton(SiriusActions.NEW_WS.getInstance(gui, true));
        add(createB);
        openB = new ToolbarButton(SiriusActions.LOAD_WS.getInstance(gui, true));
        add(openB);
        saveB = new ToolbarButton(SiriusActions.SAVE_WS.getInstance(gui, true));
        add(saveB);
        addSeparator(new Dimension(20, 20));

        //import data
        importB = new ToolbarButton(SiriusActions.IMPORT_EXP_BATCH.getInstance(gui, true));
        add(importB);

        //summarize
        addSeparator(new Dimension(20, 20));
        summB = new ToolbarButton(SiriusActions.SUMMARIZE_WS.getInstance(gui, true));
        add(summB);

        //fbmn
        addSeparator(new Dimension(20, 20));
        fbmnB = new ToolbarButton(SiriusActions.EXPORT_FBMN.getInstance(gui, true));
        add(fbmnB);

        addSeparator(new Dimension(20, 20));
        add(Box.createGlue());
        addSeparator(new Dimension(20, 20));

        //compute
        computeAllB = new ToolbarButton(SiriusActions.COMPUTE_ALL.getInstance(gui, true));
        computeAllB.putClientProperty(SoftwareTourInfoStore.TOUR_ELEMENT_PROPERTY_KEY, SoftwareTourInfoStore.ComputeAllButton);
        add(computeAllB);
        addSeparator(new Dimension(20, 20));

        db = new ToolbarButton(SiriusActions.SHOW_DB.getInstance(gui, true));
        add(db);
        addSeparator(new Dimension(20, 20));

        sample = new ToolbarButton(SiriusActions.SHOW_SAMPLE.getInstance(gui, true));
        add(sample);
        addSeparator(new Dimension(20, 20));

        jobs = new ToolbarButton(SiriusActions.SHOW_JOBS.getInstance(gui, true));
        add(jobs);

        addSeparator(new Dimension(20, 20));
        add(Box.createGlue());
        addSeparator(new Dimension(20, 20));
        logsB = new ToolbarButton(SiriusActions.SHOW_LOG.getInstance(true));
        add(logsB);
        logsB.putClientProperty(SoftwareTourInfoStore.TOUR_ELEMENT_PROPERTY_KEY, SoftwareTourInfoStore.Log);

        settings = new ToolbarButton(SiriusActions.SHOW_SETTINGS.getInstance(gui, true));
        add(settings);

        connect = new ToolbarButton(SiriusActions.CHECK_CONNECTION.getInstance(gui, true));
        add(connect);

        account = new ToolbarButton(SiriusActions.SHOW_ACCOUNT.getInstance(gui, true));
        add(account);

        help = new ToolbarButton(SiriusActions.OPEN_ONLINE_DOCUMENTATION.getInstance(gui, true));
        add(help);

        about = new ToolbarButton(SiriusActions.SHOW_ABOUT.getInstance(gui, true));
        add(about);

        setRollover(true);
        setFloatable(false);
    }

}
