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

import de.unijena.bioinf.ChemistryBase.ms.Normalization;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.SimplePeak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.OrderedSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;

public class CosineQueryUtils {
    private final AbstractSpectralMatching spectralMatchingMethod;

    private static final Normalization NORMALIZATION = Normalization.Sum(100);

    public CosineQueryUtils(AbstractSpectralMatching spectralMatchingMethod) {
        this.spectralMatchingMethod = spectralMatchingMethod;
    }

    /**
     * create a query for cosine computation
     */
    public CosineQuerySpectrum createQuery(OrderedSpectrum<Peak> spectrum, double precursorMz){
        return CosineQuerySpectrum.newInstance(spectrum, precursorMz, spectralMatchingMethod);
    }

    public CosineQuerySpectrum createQueryWithoutLoss(OrderedSpectrum<Peak> spectrum, double precursorMz){
        return CosineQuerySpectrum.newInstanceWithoutLoss(spectrum, precursorMz, spectralMatchingMethod);
    }

    public CosineQuerySpectrum createQueryWithIntensityTransformation(Spectrum<Peak> spectrum, double precursorMz, boolean transformSqrtIntensity){
        return createQuery(spectrum, precursorMz, transformSqrtIntensity, false);
    }

    /**
     *
     */
    public CosineQuerySpectrum createQuery(Spectrum<Peak> spectrum, double precursorMz, boolean transformSqrtIntensity, boolean transformIntensityByMass) {
        return createQuery(spectrum, precursorMz, transformSqrtIntensity, transformIntensityByMass, 13d);
    }

    public CosineQuerySpectrum createQuery(Spectrum<Peak> spectrum, double precursorMz, boolean transformSqrtIntensity, boolean transformIntensityByMass, double precursorRemovalWindowDa) {
        IntensityTransformation intensityTransformation = new CosineQueryUtils.IntensityTransformation(transformIntensityByMass, transformSqrtIntensity);
        SimpleMutableSpectrum mutableSpectrum;
        if (spectrum instanceof SimpleMutableSpectrum){
            mutableSpectrum = (SimpleMutableSpectrum)spectrum;
        } else {
            mutableSpectrum = new SimpleMutableSpectrum(spectrum);
        }

        Spectrums.transform(mutableSpectrum, intensityTransformation);
        Spectrums.cutByMassThreshold(mutableSpectrum, precursorMz - precursorRemovalWindowDa);

        Spectrums.normalize(mutableSpectrum, NORMALIZATION);

        if (Spectrums.isMassOrderedSpectrum(mutableSpectrum)){
            return CosineQuerySpectrum.newInstance(Spectrums.getAlreadyOrderedSpectrum(mutableSpectrum), precursorMz, spectralMatchingMethod);
        } else {
            return CosineQuerySpectrum.newInstance(new SimpleSpectrum(mutableSpectrum), precursorMz, spectralMatchingMethod);
        }

    }

    /**
     * kaidu: Zwischenloesung. Das sollte alles irgendwie einfacher aufgebaut sein.
     */
    public CosineQuerySpectrum createQueryWithIntensityTransformationNoLoss(Spectrum<Peak> spectrum, double precursorMz, boolean transformSqrtIntensity) {
        return createQueryWithIntensityTransformationNoLoss(spectrum, precursorMz, transformSqrtIntensity, 13d);
    }

    public CosineQuerySpectrum createQueryWithIntensityTransformationNoLoss(Spectrum<Peak> spectrum, double precursorMz, boolean transformSqrtIntensity, double precursorRemovalWindowDa) {
        IntensityTransformation intensityTransformation = new CosineQueryUtils.IntensityTransformation(true, transformSqrtIntensity);
        SimpleMutableSpectrum mutableSpectrum;
        if (spectrum instanceof SimpleMutableSpectrum){
            mutableSpectrum = (SimpleMutableSpectrum)spectrum;
        } else {
            mutableSpectrum = new SimpleMutableSpectrum(spectrum);
        }

        Spectrums.transform(mutableSpectrum, intensityTransformation);
        Spectrums.cutByMassThreshold(mutableSpectrum, precursorMz - precursorRemovalWindowDa);

        Spectrums.normalize(mutableSpectrum, NORMALIZATION);

        if (Spectrums.isMassOrderedSpectrum(mutableSpectrum)){
            return CosineQuerySpectrum.newInstance(Spectrums.getAlreadyOrderedSpectrum(mutableSpectrum), precursorMz, spectralMatchingMethod);
        } else {
            return CosineQuerySpectrum.newInstance(new SimpleSpectrum(mutableSpectrum), precursorMz, spectralMatchingMethod);
        }

    }



    /**
     * compute cosine of 2 instances
     */
    public SpectralSimilarity cosineProductWithLosses(CosineQuerySpectrum query1, CosineQuerySpectrum query2) {
        SpectralSimilarity similarity = cosineProduct(query1, query2);
        if (spectralMatchingMethod instanceof ModifiedCosine) {
            return similarity;
        } else {
            SpectralSimilarity similarityLosses = cosineProductOfInverse(query1, query2);
            return new SpectralSimilarity((similarity.similarity + similarityLosses.similarity) / 2d, Math.max(similarity.sharedPeaks, similarityLosses.sharedPeaks));
        }
    }

    public SpectralSimilarity cosineProduct(CosineQuerySpectrum query1, CosineQuerySpectrum query2) {
        SpectralSimilarity similarity = spectralMatchingMethod.score(query1.spectrum, query2.spectrum, query1.precursorMz, query2.precursorMz);
        return new SpectralSimilarity(similarity.similarity / Math.sqrt(query1.selfSimilarity*query2.selfSimilarity), similarity.sharedPeaks);
    }

    public SpectralSimilarity cosineProductOfInverse(CosineQuerySpectrum query, CosineQuerySpectrum query2) {
        SpectralSimilarity similarity = spectralMatchingMethod.score(query.inverseSpectrum, query2.inverseSpectrum, query.precursorMz, query2.precursorMz);
        return new SpectralSimilarity(similarity.similarity / (Math.sqrt(query.selfSimilarityLosses*query2.selfSimilarityLosses)), similarity.sharedPeaks);
    }



    protected static class IntensityTransformation implements Spectrums.Transformation<Peak, Peak> {
        final boolean multiplyByMass;
        final boolean sqrtIntensity;

        public IntensityTransformation(boolean multiplyByMass, boolean sqrtIntensity) {
            this.multiplyByMass = multiplyByMass;
            this.sqrtIntensity = sqrtIntensity;
        }

        @Override
        public Peak transform(Peak input) {
            double intensity = input.getIntensity();
            if (sqrtIntensity){
                intensity = intensity<=0?0:Math.sqrt(intensity);
            }
            if (multiplyByMass){
                intensity = input.getMass()*intensity;
            }
            return new SimplePeak(input.getMass(), intensity);
        }
    }

}
