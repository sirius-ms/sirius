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

package de.unijena.bioinf.ms.middleware.service.gui;

import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.SiriusGuiFactory;
import de.unijena.bioinf.ms.gui.dialogs.QuestionDialog;
import de.unijena.bioinf.ms.middleware.model.gui.GuiInfo;
import de.unijena.bioinf.ms.middleware.model.gui.GuiParameters;
import de.unijena.bioinf.ms.middleware.service.events.EventService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GuiServiceImpl implements GuiService {

    protected final Map<String, SiriusGui> siriusGuiInstances = new ConcurrentHashMap<>();

    protected final EventService<?> eventService;
    private final WebServerApplicationContext applicationContext;

    private SiriusGuiFactory guiFactory;

    public GuiServiceImpl(EventService<?> eventService, WebServerApplicationContext applicationContext) {
        this(null, eventService, applicationContext);
    }

    public GuiServiceImpl(SiriusGuiFactory guiFactory, EventService<?> eventService, WebServerApplicationContext applicationContext) {
        this.eventService = eventService;
        this.applicationContext = applicationContext;
        this.guiFactory = guiFactory;
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
                        if (siriusGuiInstances.containsKey(projectId)) {
                            if (siriusGuiInstances.size() == 1) {
                                closeSirius = new QuestionDialog(gui.getMainFrame(), "You are about to close the last SIRIUS GUI Window. Do you wish to shutdown SIRIUS?").isSuccess();
                                if (closeSirius) { //todo me might want yes, no and cancel instead. cancel closes GUI window but keeps service running
                                    siriusGuiInstances.remove(projectId).shutdown();
                                }
                            } else {
                                siriusGuiInstances.remove(projectId).shutdown();
                            }
                        }
                    }
                    if (closeSirius) {
                        shutdown(); //shutdown guis and clients before server shutdown is initiated starts
                        ((ConfigurableApplicationContext) applicationContext).close();
                        System.exit(0);
                    }
                }
            });
        }
    }

    @Override
    public boolean closeGuiInstance(@NotNull String projectId) {
        synchronized (siriusGuiInstances) {
            SiriusGui gui = siriusGuiInstances.remove(projectId);
            if (gui != null) {
                gui.shutdown();
                return true;
            }
        }
        return false;
    }

    protected SiriusGui makeGuiInstance(String projectId) {
        if (guiFactory == null)
            guiFactory = new SiriusGuiFactory(applicationContext.getWebServer().getPort());

        return guiFactory.newGui(projectId);
    }

    @Override
    public void applyToGuiInstance(@NotNull String projectId, @NotNull GuiParameters guiParameters) {
        SiriusGui gui = siriusGuiInstances.get(projectId);
        if (gui != null) {
            //todo change gui instance.
            System.out.println("Modifying gui instance not supported");
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No running SIRIUS GUI instance found for project id: " + projectId);
        }
    }

    @Override
    public void shutdown() {
        synchronized (siriusGuiInstances) {
            siriusGuiInstances.forEach((k, v) -> v.shutdown());
            if (guiFactory != null)
                guiFactory.shutdowm();
        }
    }
}
