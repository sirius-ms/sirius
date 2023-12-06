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
import de.unijena.bioinf.ms.gui.SiriusGuiFactory;
import de.unijena.bioinf.ms.middleware.service.events.EventService;
import de.unijena.bioinf.ms.middleware.service.projects.SiriusProjectSpaceImpl;
import de.unijena.bioinf.ms.middleware.service.projects.SiriusProjectSpaceProviderImpl;
import de.unijena.bioinf.projectspace.GuiProjectSpaceManager;
import de.unijena.bioinf.sse.DataEventType;

import java.util.concurrent.Flow;

public class SiriusProjectSpaceGuiService extends AbstractGuiService<SiriusProjectSpaceImpl> {
    private final SiriusGuiFactory guiFactory;

    @Deprecated
    private final SiriusProjectSpaceProviderImpl provider;

    public SiriusProjectSpaceGuiService(EventService<?> eventService, SiriusProjectSpaceProviderImpl provider) {
        this(new SiriusGuiFactory(), eventService, provider);
    }
    public SiriusProjectSpaceGuiService(SiriusGuiFactory guiFactory, EventService<?> eventService, SiriusProjectSpaceProviderImpl provider) {
        super(eventService);
        this.guiFactory = guiFactory;
        this.provider = provider;
    }

    @Override
    protected SiriusGui makeGuiInstance(String projectId) {
        SiriusGui gui = guiFactory.newGui(projectId, (GuiProjectSpaceManager) provider.getProjectOrThrow(projectId).getProjectSpaceManager());
        gui.getSirius().addEventListener(new Flow.Subscriber<>() {
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                System.out.println("SUBSCRIBED!");
            }

            @Override
            public void onNext(Object item) {
                System.out.println("Event delivered: " + item.toString());
            }

            @Override
            public void onError(Throwable throwable) {
                System.out.println("ERROR: ");
                throwable.printStackTrace(System.out);
            }

            @Override
            public void onComplete() {
                System.out.println("COMPLETED!");
            }
        }, projectId, DataEventType.PROJECT, DataEventType.JOB, DataEventType.GUI_STATE);

        return gui;
    }

    @Override
    public void shutdown() {
        super.shutdown();
        guiFactory.shutdowm();
    }
}
