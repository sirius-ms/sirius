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

import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.gui.utils.jCheckboxList.JCheckBoxList;
import de.unijena.bioinf.ms.nightsky.sdk.NightSkyClient;
import de.unijena.bioinf.ms.nightsky.sdk.model.SearchableDatabase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class DBSelectionList extends JCheckBoxList<SearchableDatabase> {


    public DBSelectionList(@NotNull List<SearchableDatabase> values) {
        this(null, values);
    }

    public DBSelectionList(@Nullable String descKey, @NotNull List<SearchableDatabase> values) {
        super(values, (a,b) -> Objects.equals(a.getDatabaseId(), b.getDatabaseId()));
        if (descKey != null)
            GuiUtils.assignParameterToolTip(this, descKey);
    }

    public static DBSelectionList fromSearchableDatabases(NightSkyClient client){
        return fromSearchableDatabases(true, client);
    }
    public static DBSelectionList fromSearchableDatabases(boolean includeCustom, NightSkyClient client){
        return fromSearchableDatabases(null, includeCustom, client);
    }
    public static DBSelectionList fromSearchableDatabases(@Nullable String descriptionKey, boolean includeCustom, NightSkyClient client){
        List<SearchableDatabase> dbLsit = (includeCustom ? client.databases().getDatabases(false) : client.databases().getIncludedDatabases(false))
                .stream()
                .filter(SearchableDatabase::isSearchable)
                .sorted(Comparator.comparing(SearchableDatabase::getDatabaseId))
                .toList();
        return new DBSelectionList(descriptionKey, dbLsit);
    }

}
