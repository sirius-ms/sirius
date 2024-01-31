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
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ms.middleware.model.spectra.Spectrums;
import de.unijena.bioinf.ms.middleware.model.spectra.BasicSpectrum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

/**
 * The MsData wraps all spectral input data belonging to a feature.
 * <p>
 * Each Feature has:
 * - One merged MS/MS spectrum (optional)
 * - One merged MS spectrum (optional)
 * - many MS/MS spectra
 * - many MS spectra
 * <p>
 * Each non-merged spectrum has an index which can be used to access the spectrum.
 * <p>
 * In the future we might add some additional information like chromatographic peak or something similar
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MsData {
    @Schema(nullable = true)
    protected BasicSpectrum mergedMs1;
    @Schema(nullable = true)
    protected BasicSpectrum mergedMs2;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    protected List<BasicSpectrum> ms1Spectra;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    protected List<BasicSpectrum> ms2Spectra;

    public static MsData create(Ms2Experiment exp) {
        MsDataBuilder b = MsData.builder()
                .ms1Spectra(
                        exp.getMs1Spectra().stream().map(x -> {
                            BasicSpectrum t = new BasicSpectrum(x);
                            t.setMsLevel(1);
                            //todo scannumber not available for MS1 Spectra? Maybe as annotation?
                            return t;
                        }).collect(Collectors.toList()))
                .ms2Spectra(
                        //todo ms/ms annotations for spectrum viewer
                        exp.getMs2Spectra().stream().map(x -> {
                            BasicSpectrum t = new BasicSpectrum(x);
                            t.setCollisionEnergy(new CollisionEnergy(x.getCollisionEnergy()));
                            t.setMsLevel(2);
                            t.setScanNumber(((MutableMs2Spectrum) x).getScanNumber());
                            return t;
                        }).collect(Collectors.toList()))
                .mergedMs2(Spectrums.createMergedMsMs(exp));

        Spectrum<Peak> mergedMs1 = exp.getMergedMs1Spectrum();
        if (mergedMs1 == null && !exp.getMs1Spectra().isEmpty())
            mergedMs1 = de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums.mergeSpectra(exp.getMs1Spectra());

        if (mergedMs1 != null) {
            BasicSpectrum t = new BasicSpectrum(mergedMs1);
            t.setMsLevel(1);
            b.mergedMs1(t);
        }
        return b.build();
    }
}
