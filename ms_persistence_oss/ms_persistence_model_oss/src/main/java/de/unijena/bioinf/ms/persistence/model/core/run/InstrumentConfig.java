/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2023 Bright Giant GmbH
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
 *  You should have received a copy of the GNU General Public License along with SIRIUS.
 *  If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.persistence.model.core.run;

import com.fasterxml.jackson.databind.util.StdConverter;
import lombok.*;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.regex.Pattern;

/**
 * A super class for instrument-related meta information.
 *
 */
@ToString(onlyExplicitlyIncluded = true)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public abstract class InstrumentConfig {

    //region serialization
    public static class ICSerializationConverter<T extends InstrumentConfig> extends StdConverter<T, String> {
        @Override
        public String convert(T ic) {
            return ic.hupoId;
        }
    }
    //endregion

    //region instance variables
    @Getter
    @ToString.Include
    @EqualsAndHashCode.Include
    protected @Nonnull String hupoId;

    @Getter
    @ToString.Include
    protected @Nonnull String fullName;

    @Getter
    @ToString.Include
    protected @Nullable String acronym;

    @Getter
    protected @Nonnull String[] synonyms;

    @Getter
    protected @Nullable String description;

    protected @Nonnull Pattern pattern;
    //endregion

}
