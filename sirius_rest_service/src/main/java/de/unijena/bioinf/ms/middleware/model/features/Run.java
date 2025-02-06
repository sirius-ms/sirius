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
import de.unijena.bioinf.ms.middleware.model.LuceneDocument;
import de.unijena.bioinf.ms.middleware.model.TaggableLuceneDocumentProvider;
import de.unijena.bioinf.ms.middleware.model.tags.Tag;
import de.unijena.bioinf.ms.middleware.service.search.LuceneSearchService;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.KeywordField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexableField;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static de.unijena.bioinf.ChemistryBase.utils.Utils.notNullOrBlank;
import static de.unijena.bioinf.ChemistryBase.utils.Utils.notNullOrEmpty;

@Getter
@Setter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Run implements TaggableLuceneDocumentProvider {

    @Override
    public @NotNull LuceneDocument toLuceneDocument(LuceneSearchService.ProjectSearchContext projectSearchContext) {
        return () -> new ArrayList<IndexableField>() {{
            add(new KeywordField("runId", runId, Field.Store.YES));
            if (notNullOrBlank(name))
                add(new TextField("name", name, Field.Store.YES));
            if (notNullOrBlank(source))
                add(new TextField("source", source, Field.Store.YES));
            if (notNullOrBlank(ionization))
                add(new KeywordField("ionization", ionization, Field.Store.NO));
            if (notNullOrBlank(fragmentation))
                add(new KeywordField("fragmentation", fragmentation, Field.Store.NO));
            if (massAnalyzers != null && !massAnalyzers.isEmpty())
                massAnalyzers.forEach(analyzer -> add(new KeywordField("massAnalyzers", analyzer, Field.Store.NO)));

            if (notNullOrEmpty(tags))
                tags.values().forEach(tag ->
                        add(projectSearchContext.getIndexableTagField(tag.getTagName(), tag.getValue(), Field.Store.NO)));

        }}.iterator();
    }

    @Schema(enumAsRef = true, name = "RunOptField", nullable = true)
    public enum OptField {none, tags}

    /**
     * Identifier
     */
    @NotNull
    protected String runId;

    /**
     * Informative, human-readable name of this run
     */
    protected String name;

    /**
     * Source location
     */
    protected String source;

    @Schema(nullable = true)
    protected String chromatography;

    @Schema(nullable = true)
    protected String ionization;

    @Schema(nullable = true)
    protected String fragmentation;

    @Schema(nullable = true)
    protected List<String> massAnalyzers;

    /**
     * Key: tagName, value: tag
     */
    @Schema(nullable = true)
    protected Map<String, ? extends Tag> tags;

}
