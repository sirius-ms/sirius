/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2015 Kai Dührkop
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.unijena.bioinf.FragmentationTreeConstruction.computation.filtering;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.Normalization;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.FragmentationTreeConstruction.model.Ms2ExperimentImpl;
import de.unijena.bioinf.FragmentationTreeConstruction.model.Ms2SpectrumImpl;

import java.util.ArrayList;
import java.util.List;

public class NormalizeToSumPreprocessor implements Preprocessor {

    private boolean onlyForRelativeIntensities;

    public NormalizeToSumPreprocessor() {
        this(true);
    }

    public NormalizeToSumPreprocessor(boolean onlyForRelativeIntensities) {
        this.onlyForRelativeIntensities = onlyForRelativeIntensities;
    }

    @Override
    public Ms2Experiment process(Ms2Experiment experiment) {
        List<? extends Ms2Spectrum<? extends Peak>> specs = experiment.getMs2Spectra();
        final ArrayList<Ms2Spectrum<? extends Peak>> spectra = new ArrayList<Ms2Spectrum<? extends Peak>>(specs.size());
        if (onlyForRelativeIntensities && !hasRelativeIntensities(experiment)) return experiment;
        for (Ms2Spectrum spec : specs) {
            final SimpleMutableSpectrum ms = new SimpleMutableSpectrum(spec);
            Spectrums.normalize(ms, Normalization.Sum(100d));
            final Ms2SpectrumImpl ms2 = new Ms2SpectrumImpl(ms, spec.getCollisionEnergy(), spec.getPrecursorMz(), spec.getTotalIonCount());
            spectra.add(ms2);
        }
        final Ms2ExperimentImpl exp = new Ms2ExperimentImpl(experiment);
        exp.setMs2Spectra(spectra);
        return exp;
    }

    public boolean hasRelativeIntensities(Ms2Experiment experiment) {
        // a spectrum has, with high probability, relative intensities, if the base peak in all spectra has
        // equal intensity
        List<? extends Ms2Spectrum> specs = experiment.getMs2Spectra();
        if (specs.size() <= 1) return false;
        final double basePeakIntensity = Spectrums.getMaximalIntensity(specs.get(0));
        final double delta = Math.min(Spectrums.getMinimalIntensity(specs.get(0))/10d, 0.001);
        for (Ms2Spectrum spec : specs) {
            if ((Spectrums.getMaximalIntensity(spec) - basePeakIntensity) > delta) return false;
        }
        return true;
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        if (!document.hasKeyInDictionary(dictionary, "onlyForRelativeIntensities")) this.onlyForRelativeIntensities = true; // LEGACY
        else this.onlyForRelativeIntensities = document.getBooleanFromDictionary(dictionary, "onlyForRelativeIntensities");
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        document.addToDictionary(dictionary, "onlyForRelativeIntensities", true);
    }
}
