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

package de.unijena.bioinf.ms.middleware.model.features;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ms.middleware.model.spectra.AnnotatedSpectrum;
import de.unijena.bioinf.ms.middleware.model.spectra.Spectrums;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Getter
@Builder
public class AnnotatedMsMsData {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    protected AnnotatedSpectrum mergedMs2;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    protected List<AnnotatedSpectrum> ms2Spectra;

    public static AnnotatedMsMsData of(@NotNull Ms2Experiment exp, @Nullable FTree ftree, @Nullable String candidateSmiles) {
        return AnnotatedMsMsData.builder()
                .ms2Spectra(Spectrums.createMsMsWithAnnotations(exp, ftree, candidateSmiles))
                .mergedMs2(Spectrums.createMergedMsMsWithAnnotations(exp, ftree, candidateSmiles))
                .build();
    }
}
