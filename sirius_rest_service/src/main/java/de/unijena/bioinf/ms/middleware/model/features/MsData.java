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

import com.fasterxml.jackson.annotation.JsonInclude;
import de.unijena.bioinf.ms.middleware.model.spectra.AnnotatedSpectrum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * The MsData wraps all spectral input data belonging to a feature.
 *
 * Each Feature has:
 * - One merged MS/MS spectrum (optional)
 * - One merged MS spectrum (optional)
 * - many MS/MS spectra
 * - many MS spectra
 *
 * Each non-merged spectrum has an index which can be used to access the spectrum.
 *
 * In the future we might add some additional information like chromatographic peak or something similar
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MsData {
    @Schema(nullable = true)
    protected AnnotatedSpectrum mergedMs1;
    @Schema(nullable = true)
    protected AnnotatedSpectrum mergedMs2;
    @Schema(nullable = true)
    protected List<AnnotatedSpectrum> ms2Spectra;
    @Schema(nullable = true)
    protected List<AnnotatedSpectrum> ms1Spectra;
}
