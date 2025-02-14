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

import de.unijena.bioinf.jjobs.PropertyChangeListenerEDT;
import de.unijena.bioinf.ms.frontend.core.SiriusProperties;
import org.jetbrains.annotations.NotNull;

import java.beans.PropertyChangeSupport;
import java.util.HashSet;
import java.util.Set;

public final class GuiProperties {

    // region PropertyChangeSupport
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    public void addPropertyChangeListener(PropertyChangeListenerEDT listener) {
        pcs.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListenerEDT listener) {
        pcs.removePropertyChangeListener(listener);
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListenerEDT listener) {
        pcs.addPropertyChangeListener(propertyName, listener);
    }

    public void removePropertyChangeListener(String propertyName, PropertyChangeListenerEDT listener) {
        pcs.removePropertyChangeListener(propertyName, listener);
    }
    //endregion

    // region ConfidenceDisplayMode
    public static final @NotNull String CONFIDENCE_DISPLAY_MODE_KEY = "de.unijena.bioinf.sirius.ui.confidenceDisplayMode";

    @NotNull
    private ConfidenceDisplayMode confidenceDisplayMode = SiriusProperties.getEnum(CONFIDENCE_DISPLAY_MODE_KEY, null, ConfidenceDisplayMode.APPROXIMATE);

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

    // region MolecularStructuresDisplayColors
    public static final @NotNull String MOLECULAR_STRUCTURES_DISPLAY_COLORS_KEY = "de.unijena.bioinf.sirius.ui.molecularStructuresDisplayColors";

    @NotNull
    private MolecularStructuresDisplayColors molecularStructureDisplayColors = SiriusProperties.getEnum(MOLECULAR_STRUCTURES_DISPLAY_COLORS_KEY, null, MolecularStructuresDisplayColors.CPK);

    public synchronized void setMolecularStructureDisplayColors(@NotNull MolecularStructuresDisplayColors molecularStructuresDisplayColors) {
        @NotNull MolecularStructuresDisplayColors old = this.molecularStructureDisplayColors;
        this.molecularStructureDisplayColors = molecularStructuresDisplayColors;
        pcs.firePropertyChange("molecularStructuresDisplayColors", old, this.molecularStructureDisplayColors);
    }

    public synchronized MolecularStructuresDisplayColors getMolecularStructureDisplayColors() {
        return molecularStructureDisplayColors;
    }
    //endregion

    // region showSpectraView
    public static final @NotNull String SHOW_SPECTRA_MATCH_PANEL_KEY = "de.unijena.bioinf.sirius.spectraMatchPanel.show";
    private boolean showSpectraMatchPanel = SiriusProperties.getBoolean(SHOW_SPECTRA_MATCH_PANEL_KEY, null, false);

    public synchronized boolean isShowSpectraMatchPanel() {
        return showSpectraMatchPanel;
    }

    public synchronized void setShowSpectraMatchPanel(boolean showSpectraMatchPanel) {
        boolean old = this.showSpectraMatchPanel;
        this.showSpectraMatchPanel = showSpectraMatchPanel;
        pcs.firePropertyChange("showSpectraMatchPanel", old, this.showSpectraMatchPanel);

    }
    //endregion

    //over all GUI instances
    private static Set<String> tutorialsThisSession = new HashSet<>();

    public synchronized boolean isAskedTutorialThisSession(String tutorialKey) {
        return tutorialsThisSession.contains(tutorialKey);
    }

    public synchronized void setTutorialKnownForThisSession(String tutorialKey) {
        tutorialsThisSession.add(tutorialKey);
    }
}
