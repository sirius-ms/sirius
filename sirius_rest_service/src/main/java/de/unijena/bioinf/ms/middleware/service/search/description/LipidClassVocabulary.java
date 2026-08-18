/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2024 Bright Giant GmbH
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

package de.unijena.bioinf.ms.middleware.service.search.description;

import de.unijena.bioinf.elgordo.LipidClass;
import de.unijena.bioinf.ms.middleware.service.search.mappers.LipidAnnotationMapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * The lipid classes El Gordo can annotate, as the fields of {@link LipidAnnotationMapper} hold them.
 * <p>
 * A class is indexed under its long name and its LipidMaps id, neither of which anyone recalls unprompted -
 * "Diacylglycerophosphocholine", "LMGP01010000" - which is exactly what a vocabulary is for. Two classes share
 * a long name (MGDG and DGDG are both Glycosyldiradylglycerol) and several have no LipidMaps id, so both lists
 * are what remains after that.
 * <p>
 * The abbreviations (PC, TG, HexCer) are deliberately <b>not</b> offered. They are informal shorthand, and they
 * are not what the index holds - offering one would hand a client a value that only works because a query
 * rewriter translates it. They are made to match instead, see {@code LipidClassQueryRewriter}.
 */
public class LipidClassVocabulary implements FieldVocabulary {

    @Override
    public @Nullable List<String> getPossibleValues(@NotNull String fieldName) {
        if (fieldName.endsWith(LipidAnnotationMapper.LIPID_CLASS_NAME))
            return LONG_NAMES;
        if (fieldName.endsWith(LipidAnnotationMapper.LIPID_MAPS_ID))
            return LIPID_MAPS_IDS;
        return null;
    }

    private static final List<String> LONG_NAMES = Arrays.stream(LipidClass.values())
            .map(LipidClass::longName)
            .distinct()
            .toList();

    private static final List<String> LIPID_MAPS_IDS = Arrays.stream(LipidClass.values())
            .map(LipidClass::getLipidMapsId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
}
