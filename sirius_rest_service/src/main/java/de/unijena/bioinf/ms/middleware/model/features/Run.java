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

package de.unijena.bioinf.ms.middleware.model.features;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.unijena.bioinf.ms.middleware.model.tags.Tag;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.Taggable;
import de.unijena.bioinf.ms.middleware.service.search.mappers.IndexFieldWithMapper;
import de.unijena.bioinf.ms.middleware.service.search.mappers.TagMapper;
import de.unijena.bioinf.projectspace.IndexField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;


@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Run implements Taggable {

    @Schema(enumAsRef = true, name = "RunOptField", nullable = true)
    public enum OptField {none, tags}

    /**
     * Identifier
     */
    @IndexField(documentId = true, sortable = true, defaultSearchField = true)
    @NotNull
    protected String runId;

    /**
     * Informative, human-readable name of the run
     */
    @IndexField(defaultSearchField = true, fullTextSearch = true, sortable = true)
    protected String name;

    /**
     * Source location
     */
    @IndexField(defaultSearchField = true, fullTextSearch = true)
    protected String source;

    /**
     * Chromatography the run was measured with, e.g. 'Liquid Chromatography'.
     */
    @IndexField
    @Schema(nullable = true)
    protected String chromatography;

    /**
     * Ionization the run was measured with, named as in the HUPO PSI-MS controlled vocabulary,
     * e.g. 'electrospray ionization'.
     */
    @IndexField
    @Schema(nullable = true)
    protected String ionization;

    /**
     * Fragmentation the run was measured with, named as in the HUPO PSI-MS controlled vocabulary,
     * e.g. 'beam-type collision-induced dissociation'.
     */
    @IndexField
    @Schema(nullable = true)
    protected String fragmentation;

    /**
     * Mass analyzers of the instrument the run was measured on, named as in the HUPO PSI-MS
     * controlled vocabulary, e.g. 'orbitrap'.
     */
    @IndexField
    @Schema(nullable = true)
    protected List<String> massAnalyzers;

    /**
     * Key: tagName, value: tag
     */
    @IndexFieldWithMapper(mapper = TagMapper.class)
    @Schema(nullable = true)
    protected Map<String, Tag> tags;

}
