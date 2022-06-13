/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
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
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.middleware.formulas.model;

import de.unijena.bioinf.ChemistryBase.fp.ClassyfireProperty;
import de.unijena.bioinf.ChemistryBase.fp.NPCFingerprintVersion;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Predicted compound class with name, probability and id if available.
 * (ClassyFire and NPC). This can be seen as the set of classes a compound most likely belongs to
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CompoundClass {
    /**
     * Defines compound class ontologies that are available.
     */
    public enum Type {ClassyFire, NPC}

    /**
     * Specifies the classification ontology the CompoundClass belongs to.
     */
    protected Type type;

    /**
     * Name of the compound class.
     */
    protected String name;

    /**
     * Description of the compound class.
     */
    protected String description;

    /**
     * Unique id of the class. Might be undefined for certain classification ontologies.
     */
    protected int id;
    /**
     * prediction probability
     */
    protected Double probability;

    public static CompoundClass of(NPCFingerprintVersion.NPCProperty npcClass, Double probability){
        return new CompoundClass(Type.NPC, npcClass.getName(), npcClass.getDescription(), npcClass.npcIndex, probability);
    }

    public static CompoundClass of(ClassyfireProperty cfClass, Double probability){
        return new CompoundClass(Type.ClassyFire, cfClass.getName(), cfClass.getDescription(), cfClass.getChemOntId(), probability);
    }
}
