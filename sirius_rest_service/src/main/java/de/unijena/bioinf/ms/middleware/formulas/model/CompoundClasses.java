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

import de.unijena.bioinf.ChemistryBase.fp.*;
import de.unijena.bioinf.canopus.CanopusResult;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

/**
 * Container class that holds the best matching compound class for different levels of each ontology for a
 * certain compound/feature/predicted fingerprint.
 */
@Getter
@Setter
public class CompoundClasses {

    protected CompoundClass npcPathway;

    protected CompoundClass npcSuperclass;

    protected CompoundClass npcClass;

    protected CompoundClass classyFireMostSpecific;

    protected CompoundClass classyFireLevel5;

    protected CompoundClass classyFireClass;

    protected CompoundClass classyFireSubClass;

    protected CompoundClass classyFireSuperClass;

    public static CompoundClasses of(CanopusResult canopusResult) {
        return of(canopusResult.getNpcFingerprint().orElse(null), canopusResult.getCanopusFingerprint());
    }

    public static CompoundClasses of(@Nullable ProbabilityFingerprint npcClassification, @Nullable ProbabilityFingerprint cfClassification) {
        CompoundClasses sum = new CompoundClasses();

        if (npcClassification != null) {
            double[] perLevelProp = new double[NPCFingerprintVersion.NPCLevel.values().length];
            NPCFingerprintVersion.NPCProperty[] perLevelProps = new NPCFingerprintVersion.NPCProperty[NPCFingerprintVersion.NPCLevel.values().length];
            for (FPIter fpIter : npcClassification) {
                NPCFingerprintVersion.NPCProperty prop = ((NPCFingerprintVersion.NPCProperty) fpIter.getMolecularProperty());

                if (fpIter.getProbability() >= perLevelProp[prop.level.level]) {
                    perLevelProp[prop.level.level] = fpIter.getProbability();
                    perLevelProps[prop.level.level] = prop;
                }
            }

            sum.setNpcPathway(CompoundClass.of(perLevelProps[0], perLevelProp[0]));
            sum.setNpcSuperclass(CompoundClass.of(perLevelProps[1], perLevelProp[1]));
            sum.setNpcClass(CompoundClass.of(perLevelProps[2], perLevelProp[2]));
        }

        if (cfClassification != null) {
            FingerprintVersion v = cfClassification.getFingerprintVersion();
            if (v instanceof MaskedFingerprintVersion) v = ((MaskedFingerprintVersion) v).getMaskedFingerprintVersion();
            ClassyFireFingerprintVersion CLF = (ClassyFireFingerprintVersion) v;
            ClassyfireProperty primaryClass = CLF.getPrimaryClass(cfClassification);
            final ClassyfireProperty[] lineage = primaryClass.getLineage();

            sum.setClassyFireMostSpecific(CompoundClass.of(primaryClass,
                    cfClassification.getProbability(CLF.getIndexOfMolecularProperty(primaryClass))));

            if (lineage.length > 5)
                sum.setClassyFireLevel5(CompoundClass.of(lineage[5],
                        cfClassification.getProbability(CLF.getIndexOfMolecularProperty(lineage[5]))));

            if (lineage.length > 4)
                sum.setClassyFireSubClass(CompoundClass.of(lineage[4],
                        cfClassification.getProbability(CLF.getIndexOfMolecularProperty(lineage[4]))));

            if (lineage.length > 3)
                sum.setClassyFireClass(CompoundClass.of(lineage[3],
                        cfClassification.getProbability(CLF.getIndexOfMolecularProperty(lineage[3]))));

            if (lineage.length > 2)
                sum.setClassyFireSuperClass(CompoundClass.of(lineage[2],
                        cfClassification.getProbability(CLF.getIndexOfMolecularProperty(lineage[2]))));
        }

        return sum;
    }
}
