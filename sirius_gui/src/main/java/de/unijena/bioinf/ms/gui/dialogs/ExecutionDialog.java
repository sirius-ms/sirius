/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.gui.dialogs;

import de.unijena.bioinf.jjobs.LoadingBackroundTask;
import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.compute.SubToolConfigPanel;
import de.unijena.bioinf.ms.gui.mainframe.MainFrame;
import de.unijena.bioinf.ms.nightsky.sdk.jjobs.SseProgressJJob;
import de.unijena.bioinf.ms.nightsky.sdk.model.CommandSubmission;
import de.unijena.bioinf.ms.nightsky.sdk.model.Job;
import de.unijena.bioinf.ms.nightsky.sdk.model.JobOptField;
import de.unijena.bioinf.projectspace.InstanceBean;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.util.List;


public class ExecutionDialog<P extends SubToolConfigPanel<?>> extends JDialog {


    private final SiriusGui gui;

    public ExecutionDialog(SiriusGui gui, @NotNull P configPanel, @Nullable List<InstanceBean> compounds, @Nullable List<Path> nonCompoundInput, MainFrame owner, String title, boolean modal) {
        super(owner, title, modal);
        this.gui = gui;
        init(configPanel, compounds, nonCompoundInput);
    }
    private MainFrame mf(){
        return (MainFrame) getOwner();
    }

    protected JButton execute, cancel;
    protected P configPanel;
    protected boolean indeterminateProgress = true;

    @Nullable List<InstanceBean> compounds = null;
    @Nullable List<Path> nonCompoundInput = null;

    protected void init(@NotNull P configPanel, @Nullable List<InstanceBean> compounds, @Nullable List<Path> nonCompoundInput) {
        this.configPanel = configPanel;
        this.compounds = compounds;
        this.nonCompoundInput = nonCompoundInput;

        setLayout(new BorderLayout());
        add(this.configPanel, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        execute = new JButton("Run");
        cancel = new JButton("Cancel");
        buttons.add(execute);
        buttons.add(cancel);
        add(buttons, BorderLayout.SOUTH);

        execute.addActionListener(e -> execute());

        cancel.addActionListener(e -> cancel());

//        setMinimumSize(new Dimension(350, getMinimumSize().height));
    }

    public void start() {
        setResizable(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        setLocationRelativeTo(getOwner());
        setVisible(true);
    }

    public void setIndeterminateProgress(boolean indeterminateProgress) {
        this.indeterminateProgress = indeterminateProgress;
    }

    public boolean isIndeterminateProgress() {
        return indeterminateProgress;
    }

    public void setCompounds(@Nullable List<InstanceBean> compounds) {
        this.compounds = compounds;
    }

    public void setNonCompoundInput(@Nullable List<Path> nonCompoundInput) {
        this.nonCompoundInput = nonCompoundInput;
    }

    protected void cancel() {
        dispose();
    }

    protected void execute() {
        dispose();
        try {
            final CommandSubmission sub = new CommandSubmission();
            sub.addCommandItem(configPanel.toolCommand());
            configPanel.asParameterList().forEach(sub::addCommandItem);

            if (compounds != null)
                sub.alignedFeatureIds(compounds.stream().map(ib -> ib.getID().getDirectoryName()).toList());
            if (nonCompoundInput != null)
                sub.inputPaths(nonCompoundInput.stream().map(Path::toString).toList());

            Job j = gui.getSiriusClient().jobs().startCommand(gui.getProjectId(), sub, List.of(JobOptField.PROGRESS));

            LoadingBackroundTask.runInBackground(mf(),
                    "Running '" + configPanel.toolCommand() + "'...", indeterminateProgress, null,
                    new SseProgressJJob(gui.getSiriusClient(), gui.getProjectId(), j)
            );

        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).error("Error when running '" + configPanel.toolCommand() + "'.", e);
            new ExceptionDialog(mf(), e.getMessage());
        }
    }
}
