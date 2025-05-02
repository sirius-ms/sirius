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

import de.unijena.bioinf.chemdb.DataSource;
import de.unijena.bioinf.confidence_score.ExpansiveSearchConfidenceMode;
import de.unijena.bioinf.ms.frontend.subtools.fingerblast.FingerblastOptions;
import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.gui.utils.TextHeaderBoxPanel;
import de.unijena.bioinf.ms.gui.utils.TwoColumnPanel;
import de.unijena.bioinf.ms.gui.utils.jCheckboxList.CheckBoxListItem;
import de.unijena.bioinf.ms.properties.PropertyManager;
import io.sirius.ms.sdk.model.SearchableDatabase;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Markus Fleischauer
 */

//here we can show fingerid options. If it becomes too much, we can change this to a setting like tabbed pane
public class FingerblastConfigPanel extends SubToolConfigPanel<FingerblastOptions> {
    private final JCheckBox pubChemFallback;
    private final JComboBox<ExpansiveSearchConfidenceMode.Mode> confidenceModeBox;

    protected final SiriusGui gui;
    protected final GlobalConfigPanel syncSource;

    public FingerblastConfigPanel(SiriusGui gui, @NotNull final GlobalConfigPanel searchDbSource) {
        super(FingerblastOptions.class);
        this.gui = gui;
        this.syncSource = searchDbSource;


        pubChemFallback = new JCheckBox();
        pubChemFallback.setToolTipText("Search in the specified set of databases and use the PubChem database as fallback if no good hit is available");

        parameterBindings.put("StructureSearchDB", () -> {
            List<SearchableDatabase> checkedDBs = searchDbSource.getSearchDBs();
            return checkedDBs.isEmpty() ? "," : checkedDBs.stream()
                    .map(SearchableDatabase::getDatabaseId)
                    .filter(db -> !(db.equals(DataSource.PUBCHEM.name()) && pubChemFallback.isSelected()))
                    .collect(Collectors.joining(","));
        });

        // listen for pubchem selection to enable/disable pubchem as fallback option
        searchDbSource.getSearchDBList().checkBoxList.addCheckBoxListener(e -> {
            @SuppressWarnings("unchecked")
            SearchableDatabase item = (SearchableDatabase) ((CheckBoxListItem<Object>) e.getItem()).getValue();
            if (item.getDatabaseId().equals(DataSource.PUBCHEM.name())) {
                pubChemFallback.setSelected(e.getStateChange() != ItemEvent.SELECTED);
            }
        });

        pubChemFallback.setSelected(searchDbSource.getSearchDBList().checkBoxList.getCheckedItems().stream()
                .map(SearchableDatabase::getDatabaseId)
                .anyMatch(n -> n.equals(DataSource.PUBCHEM.name())));

        //confidence score approximate mode settings
        confidenceModeBox = GuiUtils.makeParameterComboBoxFromDescriptiveValues(
                ExpansiveSearchConfidenceMode.Mode.getActiveModes(),
                PropertyManager.DEFAULTS.createInstanceWithDefaults(ExpansiveSearchConfidenceMode.class).confidenceScoreSimilarityMode);

        //layout the panel
        final TwoColumnPanel additionalOptions = new TwoColumnPanel();
        JPanel checkBoxPanel = new JPanel();
        checkBoxPanel.setLayout(new FlowLayout(FlowLayout.LEADING));
        checkBoxPanel.add(pubChemFallback);
        additionalOptions.addNamed("PubChem as fallback", checkBoxPanel);
        JLabel confLabel = new JLabel("Confidence mode");
        additionalOptions.add(confLabel, confidenceModeBox);

        checkBoxPanel.setPreferredSize(new Dimension(confidenceModeBox.getPreferredSize().width, checkBoxPanel.getPreferredSize().height));  // Prevent resizing on unchecking checkbox

        add(new TextHeaderBoxPanel("Search strategy", additionalOptions));

        parameterBindings.put("ExpansiveSearchConfidenceMode.confidenceScoreSimilarityMode", () -> {
            if (!pubChemFallback.isSelected()) {
                return "OFF";
            }
            if (confidenceModeBox.getSelectedItem() == ExpansiveSearchConfidenceMode.Mode.EXACT) {
                return "EXACT";
            }
            return "APPROXIMATE";
        });

        pubChemFallback.addChangeListener(e -> {
            List.of(confLabel, confidenceModeBox).forEach(c -> c.setVisible(pubChemFallback.isSelected()));
        });
    }

    @Override
    public void applyValuesFromPreset(Map<String, String> preset) {
        ExpansiveSearchConfidenceMode.Mode expansiveMode = ExpansiveSearchConfidenceMode.Mode.valueOf(preset.get("ExpansiveSearchConfidenceMode.confidenceScoreSimilarityMode"));
        if (expansiveMode == ExpansiveSearchConfidenceMode.Mode.OFF) {
            pubChemFallback.setSelected(false);
        } else {
            pubChemFallback.setSelected(true);
            confidenceModeBox.setSelectedItem(expansiveMode);
        }
    }
}
