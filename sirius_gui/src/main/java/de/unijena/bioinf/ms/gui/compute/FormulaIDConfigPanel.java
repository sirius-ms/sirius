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

import de.unijena.bioinf.ms.frontend.subtools.sirius.SiriusOptions;
import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.utils.TextHeaderBoxPanel;
import de.unijena.bioinf.ms.gui.utils.TwoColumnPanel;
import de.unijena.bioinf.projectspace.InstanceBean;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;

/**
 * Panel to configure SIRIUS Computations
 * Provides CONFIGS for SiriusSubTool
 *
 * @author Marcus Ludwig, Markus Fleischauer
 * @since 12.01.17
 */
public class FormulaIDConfigPanel extends SubToolConfigPanelAdvancedParams<SiriusOptions> {
    protected Logger logger = LoggerFactory.getLogger(FormulaIDConfigPanel.class);


    protected JSpinner candidatesSpinner, candidatesPerIonSpinner, treeTimeout, comoundTimeout, mzHeuristic, mzHeuristicOnly;
    protected JCheckBox isotopeSettingsFilter, enforceElGordo;

    public enum Strategy {IGNORE, SCORE} //todo remove if Filter is implemented

    protected JComboBox<Strategy> ms2IsotpeSetting;

    @Getter
    protected FormulaSearchStrategy formulaSearchStrategy;


    protected final List<InstanceBean> allInstances;
    protected final List<InstanceBean> ecs;

    private final GlobalConfigPanel computeConfigPanel;

    protected final SiriusGui gui;

    protected boolean hasMs2;


    public FormulaIDConfigPanel(SiriusGui gui, List<InstanceBean> ecs, GlobalConfigPanel computeConfigPanel, boolean ms2) {
        super(SiriusOptions.class, false);
        this.allInstances = gui.getMainFrame().getCompounds();
        this.ecs = ecs;
        this.gui = gui;
        this.computeConfigPanel = computeConfigPanel;
        this.hasMs2 = ms2;

        createPanel();
    }

    private void createPanel() {

        parameterBindings.put("AdductSettings.prioritizeInputFileAdducts", () -> Boolean.toString(isBatchDialog()));

        // configure small stuff panel
        {
            final TwoColumnPanel smallParameters = new TwoColumnPanel();
            isotopeSettingsFilter = makeParameterCheckBox("IsotopeSettings.filter");
            smallParameters.addNamed("Filter by isotope pattern", isotopeSettingsFilter);

            ms2IsotpeSetting = makeParameterComboBox("IsotopeMs2Settings", Strategy.class);

            if (hasMs2)
                smallParameters.addNamed("MS/MS isotope scorer", ms2IsotpeSetting);

            candidatesSpinner = makeIntParameterSpinner("NumberOfCandidates", 1, 10000, 1);
            smallParameters.addNamed("Candidates stored", candidatesSpinner);

            candidatesPerIonSpinner = makeIntParameterSpinner("NumberOfCandidatesPerIonization", 0, 10000, 1);
            smallParameters.addNamed("Min candidates per ionization stored", candidatesPerIonSpinner);

            enforceElGordo = makeParameterCheckBox("EnforceElGordoFormula");  //El Gordo detects lipids and by default fixes the formula
            smallParameters.addNamed("Fix formula for detected lipid", enforceElGordo);

            TextHeaderBoxPanel header = new TextHeaderBoxPanel("General", smallParameters);
            add(header, "aligny top, wrap");
            addAdvancedComponent(header);
        }

        //configure formulaSearchStrategy
        formulaSearchStrategy = new FormulaSearchStrategy(gui, ecs, hasMs2, isBatchDialog(), parameterBindings, computeConfigPanel);
        add(formulaSearchStrategy, "aligny top, wrap");

        treeTimeout = makeIntParameterSpinner("Timeout.secondsPerTree", 0, Integer.MAX_VALUE, 1);
        comoundTimeout = makeIntParameterSpinner("Timeout.secondsPerInstance", 0, Integer.MAX_VALUE, 1);
        mzHeuristic = makeIntParameterSpinner("UseHeuristic.useHeuristicAboveMz", 0, 3000, 5);
        mzHeuristicOnly = makeIntParameterSpinner("UseHeuristic.useOnlyHeuristicAboveMz", 0, 3000, 5);

        // ilp timeouts
        if (hasMs2) {
            final TwoColumnPanel ilpOptions = new TwoColumnPanel();

            ilpOptions.addNamed("Tree timeout", treeTimeout);
            ilpOptions.addNamed("Compound timeout", comoundTimeout);
            ilpOptions.addNamed("Use heuristic above m/z", mzHeuristic);
            ilpOptions.addNamed("Use heuristic only above m/z", mzHeuristicOnly);

            TextHeaderBoxPanel technicalParameters = new TextHeaderBoxPanel("Fragmentation tree computation", ilpOptions);
            add(technicalParameters, "aligny top, wrap");
            addAdvancedComponent(technicalParameters);
        }
    }

    protected boolean isBatchDialog() {
        return ecs.size() != 1;
    }

    public int getNumOfCandidates() {
        return ((SpinnerNumberModel) candidatesSpinner.getModel()).getNumber().intValue();
    }

    public int getNumOfCandidatesPerIon() {
        return ((SpinnerNumberModel) candidatesPerIonSpinner.getModel()).getNumber().intValue();
    }

    public void applyValuesFromPreset(Map<String, String> preset) {
        isotopeSettingsFilter.setSelected(Boolean.parseBoolean(preset.get("IsotopeSettings.filter")));

        String isotopeMs2Setting = preset.get("IsotopeMs2Settings");
        try {
            ms2IsotpeSetting.setSelectedItem(Strategy.valueOf(isotopeMs2Setting));
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Could not parse MS/MS isotope scorer " + isotopeMs2Setting + ".");
        }

        candidatesSpinner.setValue(Integer.parseInt(preset.get("NumberOfCandidates")));
        candidatesPerIonSpinner.setValue(Integer.parseInt(preset.get("NumberOfCandidatesPerIonization")));

        enforceElGordo.setSelected(Boolean.parseBoolean(preset.get("EnforceElGordoFormula")));

        treeTimeout.setValue(Integer.parseInt(preset.get("Timeout.secondsPerTree")));
        comoundTimeout.setValue(Integer.parseInt(preset.get("Timeout.secondsPerInstance")));
        mzHeuristic.setValue(Integer.parseInt(preset.get("UseHeuristic.useHeuristicAboveMz")));
        mzHeuristicOnly.setValue(Integer.parseInt(preset.get("UseHeuristic.useOnlyHeuristicAboveMz")));

        formulaSearchStrategy.applyValuesFromPreset(preset);
    }
}
