/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
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

package de.unijena.bioinf.ms.middleware.model.events;

import de.unijena.bioinf.ms.middleware.model.compute.Job;
import de.unijena.bioinf.ms.middleware.model.gui.GuiParameters;
import org.jetbrains.annotations.NotNull;

public class ServerEvents {
    public static ServerEventImpl<Job> newJobEvent(@NotNull Job job, @NotNull String projectId) {
        return new ServerEventImpl<>(job, projectId, ServerEvent.Type.JOB);
    }

    public static ServerEventImpl<ProjectChangeEvent> newProjectEvent(@NotNull ProjectChangeEvent projectEvent) {
        return new ServerEventImpl<>(projectEvent, projectEvent.getProjectId(), ServerEvent.Type.PROJECT);
    }

    public static ServerEventImpl<ProjectChangeEvent> newProjectEvent(@NotNull String projectId, @NotNull ProjectChangeEvent.Type projectEventType) {
        assert projectEventType.ordinal() <= ProjectChangeEvent.Type.PROJECT_CLOSED.ordinal();
        return newProjectEvent(ProjectChangeEvent.builder()
                .eventType(projectEventType)
                .projectId(projectId)
                .build()
        );
    }

//    public static ServerEventImpl<GuiParameters> newGuiEvent(@NotNull GuiParameters guiParameters, @NotNull String projectId) {
//        return new ServerEventImpl<>(guiParameters, projectId, ServerEvent.Type.GUI_STATE);
//    }

    public static ServerEventImpl<BackgroundComputationsStateEvent> newComputeStateEvent(@NotNull BackgroundComputationsStateEvent evt, @NotNull String projectId) {
        return new ServerEventImpl<>(evt, projectId, ServerEvent.Type.BACKGROUND_COMPUTATIONS_STATE);
    }

    private static ServerEventImpl<?> EMPTY = null;
    public static synchronized ServerEventImpl<?> EMPTY_EVENT(){
        if (EMPTY == null)
            EMPTY = new ServerEventImpl<>(null, null, null);
        return EMPTY;
    }
}
