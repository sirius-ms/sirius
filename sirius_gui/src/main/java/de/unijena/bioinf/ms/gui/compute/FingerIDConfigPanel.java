/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.gui.compute;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.PossibleAdducts;
import de.unijena.bioinf.chemdb.DataSource;
import de.unijena.bioinf.chemdb.custom.CustomDataSources;
import de.unijena.bioinf.ms.frontend.subtools.fingerid.FingerIdOptions;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.gui.utils.TextHeaderBoxPanel;
import de.unijena.bioinf.ms.gui.utils.TwoColumnPanel;
import de.unijena.bioinf.ms.gui.utils.jCheckboxList.JCheckBoxList;
import de.unijena.bioinf.ms.gui.utils.jCheckboxList.JCheckboxListPanel;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */

//here we can show fingerid options. If it becomes to much, we can change this to a setting like tabbed pane
public class FingerIDConfigPanel extends SubToolConfigPanel<FingerIdOptions> {
    //todo sync db selection with sirius panel

    protected final JCheckboxListPanel<CustomDataSources.Source> searchDBList;
    public final JCheckboxListPanel<String> adductOptions;
    protected final JToggleButton enforceAdducts;

    public FingerIDConfigPanel(final JCheckBoxList<String> sourceIonization, @Nullable final JCheckBoxList<CustomDataSources.Source> syncSource) {
        super(FingerIdOptions.class);

        // configure database to search list
        searchDBList = new JCheckboxListPanel<>(new DBSelectionList(), "Search in DBs:");
        GuiUtils.assignParameterToolTip(searchDBList, "StructureSearchDB");
        parameterBindings.put("StructureSearchDB", () -> searchDBList.checkBoxList.getCheckedItems().isEmpty() ? null : String.join(",", getStructureSearchDBStrings()));
        add(searchDBList);

        adductOptions = new JCheckboxListPanel<>(new AdductSelectionList(sourceIonization), "Fallback Adducts");
        GuiUtils.assignParameterToolTip(adductOptions, "AdductSettings.fallback");
        parameterBindings.put("AdductSettings.fallback", () -> getSelectedAdducts().toString());
        add(adductOptions);
        enforceAdducts =  new JToggleButton("enforce", false);
        enforceAdducts.setToolTipText(GuiUtils.formatToolTip("Enforce the selected adducts instead of using them only as fallback."));
        adductOptions.buttons.add(enforceAdducts);
        parameterBindings.put("AdductSettings.enforced", () -> enforceAdducts.isSelected() ? getSelectedAdducts().toString() : null);

        final TwoColumnPanel additionalOptions = new TwoColumnPanel();
        additionalOptions.addNamed("Formula score threshold", makeParameterCheckBox("FormulaResultThreshold"));

        add(new TextHeaderBoxPanel("Additional Options", additionalOptions));

        searchDBList.checkBoxList.check(CustomDataSources.getSourceFromName(DataSource.BIO.realName()));

        if (syncSource != null)
            syncSource.addListSelectionListener(e -> {
                searchDBList.checkBoxList.uncheckAll();
                if (syncSource.getCheckedItems().isEmpty())
                    searchDBList.checkBoxList.check(CustomDataSources.getSourceFromName(DataSource.BIO.realName()));
                else
                    searchDBList.checkBoxList.checkAll(syncSource.getCheckedItems());
            });
    }

    public PossibleAdducts getSelectedAdducts() {
        return adductOptions.checkBoxList.getCheckedItems().stream().map(PrecursorIonType::parsePrecursorIonType)
                .flatMap(Optional::stream).collect(Collectors.collectingAndThen(Collectors.toSet(), PossibleAdducts::new));
    }

    public List<CustomDataSources.Source> getStructureSearchDBs() {
        return searchDBList.checkBoxList.getCheckedItems();
    }

    public List<String> getStructureSearchDBStrings() {
        return getStructureSearchDBs().stream().map(CustomDataSources.Source::id).filter(Objects::nonNull).collect(Collectors.toList());
    }
}
