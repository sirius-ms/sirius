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

import de.unijena.bioinf.ms.middleware.model.search.SearchableField;
import de.unijena.bioinf.ms.middleware.service.search.mappers.LipidAnnotationMapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * {@code lipid} is a flag, not text.
 * <p>
 * The mapper writes it as {@code true} and only for a feature that has a lipid annotation at all, so the value
 * says nothing and its presence says everything. Reported as a boolean with the single value it can take, a
 * client can see that for itself; reported as the keyword lucene stores, it looks like a text field that
 * accepts anything.
 * <p>
 * The other fields of the same mapper hold what they look like they hold, so nothing is said about them.
 */
public class LipidFieldTypes implements FieldTypes {

    @Override
    public @Nullable SearchableField.FieldType typeOf(@NotNull String fieldName) {
        return fieldName.endsWith(LipidAnnotationMapper.LIPID)
                ? SearchableField.FieldType.BOOLEAN
                : null;
    }
}
