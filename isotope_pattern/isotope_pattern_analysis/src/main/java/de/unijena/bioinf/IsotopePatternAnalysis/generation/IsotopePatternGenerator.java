
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
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.chem.utils.IsotopicDistribution;
import de.unijena.bioinf.ChemistryBase.ms.Normalization;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;

/*
    Simulates isotopic patterns for a given molecular formula with one peak per nominal mass.

    minimalProbabilityThreshold and maximalNumberOfPeaks are important for the performance of the method.
    Although pattern simulation  is fast for small molecules, the performance can be increased dramatically by chosing
    good parameter values for both variables. Usually, only very few isotope peaks are measured, so generating hundred
    of isotopologue peaks is just a waste of computing time. Remark that not all implementations can benefit from both
    parameters in the same way: e.g. folding speeds up with a small value for maximalNumberOfPeaks while
    minimalProbabilityThreshold gives no speedup. Finestructured pattern generation instead benefit only from
    minimalProbabilityThreshold but not from maximalNumberOfPeaks. So setting both parameters is recommended.
 */
public abstract class IsotopePatternGenerator {

    protected final IsotopicDistribution distribution;
    protected final Normalization mode;

    /**
     * The minimal intensity of generated peaks. Peaks with lower intensities are not generated.
     * Important: The threshold is given as relative value between 0.0 and 1.0 corresponding to the
     * isotopologue probability. It is independend from the normalization mode and the real intensity value.
     */
    protected double minimalProbabilityThreshold;

    /**
     * maximal number of peaks which should be generated. This is also the maximal mass of peaks that should be generated
     * (so maximalNumberOfPeaks=10 means that all peaks with 10 Da above the nominal mass should be generated).
     * <p>
     * This parameter has high impact on the performance, so it is recommended to set it on some reasonable small value
     * (e.g. 5 or 10).
     */
    protected int maximalNumberOfPeaks;

    /**
     * @param distribution distribution with intensities for the alphabet of elements
     * @param mode         the normalization mode for the spectrum
     */
    protected IsotopePatternGenerator(IsotopicDistribution distribution, Normalization mode) {
        this.distribution = distribution;
        this.mode = mode;
        this.minimalProbabilityThreshold = 0.001d;
        this.maximalNumberOfPeaks = 10;
        if (mode == null || distribution == null) throw new NullPointerException("Expect non null parameters");
    }

    protected IsotopePatternGenerator() {
        this(Normalization.Sum(1d));
    }

    protected IsotopePatternGenerator(Normalization mode) {
        this(PeriodicTable.getInstance().getDistribution(), mode);
    }

    public abstract SimpleSpectrum simulatePattern(MolecularFormula formula, Ionization ionization);

    public int getMaximalNumberOfPeaks() {
        return maximalNumberOfPeaks;
    }

    public void setMaximalNumberOfPeaks(int maximalNumberOfPeaks) {
        this.maximalNumberOfPeaks = maximalNumberOfPeaks;
    }

    public double getMinimalProbabilityThreshold() {
        return minimalProbabilityThreshold;
    }

    public void setMinimalProbabilityThreshold(double minimalProbabilityThreshold) {
        this.minimalProbabilityThreshold = minimalProbabilityThreshold;
    }

    public IsotopicDistribution getDistribution() {
        return distribution;
    }

    public Normalization getMode() {
        return mode;
    }
}
