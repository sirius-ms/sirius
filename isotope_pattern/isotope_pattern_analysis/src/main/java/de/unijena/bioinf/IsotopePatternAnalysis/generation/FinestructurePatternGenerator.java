
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

package de.unijena.bioinf.IsotopePatternAnalysis.generation;

import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.utils.IsotopicDistribution;
import de.unijena.bioinf.ChemistryBase.ms.Normalization;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;

/**
 * Created by kaidu on 07.12.2014.
 */
public class FinestructurePatternGenerator extends IsotopePatternGenerator {

    protected final CachedIsoTable cache;
    protected double resolution = 75000d;


    public FinestructurePatternGenerator(IsotopicDistribution distribution, Normalization mode) {
        super(distribution, mode);
        this.cache = new CachedIsoTable(distribution);
    }

    public FinestructurePatternGenerator() {
        super();
        this.cache = new CachedIsoTable(distribution);
    }

    public FinestructurePatternGenerator(Normalization mode) {
        super(mode);
        this.cache = new CachedIsoTable(distribution);
    }

    @Override
    public SimpleSpectrum simulatePattern(MolecularFormula formula, Ionization ionization) {
        final FineStructureMerger merger = new FineStructureMerger(resolution);
        final SimpleSpectrum spectrum = merger.merge(new FinestructureGenerator(distribution, mode, cache).iteratorSumingUpTo(formula, ionization, 0.999d), ionization.addToMass(formula.getMass()));
        //final SimpleSpectrum spectrum = merger.merge(new FinestructureGenerator(distribution, mode, cache).iterator(formula, ionization), ionization.addToMass(formula.getMass()));
        //final SimpleSpectrum spectrum = merger.merge(new FinestructureGenerator(distribution, mode, cache).iteratorWithIntensityThreshold(formula, ionization, 0.0001), ionization.addToMass(formula.getMass()));
        // cut spectrum to allow only maxNumber peaks
        if (spectrum.size() <= maximalNumberOfPeaks) return Spectrums.getNormalizedSpectrum(spectrum, mode);
        final SimpleMutableSpectrum spec = new SimpleMutableSpectrum(spectrum);
        for (int k = spec.size() - 1; k >= maximalNumberOfPeaks; --k) {
            spec.removePeakAt(k);
        }
        Spectrums.normalize(spec, mode);
        return new SimpleSpectrum(spec);
    }

    public double getResolution() {
        return resolution;
    }

    public void setResolution(double resolution) {
        this.resolution = resolution;
    }


}
