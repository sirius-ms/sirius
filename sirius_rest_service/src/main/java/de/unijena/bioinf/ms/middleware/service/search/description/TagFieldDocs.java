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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * What a project's own tag definitions say about their tags.
 * <p>
 * Everything a client is told about a tag comes from one place - the definition stored in the project - so it
 * is asked for in one go: looking the definition up once for its values and again for its description would be
 * two reads of the same record for every tag of every description.
 */
public interface TagFieldDocs {

    /**
     * @param tagName the tag as it was defined, i.e. the key of the {@code tags.<tagName>} search field
     * @return what to tell a client about that tag, or null if this project has no definition for it
     */
    @Nullable
    TagFieldDoc describe(@NotNull String tagName);

    /**
     * @param possibleValues the values the definition restricts the tag to, in the form a query has to contain
     *                       them, or null if it accepts any value
     * @param description    what the definition says the tag means, or null if it says nothing
     */
    record TagFieldDoc(@Nullable List<String> possibleValues, @Nullable String description) {
    }
}
