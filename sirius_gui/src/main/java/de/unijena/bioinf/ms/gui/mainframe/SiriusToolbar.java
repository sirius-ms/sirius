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

import de.unijena.bioinf.ms.frontend.BackgroundRuns;
import de.unijena.bioinf.ms.gui.actions.OpenLogAction;
import de.unijena.bioinf.ms.gui.actions.ShowJobsDialogAction;
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
import java.beans.PropertyChangeListener;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class SiriusToolbar extends JToolBar {
    private final ToolbarToggleButton logsB;
    private final PropertyChangeListener backgroundRunListener;
    private ToolbarButton importCompB, createB, openB, saveB, exportB, summB, fbmnB, importB, computeAllB, jobs, db, connect, settings, account, /*bug,*/
            help, about;



    SiriusToolbar(MainFrame mf) {

        setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, Colors.ICON_BLUE));
        //create/open
        createB = new ToolbarButton(SiriusActions.NEW_WS.getInstance(mf, true, getActionMap()));
        add(createB);
        openB = new ToolbarButton(SiriusActions.LOAD_WS.getInstance(mf, true, getActionMap()));
        add(openB);
        addSeparator(new Dimension(20, 20));

        //save
        saveB = new ToolbarButton(SiriusActions.SAVE_WS.getInstance(mf, true, getActionMap()));
        add(saveB);
        exportB = new ToolbarButton(SiriusActions.EXPORT_WS.getInstance(mf, true, getActionMap()));
        add(exportB);
        addSeparator(new Dimension(20, 20));

        //import
        importCompB = new ToolbarButton(SiriusActions.IMPORT_EXP.getInstance(mf, true, getActionMap()));
        add(importCompB);
        importB = new ToolbarButton(SiriusActions.IMPORT_EXP_BATCH.getInstance(mf, true, getActionMap()));
        add(importB);

        //summarize
        addSeparator(new Dimension(20, 20));
        summB = new ToolbarButton(SiriusActions.SUMMARIZE_WS.getInstance(mf, true, getActionMap()));
        add(summB);

        //fbmn
        addSeparator(new Dimension(20, 20));
        fbmnB = new ToolbarButton(SiriusActions.EXPORT_FBMN.getInstance(mf, true, getActionMap()));
        add(fbmnB);

        addSeparator(new Dimension(20, 20));
        add(Box.createGlue());
        addSeparator(new Dimension(20, 20));

        //compute
        computeAllB = new ToolbarButton(SiriusActions.COMPUTE_ALL.getInstance(mf, true, getActionMap()));
        add(computeAllB);
        addSeparator(new Dimension(20, 20));

        db = new ToolbarButton(SiriusActions.SHOW_DB.getInstance(mf, true, getActionMap()));
        add(db);
        addSeparator(new Dimension(20, 20));

        jobs = new ToolbarButton(SiriusActions.SHOW_JOBS.getInstance(mf, true, getActionMap()));
        add(jobs);

        addSeparator(new Dimension(20, 20));
        add(Box.createGlue());
        addSeparator(new Dimension(20, 20));
        logsB = createLogToggleButton((OpenLogAction) SiriusActions.SHOW_LOG.getInstance(mf, true, getActionMap()));
        add(logsB);

        settings = new ToolbarButton(SiriusActions.SHOW_SETTINGS.getInstance(mf, true, getActionMap()));
        add(settings);

        connect = new ToolbarButton(SiriusActions.CHECK_CONNECTION.getInstance(mf, true, getActionMap()));
        add(connect);

        account = new ToolbarButton(SiriusActions.SHOW_ACCOUNT.getInstance(mf, true, getActionMap()));
        add(account);

        help = new ToolbarButton(SiriusActions.OPEN_ONLINE_DOCUMENTATION.getInstance(mf, true, getActionMap()));
        add(help);

      /*  bug = new ToolbarButton(SiriusActions.SHOW_BUGS.getInstance(mf, true, getActionMap()));
        add(bug);*/

        about = new ToolbarButton(SiriusActions.SHOW_ABOUT.getInstance(mf, true, getActionMap()));
        add(about);

        backgroundRunListener = evt -> {
            if (BackgroundRuns.ACTIVE_RUNS_PROPERTY.equals(evt.getPropertyName())) {
                int size = (int) evt.getNewValue();
                ((ShowJobsDialogAction) jobs.getAction()).setComputing(size > 0);
                summB.getAction().setEnabled(size == 0);
                fbmnB.getAction().setEnabled(size == 0);
            }
        };
        BackgroundRuns.addPropertyChangeListener(backgroundRunListener);

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

    public ToolbarToggleButton getLogsB() {
        return logsB;
    }

    public ToolbarButton getImportCompB() {
        return importCompB;
    }

    public ToolbarButton getCreateB() {
        return createB;
    }

    public ToolbarButton getOpenB() {
        return openB;
    }

    public ToolbarButton getSaveB() {
        return saveB;
    }

    public ToolbarButton getExportB() {
        return exportB;
    }

    public ToolbarButton getSummB() {
        return summB;
    }

    public ToolbarButton getFbmnB() {
        return fbmnB;
    }

    public ToolbarButton getImportB() {
        return importB;
    }

    public ToolbarButton getComputeAllB() {
        return computeAllB;
    }

    public ToolbarButton getJobs() {
        return jobs;
    }

    public ToolbarButton getDb() {
        return db;
    }

    public ToolbarButton getConnect() {
        return connect;
    }

    public ToolbarButton getSettings() {
        return settings;
    }

    public ToolbarButton getAccount() {
        return account;
    }

    public ToolbarButton getHelp() {
        return help;
    }

    public ToolbarButton getAbout() {
        return about;
    }
}
