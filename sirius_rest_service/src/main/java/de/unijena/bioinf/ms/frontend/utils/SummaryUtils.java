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

package de.unijena.bioinf.ms.frontend.utils;

import de.unijena.bioinf.ChemistryBase.fp.*;
import de.unijena.bioinf.ms.middleware.compounds.CanopusResultSummary;
import de.unijena.bioinf.ms.middleware.compounds.CompoundClass;
import org.jetbrains.annotations.Nullable;

public class SummaryUtils {

    public static CanopusResultSummary chooseBestNPCAssignments(@Nullable ProbabilityFingerprint npcClassification, @Nullable ProbabilityFingerprint cfClassification) {
        CanopusResultSummary sum = new CanopusResultSummary();

        if (npcClassification != null) {
            double[] perLevelProp = new double[NPCFingerprintVersion.NPCLevel.values().length];
            String[] perLevelName = new String[NPCFingerprintVersion.NPCLevel.values().length];
            for (FPIter fpIter : npcClassification) {
                NPCFingerprintVersion.NPCProperty prop = ((NPCFingerprintVersion.NPCProperty) fpIter.getMolecularProperty());

                if (fpIter.getProbability() >= perLevelProp[prop.level.level]) {
                    perLevelProp[prop.level.level] = fpIter.getProbability();
                    perLevelName[prop.level.level] = prop.getName();
                }
            }

            sum.setNpcPathway(new CompoundClass(CompoundClass.Type.NPC, perLevelName[0], perLevelProp[0]));
            sum.setNpcSuperclass(new CompoundClass(CompoundClass.Type.NPC, perLevelName[1], perLevelProp[1]));
            sum.setNpcClass(new CompoundClass(CompoundClass.Type.NPC, perLevelName[2], perLevelProp[2]));
        }

        if (cfClassification != null) {
            FingerprintVersion v = cfClassification.getFingerprintVersion();
            if (v instanceof MaskedFingerprintVersion) v = ((MaskedFingerprintVersion) v).getMaskedFingerprintVersion();
            ClassyFireFingerprintVersion CLF = (ClassyFireFingerprintVersion) v;
            ClassyfireProperty primaryClass = CLF.getPrimaryClass(cfClassification);
            final ClassyfireProperty[] lineage = primaryClass.getLineage();

            sum.setClassyFireMostSpecific(new CompoundClass(CompoundClass.Type.ClassyFire,
                    primaryClass.getName(), cfClassification.getProbability(CLF.getIndexOfMolecularProperty(primaryClass))));

            if (lineage.length > 5)
                sum.setClassyFireLevel5(new CompoundClass(CompoundClass.Type.ClassyFire,
                        lineage[5].getName(), cfClassification.getProbability(CLF.getIndexOfMolecularProperty(lineage[5]))));

            if (lineage.length > 4)
                sum.setClassyFireSubClass(new CompoundClass(CompoundClass.Type.ClassyFire,
                        lineage[4].getName(), cfClassification.getProbability(CLF.getIndexOfMolecularProperty(lineage[4]))));

            if (lineage.length > 3)
                sum.setClassyFireClass(new CompoundClass(CompoundClass.Type.ClassyFire,
                        lineage[3].getName(), cfClassification.getProbability(CLF.getIndexOfMolecularProperty(lineage[3]))));

            if (lineage.length > 2)
                sum.setClassyFireSuperClass(new CompoundClass(CompoundClass.Type.ClassyFire,
                        lineage[2].getName(), cfClassification.getProbability(CLF.getIndexOfMolecularProperty(lineage[2]))));

        }

        return sum;
    }
}
