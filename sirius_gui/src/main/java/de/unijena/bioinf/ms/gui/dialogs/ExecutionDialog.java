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
import de.unijena.bioinf.ms.frontend.Run;
import de.unijena.bioinf.ms.frontend.subtools.config.DefaultParameterConfigLoader;
import de.unijena.bioinf.ms.frontend.subtools.gui.GuiComputeRoot;
import de.unijena.bioinf.ms.frontend.workflow.WorkflowBuilder;
import de.unijena.bioinf.ms.frontend.workfow.GuiInstanceBufferFactory;
import de.unijena.bioinf.ms.gui.compute.SubToolConfigPanel;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.logging.TextAreaJJobContainer;
import de.unijena.bioinf.projectspace.InstanceBean;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static de.unijena.bioinf.ms.gui.mainframe.MainFrame.MF;

public class ExecutionDialog<P extends SubToolConfigPanel<?>> extends JDialog {

    public ExecutionDialog(@NotNull P configPanel) {
        init(configPanel, compounds, nonCompoundInput);
    }

    public ExecutionDialog(@NotNull P configPanel, @Nullable List<InstanceBean> compounds, @Nullable List<Path> nonCompoundInput, Frame owner) {
        super(owner);
        init(configPanel, compounds, nonCompoundInput);
    }

    public ExecutionDialog(@NotNull P configPanel, @Nullable List<InstanceBean> compounds, @Nullable List<Path> nonCompoundInput, Frame owner, boolean modal) {
        super(owner, modal);
        init(configPanel, compounds, nonCompoundInput);
    }

    public ExecutionDialog(@NotNull P configPanel, @Nullable List<InstanceBean> compounds, @Nullable List<Path> nonCompoundInput, Frame owner, String title) {
        super(owner, title);
        init(configPanel, compounds, nonCompoundInput);
    }

    public ExecutionDialog(@NotNull P configPanel, @Nullable List<InstanceBean> compounds, @Nullable List<Path> nonCompoundInput, Frame owner, String title, boolean modal) {
        super(owner, title, modal);
        init(configPanel, compounds, nonCompoundInput);
    }


    public ExecutionDialog(@NotNull P configPanel, @Nullable List<InstanceBean> compounds, @Nullable List<Path> nonCompoundInput, Dialog owner) {
        super(owner);
        init(configPanel, compounds, nonCompoundInput);
    }

    public ExecutionDialog(@NotNull P configPanel, @Nullable List<InstanceBean> compounds, @Nullable List<Path> nonCompoundInput, Dialog owner, boolean modal) {
        super(owner, modal);
        init(configPanel, compounds, nonCompoundInput);
    }

    public ExecutionDialog(@NotNull P configPanel, @Nullable List<InstanceBean> compounds, @Nullable List<Path> nonCompoundInput, Dialog owner, String title) {
        super(owner, title);
        init(configPanel, compounds, nonCompoundInput);
    }

    public ExecutionDialog(@NotNull P configPanel, @Nullable List<InstanceBean> compounds, @Nullable List<Path> nonCompoundInput, Dialog owner, String title, boolean modal) {
        super(owner, title, modal);
        init(configPanel, compounds, nonCompoundInput);
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
        try {
            final DefaultParameterConfigLoader configOptionLoader = new DefaultParameterConfigLoader();
            final WorkflowBuilder<GuiComputeRoot> wfBuilder = new WorkflowBuilder<>(new GuiComputeRoot(MF.ps(), compounds), configOptionLoader, new GuiInstanceBufferFactory());
            if (nonCompoundInput != null)
                wfBuilder.rootOptions.setNonCompoundInput(nonCompoundInput);

            final Run computation = new Run(wfBuilder);
            List<String> command = new ArrayList<>();
            command.add(configPanel.toolCommand());
            command.addAll(configPanel.asParameterList());

            computation.parseArgs(command.toArray(String[]::new));

            if (computation.isWorkflowDefined()) {
                final TextAreaJJobContainer<Boolean> j = Jobs.runWorkflow(computation.getFlow(), compounds == null ? List.of() : compounds);
                LoadingBackroundTask.connectToJob(this, "Running '" + configPanel.toolCommand() + "'...", indeterminateProgress, j);
            }
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).error("Error when running '" + configPanel.toolCommand() + "'.", e);
            new ExceptionDialog(MF, e.getMessage());
        }finally {
            dispose();
        }
    }
}
