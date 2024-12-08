package de.unijena.bioinf.ms.gui.actions;/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2021 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
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
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

import de.unijena.bioinf.jjobs.PropertyChangeListenerEDT;
import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.properties.ConfidenceDisplayMode;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;

/**
 * this allows to switch between displaying (and sorting by) exact and approximate confidence mode
 */
public class SwitchConfidenceModeAction extends AbstractGuiAction implements PropertyChangeListenerEDT {

    private final static String SWITCH_TO_EXACT = "Use exact confidence mode";
    private final static String SWITCH_TO_APPROX = "Use approximate confidence mode";

    public SwitchConfidenceModeAction(SiriusGui gui) {
        super(SWITCH_TO_EXACT, gui);
        gui.getProperties().addPropertyChangeListener("confidenceDisplayMode", this);
        setActionName(gui.getProperties().getConfidenceDisplayMode());
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        //switch confidence, property change support of properties class will fo the rest.
        gui.getProperties().switchConfidenceDisplayMode();
    }

    private void setActionName(ConfidenceDisplayMode mode) {
        if (mode == ConfidenceDisplayMode.APPROXIMATE) {
            this.putValue(Action.NAME, SWITCH_TO_EXACT);
        } else {
            this.putValue(Action.NAME, SWITCH_TO_APPROX);
        }
    }

    //callback if node has changed. triggers also if the mode changes externally.
    @Override
    public void propertyChangeInEDT(PropertyChangeEvent evt) {
        if (evt.getNewValue() instanceof ConfidenceDisplayMode state)
            setActionName(state);
    }
}
