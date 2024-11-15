/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
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

package de.unijena.bioinf.ms.gui.compute;

import de.unijena.bioinf.ms.frontend.subtools.zodiac.ZodiacOptions;
import de.unijena.bioinf.ms.gui.utils.TextHeaderBoxPanel;
import de.unijena.bioinf.ms.gui.utils.TwoColumnPanel;

import javax.swing.*;
import java.util.Map;

public class ZodiacConfigPanel extends SubToolConfigPanelAdvancedParams<ZodiacOptions> {

    private final JSpinner candidatesAt300, candidatesAt800, edgeThreshold, minLocalConnections;
    private final JCheckBox twoStep;

    public ZodiacConfigPanel(boolean displayAdvancedParameters) {
        super(ZodiacOptions.class, displayAdvancedParameters);

        candidatesAt300 = makeIntParameterSpinner("ZodiacNumberOfConsideredCandidatesAt300Mz", -1, 10000, 1);
        candidatesAt800 = makeIntParameterSpinner("ZodiacNumberOfConsideredCandidatesAt800Mz", -1, 10000, 1);
        twoStep = makeParameterCheckBox("ZodiacRunInTwoSteps");
        edgeThreshold = makeDoubleParameterSpinner("ZodiacEdgeFilterThresholds.thresholdFilter", .5, 1, .01);
        minLocalConnections = makeIntParameterSpinner("ZodiacEdgeFilterThresholds.minLocalConnections", 0, 10000, 1);

        createPanel();
    }
    private void createPanel() {

        final TwoColumnPanel general = new TwoColumnPanel();
        general.addNamed("Considered candidates at 300m/z", candidatesAt300);
        general.addNamed("Considered candidates at 800m/z", candidatesAt800);
        general.addNamed("Use  2-step approach", twoStep);
        TextHeaderBoxPanel generalPanel = new TextHeaderBoxPanel("General", general);
        addAdvancedComponent(generalPanel);
        add(generalPanel);

        final TwoColumnPanel edgeFilter = new TwoColumnPanel();
        edgeFilter.addNamed("Edge Threshold", edgeThreshold);
        edgeFilter.addNamed("Min Local Connections", minLocalConnections);
        TextHeaderBoxPanel edgePanel = new TextHeaderBoxPanel("Edge Filters", edgeFilter);
        addAdvancedComponent(edgePanel);
        add(edgePanel);
    }

    @Override
    public void applyValuesFromPreset(Map<String, String> preset) {
        candidatesAt300.setValue(Integer.parseInt(preset.get("ZodiacNumberOfConsideredCandidatesAt300Mz")));
        candidatesAt800.setValue(Integer.parseInt(preset.get("ZodiacNumberOfConsideredCandidatesAt800Mz")));
        twoStep.setSelected(Boolean.parseBoolean(preset.get("ZodiacRunInTwoSteps")));
        edgeThreshold.setValue(Double.parseDouble(preset.get("ZodiacEdgeFilterThresholds.thresholdFilter")));
        minLocalConnections.setValue(Integer.parseInt(preset.get("ZodiacEdgeFilterThresholds.minLocalConnections")));
    }
}
