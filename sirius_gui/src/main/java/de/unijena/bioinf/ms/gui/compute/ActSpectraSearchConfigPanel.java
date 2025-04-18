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

import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.gui.utils.jCheckboxList.CheckBoxListItem;
import io.sirius.ms.sdk.model.SearchableDatabase;

import java.awt.event.ItemEvent;
import java.util.Map;

public class ActSpectraSearchConfigPanel extends ActivatableConfigPanel<SpectraSearchConfigPanel> {

    public ActSpectraSearchConfigPanel(SiriusGui gui, GlobalConfigPanel globalConfigPanel, boolean ms2) {
        super(gui, "Spectral Matching", Icons.SIRIUS.derive(32,32), () -> new SpectraSearchConfigPanel(globalConfigPanel, ms2));

        globalConfigPanel.getSearchDBList().checkBoxList.addCheckBoxListener(e -> {
            @SuppressWarnings("unchecked")
            SearchableDatabase item = (SearchableDatabase) ((CheckBoxListItem<Object>) e.getItem()).getValue();
            if (item.isCustomDb()){
                if (e.getStateChange() == ItemEvent.SELECTED && !activationButton.isSelected() ) {
                    activationButton.setSelected(true);
                    setComponentsEnabled(activationButton.isSelected());
                } else if (e.getStateChange() == ItemEvent.DESELECTED && activationButton.isSelected()) {
                    if (globalConfigPanel.getSearchDBList().checkBoxList.getCheckedItems().stream().noneMatch(SearchableDatabase::isCustomDb)) {
                        activationButton.setSelected(false);
                        setComponentsEnabled(activationButton.isSelected());
                    }
                }
            }
        });

        if (activationButton.isSelected()) {
            if (globalConfigPanel.getSearchDBList().checkBoxList.getCheckedItems().stream().noneMatch(SearchableDatabase::isCustomDb)) {
                activationButton.setSelected(false);
                setComponentsEnabled(activationButton.isSelected());
            }
        }
    }

    @Override
    public void applyValuesFromPreset(boolean enable, Map<String, String> preset) {
        if (enable)
            if (content.globalConfigPanel.getSearchDBList().checkBoxList.getCheckedItems().stream().noneMatch(SearchableDatabase::isCustomDb))
                enable = false;
        super.applyValuesFromPreset(enable, preset);
    }
}
