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

import de.unijena.bioinf.chemdb.custom.CustomDataSources;
import de.unijena.bioinf.confidence_score.ExpansiveSearchConfidenceMode;
import de.unijena.bioinf.ms.frontend.subtools.fingerblast.FingerblastOptions;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.gui.utils.TextHeaderBoxPanel;
import de.unijena.bioinf.ms.gui.utils.TwoColumnPanel;
import de.unijena.bioinf.ms.gui.utils.jCheckboxList.JCheckBoxList;
import de.unijena.bioinf.ms.gui.utils.jCheckboxList.JCheckboxListPanel;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ItemEvent;

/**
 * @author Markus Fleischauer
 */

//here we can show fingerid options. If it becomes to much, we can change this to a setting like tabbed pane
public class FingerblastConfigPanel extends SubToolConfigPanel<FingerblastOptions> {
    protected StructureSearchStrategy structureSearchStrategy;

    public FingerblastConfigPanel(@Nullable final JCheckBoxList<CustomDataSources.Source> syncSource) {
        super(FingerblastOptions.class);


        //select search strategy
        JComboBox<StructureSearchStrategy.Strategy> strategyBox =  GuiUtils.makeParameterComboBoxFromDescriptiveValues(StructureSearchStrategy.Strategy.values());
        structureSearchStrategy = new StructureSearchStrategy((StructureSearchStrategy.Strategy) strategyBox.getSelectedItem(), parameterBindings, syncSource);


        //confidence score approximate mode settings
        JComboBox<ExpansiveSearchConfidenceMode.Mode> confidenceModeBox =  makeGenericOptionComboBox("ExpansiveSearchConfidenceMode.confidenceScoreSimilarityMode", ExpansiveSearchConfidenceMode.Mode.class);
        confidenceModeBox.setSelectedItem(ExpansiveSearchConfidenceMode.Mode.APPROXIMATE);
        parameterBindings.put("ExpansiveSearchConfidenceMode.confidenceScoreSimilarityMode", () ->
                isStrategy(StructureSearchStrategy.Strategy.NO_FALLBACK, strategyBox) ? ExpansiveSearchConfidenceMode.Mode.OFF.name() : ((ExpansiveSearchConfidenceMode.Mode) confidenceModeBox.getSelectedItem()).name());
        confidenceModeBox.setVisible(isStrategy(StructureSearchStrategy.Strategy.PUBCHEM_AS_FALLBACK, strategyBox));

        //layout the panel
        final TwoColumnPanel additionalOptions = new TwoColumnPanel();
        additionalOptions.addNamed("Database search strategy", strategyBox);
        JLabel confLabel = new JLabel("Confidence mode");
        additionalOptions.add(confLabel, confidenceModeBox);

        add(new TextHeaderBoxPanel("General", additionalOptions));
        add(structureSearchStrategy);


        //set confidence mode based on strategy
        strategyBox.addItemListener(e -> {
            if (e.getStateChange() != ItemEvent.SELECTED) {
                return;
            }
            //todo NewWorkflow: implement to use this parameter 'StructureSearchStrategy'
            int panelIndex = GuiUtils.getComponentIndex(this, structureSearchStrategy);
            this.remove(panelIndex);
            structureSearchStrategy = new StructureSearchStrategy((StructureSearchStrategy.Strategy) strategyBox.getSelectedItem(), parameterBindings, syncSource);
            this.add(structureSearchStrategy, panelIndex);

            if (isStrategy(StructureSearchStrategy.Strategy.NO_FALLBACK, strategyBox)) {
                confLabel.setVisible(false);
                confidenceModeBox.setVisible(false);
            } else {
                confLabel.setVisible(true);
                confidenceModeBox.setVisible(true);
            }

            revalidate();
        });
    }

    public JCheckboxListPanel<CustomDataSources.Source> getSearchDBList() {
        return structureSearchStrategy.getSearchDBList();
    }

    private boolean isStrategy(StructureSearchStrategy.Strategy strategy, JComboBox<StructureSearchStrategy.Strategy> strategyBox) {
        return (StructureSearchStrategy.Strategy) strategyBox.getSelectedItem() == strategy;
    }
}
