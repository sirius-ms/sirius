/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.persistence.model.sirius;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.unijena.bioinf.confidence_score.ExpansiveSearchConfidenceMode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@SuperBuilder
@Getter
@Setter
@NoArgsConstructor
public class CsiStructureSearchResult extends StructureSearchResult<CsiStructureMatch> {
    //ID: alignedFeatureId is the pk
    private Double confidenceExact;
    private Double confidenceApprox;
    private ExpansiveSearchConfidenceMode.Mode expansiveSearchConfidenceMode;

    private List<String> specifiedDatabases;
    //usually just pubchem or empty
    private List<String> expandedDatabases;

    @JsonIgnore
    public boolean isExpansionHasHappened(){
        return expandedDatabases != null && !expandedDatabases.isEmpty();
    }

    @JsonIgnore
    public Stream<String> getSearchedDatabases(){
        return Stream.concat(
                Optional.ofNullable(specifiedDatabases).stream().flatMap(Collection::stream),
                Optional.ofNullable(expandedDatabases).stream().flatMap(Collection::stream));
    }
}
