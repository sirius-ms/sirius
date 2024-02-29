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
import java.awt.*;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */

//here we can show fingerid options. If it becomes to much, we can change this to a setting like tabbed pane
public class FingerblastConfigPanel extends SubToolConfigPanel<FingerblastOptions> {
    private final StructureSearchStrategy structureSearchStrategy;
    private final JCheckBox pubChemFallback;

    public FingerblastConfigPanel(@Nullable final JCheckBoxList<CustomDataSources.Source> syncSource) {
        super(FingerblastOptions.class);

        structureSearchStrategy = new StructureSearchStrategy(syncSource);
        pubChemFallback = new JCheckBox();

        parameterBindings.put("StructureSearchDB", () -> {
            List<CustomDataSources.Source> checkedDBs = structureSearchStrategy.getStructureSearchDBs();
            return checkedDBs.isEmpty() ? null : checkedDBs.stream()
                    .map(CustomDataSources.Source::id)
                    .filter(Objects::nonNull)
                    .filter(db -> !(db.equals(DataSource.PUBCHEM.name()) && pubChemFallback.isSelected()))
                    .collect(Collectors.joining(","));
        });

        pubChemFallback.setSelected(true);
        pubChemFallback.setToolTipText("Search in the specified set of databases and use the PubChem database as fallback if no good hit is available");

        //confidence score approximate mode settings
        JComboBox<ExpansiveSearchConfidenceMode.Mode> confidenceModeBox =  GuiUtils.makeParameterComboBoxFromDescriptiveValues(ExpansiveSearchConfidenceMode.Mode.getActiveModes());

        //layout the panel
        final TwoColumnPanel additionalOptions = new TwoColumnPanel();
        JPanel checkBoxPanel = new JPanel();
        checkBoxPanel.setLayout(new FlowLayout(FlowLayout.LEADING));
        checkBoxPanel.add(pubChemFallback);
        additionalOptions.addNamed("PubChem as fallback", checkBoxPanel);
        JLabel confLabel = new JLabel("Confidence mode");
        additionalOptions.add(confLabel, confidenceModeBox);

        checkBoxPanel.setPreferredSize(new Dimension(confidenceModeBox.getPreferredSize().width, checkBoxPanel.getPreferredSize().height));  // Prevent resizing on unchecking checkbox

        add(new TextHeaderBoxPanel("General", additionalOptions));
        add(structureSearchStrategy);

        parameterBindings.put("ExpansiveSearchConfidenceMode.confidenceScoreSimilarityMode", () -> {
            if (!pubChemFallback.isSelected()) {
                return "OFF";
            }
            if (confidenceModeBox.getSelectedItem() == ExpansiveSearchConfidenceMode.Mode.EXACT) {
                return "EXACT";
            }
            return "APPROXIMATE";
        });

        pubChemFallback.addActionListener(e -> {
            List.of(confLabel, confidenceModeBox).forEach(c -> c.setVisible(pubChemFallback.isSelected()));
            refreshPubChem();
        });
    }

    public void refreshPubChem() {
        JCheckBoxList<CustomDataSources.Source> dbList = structureSearchStrategy.getSearchDBList().checkBoxList;
        CustomDataSources.Source pubChemDB = CustomDataSources.getSourceFromName(DataSource.PUBCHEM.realName());
        if (pubChemFallback.isSelected()) {
            dbList.setItemEnabled(pubChemDB, false);
            dbList.setItemToolTip(pubChemDB, "PubChem will be used as fallback");
        } else {
            dbList.setItemEnabled(pubChemDB, true);
            dbList.setItemToolTip(pubChemDB, null);
        }
    }

    public JCheckboxListPanel<CustomDataSources.Source> getSearchDBList() {
        return structureSearchStrategy.getSearchDBList();
    }
}
