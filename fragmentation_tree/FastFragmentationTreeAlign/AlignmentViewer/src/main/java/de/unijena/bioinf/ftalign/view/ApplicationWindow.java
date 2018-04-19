/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2015 Kai Dührkop
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.unijena.bioinf.ftalign.view;

import javax.swing.*;

public class ApplicationWindow extends JFrame {

    private final ApplicationState state;

    public ApplicationWindow(ApplicationState state) {
        this.state = state;
    }

    public void showLoadWindow() {
        getContentPane().removeAll();
        getContentPane().add(new LoadWindow(this, state));
        pack();
        setVisible(true);
    }

    public void showPlotWindow() {
        getContentPane().removeAll();
        getContentPane().add(new PlotWindow(this, state));
        pack();
        setVisible(true);
    }

    public void showAlignmentWindow() {
        getContentPane().removeAll();
        getContentPane().add(new AlignmentWindow(this, state));
        pack();
        setVisible(true);
    }

}
