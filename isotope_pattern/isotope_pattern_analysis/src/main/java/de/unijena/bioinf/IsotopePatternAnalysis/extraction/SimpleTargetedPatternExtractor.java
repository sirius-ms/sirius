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

package de.unijena.bioinf.IsotopePatternAnalysis.extraction;

import de.unijena.bioinf.ChemistryBase.chem.ChemicalAlphabet;
import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.ms.MS1MassDeviation;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import org.apache.commons.lang3.Range;

import java.util.ArrayList;

@Deprecated
public class SimpleTargetedPatternExtractor implements TargetedPatternExtraction {

    @Override
    public SimpleSpectrum extractSpectrum(Ms2Experiment experiment) {

        if (experiment.getIonMass()<=0 || Double.isNaN(experiment.getIonMass()) )
            throw new IllegalArgumentException("ion mass is not set.");

        final SimpleSpectrum ms1;
        if (experiment.getMergedMs1Spectrum()==null || experiment.getMergedMs1Spectrum().size()>0) {
            ms1 = merge(experiment);
        } else ms1 = experiment.getMergedMs1Spectrum();

        if (ms1==null) return null;


        final ChemicalAlphabet stdalphabet =
                experiment.getAnnotationOrDefault(FormulaConstraints.class).getExtendedConstraints(new FormulaConstraints(ChemicalAlphabet.getExtendedAlphabet())).getChemicalAlphabet();

        final Spectrum<Peak> massOrderedSpectrum = Spectrums.getMassOrderedSpectrum(ms1);
        final ArrayList<SimpleSpectrum> patterns = new ArrayList<SimpleSpectrum>();
        MS1MassDeviation dev = experiment.getAnnotationOrDefault(MS1MassDeviation.class);
        final int index = Spectrums.mostIntensivePeakWithin(massOrderedSpectrum, experiment.getIonMass(), dev.allowedMassDeviation);
        if (index < 0) return null;
        final SimpleMutableSpectrum spec = new SimpleMutableSpectrum();
        spec.addPeak(massOrderedSpectrum.getPeakAt(index));
        // add additional peaks
        final PeriodicTable T = PeriodicTable.getInstance();
        for (int k=1; k <= 5; ++k) {
            final Range<Double> nextMz = T.getIsotopicMassWindow(stdalphabet, dev.allowedMassDeviation, spec.getMzAt(0), k);
            final double a = nextMz.getMinimum();
            final double b = nextMz.getMaximum();
            final double startPoint = a - dev.massDifferenceDeviation.absoluteFor(a);
            final double endPoint = b + dev.massDifferenceDeviation.absoluteFor(b);
            final int nextIndex = Spectrums.indexOfFirstPeakWithin(massOrderedSpectrum, startPoint, endPoint);
            if (nextIndex < 0) break;
            double mzBuffer = 0d;
            double intensityBuffer = 0d;
            for (int i=nextIndex; i < massOrderedSpectrum.size(); ++i) {
                final double mz = massOrderedSpectrum.getMzAt(i);
                if (mz > endPoint) break;
                final double intensity = massOrderedSpectrum.getIntensityAt(i);
                mzBuffer += mz*intensity;
                intensityBuffer += intensity;
            }
            mzBuffer /= intensityBuffer;
            spec.addPeak(mzBuffer, intensityBuffer);
        }
        return new SimpleSpectrum(spec);
    }

    private SimpleSpectrum merge(Ms2Experiment experiment) {
        if (experiment.getMs1Spectra().size()>0) {
            return Spectrums.mergeSpectra(experiment.<Spectrum<Peak>>getMs1Spectra());
        } else return null;
    }
}
