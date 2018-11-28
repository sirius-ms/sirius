package de.unijena.bioinf.ms.utils;

import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.OrderedSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.babelms.MsExperimentParser;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.List;

public class SpectralLibrarySearch {
    //todo remove precursor mass?! or +-17Da or +-50Da?
    private final LibrarySpectrum[] librarySpectra;
    private final OrderedSpectrum<Peak>[] librarySpectraInverse;
    private final double[] selfSimilarity;
    private final double[] selfSimilarityLosses;
    private final IntensityTransformation intensityTransformation;
    private final int minSharedPeaks;
    private final Deviation deviation;
    private static final Normalization NORMALIZATION = Normalization.Sum(100);


    public SpectralLibrarySearch(LibrarySpectrum[] librarySpectra, Deviation ms2Deviation, boolean transformSqrtIntensity, boolean multiplyIntensityByMass, int minSharedPeaks) {
        this.librarySpectra = librarySpectra.clone();
        this.minSharedPeaks = minSharedPeaks;
        this.deviation = ms2Deviation;
        Arrays.sort(this.librarySpectra, new Comparator<LibrarySpectrum>() {
            @Override
            public int compare(LibrarySpectrum o1, LibrarySpectrum o2) {
                return Double.compare(o1.getIonMass(), o2.getIonMass());
            }
        });

        if (transformSqrtIntensity) {
            intensityTransformation = new IntensityTransformation(multiplyIntensityByMass, transformSqrtIntensity);
            for (int i = 0; i < this.librarySpectra.length; i++) {
                LibrarySpectrum ls = this.librarySpectra[i];
                Spectrum<Peak> transformedSpectrum =  Spectrums.transform(new SimpleMutableSpectrum(ls.getFragmentationSpectrum()), intensityTransformation);
                LibrarySpectrum transformedLibrarySpectrum = new LibrarySpectrum(ls.getName(), transformedSpectrum, ls.getMolecularFormula(), ls.getIonType(), ls.getSmiles(), ls.getInChI());
                this.librarySpectra[i] = transformedLibrarySpectrum;
            }
        } else {
            intensityTransformation = null;
        }

        //compute self-similarities
        librarySpectraInverse = new OrderedSpectrum[this.librarySpectra.length];
        selfSimilarity = new double[this.librarySpectra.length];
        selfSimilarityLosses = new double[this.librarySpectra.length];
        for (int i = 0; i < this.librarySpectra.length; i++) {
            LibrarySpectrum ls = this.librarySpectra[i];
            Spectrum<Peak> normalized = Spectrums.getNormalizedSpectrum(ls.getFragmentationSpectrum(), NORMALIZATION);
            LibrarySpectrum librarySpectrum = new LibrarySpectrum(ls.getName(), normalized, ls.getMolecularFormula(), ls.getIonType(), ls.getSmiles(), ls.getInChI());
            this.librarySpectra[i] = librarySpectrum;
            OrderedSpectrum<Peak> spec = librarySpectrum.getFragmentationSpectrum();
            selfSimilarity[i] = Spectrums.dotProductPeaks(spec,spec,deviation);

            SimpleSpectrum inverse = Spectrums.getInversedSpectrum(spec, librarySpectrum.getIonMass());//todo ionmass?vs measured
            librarySpectraInverse[i] = inverse;
            selfSimilarityLosses[i] = Spectrums.dotProductPeaks(inverse,inverse,deviation);
        }
    }


    public static SpectralLibrarySearch newInstance(Ms2Experiment[] library, Deviation ms2MergeDeviation, boolean transformSqrtIntensity, boolean multiplyIntensityByMass, int minSharedPeaks) {
        LibrarySpectrum[] librarySpectra = new LibrarySpectrum[library.length];

        for (int i = 0; i < librarySpectra.length; i++) {
            Ms2Experiment experiment = library[i];
            MergedMs2Spectrum mergedMs2Spectrum;
            if (experiment.hasAnnotation(MergedMs2Spectrum.class)){
                mergedMs2Spectrum = experiment.getAnnotation(MergedMs2Spectrum.class);
            } else {
                mergedMs2Spectrum = mergeMs2Spectra(experiment, ms2MergeDeviation);
            }

            final LibrarySpectrum librarySpectrum = LibrarySpectrum.fromExperiment(experiment, mergedMs2Spectrum);
            librarySpectra[i] = librarySpectrum;
        }

        return new SpectralLibrarySearch(librarySpectra, ms2MergeDeviation, transformSqrtIntensity, multiplyIntensityByMass, minSharedPeaks);
    }


    public SpectralLibraryHit findBestHit(Ms2Experiment compound){
        MergedMs2Spectrum mergedMs2Spectrum;
        if (compound.hasAnnotation(MergedMs2Spectrum.class)){
            mergedMs2Spectrum = compound.getAnnotation(MergedMs2Spectrum.class);
        } else {
            mergedMs2Spectrum = mergeMs2Spectra(compound, deviation); //todo extra ms2MergeDeviation?
        }
        return findBestHit(mergedMs2Spectrum, compound.getIonMass());
    }

