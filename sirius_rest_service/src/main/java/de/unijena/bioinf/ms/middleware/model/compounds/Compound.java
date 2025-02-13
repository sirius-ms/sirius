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

package de.unijena.bioinf.ms.middleware.model.compounds;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.unijena.bioinf.ms.middleware.model.LuceneDocument;
import de.unijena.bioinf.ms.middleware.model.TaggableLuceneDocumentProvider;
import de.unijena.bioinf.ms.middleware.model.annotations.ConsensusAnnotationsCSI;
import de.unijena.bioinf.ms.middleware.model.annotations.ConsensusAnnotationsDeNovo;
import de.unijena.bioinf.ms.middleware.model.features.AlignedFeature;
import de.unijena.bioinf.ms.middleware.model.tags.Tag;
import de.unijena.bioinf.ms.middleware.service.search.LuceneSearchService;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.Taggable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexableField;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static de.unijena.bioinf.ChemistryBase.utils.Utils.notNullOrEmpty;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Compound implements TaggableLuceneDocumentProvider, Taggable {
    @JsonIgnore
    @NotNull
    @Override
    public LuceneDocument toLuceneDocument(LuceneSearchService.ProjectSearchContext projectSearchContext) {
        return () -> new ArrayList<IndexableField>() {{
            add(new StringField("uuid", compoundId, Field.Store.NO)); // standard field name for uuid to identify document

            add(new StringField("compoundId", compoundId, Field.Store.YES));
            if (name != null && !name.isBlank())
                add(new TextField("name", name, Field.Store.NO));
            if (neutralMass != null)
                add(new DoublePoint("neutralMass", neutralMass));
            if (rtStartSeconds != null)
                add(new DoublePoint("rtStartSeconds", rtStartSeconds));
            if (rtEndSeconds != null)
                add(new DoublePoint("rtEndSeconds", rtEndSeconds));
            //tags
            if (notNullOrEmpty(tags))
                tags.values().forEach(tag ->
                        addAll(projectSearchContext.getIndexableTagFields(tag.getTagName(), tag.getValue(), Field.Store.YES)));

        }}.iterator();
    }

    @Schema(name = "CompoundOptField", nullable = true)
    public enum OptField {none, consensusAnnotations, consensusAnnotationsDeNovo, customAnnotations, tags}

    /**
     * uid of this compound Entity
     */
    @NotNull
    protected String compoundId;

    /**
     * Some (optional) human-readable name
     */
    @Schema(nullable = true)
    protected String name;

    /**
     * The merged/consensus retention time start (earliest rt) of this compound
     */
    @Schema(nullable = true)
    protected Double rtStartSeconds;

    /**
     * The merged/consensus retention time end (latest rt) of this compound
     */
    @Schema(nullable = true)
    protected Double rtEndSeconds;

    /**
     * Neutral mass of this compound. Ion masse minus the mass of the assigned adduct of each feature of
     * this compound should result in the same neutral mass
     */
    @Schema(nullable = true)
    protected Double neutralMass;

    /**
     * List of aligned features (adducts) that belong to the same (this) compound
     */
    protected List<AlignedFeature> features;

    /**
     * The consensus of the top annotations from all the features of this compound.
     * Null if it was not requested und non-null otherwise. Might contain empty fields if results are not available
     */
    @Schema(nullable = true)
    ConsensusAnnotationsCSI consensusAnnotations;

    /**
     * The consensus of the top de novo annotations from all the features of this compound.
     * Null if it was not requested und non-null otherwise. Might contain empty fields if results are not available
     */
    @Schema(nullable = true)
    ConsensusAnnotationsDeNovo consensusAnnotationsDeNovo;

    /**
     * Alternative annotations selected by the User.
     */
    @Schema(nullable = true)
    ConsensusAnnotationsCSI customAnnotations;

    /**
     * Key: tagName, value: tag
     */
    @Schema(nullable = true)
    protected Map<String, Tag> tags;
}
