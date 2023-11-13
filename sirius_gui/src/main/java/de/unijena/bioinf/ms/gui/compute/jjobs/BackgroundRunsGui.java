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

package de.unijena.bioinf.ms.gui.compute.jjobs;

import de.unijena.bioinf.jjobs.LoadingBackroundTask;
import de.unijena.bioinf.ms.frontend.BackgroundRuns;
import de.unijena.bioinf.ms.frontend.subtools.InputFilesOptions;
import de.unijena.bioinf.ms.gui.logging.TextAreaJJobContainer;
import de.unijena.bioinf.projectspace.GuiProjectSpaceManager;
import de.unijena.bioinf.projectspace.InstanceBean;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.io.IOException;
import java.util.List;

/**
 * Provides access to {@link de.unijena.bioinf.ms.frontend.BackgroundRuns} service to execute commandline runs
 * with GUI loader support.
 *
 * WILL BE REPLACED BY NIGHTSKY CALLS!
 */
@Deprecated
public class BackgroundRunsGui {

    @NotNull
    private GuiProjectSpaceManager ps;

    public BackgroundRunsGui(@NotNull GuiProjectSpaceManager ps) {
        this.ps = ps;
    }

    @Deprecated
    public void setProject(@NotNull GuiProjectSpaceManager project) {
        ps = project;
    }

    @Deprecated
    @NotNull
    public GuiProjectSpaceManager getProject() {
        return ps;
    }

    @Deprecated
    public TextAreaJJobContainer<Boolean> runCommand(@Nullable List<String> command, List<InstanceBean> compoundsToProcess, @Nullable String description) throws IOException {
        return runCommand(command, compoundsToProcess, null, description);
    }

    @Deprecated
    public TextAreaJJobContainer<Boolean> runCommand(@Nullable List<String> command, List<InstanceBean> compoundsToProcess, @Nullable InputFilesOptions input, @Nullable String description) throws IOException {
        BackgroundRuns.BackgroundRunJob<GuiProjectSpaceManager, InstanceBean> job =
                BackgroundRuns.makeBackgroundRun(command, compoundsToProcess, input, ps);

        return Jobs.submit(job, job.getRunId() + ": " + (description == null ? "" : description),
                "Computation");
    }

    @Deprecated
    public LoadingBackroundTask<Boolean> runCommandAndLoad(@Nullable List<String> command,
                                                                  List<InstanceBean> compoundsToProcess,
                                                                  @Nullable InputFilesOptions input,
                                                                  Window owner, String title, boolean indeterminateProgress
    ) throws IOException {
        BackgroundRuns.BackgroundRunJob<GuiProjectSpaceManager, InstanceBean> job =
                BackgroundRuns.makeBackgroundRun(command, compoundsToProcess, input, ps);
        return Jobs.runInBackgroundAndLoad(owner, title, indeterminateProgress, job);
    }

    @Deprecated
    public LoadingBackroundTask<Boolean> runCommandAndLoad(@Nullable List<String> command,
                                                                  List<InstanceBean> compoundsToProcess,
                                                                  @Nullable InputFilesOptions input,
                                                                  Dialog owner, String title, boolean indeterminateProgress
    ) throws IOException {
        BackgroundRuns.BackgroundRunJob<GuiProjectSpaceManager, InstanceBean> job =
                BackgroundRuns.makeBackgroundRun(command, compoundsToProcess, input, ps);
        return Jobs.runInBackgroundAndLoad(owner, title, indeterminateProgress, job);
    }

    @Deprecated
    public void cancelAllRuns() {
        BackgroundRuns.cancelAllRuns();
    }
}
