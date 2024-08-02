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

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.util.StdConverter;
import lombok.ToString;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Optional;
import java.util.regex.Pattern;

@ToString(callSuper = true)
@JsonSerialize(converter = InstrumentConfig.ICSerializationConverter.class)
@JsonDeserialize(converter = Ionization.IonizationDeserializationConverter.class)
public final class Ionization extends InstrumentConfig {
    Ionization(@Nonnull String hupoId, @Nonnull String fullName, @Nullable String acronym, @Nonnull String[] synonyms, @Nullable String description, @Nonnull Pattern pattern) {
        super(hupoId, fullName, acronym, synonyms, description, pattern);
    }

    //region instance getters
    public static Optional<Ionization> byHupoId(@Nonnull String hupoId) {
        return InstrumentConfigs.byHupoId(hupoId, InstrumentConfigs.ionizationMap);
    }

    public static Optional<Ionization> byValue(@Nonnull String value) {
        return InstrumentConfigs.byValue(value, InstrumentConfigs.ionizationMap);
    }
    //endregion

    //region serialization
    public static class IonizationDeserializationConverter extends StdConverter<String, Ionization> {
        @Override
        public Ionization convert(String string) {
            return byHupoId(string).orElse(null);
        }
    }
    //endregion

}
