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

package de.unijena.bionf.spectral_alignment;

import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.utils.OrderedSpectrum;

import java.util.List;

/**
 * A class that provides an API for spectral alignment over {@link OrderedSpectrum}
 */
public class OrderedSpectraMatcher {

    private final CosineQueryUtils queryUtils;
    private final CosineSpectraMatcher cosineMatcher;

    public OrderedSpectraMatcher(SpectralMatchingType alignmentType, Deviation maxPeakDeviation) {
        if (alignmentType == SpectralMatchingType.MODIFIED_COSINE) {
            throw new IllegalArgumentException("Modified cosine scoring needs precursor mass, use CosineSpectraMatcher.");
        }
        queryUtils = new CosineQueryUtils(alignmentType.getScorer(maxPeakDeviation));
        cosineMatcher = new CosineSpectraMatcher(queryUtils);
    }

    SpectralSimilarity match(OrderedSpectrum<Peak> left, OrderedSpectrum<Peak> right) {
        return queryUtils.cosineProduct(cosineSpectrum(left), cosineSpectrum(right));
    }

    /**
     * See {@link CosineSpectraMatcher#matchParallel(CosineQuerySpectrum, List)}
     */
    List<SpectralSimilarity> matchParallel(OrderedSpectrum<Peak> query, List<OrderedSpectrum<Peak>> references) {
        return cosineMatcher.matchParallel(cosineSpectrum(query), references.stream().map(this::cosineSpectrum).toList());
    }

    /**
     * See {@link CosineSpectraMatcher#matchAllParallel(List)}
     */
    List<List<SpectralSimilarity>> matchAllParallel(List<OrderedSpectrum<Peak>> spectra) {
        return cosineMatcher.matchAllParallel(spectra.stream().map(this::cosineSpectrum).toList());
    }

    private CosineQuerySpectrum cosineSpectrum(OrderedSpectrum<Peak> spectrum) {
        return queryUtils.createQueryWithoutLoss(spectrum, Double.NaN);
    }
}