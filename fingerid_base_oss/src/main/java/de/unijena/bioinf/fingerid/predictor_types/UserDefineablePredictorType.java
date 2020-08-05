/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
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
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.fingerid.predictor_types;

import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.stream.Collectors;

/**
 * Defines default PredictorTypes that can be used for Structure prediction.
 * Currently CSI_FINGERID is the ONLY valid option.
* */

public enum UserDefineablePredictorType {
    CSI_FINGERID(PredictorType.CSI_FINGERID_POSITIVE, PredictorType.CSI_FINGERID_NEGATIVE);//CSI for negative ionization
//    IOKR(PredictorType.IOKR_POSITIVE, PredictorType.IOKR_NEGATIVE);

    private final PredictorType positive;
    private final PredictorType negative;

    UserDefineablePredictorType(PredictorType positive, PredictorType negative) {
        this.positive = positive;
        this.negative = negative;
    }

    public PredictorType toPredictorType(@NotNull PrecursorIonType ion) {
        return toPredictorType(ion.getCharge());
    }

    public PredictorType toPredictorType(@NotNull Ionization ion) {
        return toPredictorType(ion.getCharge());
    }

    public PredictorType toPredictorType(final int charge) {
        if (charge < 0)
            return negative;
        if (charge > 0)
            return positive;
        throw new IllegalArgumentException("Uncharged is not allowed!");
    }

    public boolean contains(@NotNull final PredictorType type) {
        return positive == type || negative == type;
    }

    public EnumSet<PredictorType> asEnumSet() {
        return EnumSet.of(positive, negative);
    }

    public static EnumSet<PredictorType> toPredictorTypes(@NotNull final PrecursorIonType ion, @NotNull UserDefineablePredictorType... types) {
        return toPredictorTypes(ion.getIonization(), types);
    }

    public static EnumSet<PredictorType> toPredictorTypes(@NotNull final Ionization ion, @NotNull UserDefineablePredictorType... types) {
        return EnumSet.copyOf(Arrays.stream(types).map((it) -> it.toPredictorType(ion)).collect(Collectors.toSet()));
    }

    public static EnumSet<PredictorType> toPredictorTypes(@NotNull final PrecursorIonType ion, @NotNull Collection<UserDefineablePredictorType> types) {
        return toPredictorTypes(ion.getIonization(), types);
    }

    public static EnumSet<PredictorType> toPredictorTypes(@NotNull final Ionization ion, @NotNull Collection<UserDefineablePredictorType> types) {
        return EnumSet.copyOf(types.stream().map((it) -> it.toPredictorType(ion)).collect(Collectors.toSet()));
    }
}
