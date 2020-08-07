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

import de.unijena.bioinf.chemdb.DataSource;
import de.unijena.bioinf.chemdb.SearchableDatabase;
import de.unijena.bioinf.chemdb.SearchableDatabases;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.gui.utils.jCheckboxList.JCheckBoxList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DBSelectionList extends JCheckBoxList<SearchableDatabase> {
    public final static Set<String> BLACK_LIST = Set.of(DataSource.ADDITIONAL.realName, DataSource.TRAIN.realName,
            DataSource.PUBCHEMANNOTATIONBIO.realName, DataSource.PUBCHEMANNOTATIONDRUG.realName, DataSource.PUBCHEMANNOTATIONFOOD.realName, DataSource.PUBCHEMANNOTATIONSAFETYANDTOXIC.realName,
            DataSource.SUPERNATURAL.realName
    );

    public DBSelectionList() {
        this((String) null);
    }

    public DBSelectionList(@Nullable String descriptionKey) {
        this(descriptionKey, SearchableDatabases.getAvailableDatabases().stream().
                filter(db -> !BLACK_LIST.contains(db.name())).
                collect(Collectors.toList()));
    }

    protected DBSelectionList(@Nullable String descKey, @NotNull DataSource... values) {
        this(descKey, Stream.of(values).map(DataSource::name).toArray(String[]::new));
    }

    protected DBSelectionList(@Nullable String descKey, @NotNull String... dbNameOrPath) {
        this(descKey, Stream.of(dbNameOrPath).map(SearchableDatabases::getDatabase).flatMap(Optional::stream).
                collect(Collectors.toList()));
    }

    public DBSelectionList(@NotNull List<SearchableDatabase> values) {
        this(null, values);
    }

    public DBSelectionList(@Nullable String descKey, @NotNull List<SearchableDatabase> values) {
        super(values, (a,b) -> a.name().equals(b.name()));
        if (descKey != null)
            GuiUtils.assignParameterToolTip(this, descKey);
    }
}