    public SpectralLibraryHit findBestHit(Spectrum<Peak> spectrum, double precursorMass){
        if (intensityTransformation !=null){
            spectrum = Spectrums.transform(new SimpleMutableSpectrum(spectrum), intensityTransformation);
        }

        spectrum = Spectrums.getNormalizedSpectrum(spectrum, NORMALIZATION);


        //todo if obsolete?
        if (!(spectrum instanceof OrderedSpectrum)) {
            spectrum = new SimpleSpectrum(spectrum);
        }
//        double dotProduct =
        LibrarySpectrum bestHit = null;
        Similarity bestSimilarity = null;
        QueryWithSelfSimilarity query = new QueryWithSelfSimilarity((OrderedSpectrum<Peak>)spectrum, precursorMass);
        for (int i = 0; i < librarySpectra.length; i++) {
            //i is the library spectrum
            Similarity similarity = score(query, i);
            if (similarity.shardPeaks<=minSharedPeaks) continue;
            if (bestHit==null || bestSimilarity.similarity<similarity.similarity){
                bestHit = librarySpectra[i];
                bestSimilarity = similarity;
            }
        }
        if (bestHit==null) {
            return new SpectralLibraryHit(bestHit, 0, 0);
        }
        return new SpectralLibraryHit(bestHit, bestSimilarity.similarity, bestSimilarity.shardPeaks);
    }

    private Similarity score(QueryWithSelfSimilarity query, int librarySpectrum) {
        return cosineProductWithLosses(query, librarySpectrum);
    }


    Similarity cosineProduct(QueryWithSelfSimilarity query, int libIdx) {
        Similarity similarity = dotProductPeaksWithMinSharedPeaksThreshold(query.spectrum, librarySpectra[libIdx].getFragmentationSpectrum());
        return new Similarity(similarity.similarity /Math.sqrt(query.selfSimilarity*selfSimilarity[libIdx]), similarity.shardPeaks);
    }

    Similarity cosineProductOfInverse(QueryWithSelfSimilarity query, int libIdx) {
        Similarity similarity = dotProductPeaksWithMinSharedPeaksThreshold(query.inverseSpectrum, librarySpectraInverse[libIdx]);
        return new Similarity(similarity.similarity /(Math.sqrt(query.selfSimilarityLosses*selfSimilarityLosses[libIdx])), similarity.shardPeaks);
    }

    private Similarity cosineProductWithLosses(QueryWithSelfSimilarity query, int libIdx) {
        Similarity similarity = cosineProduct(query, libIdx);
        Similarity similarityLosses = cosineProductOfInverse(query, libIdx);

        return new Similarity((similarity.similarity +similarityLosses.similarity)/2d, Math.max(similarity.shardPeaks, similarityLosses.shardPeaks));
    }

    private  <P extends Peak, S extends OrderedSpectrum<P>, P2 extends Peak, S2 extends OrderedSpectrum<P2>>
    Similarity dotProductPeaksWithMinSharedPeaksThreshold(S left, S2 right) {
        int i=0, j=0;
        final int nl=left.size(), nr=right.size();
        final BitSet usedIndicesLeft = new BitSet();
        final BitSet usedIndicesRight = new BitSet();
        double score=0d;
        while (i < nl && left.getMzAt(i) < 0.5d) ++i;
        while (j < nr && right.getMzAt(j) < 0.5d) ++j;
        while (i < nl && j < nr) {
            final double difference = left.getMzAt(i)- right.getMzAt(j);
            final double allowedDifference = deviation.absoluteFor(Math.min(left.getMzAt(i), right.getMzAt(j)));
            if (Math.abs(difference) <= allowedDifference) {
                score += left.getIntensityAt(i)*right.getIntensityAt(j);
                usedIndicesLeft.set(i);
                usedIndicesRight.set(j);
                for (int k=i+1; k < nl; ++k) {
                    final double difference2 = left.getMzAt(k)- right.getMzAt(j);
                    if (Math.abs(difference2) <= allowedDifference) {
                        score += left.getIntensityAt(k)*right.getIntensityAt(j);
                        usedIndicesLeft.set(k);
                    } else break;
                }
                for (int l=j+1; l < nr; ++l) {
                    final double difference2 = left.getMzAt(i)- right.getMzAt(l);
                    if (Math.abs(difference2) <= allowedDifference) {
                        score += left.getIntensityAt(i)*right.getIntensityAt(l);
                        usedIndicesRight.set(l);
                    } else break;
                }
                ++i; ++j;
            } else if (difference > 0) {
                ++j;
            } else {
                ++i;
            }
        }
        return new Similarity(score, Math.min(usedIndicesLeft.cardinality(), usedIndicesRight.cardinality()));
    }




    private static MergedMs2Spectrum mergeMs2Spectra(Ms2Experiment experiment, Deviation deviation){
        //todo best to merge spectra? only combining MS2 without merging results in less cosines >1, but overall lower values
        return new MergedMs2Spectrum(Spectrums.mergeSpectra(experiment.getMs2Spectra()));
//        return new MergedMs2Spectrum(Spectrums.mergeSpectra(deviation, true, true, experiment.getMs2Spectra()));
    }

    protected class IntensityTransformation implements Spectrums.Transformation<Peak, Peak> {
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

    private class Similarity {
        final double similarity;
        final int shardPeaks;

        public Similarity(double similarity, int shardPeaks) {
            this.similarity = similarity;
            this.shardPeaks = shardPeaks;
        }
    }

    private class QueryWithSelfSimilarity {
        final OrderedSpectrum<Peak> spectrum;
        final SimpleSpectrum inverseSpectrum;
        final double selfSimilarity;
        final double selfSimilarityLosses;
        final double precursorMz;

        public QueryWithSelfSimilarity(OrderedSpectrum<Peak> spectrum, double precursorMz) {
            this.spectrum = spectrum;
            this.precursorMz = precursorMz;
            //todo remove parent from inversed!?
            this.inverseSpectrum = Spectrums.getInversedSpectrum(this.spectrum, precursorMz);
            this.selfSimilarity = Spectrums.dotProductPeaks(this.spectrum, this.spectrum, deviation);
            this.selfSimilarityLosses = Spectrums.dotProductPeaks(inverseSpectrum, inverseSpectrum, deviation);

        }
    }

}
