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

package de.unijena.bioinf.ms.gui.mainframe;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 27.01.17.
 */

import de.unijena.bioinf.ms.gui.actions.OpenLogAction;
import de.unijena.bioinf.ms.gui.actions.SiriusActions;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.configs.Colors;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.gui.utils.ToolbarButton;
import de.unijena.bioinf.ms.gui.utils.ToolbarToggleButton;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
class SiriusToolbar extends JToolBar {
    private final ToolbarToggleButton logs;
    private ToolbarButton imCompB, createB, openB, saveB,exportB, summB, fbmnB, imB, computeAllB, configFingerID, jobs, db, connect, settings, bug, about;

    SiriusToolbar() {

        setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, Colors.ICON_BLUE));
        //create/open
        createB = new ToolbarButton(SiriusActions.NEW_WS.getInstance());
        add(createB);
        openB = new ToolbarButton(SiriusActions.LOAD_WS.getInstance());
        add(openB);
        addSeparator(new Dimension(20, 20));

        //save
        saveB = new ToolbarButton(SiriusActions.SAVE_WS.getInstance());
        add(saveB);
        exportB = new ToolbarButton(SiriusActions.EXPORT_WS.getInstance());
        add(exportB);
        addSeparator(new Dimension(20, 20));

        //import
        imCompB = new ToolbarButton(SiriusActions.IMPORT_EXP.getInstance());
        add(imCompB);
        imB = new ToolbarButton(SiriusActions.IMPORT_EXP_BATCH.getInstance());
        add(imB);

        //summarize
        addSeparator(new Dimension(20, 20));
        summB = new ToolbarButton(SiriusActions.SUMMARY_WS.getInstance());
        add(summB);

        //fbmn
        addSeparator(new Dimension(20, 20));
        fbmnB = new ToolbarButton(SiriusActions.EXPORT_FBMN.getInstance());
        add(fbmnB);

        addSeparator(new Dimension(20, 20));
        add(Box.createGlue());
        addSeparator(new Dimension(20, 20));

        //compute
        computeAllB = new ToolbarButton(SiriusActions.COMPUTE_ALL.getInstance());
        add(computeAllB);
        addSeparator(new Dimension(20, 20));

        //todo implement database menu
        //todo reenable if fixed
        db = new ToolbarButton(SiriusActions.SHOW_DB.getInstance());
        add(db);
        addSeparator(new Dimension(20,20));

        jobs = new ToolbarButton(SiriusActions.SHOW_JOBS.getInstance());
        add(jobs);

        addSeparator(new Dimension(20, 20));
        add(Box.createGlue());
        addSeparator(new Dimension(20, 20));
        logs = createLogToggleButton((OpenLogAction) SiriusActions.SHOW_LOG.getInstance());
        add(logs);

        settings = new ToolbarButton(SiriusActions.SHOW_SETTINGS.getInstance());
        add(settings);

        connect = new ToolbarButton(SiriusActions.CHECK_CONNECTION.getInstance());
        add(connect);

        bug = new ToolbarButton(SiriusActions.SHOW_BUGS.getInstance());
        add(bug);

        about = new ToolbarButton(SiriusActions.SHOW_ABOUT.getInstance());
        add(about);

        setRollover(true);
        setFloatable(false);
    }

    private ToolbarToggleButton createLogToggleButton(OpenLogAction action) {
        final ToolbarToggleButton tb = new ToolbarToggleButton((Icon) action.getValue(Action.LARGE_ICON_KEY));
        tb.setText((String) action.getValue(Action.NAME));
        tb.setToolTipText(GuiUtils.formatToolTip((String) action.getValue(Action.SHORT_DESCRIPTION)));
        tb.addActionListener(action);
        // add a window listener
        action.addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent e) {
                if (tb.isSelected())
                    Jobs.runEDTLater(() -> tb.setSelected(false));
            }

            public void windowClosing(WindowEvent e) {
                if (tb.isSelected())
                    Jobs.runEDTLater(() -> tb.setSelected(false));
            }
        });

        // add a component listener
        action.addComponentListener(new ComponentListener() {
            public void componentHidden(ComponentEvent e) {
            }

            public void componentMoved(ComponentEvent e) {
            }

            public void componentResized(ComponentEvent e) {
            }

            public void componentShown(ComponentEvent e) {
                if (!tb.isSelected())
                    Jobs.runEDTLater(() -> tb.setSelected(true));
            }
        });
        return tb;
    }
}
