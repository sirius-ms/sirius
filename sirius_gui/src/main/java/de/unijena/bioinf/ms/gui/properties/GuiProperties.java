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

package de.unijena.bioinf.ms.gui.properties;

import de.unijena.bioinf.jjobs.PropertyChangeOrator;
import de.unijena.bioinf.ms.frontend.core.SiriusProperties;
import org.jetbrains.annotations.NotNull;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

public final class GuiProperties implements PropertyChangeOrator {

    // region PropertyChangeSupport
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(propertyName, listener);
    }

    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(propertyName, listener);
    }
    //endregion

    // region ConfidenceDisplayMode
    @NotNull
    private ConfidenceDisplayMode confidenceDisplayMode = SiriusProperties.getEnum("de.unijena.bioinf.sirius.ui.ConfidenceDisplayMode", null, ConfidenceDisplayMode.APPROXIMATE);

    public synchronized void setConfidenceDisplayMode(@NotNull ConfidenceDisplayMode confidenceDisplayMode) {
        @NotNull ConfidenceDisplayMode old = this.confidenceDisplayMode;
        this.confidenceDisplayMode = confidenceDisplayMode;
        pcs.firePropertyChange("confidenceDisplayMode", old, this.confidenceDisplayMode);
    }

    public synchronized ConfidenceDisplayMode getConfidenceDisplayMode() {
        return confidenceDisplayMode;
    }

    public synchronized boolean isConfidenceViewMode(@NotNull ConfidenceDisplayMode mode) {
        return getConfidenceDisplayMode() == mode;
    }

    public synchronized void switchConfidenceDisplayMode() {
        setConfidenceDisplayMode(isConfidenceViewMode(ConfidenceDisplayMode.EXACT) ? ConfidenceDisplayMode.APPROXIMATE : ConfidenceDisplayMode.EXACT);
    }
    //endregion
}
