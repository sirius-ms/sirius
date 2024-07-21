/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
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

package de.unijena.bioinf.ms.middleware.model.compute.tools;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import de.unijena.bioinf.ms.frontend.subtools.msnovelist.MsNovelistOptions;
import de.unijena.bioinf.ms.frontend.subtools.msnovelist.NumberOfMsNovelistCandidates;
import de.unijena.bioinf.ms.middleware.model.compute.NullCheckMapBuilder;
import de.unijena.bioinf.ms.properties.PropertyManager;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.Map;

@Getter
@Setter
@SuperBuilder
@JsonIgnoreProperties(ignoreUnknown = true)
public class MsNovelist extends Tool<MsNovelistOptions> {

    private MsNovelist() {
        super(MsNovelistOptions.class);
    }

    /**
     * Number of structure candidates to be predicted by MsNovelist.
     * Max Value 128. Values > 128 will be set to 128.
     * Actual number of returned candidate might be lower du to duplicates being created by MsNovelist.
     */
    @Schema(nullable = true)
    Integer numberOfCandidateToPredict;


    @JsonIgnore
    @Override
    public Map<String, String> asConfigMap() {
        return new NullCheckMapBuilder()
                .putIfNonNull("NumberOfMsNovelistCandidates", numberOfCandidateToPredict)
                .toUnmodifiableMap();
    }

    public static MsNovelist buildDefault() {
        return builderWithDefaults().build();
    }
    public static MsNovelist.MsNovelistBuilder<?,?> builderWithDefaults() {
        return MsNovelist.builder()
                .enabled(false)
                .numberOfCandidateToPredict(PropertyManager.DEFAULTS.createInstanceWithDefaults(NumberOfMsNovelistCandidates.class).value);
    }
}
