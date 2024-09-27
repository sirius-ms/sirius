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
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.gui.utils.jCheckboxList.JCheckBoxList;
import io.sirius.ms.sdk.SiriusClient;
import io.sirius.ms.sdk.model.SearchableDatabase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class DBSelectionList extends JCheckBoxList<SearchableDatabase> {


    public DBSelectionList(@NotNull List<SearchableDatabase> values) {
        this(null, values);
    }

    public DBSelectionList(@Nullable String descKey, @NotNull List<SearchableDatabase> values) {
        super(values, (a,b) -> Objects.equals(a.getDatabaseId(), b.getDatabaseId()));
        if (descKey != null)
            GuiUtils.assignParameterToolTip(this, descKey);
    }

    public static DBSelectionList fromSearchableDatabases(SiriusClient client){
        return fromSearchableDatabases(true, client);
    }
    public static DBSelectionList fromSearchableDatabases(boolean includeCustom, SiriusClient client){
        return fromSearchableDatabases(null, includeCustom, client);
    }
    public static DBSelectionList fromSearchableDatabases(@Nullable String descriptionKey, boolean includeCustom, SiriusClient client){
        return fromSearchableDatabases(descriptionKey, includeCustom, client, Collections.emptyList());
    }

    public static DBSelectionList fromSearchableDatabases(SiriusClient client, Collection<SearchableDatabase> exclude){
        return fromSearchableDatabases(null, true, client, exclude);
    }

    public static DBSelectionList fromSearchableDatabases(@Nullable String descriptionKey, boolean includeCustom, SiriusClient client, Collection<SearchableDatabase> exclude){
        List<SearchableDatabase> dbLsit = (includeCustom ? client.databases().getDatabases(false) : client.databases().getIncludedDatabases(false))
                .stream()
                .filter(SearchableDatabase::isSearchable)
                .filter(s -> !exclude.contains(s))
                .sorted(getDatabaseComparator(client))
                .toList();
        return new DBSelectionList(descriptionKey, dbLsit);
    }


    @Override
    public boolean isSelectionEqual(JCheckBoxList<SearchableDatabase> other){
        Set<String> checked1 = this.getCheckedItems().stream().map(SearchableDatabase::getDatabaseId).collect(Collectors.toSet());
        Set<String> checked2 = other.getCheckedItems().stream().map(SearchableDatabase::getDatabaseId).collect(Collectors.toSet());
        return checked1.equals(checked2);
    }

    public static Comparator<SearchableDatabase> getDatabaseComparator(SiriusClient client) {
        SearchableDatabase pubchem = client.databases().getDatabase(DataSource.PUBCHEM.name(), false);
        return Comparator
                .comparing(SearchableDatabase::isCustomDb, Comparator.reverseOrder())
                .thenComparing(p -> p.equals(pubchem), Comparator.reverseOrder())
                .thenComparing(SearchableDatabase::getDisplayName);
    }

}
