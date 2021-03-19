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

package de.unijena.bionf.spectral_alignment;

import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.utils.OrderedSpectrum;

/**
 * find best scoring alignment, intensity weighted. Each peak matches at most one peak in the other spectrum.
 */
public class IntensityWeightedSpectralAlignment extends AbstractSpectralAlignment {

    public IntensityWeightedSpectralAlignment(Deviation deviation) {
        super(deviation);
    }

    @Override
    public SpectralSimilarity score(OrderedSpectrum<Peak> left, OrderedSpectrum<Peak> right) {
        return score1To1(left, right);
    }

    @Override
    protected double scorePeaks(Peak lp, Peak rp) {
        return lp.getIntensity()*rp.getIntensity();
    }

    @Override
    protected double maxAllowedDifference(double mz) {
        return deviation.absoluteFor(mz);
    }
}
