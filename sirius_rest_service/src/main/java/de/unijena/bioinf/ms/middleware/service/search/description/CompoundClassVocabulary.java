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

import de.unijena.bioinf.ChemistryBase.fp.ClassyFireFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.NPCFingerprintVersion;
import de.unijena.bioinf.ms.middleware.service.search.mappers.CompoundClassesMapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.IntStream;

/**
 * The compound class names of both ontologies, as the fields of {@link CompoundClassesMapper} hold them.
 * <p>
 * Predicted classes are indexed under their ontology name, which is searchable but not guessable - nobody types
 * "Carboxylic acids and derivatives" from memory. The whole ontology is offered, not just the classes predicted
 * in the current project: which classes occur is a property of the data, while this says what the field can
 * hold.
 * <p>
 * The names come from the ontology singletons the indexed values are built from, so what is offered is what is
 * indexed; the field names come from the mapper that writes them, so neither side can rename without the other.
 */
public class CompoundClassVocabulary implements FieldVocabulary {

    @Override
    public @Nullable List<String> getPossibleValues(@NotNull String fieldName) {
        if (fieldName.endsWith(CompoundClassesMapper.CLASSY_FIRE))
            return CLASSY_FIRE_CLASSES;
        if (fieldName.endsWith(CompoundClassesMapper.NPC_PATHWAY))
            return NPC_PATHWAYS;
        if (fieldName.endsWith(CompoundClassesMapper.NPC_SUPERCLASS))
            return NPC_SUPERCLASSES;
        if (fieldName.endsWith(CompoundClassesMapper.NPC_CLASS))
            return NPC_CLASSES;
        return null;
    }

    private static final List<String> CLASSY_FIRE_CLASSES = classyFireClasses();
    private static final List<String> NPC_PATHWAYS = npcClassesOfLevel(NPCFingerprintVersion.NPCLevel.PATHWAY);
    private static final List<String> NPC_SUPERCLASSES = npcClassesOfLevel(NPCFingerprintVersion.NPCLevel.SUPERCLASS);
    private static final List<String> NPC_CLASSES = npcClassesOfLevel(NPCFingerprintVersion.NPCLevel.CLASS);

    private static List<String> classyFireClasses() {
        ClassyFireFingerprintVersion ontology = ClassyFireFingerprintVersion.getDefault();
        return IntStream.range(0, ontology.size())
                .mapToObj(i -> ontology.getMolecularProperty(i).getName())
                .toList();
    }

    private static List<String> npcClassesOfLevel(NPCFingerprintVersion.NPCLevel level) {
        NPCFingerprintVersion ontology = NPCFingerprintVersion.get();
        return IntStream.range(0, ontology.size())
                .mapToObj(ontology::getMolecularProperty)
                .filter(property -> property.getLevel() == level)
                .map(NPCFingerprintVersion.NPCProperty::getName)
                .toList();
    }
}
