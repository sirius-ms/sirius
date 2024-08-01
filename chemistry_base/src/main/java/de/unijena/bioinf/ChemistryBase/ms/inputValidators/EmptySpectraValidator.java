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

package de.unijena.bioinf.ChemistryBase.ms.inputValidators;

import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;

import java.util.ArrayList;
import java.util.List;

public class EmptySpectraValidator implements Ms2ExperimentValidator {
    @Override
    public boolean validate(MutableMs2Experiment input, Warning warning, boolean repair) throws InvalidException {
        if  (!anyEmpty(input.getMs1Spectra()) & !anyEmpty(input.getMs2Spectra())){
            return true;
        }
        List<SimpleSpectrum> ms1SpectraNonEmpty = new ArrayList<>();
        List<MutableMs2Spectrum> ms2SpectraNonEmpty = new ArrayList<>();
        MutableMs2Experiment mutableMs2Experiment = new MutableMs2Experiment(input);
        for (Spectrum<Peak> spectrum : input.getMs1Spectra()) {
            if (spectrum.size()>0) ms1SpectraNonEmpty.add(new SimpleSpectrum(spectrum));
        }
        for (Ms2Spectrum<Peak> spectrum : input.getMs2Spectra()) {
            if (spectrum.size()>0) ms2SpectraNonEmpty.add(new MutableMs2Spectrum(spectrum));
        }
        mutableMs2Experiment.setMs1Spectra(ms1SpectraNonEmpty);
        mutableMs2Experiment.setMs2Spectra(ms2SpectraNonEmpty);
        return true;
    }

    private boolean anyEmpty(List<? extends Spectrum> spectrumList){
        for (Spectrum<Peak> spectrum : spectrumList) {
            if (spectrum.size()==0) return true;
        }
        return false;
    }
}
