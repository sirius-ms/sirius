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

    private static GuiProjectSpaceManager PS;

    @Deprecated
    public static synchronized void setProject(GuiProjectSpaceManager project) {
        PS = project;
    }

    @Deprecated
    public static synchronized GuiProjectSpaceManager getProject() {
        return PS;
    }

    @Deprecated
    public static TextAreaJJobContainer<Boolean> runCommand(@Nullable List<String> command, List<InstanceBean> compoundsToProcess, @Nullable String description) throws IOException {
        return runCommand(command, compoundsToProcess, null, description);
    }

    @Deprecated
    public static TextAreaJJobContainer<Boolean> runCommand(@Nullable List<String> command, List<InstanceBean> compoundsToProcess, @Nullable InputFilesOptions input, @Nullable String description) throws IOException {
        BackgroundRuns.BackgroundRunJob<GuiProjectSpaceManager, InstanceBean> job =
                BackgroundRuns.makeBackgroundRun(command, compoundsToProcess, input, PS);

        return Jobs.submit(job, job.getRunId() + ": " + (description == null ? "" : description),
                "Computation");
    }

    @Deprecated
    public static LoadingBackroundTask<Boolean> runCommandAndLoad(@Nullable List<String> command,
                                                                  List<InstanceBean> compoundsToProcess,
                                                                  @Nullable InputFilesOptions input,
                                                                  Window owner, String title, boolean indeterminateProgress
    ) throws IOException {
        BackgroundRuns.BackgroundRunJob<GuiProjectSpaceManager, InstanceBean> job =
                BackgroundRuns.makeBackgroundRun(command, compoundsToProcess, input, PS);
        return Jobs.runInBackgroundAndLoad(owner, title, indeterminateProgress, job);
    }

    @Deprecated
    public static LoadingBackroundTask<Boolean> runCommandAndLoad(@Nullable List<String> command,
                                                                  List<InstanceBean> compoundsToProcess,
                                                                  @Nullable InputFilesOptions input,
                                                                  Dialog owner, String title, boolean indeterminateProgress
    ) throws IOException {
        BackgroundRuns.BackgroundRunJob<GuiProjectSpaceManager, InstanceBean> job =
                BackgroundRuns.makeBackgroundRun(command, compoundsToProcess, input, PS);
        return Jobs.runInBackgroundAndLoad(owner, title, indeterminateProgress, job);
    }

    @Deprecated
    public static void cancelAllRuns() {
        BackgroundRuns.cancelAllRuns();
    }
}
