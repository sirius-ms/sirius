/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
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

package de.unijena.bioinf.ms.middleware.service.gui;

import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.dialogs.QuestionDialog;
import de.unijena.bioinf.ms.middleware.model.events.ServerEvents;
import de.unijena.bioinf.ms.middleware.model.gui.GuiInfo;
import de.unijena.bioinf.ms.middleware.model.gui.GuiParameters;
import de.unijena.bioinf.ms.middleware.service.events.EventService;
import de.unijena.bioinf.ms.middleware.service.projects.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractGuiService<P extends Project> implements GuiService<P> {

    protected final Map<String, SiriusGui> siriusGuiInstances = new ConcurrentHashMap<>();

    protected final EventService<?> eventService;
    private final ApplicationContext applicationContext;

    protected AbstractGuiService(EventService<?> eventService, ApplicationContext applicationContext) {
        this.eventService = eventService;
        this.applicationContext = applicationContext;
    }

    @Override
    public List<GuiInfo> findGui() {
        synchronized (siriusGuiInstances) {
        return siriusGuiInstances.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(e -> GuiInfo.builder().projectId(e.getKey()).build()).toList();
        }
    }

    @Override
    public void createGuiInstance(@NotNull final String projectId, @Nullable GuiParameters guiParameters) {
        SiriusGui gui;
        synchronized (siriusGuiInstances) {
            if (siriusGuiInstances.containsKey(projectId))
                throw new ResponseStatusException(HttpStatus.CONFLICT, "There is already a SIRIUS GUI instance running on project: " + projectId);
            gui = makeGuiInstance(projectId);
            siriusGuiInstances.put(projectId, gui);

            gui.getMainFrame().addWindowListener(new WindowAdapter() { //special shutdown procedure for bundled GUI instances
                @Override
                public void windowClosing(WindowEvent e) {
                    boolean closeSirius = false;
                    synchronized (siriusGuiInstances) {
                        SiriusGui gui = siriusGuiInstances.remove(projectId);
                        if (gui != null) {
                            if (siriusGuiInstances.size() == 0)
                                closeSirius = new QuestionDialog(gui.getMainFrame(), "You are about to close the last SIRIUS GUI Window. Do you wish to shutdown SIRIUS?").isSuccess();
                            gui.shutdown();
                        }
                    }
                    if (closeSirius){
                        ((ConfigurableApplicationContext) applicationContext).close();
                        System.exit(0);
                    }
                }
            });
        }
        if (guiParameters != null)
            eventService.sendEvent(ServerEvents.newGuiEvent(guiParameters, projectId));
    }

    @Override
    public boolean closeGuiInstance(@NotNull String projectId) {
        synchronized (siriusGuiInstances) {
            SiriusGui gui = siriusGuiInstances.remove(projectId);
            if (gui != null){
                gui.shutdown();
                return true;
            }
        }
        return false;
    }

    @Override
    public void applyToGuiInstance(@NotNull String projectId, @NotNull GuiParameters guiParameters) {
        SiriusGui gui = siriusGuiInstances.get(projectId);
        if (gui != null) {
            eventService.sendEvent(ServerEvents.newGuiEvent(guiParameters, projectId));
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No running SIRIUS GUI instance found for project id: " + projectId);
        }
    }

    @Override
    public void shutdown() {
        siriusGuiInstances.forEach((k, v) -> v.shutdown());
    }

    protected abstract SiriusGui makeGuiInstance(String projectId);
}
