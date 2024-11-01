/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
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

package de.unijena.bioinf.ms.middleware.model.features;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.utils.DataQuality;
import de.unijena.bioinf.ms.middleware.model.LuceneDocument;
import de.unijena.bioinf.ms.middleware.model.annotations.FeatureAnnotations;
import de.unijena.bioinf.ms.middleware.model.tags.Tag;
import de.unijena.bioinf.ms.persistence.model.sirius.ComputedSubtools;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexableField;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * The AlignedFeature contains the ID of a feature (aligned over runs) together with some read-only information
 * that might be displayed in some summary view.
 */
@Getter
@Setter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AlignedFeature implements LuceneDocument {
    @JsonIgnore
    @NotNull
    @Override
    public Iterator<IndexableField> iterator() {
        return new ArrayList<IndexableField>() {{ //storing all the base fields allows to load feature list solely based on index.

            add(new StringField("alignedFeatureId", alignedFeatureId, Field.Store.YES));
            add(new StringField("compoundId", alignedFeatureId, Field.Store.YES));
            add(new StringField("externalFeatureId", alignedFeatureId, Field.Store.YES));
            if (name != null && !name.isBlank())
                add(new TextField("name", name, Field.Store.YES));
            add(new DoublePoint("ionMass", ionMass));
            add(new IntPoint("charge", charge));
            if (rtStartSeconds != null)
                add(new DoublePoint("rtStartSeconds", rtStartSeconds));
            if (rtEndSeconds != null)
                add(new DoublePoint("rtEndSeconds", rtEndSeconds));
            if (rtApexSeconds != null)
                add(new DoublePoint("rtApexSeconds", rtApexSeconds));
            add(new StringField("hasMs1", String.valueOf(hasMs1), Field.Store.NO));
            add(new StringField("hasMsMs", String.valueOf(hasMsMs), Field.Store.NO));

            //weired fields
            if (quality != null)
                add(new KeywordField("quality", quality.name(), Field.Store.NO));
            if (detectedAdducts != null && !detectedAdducts.isEmpty())
                detectedAdducts.forEach(adduct -> add(new KeywordField("detectedAdducts", adduct, Field.Store.NO)));
            else
                add(new KeywordField("detectedAdducts", PrecursorIonType.unknown(charge).toString(), Field.Store.NO));
        }}.iterator();
    }


    @Schema(name = "AlignedFeatureOptField", nullable = true)
    public enum OptField {none, msData, topAnnotations, topAnnotationsDeNovo, computedTools, tags}

    // identifier
    @NotNull
    protected String alignedFeatureId;

    protected String compoundId;

    // identifier source
    protected String name;

    /**
     * Externally provided FeatureId (e.g. by some preprocessing tool).
     * This FeatureId is NOT used by SIRIUS but is stored to ease mapping information back to the source.
     */
    protected String externalFeatureId;

    // additional attributes
    protected Double ionMass;

    /**
     * Ion mode (charge) this feature has been measured in.
     */
    @Schema(nullable = false, requiredMode = Schema.RequiredMode.REQUIRED)
    protected int charge;

    /**
     *  Adducts of this feature that have been detected during preprocessing.
     */
    @Schema(nullable = false, requiredMode = Schema.RequiredMode.REQUIRED)
    protected Set<String> detectedAdducts;

    @Schema(nullable = true)
    protected Double rtStartSeconds;
    @Schema(nullable = true)
    protected Double rtEndSeconds;
    @Schema(nullable = true)
    protected Double rtApexSeconds;

    /**
     * Quality of this feature.
     */
    protected DataQuality quality;
    /**
     * If true, the feature has at lease one MS1 spectrum
     */
    protected boolean hasMs1;
    /**
     * If true, the feature has at lease one MS/MS spectrum
     */
    protected boolean hasMsMs;

    /**
     * Mass Spec data of this feature (input data)
     */
    @Schema(nullable = true)
    protected MsData msData;

    /**
     * Top annotations of this feature.
     * If a CSI:FingerID structureAnnotation is available, the FormulaCandidate that corresponds to the
     * structureAnnotation is returned. Otherwise, it's the FormulaCandidate with the highest SiriusScore is returned.
     * CANOPUS Compound classes correspond to the FormulaCandidate no matter how it was selected
     *
     * Null if it was not requested und non-null otherwise.
     */
    @Schema(nullable = true)
    protected FeatureAnnotations topAnnotations;

    /**
     * Top de novo annotations of this feature.
     * The FormulaCandidate with the highest SiriusScore is returned. MSNovelist structureAnnotation and
     * CANOPUS compoundClasses correspond to the FormulaCandidate.
     *
     * Null if it was not requested und non-null otherwise.
     */
    @Schema(nullable = true)
    protected FeatureAnnotations topAnnotationsDeNovo;


    /**
     * Write lock for this feature. If the feature is locked no write operations are possible.
     * True if any computation is modifying this feature or its results
     */
    protected boolean computing;

    @Schema(nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED, description =
                    "Specifies which tools have been executed for this feature. " +
                    "Can be used to estimate which results can be expected. " +
                            "Null if it was not requested und non-null otherwise.")
    @JsonIgnoreProperties(value = { "alignedFeatureId" })
    protected ComputedSubtools computedTools;

    /**
     * Key: tagName, value: tag
     */
    @Schema(nullable = true)
    protected Map<String, ? extends Tag> tags;
}
