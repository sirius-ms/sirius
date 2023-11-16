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
import de.unijena.bioinf.ms.middleware.service.projects.SiriusProjectSpaceImpl;
import de.unijena.bioinf.projectspace.GuiProjectSpaceManager;

public class SiriusProjectSpaceGuiService extends AbstractGuiService<SiriusProjectSpaceImpl> {
    private final SiriusGuiFactory guiFactory;

    public SiriusProjectSpaceGuiService() {
        this(new SiriusGuiFactory());
    }
    public SiriusProjectSpaceGuiService(SiriusGuiFactory guiFactory) {
        this.guiFactory = guiFactory;
    }

    @Override
    protected SiriusGui makeGuiInstance(SiriusProjectSpaceImpl project) {
        return guiFactory.newGui((GuiProjectSpaceManager) project.getProjectSpaceManager());
    }

    @Override
    public void shutdown() {
        super.shutdown();
        guiFactory.shutdowm();
    }
}
