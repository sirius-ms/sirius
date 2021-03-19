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
    private final AbstractSpectralAlignment spectralAlignmentMethod;

    private static final Normalization NORMALIZATION = Normalization.Sum(100);

    public CosineQueryUtils(AbstractSpectralAlignment spectralAlignmentMethod) {
        this.spectralAlignmentMethod = spectralAlignmentMethod;
    }

    /**
     * create a query for cosine computation
     * @param spectrum
     * @param precursorMz
     * @return
     */
    public CosineQuerySpectrum createQuery(OrderedSpectrum<Peak> spectrum, double precursorMz){
        return CosineQuerySpectrum.newInstance(spectrum, precursorMz, spectralAlignmentMethod);
    }


    /**
     * create a query for cosine computation
     * @param spectrum
     * @param precursorMz
     * @return
     */
    public CosineQuerySpectrum createQueryWithIntensityTransformation(Spectrum<Peak> spectrum, double precursorMz, boolean transformSqrtIntensity){
        //todo transfomation first or other way around?

        IntensityTransformation intensityTransformation = new CosineQueryUtils.IntensityTransformation(true, transformSqrtIntensity);
        SimpleMutableSpectrum mutableSpectrum;
        if (spectrum instanceof SimpleMutableSpectrum){
            mutableSpectrum = (SimpleMutableSpectrum)spectrum;
        } else {
            mutableSpectrum = new SimpleMutableSpectrum(spectrum);
        }

        mutableSpectrum =  Spectrums.transform(mutableSpectrum, intensityTransformation);
        Spectrums.cutByMassThreshold(mutableSpectrum, precursorMz-20);

        Spectrums.normalize(mutableSpectrum, NORMALIZATION);

        if (Spectrums.isMassOrderedSpectrum(mutableSpectrum)){
            return CosineQuerySpectrum.newInstance(Spectrums.getAlreadyOrderedSpectrum(mutableSpectrum), precursorMz, spectralAlignmentMethod);
        } else {
            return CosineQuerySpectrum.newInstance(new SimpleSpectrum(mutableSpectrum), precursorMz, spectralAlignmentMethod);
        }

    }

    /**
     * kaidu: Zwischenloesung. Das sollte alles irgendwie einfacher aufgebaut sein.
     */
    public CosineQuerySpectrum createQueryWithIntensityTransformationNoLoss(Spectrum<Peak> spectrum, double precursorMz, boolean transformSqrtIntensity){
        IntensityTransformation intensityTransformation = new CosineQueryUtils.IntensityTransformation(true, transformSqrtIntensity);
        SimpleMutableSpectrum mutableSpectrum;
        if (spectrum instanceof SimpleMutableSpectrum){
            mutableSpectrum = (SimpleMutableSpectrum)spectrum;
        } else {
            mutableSpectrum = new SimpleMutableSpectrum(spectrum);
        }

        mutableSpectrum =  Spectrums.transform(mutableSpectrum, intensityTransformation);
        Spectrums.cutByMassThreshold(mutableSpectrum, precursorMz-20);

        Spectrums.normalize(mutableSpectrum, NORMALIZATION);

        if (Spectrums.isMassOrderedSpectrum(mutableSpectrum)){
            return CosineQuerySpectrum.newInstance(Spectrums.getAlreadyOrderedSpectrum(mutableSpectrum), precursorMz, spectralAlignmentMethod);
        } else {
            return CosineQuerySpectrum.newInstance(new SimpleSpectrum(mutableSpectrum), precursorMz, spectralAlignmentMethod);
        }

    }



    /**
     * compute cosine of 2 instances
     * @param query1
     * @param query2
     * @return
     */
    public SpectralSimilarity cosineProductWithLosses(CosineQuerySpectrum query1, CosineQuerySpectrum query2) {
        SpectralSimilarity similarity = cosineProduct(query1, query2);
        SpectralSimilarity similarityLosses = cosineProductOfInverse(query1, query2);

        return new SpectralSimilarity((similarity.similarity +similarityLosses.similarity)/2d, Math.max(similarity.shardPeaks, similarityLosses.shardPeaks));
    }

    public SpectralSimilarity cosineProduct(CosineQuerySpectrum query1, CosineQuerySpectrum query2) {
        SpectralSimilarity similarity = spectralAlignmentMethod.score(query1.spectrum, query2.spectrum);
        return new SpectralSimilarity(similarity.similarity /Math.sqrt(query1.selfSimilarity*query2.selfSimilarity), similarity.shardPeaks);
    }

    public SpectralSimilarity cosineProductOfInverse(CosineQuerySpectrum query, CosineQuerySpectrum query2) {
        SpectralSimilarity similarity = spectralAlignmentMethod.score(query.inverseSpectrum, query2.inverseSpectrum);
        return new SpectralSimilarity(similarity.similarity /(Math.sqrt(query.selfSimilarityLosses*query2.selfSimilarityLosses)), similarity.shardPeaks);
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
