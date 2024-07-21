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

import de.unijena.bioinf.ChemistryBase.fp.*;
import de.unijena.bioinf.canopus.CanopusResult;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Container class that holds the most likely compound class for different levels of each ontology for a
 * certain Compound/Feature/FormulaCandidate/PredictedFingerprint.
 */
@Getter
@Setter
public class CompoundClasses {

    /**
     * Pathway level NPC class with the highest probability
     */
    @Schema(nullable = true)
    protected CompoundClass npcPathway;
    /**
     * Superclass level NPC class with the highest probability
     */
    @Schema(nullable = true)
    protected CompoundClass npcSuperclass;
    /**
     * Class level NPC class with the highest probability
     */
    @Schema(nullable = true)
    protected CompoundClass npcClass;
    /**
     * Most likely ClassyFire lineage from ordered from least specific to most specific class
     * classyFireLineage.get(classyFireLineage.size() - 1) gives the most specific ClassyFire compound class annotation
     */
    @Schema(nullable = true)
    protected List<CompoundClass> classyFireLineage;
    /**
     * Alternative ClassyFire classes with high probability that do not fit into the linage
     */
    @Schema(nullable = true)
    protected List<CompoundClass> classyFireAlternatives;

    public static CompoundClasses of(CanopusResult canopusResult) {
        return of(canopusResult.getNpcFingerprint().orElse(null), canopusResult.getCanopusFingerprint());
    }

    public static CompoundClasses of(@Nullable ProbabilityFingerprint npcClassification, @Nullable ProbabilityFingerprint cfClassification) {
        CompoundClasses sum = new CompoundClasses();

        if (npcClassification != null) {
            double[] perLevelProp = new double[NPCFingerprintVersion.NPCLevel.values().length];
            NPCFingerprintVersion.NPCProperty[] perLevelProps = new NPCFingerprintVersion.NPCProperty[NPCFingerprintVersion.NPCLevel.values().length];
            int[] indices = new int[NPCFingerprintVersion.NPCLevel.values().length];
            for (FPIter fpIter : npcClassification) {
                NPCFingerprintVersion.NPCProperty prop = ((NPCFingerprintVersion.NPCProperty) fpIter.getMolecularProperty());

                if (fpIter.getProbability() >= perLevelProp[prop.level.level]) {
                    perLevelProp[prop.level.level] = fpIter.getProbability();
                    perLevelProps[prop.level.level] = prop;
                    indices[prop.level.level] = fpIter.getIndex();
                }
            }

            sum.setNpcPathway(CompoundClass.of(perLevelProps[0], perLevelProp[0], indices[0]));
            sum.setNpcSuperclass(CompoundClass.of(perLevelProps[1], perLevelProp[1], indices[1]));
            sum.setNpcClass(CompoundClass.of(perLevelProps[2], perLevelProp[2], indices[2]));
        }

        if (cfClassification != null) {
            FingerprintVersion v = cfClassification.getFingerprintVersion();
            if (v instanceof MaskedFingerprintVersion) v = ((MaskedFingerprintVersion) v).getMaskedFingerprintVersion();
            ClassyFireFingerprintVersion CLF = (ClassyFireFingerprintVersion) v;
            ClassyfireProperty primaryClass = CLF.getPrimaryClass(cfClassification);
            final List<ClassyfireProperty> lineage = Stream.of(primaryClass.getLineageRootToNode(false)).toList();
            final Set<ClassyfireProperty> alternatives =
                    Stream.of(CLF.getPredictedLeafs(cfClassification, 0.5)).collect(Collectors.toSet());
            lineage.forEach(alternatives::remove);

            sum.setClassyFireLineage(lineage.stream().map(cp -> CompoundClass.of(cp,
                    cfClassification.getProbability(CLF.getIndexOfMolecularProperty(cp)),
                    CLF.getIndexOfMolecularProperty(cp))
            ).toList());

            sum.setClassyFireAlternatives(alternatives.stream()
                    .sorted(Comparator.comparingInt(x -> -x.getPriority()))
                    .map(cp -> CompoundClass.of(cp,
                            cfClassification.getProbability(CLF.getIndexOfMolecularProperty(cp)),
                            CLF.getIndexOfMolecularProperty(cp))
                    ).toList());
        }
        return sum;
    }
}
