/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
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
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import de.unijena.bioinf.projectspace.FieldVocabulary;
import de.unijena.bioinf.projectspace.IndexField;
import de.unijena.bioinf.projectspace.SearchableFieldDoc;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

// ATTENTION: This class is also used in public Nightsky API please modify with care and check implications for API.

@SuperBuilder
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ComputedSubtools extends AlignedFeatureAnnotation {
    // each flag says whether a tool has run, so each is asked about in the positive: a client completes
    // "computedTools.zodiac" the moment it is picked, and "has NOT run" is the negation of it
    @IndexField
    @SearchableFieldDoc(possibleValues = FieldVocabulary.PresenceFlag.class)
    private boolean librarySearch = false;
    @IndexField
    @SearchableFieldDoc(possibleValues = FieldVocabulary.PresenceFlag.class)
    private boolean formulaSearch = false;
    @IndexField
    @SearchableFieldDoc(possibleValues = FieldVocabulary.PresenceFlag.class)
    private boolean zodiac = false;
    @IndexField
    @SearchableFieldDoc(possibleValues = FieldVocabulary.PresenceFlag.class)
    private boolean fingerprint = false;
    @IndexField
    @SearchableFieldDoc(possibleValues = FieldVocabulary.PresenceFlag.class)
    private boolean canopus = false;
    @IndexField
    @SearchableFieldDoc(possibleValues = FieldVocabulary.PresenceFlag.class)
    private boolean structureSearch = false;
    @IndexField
    @SearchableFieldDoc(possibleValues = FieldVocabulary.PresenceFlag.class)
    private boolean deNovoSearch = false;

    @JsonIgnore
    public boolean hasResults(){
        return librarySearch || formulaSearch || zodiac || fingerprint || canopus || structureSearch || deNovoSearch;
    }

    @JsonIgnore
    public boolean noResults(){
        return !hasResults();
    }
}
