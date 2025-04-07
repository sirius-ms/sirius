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

package de.unijena.bioinf.ms.frontend.completion;

import de.unijena.bioinf.chemdb.DataSource;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class DataSourceCandidates implements Iterable<String> {
    public static final List<String> VALID_DATA_SCOURCES = List.copyOf(Arrays.stream(
            DataSource.values()).filter(it -> it != DataSource.TRAIN).map(DataSource::name)
            .collect(Collectors.toList()));

    public static final String VALID_DATA_STRING = "Example: possible DBs: 'ALL,BIO,PUBCHEM,MESH,HMDB,KNAPSACK,CHEBI,PUBMED,KEGG,HSDB,MACONDA,METACYC,GNPS,ZINCBIO,YMDB,PLANTCYC,NORMAN,ADDITIONAL,PUBCHEMANNOTATIONBIO,PUBCHEMANNOTATIONDRUG,PUBCHEMANNOTATIONSAFETYANDTOXIC,PUBCHEMANNOTATIONFOOD,KEGGMINE,ECOCYCMINE,YMDBMINE'";
    public static final String PARAM_LABEL = "<dbName>[,<dbName>...]";

    @NotNull
    @Override
    public Iterator<String> iterator() {
        return VALID_DATA_SCOURCES.iterator();
    }
}
