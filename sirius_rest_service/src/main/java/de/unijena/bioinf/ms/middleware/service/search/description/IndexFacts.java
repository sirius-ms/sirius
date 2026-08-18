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

import de.unijena.bioinf.ms.middleware.service.search.SearchService;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.SearchContext;
import de.unijena.bioinf.ms.middleware.service.search.mappers.IndexSchema;
import de.unijena.bioinf.ms.persistence.model.core.tags.ValueType;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;

/**
 * What a description needs to know from an index, stated by the side that needs it.
 * <p>
 * The index does not implement this - it is adapted to it - so that nothing in the search engine has to know
 * that anything describes it.
 */
public interface IndexFacts {

    /** What the indexed type is held as, as recorded when its index was configured. */
    @NotNull
    IndexSchema schemaOf(@NotNull Class<?> modelClass);

    /** The field names actually present in the index, which is what turns a {@code prefix.*} into real keys. */
    @NotNull
    Collection<String> materializedFieldNames(@NotNull Class<?> modelClass);

    /** The tags this project has definitions for, by name. */
    @NotNull
    Map<String, ValueType> tagValueTypes();

    /**
     * The facts of one index, for a caller that already holds it.
     */
    static IndexFacts of(@NotNull SearchContext context) {
        return new IndexFacts() {
            @Override
            public @NotNull IndexSchema schemaOf(@NotNull Class<?> modelClass) {
                return context.getIndexSchema(modelClass);
            }

            @Override
            public @NotNull Collection<String> materializedFieldNames(@NotNull Class<?> modelClass) {
                return context.getMaterializedFieldNames(modelClass);
            }

            @Override
            public @NotNull Map<String, ValueType> tagValueTypes() {
                return context.getTagValueTypes();
            }
        };
    }

    /**
     * The facts of one project's index, looked up through the service that owns the open projects.
     */
    static IndexFacts of(@NotNull SearchService searchService, @NotNull String projectId) {
        return new IndexFacts() {
            @Override
            public @NotNull IndexSchema schemaOf(@NotNull Class<?> modelClass) {
                return searchService.getIndexSchema(projectId, modelClass);
            }

            @Override
            public @NotNull Collection<String> materializedFieldNames(@NotNull Class<?> modelClass) {
                return searchService.getMaterializedFieldNames(projectId, modelClass);
            }

            @Override
            public @NotNull Map<String, ValueType> tagValueTypes() {
                return searchService.getTagValueTypes(projectId);
            }
        };
    }
}
