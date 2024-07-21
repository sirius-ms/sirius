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
import de.unijena.bioinf.ChemistryBase.fp.FPIter;
import de.unijena.bioinf.ChemistryBase.fp.NPCFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.canopus.CanopusResult;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Container class that holds the CANOPUS compound class predictions for alle predictable compound classes.
 * This is the full CANOPUS result.
 */
@Getter
@Setter
public class CanopusPrediction {
    /**
     * All predicted ClassyFire classes
     */
    @Schema(nullable = true)
    CompoundClass[] classyFireClasses;
    /**
     * All predicted NPC classes
     */
    @Schema(nullable = true)
    CompoundClass[] npcClasses;


    public static CanopusPrediction of(CanopusResult canopusResult) {
        return of(canopusResult.getNpcFingerprint().orElse(null), canopusResult.getCanopusFingerprint());
    }

    public static CanopusPrediction of(@Nullable ProbabilityFingerprint npcClassification, @Nullable ProbabilityFingerprint cfClassification) {
        final CanopusPrediction sum = new CanopusPrediction();

        if (npcClassification != null) {
            List<CompoundClass> cc = new ArrayList<>();
            for (FPIter fpIter : npcClassification) {
                NPCFingerprintVersion.NPCProperty prop = ((NPCFingerprintVersion.NPCProperty) fpIter.getMolecularProperty());
                cc.add(CompoundClass.of(prop, fpIter.getProbability(), fpIter.getIndex()));
            }
            sum.setNpcClasses(cc.toArray(CompoundClass[]::new));
        }

        if (cfClassification != null) {
            List<CompoundClass> cc = new ArrayList<>();
            for (FPIter fpIter : cfClassification) {
                ClassyfireProperty prop = (ClassyfireProperty) fpIter.getMolecularProperty();
                cc.add(CompoundClass.of(prop, fpIter.getProbability(), fpIter.getIndex()));
            }
            sum.setClassyFireClasses(cc.toArray(CompoundClass[]::new));
        }
        return sum;
    }
}