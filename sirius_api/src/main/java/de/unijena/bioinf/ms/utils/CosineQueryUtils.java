package de.unijena.bioinf.ms.utils;

import de.unijena.bioinf.ChemistryBase.ms.Normalization;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
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
        Spectrums.cutByMassThreshold(mutableSpectrum, precursorMz-0.5);

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
            return new Peak(input.getMass(), intensity);
        }
    }

}
