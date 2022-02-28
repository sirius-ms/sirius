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
import de.unijena.bioinf.ms.frontend.subtools.fingerblast.FingerblastOptions;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.gui.utils.TextHeaderBoxPanel;
import de.unijena.bioinf.ms.gui.utils.TwoColumnPanel;
import de.unijena.bioinf.ms.gui.utils.jCheckboxList.JCheckBoxList;
import de.unijena.bioinf.ms.gui.utils.jCheckboxList.JCheckboxListPanel;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */

//here we can show fingerid options. If it becomes to much, we can change this to a setting like tabbed pane
public class FingerblastConfigPanel extends SubToolConfigPanel<FingerblastOptions> {
    protected final JCheckboxListPanel<CustomDataSources.Source> searchDBList;


    public FingerblastConfigPanel(@Nullable final JCheckBoxList<CustomDataSources.Source> syncSource) {
        super(FingerblastOptions.class);

        // configure database to search list
        DBSelectionList innerList = new DBSelectionList();
        searchDBList = new JCheckboxListPanel<>(innerList, "Search DBs");
        GuiUtils.assignParameterToolTip(searchDBList, "StructureSearchDB");
        parameterBindings.put("StructureSearchDB", () -> searchDBList.checkBoxList.getCheckedItems().isEmpty() ? null : String.join(",", getStructureSearchDBStrings()));
        JButton allBut = new JButton("non in silico");
        allBut.setToolTipText(GuiUtils.formatToolTip("Select all but combinatorial databases."));
        allBut.addActionListener(c -> {
            innerList.uncheckAll();
            innerList.checkAll(allButInsilico());
        });
        searchDBList.buttons.add(allBut);
        add(searchDBList);

        final TwoColumnPanel additionalOptions = new TwoColumnPanel();
        additionalOptions.addNamed("Tag Lipids", makeParameterCheckBox("InjectElGordoCompounds"));

        add(new TextHeaderBoxPanel("General", additionalOptions));

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

    public List<CustomDataSources.Source> getStructureSearchDBs() {
        return searchDBList.checkBoxList.getCheckedItems();
    }

    public List<String> getStructureSearchDBStrings() {
        return getStructureSearchDBs().stream().map(CustomDataSources.Source::id).filter(Objects::nonNull).collect(Collectors.toList());
    }

    List<CustomDataSources.Source> allButInsilico = null;
    private List<CustomDataSources.Source> allButInsilico(){
       if (allButInsilico == null){
           allButInsilico = CustomDataSources.getSourcesFromNames(
                   Arrays.stream(DataSource.valuesNoALLNoMINES()).map(DataSource::realName)
                           .filter(s -> !DBSelectionList.BLACK_LIST.contains(s)).collect(Collectors.toList()));
       }
       return allButInsilico;

    }
}
