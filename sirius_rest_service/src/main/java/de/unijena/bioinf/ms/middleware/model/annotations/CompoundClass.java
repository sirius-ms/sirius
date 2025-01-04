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

package de.unijena.bioinf.ms.middleware.model.annotations;

import de.unijena.bioinf.ChemistryBase.fp.ClassyfireProperty;
import de.unijena.bioinf.ChemistryBase.fp.NPCFingerprintVersion;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

/**
 * Predicted compound class with name, probability and id if available.
 * (ClassyFire and NPC). This can be seen as the set of classes a feature most likely belongs to
 */
@Getter
@Setter
@Builder
public class CompoundClass {
    /**
     * Defines compound class ontologies that are available.
     */
    @Schema(name = "CompoundClassType", nullable = true)
    public enum Type {ClassyFire, NPC}

    /**
     * Specifies the classification ontology the CompoundClass belongs to.
     */
    protected Type type;

    /**
     * Name of the level this compound class belongs to
     */
    @Schema(nullable = true)
    protected String level;

    /**
     * Index of the level this compound class belongs to
     */
    @Schema(nullable = true)
    protected Integer levelIndex;

    /**
     * Name of the compound class.
     */
    @Schema(nullable = true)
    protected String name;

    /**
     * Description of the compound class.
     */
    @Schema(nullable = true)
    protected String description;

    /**
     * Unique id of the class. Might be undefined for certain classification ontologies.
     */
    @Schema(nullable = true)
    protected Integer id;
    /**
     * prediction probability
     */
    protected Double probability;

    /**
     * Absolute index of this property in the predicted vector/embedding
     */
    protected Integer index;

    /**
     * Unique id of the parent class. Might be undefined for certain classification ontologies.
     */
    @Schema(nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    protected Integer parentId;

    /**
     * Name of the parent compound class.
     */
    @Schema(nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    protected String parentName;


    public static CompoundClass of(NPCFingerprintVersion.NPCProperty npcClass, Double probability, Integer index){
        return CompoundClass.builder()
                .type(Type.NPC)
                .level(npcClass.getLevel().name())
                .levelIndex(npcClass.getLevel().level)
                .name(npcClass.getName())
                .description(npcClass.getDescription())
                .id(npcClass.getNpcIndex())
                .probability(probability)
                .index(index)
                .build();
        //we do not parent annotations here because it's stored as hierarchie anyway.
    }

    public static CompoundClass of(ClassyfireProperty cfClass, Double probability, Integer index){
        CompoundClassBuilder builder = CompoundClass.builder()
                .type(Type.ClassyFire)
                .level(CanopusLevels.getClassyFireLevelName(cfClass.getLevel()))
                .levelIndex(cfClass.getLevel())
                .name(cfClass.getName())
                .description(cfClass.getDescription())
                .id(cfClass.getChemOntId())
                .probability(probability)
                .index(index);

        if (cfClass.getParent() != null) {
            builder.parentName(cfClass.getParent().getName());
            builder.parentId(cfClass.getParent().getChemOntId());
        }

        return builder.build();
    }
}
