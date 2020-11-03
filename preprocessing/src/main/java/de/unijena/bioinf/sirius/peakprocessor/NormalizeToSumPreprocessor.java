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

package de.unijena.bioinf.sirius.peakprocessor;

import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.Normalization;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;

/**
 * If each input spectra is already normalized by its base peak, we renormalize them such that every spectrum sum up to 1.
 */
public class NormalizeToSumPreprocessor implements UnmergedSpectrumProcessor {
    @Override
    public void process(MutableMs2Experiment experiment) {
        if (experiment.getMs2Spectra().size()<=1) return;
        double basePeak = Double.NaN;
        // first: find base peak
        for (MutableMs2Spectrum spec : experiment.getMs2Spectra()) {
            double base = Spectrums.getMaximalIntensity(spec);
            if (Double.isNaN(basePeak)) {
                basePeak = base;
            } else if (Math.abs(basePeak-base)>1e-6) {
                return;
            }
        }
        // now renormalize
        for (MutableMs2Spectrum spec : experiment.getMs2Spectra()) {
            Spectrums.normalize(spec, Normalization.Sum(1d));
        }
    }
}
