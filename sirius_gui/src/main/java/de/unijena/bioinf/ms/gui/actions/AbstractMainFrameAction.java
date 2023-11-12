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

package de.unijena.bioinf.ms.gui.actions;

import de.unijena.bioinf.ms.gui.mainframe.MainFrame;

import javax.swing.*;

public abstract class AbstractMainFrameAction extends AbstractAction {
    protected final MainFrame MF;

    public AbstractMainFrameAction(MainFrame mainFrame) {
        this.MF = mainFrame;
    }

    public AbstractMainFrameAction(String name, MainFrame mainFrame) {
        super(name);
        this.MF = mainFrame;
    }

    public AbstractMainFrameAction(String name, Icon icon, MainFrame mainFrame) {
        super(name, icon);
        this.MF = mainFrame;
    }
}
