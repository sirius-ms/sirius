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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import de.unijena.bioinf.ms.middleware.model.spectra.BasicSpectrum;
import de.unijena.bioinf.ms.middleware.model.spectra.Spectrums;
import de.unijena.bioinf.ms.persistence.model.core.spectrum.MergedMSnSpectrum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * The MsData wraps all spectral input data belonging to a (aligned) feature. All spectra fields are optional.
 * However, at least one Spectrum field needs to be set to create a valid MsData Object.
 * The different types of spectra fields can be extended to adapt to other MassSpec measurement techniques not covered yet.
 * <p>
 * Each Feature can have:
 * - One merged MS/MS spectrum (optional)
 * - One merged MS spectrum (optional)
 * - many MS/MS spectra (optional)
 * - many MS spectra (optional)
 * <p>
 * Each non-merged spectrum has an index which can be used to access the spectrum.
 * <p>
 * In the future we might add some additional information like chromatographic peak or something similar
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MsData {
    @Schema(nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    protected BasicSpectrum mergedMs1;
    @Schema(nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    protected BasicSpectrum mergedMs2;
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    protected List<BasicSpectrum> ms1Spectra;
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    protected List<BasicSpectrum> ms2Spectra;

    public static MsData of(@NotNull de.unijena.bioinf.ms.persistence.model.core.spectrum.MSData msData, boolean asCosineQuery) {
        MsData.MsDataBuilder builder = MsData.builder();
        if (msData.getMergedMs1Spectrum() != null)
            builder.mergedMs1(Spectrums.createMs1(msData.getMergedMs1Spectrum()));
        if (msData.getMergedMSnSpectrum() != null){
            double precursorMz = msData.getMsnSpectra().stream()
                    .mapToDouble(MergedMSnSpectrum::getMergedPrecursorMz).average().getAsDouble();
            builder.mergedMs2(Spectrums.createMergedMsMs(msData.getMergedMSnSpectrum(), precursorMz, asCosineQuery));
        }

        builder.ms2Spectra(msData.getMsnSpectra() != null ? msData.getMsnSpectra().stream()
                .map(s -> Spectrums.createMsMs(s, asCosineQuery)).toList() : List.of());
        //MS1Spectra are not set since they are not stored in default MSData object.
        return builder.build();
    }
}
