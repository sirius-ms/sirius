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

package de.unijena.bioinf.sirius.merging;

import de.unijena.bioinf.ChemistryBase.ms.MergedMs1Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.ms.annotations.Provides;
import de.unijena.bioinf.sirius.ProcessedInput;

@Provides(MergedMs1Spectrum.class)
public class Ms1Merging {


    public MergedMs1Spectrum getMergedSpectrum(ProcessedInput processedInput) {
        final Ms2Experiment exp = processedInput.getExperimentInformation();
        if (exp.getMergedMs1Spectrum()!=null) {
            return new MergedMs1Spectrum(true, exp.getMergedMs1Spectrum());
        } else if (!exp.getMs1Spectra().isEmpty()){
            final SimpleSpectrum merged = Spectrums.mergeSpectra(exp.getMs1Spectra());
            return new MergedMs1Spectrum(false, merged);
        } else {
            return MergedMs1Spectrum.empty();
        }
    }

    public void merge(ProcessedInput processedInput) {
        processedInput.setAnnotation(MergedMs1Spectrum.class, getMergedSpectrum(processedInput));;
    }

}
