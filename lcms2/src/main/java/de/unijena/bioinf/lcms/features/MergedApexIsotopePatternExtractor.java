/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2023 Bright Giant GmbH
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
 *  You should have received a copy of the GNU General Public License along with SIRIUS.
 *  If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.lcms.features;

import de.unijena.bioinf.ms.persistence.model.core.spectrum.IsotopePattern;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedIsotopicFeatures;

import java.util.List;

public class MergedApexIsotopePatternExtractor implements IsotopePatternExtractionStrategy {

    @Override
    public IsotopePattern extractIsotopePattern(AlignedFeatures monoisotopicFeature, List<AlignedIsotopicFeatures> isotopicFeatures) {
        double[] mz = new double[isotopicFeatures.size() + 1];
        double[] ints = new double[isotopicFeatures.size() + 1];

        mz[0] = monoisotopicFeature.getApexMass();
        ints[0] = monoisotopicFeature.getApexIntensity();

        for (int i = 0; i < isotopicFeatures.size(); i++) {
            mz[i] = isotopicFeatures.get(i).getApexMass();
            ints[i] = isotopicFeatures.get(i).getApexIntensity();
        }

        return new IsotopePattern(mz, ints, IsotopePattern.Type.MERGED_APEX);
    }

}
