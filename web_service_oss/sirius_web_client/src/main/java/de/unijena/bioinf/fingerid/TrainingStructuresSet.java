/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
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
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.fingerid;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ms.rest.model.fingerid.TrainingStructures;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.util.Set;

@Getter
@Builder
@Jacksonized
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE
)
public class TrainingStructuresSet {
    private final Set<String> kernelInchiKeys;
    private final Set<String> extraInchiKeys;


    public boolean isInKernelTrainingData(String inchiKey2D){
        return kernelInchiKeys.contains(inchiKey2D);

    }

    public boolean isInExtraTrainingData(String inchiKey2D){
        return extraInchiKeys.contains(inchiKey2D);
    }


    public boolean isInTrainingData(String inchiKey2D){
        return isInKernelTrainingData(inchiKey2D) || isInExtraTrainingData(inchiKey2D);

    }
    public boolean isInTrainingData(InChI inchi){
        return isInTrainingData(inchi.key2D());
    }

    public static TrainingStructuresSet of(TrainingStructures structures){
        return TrainingStructuresSet.builder()
                .extraInchiKeys(structures.getExtraInchiKeys())
                .kernelInchiKeys(structures.getKernelInchiKeys())
                .build();
    }
}
