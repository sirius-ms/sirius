package de.unijena.bioinf.ms.utils;

import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.math.HighQualityRandom;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.OrderedSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.babelms.MsExperimentParser;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.array.TDoubleArrayList;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class SpectralLibrarySearch {
    public static final boolean TEST = false;

    public static void main(String... args) throws IOException {
        PeriodicTable periodicTable = PeriodicTable.getInstance();
        File file = new File("/home/ge28quv/Data_work/Data/data_and_databases/massbank.ms");
        List<Ms2Experiment> experiments = new MsExperimentParser().getParser(file).parseFromFile(file);


        Deviation deviation = new Deviation(20,0.005);
        boolean sqrtIntensity = true;
        boolean multiplyByMass = true;
        int minSharedPeaks = 5;
        SpectralLibrarySearch spectralLibrarySearch = SpectralLibrarySearch.newInstance(experiments.toArray(new Ms2Experiment[0]), new GaussianSpectralAlignment(deviation), deviation, sqrtIntensity, multiplyByMass, minSharedPeaks);


        TDoubleArrayList doubleArrayList = new TDoubleArrayList();
        for (Ms2Experiment experiment : experiments) {
            SpectralLibraryHit hit = spectralLibrarySearch.findBestHit(experiment, AllowedMassDifference.allowDirectMatchesAndBiotransformations());
            if (hit.getLibraryHit()==null){
                System.out.println("none");
                continue;
            }

            doubleArrayList.add(hit.getCosine());
            System.out.println(hit.getCosine()+" "+hit.getNumberOfSharedPeaks()+" "+hit.getLibraryHit().getName()+" "+experiment.getName());

        }

        double min = 0;
        double max = 1d;
        double step = 0.02;

        for (double i = min; i < max; i+=step) {
            double finalI = i;
            int count = doubleArrayList.grep(d-> (d>finalI&&d<=finalI+step)).size();
            System.out.println(i+" "+count);

        }

    }

    //todo remove precursor mass?! or +-17Da or +-50Da?
    private final LibrarySpectrum[] librarySpectra;
    private final OrderedSpectrum<Peak>[] librarySpectraInverse;
    private final double[] selfSimilarity;
    private final double[] selfSimilarityLosses;
    private final IntensityTransformation intensityTransformation;
    private final int minSharedPeaks;
    private final Deviation deviation;
    private static final Normalization NORMALIZATION = Normalization.Sum(100);

    private final AbstractSpectralAlignment spectralAlignment;

    public SpectralLibrarySearch(LibrarySpectrum[] librarySpectra, AbstractSpectralAlignment spectralAlignment, Deviation ms2Deviation, boolean transformSqrtIntensity, boolean multiplyIntensityByMass, int minSharedPeaks) {
        //todo remove parent peaks!?
        this.librarySpectra = librarySpectra.clone();
        this.minSharedPeaks = minSharedPeaks;
        this.deviation = ms2Deviation;
        this.spectralAlignment = spectralAlignment;


        //sort for binary mz search
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
            selfSimilarity[i] = spectralAlignment.score(spec,spec).similarity;

            SimpleSpectrum inverse = Spectrums.getInversedSpectrum(spec, librarySpectrum.getIonMass());//todo ionmass?vs measured
            librarySpectraInverse[i] = inverse;
            selfSimilarityLosses[i] = spectralAlignment.score(inverse,inverse).similarity;
        }
    }

    public static SpectralLibrarySearch newInstance(Ms2Experiment[] library) {
        return newInstance(library, new GaussianSpectralAlignment(new Deviation(20, 005)), new Deviation(20, 0.005), true, true, 5);
    }

    public static SpectralLibrarySearch newInstance(Ms2Experiment[] library, AbstractSpectralAlignment spectralAlignment,  Deviation ms2MergeDeviation, boolean transformSqrtIntensity, boolean multiplyIntensityByMass, int minSharedPeaks) {
        LibrarySpectrum[] librarySpectra = new LibrarySpectrum[library.length];

        boolean mergePeaks = (spectralAlignment instanceof IntensityWeightedSpectralAlignment);
        for (int i = 0; i < librarySpectra.length; i++) {
            Ms2Experiment experiment = library[i];
            MergedMs2Spectrum mergedMs2Spectrum;
            if (experiment.hasAnnotation(MergedMs2Spectrum.class)){
                mergedMs2Spectrum = experiment.getAnnotation(MergedMs2Spectrum.class);
            } else {
                mergedMs2Spectrum = mergeMs2Spectra(experiment, ms2MergeDeviation, mergePeaks);
            }

            final LibrarySpectrum librarySpectrum = LibrarySpectrum.fromExperiment(experiment, mergedMs2Spectrum);
            librarySpectra[i] = librarySpectrum;
        }

        return new SpectralLibrarySearch(librarySpectra, spectralAlignment, ms2MergeDeviation, transformSqrtIntensity, multiplyIntensityByMass, minSharedPeaks);
    }

    /**
     * if iontype unknown, test common ones
     * @param compound
     * @return
     */
    public SpectralLibraryHit findBestHit(Ms2Experiment compound, AllowedMassDifference allowedMassDifference){
        if (compound.getPrecursorIonType().isIonizationUnknown()){
            Set<PrecursorIonType> commonIonTypes = PeriodicTable.getInstance().getIonizations(compound.getPrecursorIonType().getCharge());
            SpectralLibraryHit libraryHit = null;
            for (PrecursorIonType commonIonType : commonIonTypes) {
                SpectralLibraryHit currentHit = findBestHit(compound, commonIonType, allowedMassDifference);
                if (libraryHit==null || currentHit.getCosine()>libraryHit.getCosine() ||
                        (currentHit.getCosine()==libraryHit.getCosine() && currentHit.getNumberOfSharedPeaks()>libraryHit.getNumberOfSharedPeaks())
                ) {
                    libraryHit = currentHit;
                }
            }
            return libraryHit;
        } else {
            return findBestHit(compound, compound.getPrecursorIonType(), allowedMassDifference);
        }

    }

    public SpectralLibraryHit findBestHit(Ms2Experiment compound, PrecursorIonType ionType, AllowedMassDifference allowedMassDifference){
        MergedMs2Spectrum mergedMs2Spectrum;
        if (compound.hasAnnotation(MergedMs2Spectrum.class)){
            mergedMs2Spectrum = compound.getAnnotation(MergedMs2Spectrum.class);
        } else {
            boolean mergePeaks = (spectralAlignment instanceof IntensityWeightedSpectralAlignment);
            mergedMs2Spectrum = mergeMs2Spectra(compound, deviation, mergePeaks);
        }
        return findBestHit(mergedMs2Spectrum, compound.getIonMass(), ionType, allowedMassDifference);
    }

    public SpectralLibraryHit findBestHit(Spectrum<Peak> spectrum, double precursorMass, PrecursorIonType ionType, AllowedMassDifference allowedMassDifference){
        if (TEST){
            NoiseTransformation noiseTransformation = new NoiseTransformation();
            spectrum = Spectrums.transform(new SimpleMutableSpectrum(spectrum), noiseTransformation);
        }


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
        SpectralSimilarity bestSimilarity = null;
        QueryWithSelfSimilarity query = new QueryWithSelfSimilarity((OrderedSpectrum<Peak>)spectrum, precursorMass);
        TIntIterator candidateIterator = canditateIterator(precursorMass, allowedMassDifference.maxAllowedShift());
        while (candidateIterator.hasNext()) {
            int current = candidateIterator.next();
            if (ionType!=null && !librarySpectra[current].getIonType().equals(ionType)) continue; //only same or unknown iontype
            if (!allowedMassDifference.isAllowed(precursorMass, librarySpectra[current].getIonMass(), deviation)) continue; //only specific mass differences allowed. e.g. 0 or biotransformation
            //i is the library spectrum
            SpectralSimilarity similarity = score(query, current);
            if (similarity.shardPeaks<=minSharedPeaks) continue;
            if (bestHit==null || bestSimilarity.similarity<similarity.similarity){
                bestHit = librarySpectra[current];
                bestSimilarity = similarity;
            }
        }
        if (bestHit==null) {
            return new SpectralLibraryHit(bestHit, 0, 0);
        }
        return new SpectralLibraryHit(bestHit, bestSimilarity.similarity, bestSimilarity.shardPeaks);
    }

    private TIntIterator canditateIterator(double precursorMass, double allowedShift) {
        //iterate library spectra with matching mz
        //todo what about different precursoriontypes?
        int mid = binarySearch(librarySpectra, precursorMass);
        if (mid<0) mid = -(mid+1);
        int upper = mid-1; //mid-1 tested for lower
        for (int i = mid; i < librarySpectra.length; i++) {
            LibrarySpectrum librarySpectrum = librarySpectra[i];
            double libMz = librarySpectrum.getIonMass();
            if (precursorMass>=libMz-allowedShift-deviation.absoluteFor(libMz)){
                upper = i;
            } else break;
        }

        int lower = mid; //mid is tested for upper
        for (int i = mid - 1; i >= 0; i--) {
            LibrarySpectrum librarySpectrum = librarySpectra[i];
            double libMz = librarySpectrum.getIonMass();
            if (precursorMass<=libMz+allowedShift+deviation.absoluteFor(libMz)){
                lower = i;
            } else break;
        }
        int lowerFinal = lower;
        int upperFinal = upper;
        TIntIterator intIterator = new TIntIterator() {
            int current = lowerFinal;
            @Override
            public int next() {
                ++current;
                return current-1;
            }

            @Override
            public boolean hasNext() {
                return current<=upperFinal;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("removal not supported");
            }
        };
        return intIterator;
    }

    private int binarySearch(LibrarySpectrum[] librarySpectra, double precursorMass) {
        if (librarySpectra.length > 0) {
            int low = 0;
            int high = librarySpectra.length - 1;
            while (low <= high) {
                int mid = (low + high) >>> 1;
                int c = Double.compare(librarySpectra[mid].getIonMass(), precursorMass);
                if (c < 0)
                    low = mid + 1;
                else if (c > 0)
                    high = mid - 1;
                else
                    return mid; // key found
            }
            return -(low + 1);
        }
        return -1;
    }

    private SpectralSimilarity score(QueryWithSelfSimilarity query, int librarySpectrum) {
        return cosineProductWithLosses(query, librarySpectrum);
    }


    SpectralSimilarity cosineProduct(QueryWithSelfSimilarity query, int libIdx) {
        SpectralSimilarity similarity = spectralAlignment.score(query.spectrum, librarySpectra[libIdx].getFragmentationSpectrum());
        return new SpectralSimilarity(similarity.similarity /Math.sqrt(query.selfSimilarity*selfSimilarity[libIdx]), similarity.shardPeaks);
    }

    SpectralSimilarity cosineProductOfInverse(QueryWithSelfSimilarity query, int libIdx) {
        SpectralSimilarity similarity = spectralAlignment.score(query.inverseSpectrum, librarySpectraInverse[libIdx]);
        return new SpectralSimilarity(similarity.similarity /(Math.sqrt(query.selfSimilarityLosses*selfSimilarityLosses[libIdx])), similarity.shardPeaks);
    }

    private SpectralSimilarity cosineProductWithLosses(QueryWithSelfSimilarity query, int libIdx) {
        SpectralSimilarity similarity = cosineProduct(query, libIdx);
        SpectralSimilarity similarityLosses = cosineProductOfInverse(query, libIdx);

        return new SpectralSimilarity((similarity.similarity +similarityLosses.similarity)/2d, Math.max(similarity.shardPeaks, similarityLosses.shardPeaks));
    }

    @Deprecated
    private  <P extends Peak, S extends OrderedSpectrum<P>, P2 extends Peak, S2 extends OrderedSpectrum<P2>>
    SpectralSimilarity dotProductPeaksWithMinSharedPeaksThreshold(S left, S2 right) {
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
//                for (int k=i+1; k < nl; ++k) {
//                    final double difference2 = left.getMzAt(k)- right.getMzAt(j);
//                    if (Math.abs(difference2) <= allowedDifference) {
//                        score += left.getIntensityAt(k)*right.getIntensityAt(j);
//                        usedIndicesLeft.set(k);
//                    } else break;
//                }
//                for (int l=j+1; l < nr; ++l) {
//                    final double difference2 = left.getMzAt(i)- right.getMzAt(l);
//                    if (Math.abs(difference2) <= allowedDifference) {
//                        score += left.getIntensityAt(i)*right.getIntensityAt(l);
//                        usedIndicesRight.set(l);
//                    } else break;
//                }
                ++i; ++j;
            } else if (difference > 0) {
                ++j;
            } else {
                ++i;
            }
        }
        return new SpectralSimilarity(score, Math.min(usedIndicesLeft.cardinality(), usedIndicesRight.cardinality()));
    }




    private static MergedMs2Spectrum mergeMs2Spectra(Ms2Experiment experiment, Deviation deviation, boolean mergePeaks){
        if (mergePeaks){
            return new MergedMs2Spectrum(Spectrums.mergeSpectra(deviation, true, true, experiment.getMs2Spectra()));
        } else {
            //todo don't merge for gaussian. merge for alignment?
            return new MergedMs2Spectrum(Spectrums.mergeSpectra(experiment.getMs2Spectra()));
        }
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

    protected class NoiseTransformation implements Spectrums.Transformation<Peak, Peak> {
        final Deviation deviation = new Deviation(5,0.001);
        final Random random = new HighQualityRandom();

        @Override
        public Peak transform(Peak input) {
            double n = random.nextGaussian()*deviation.absoluteFor(input.getMass());
            return new Peak(input.getMass()+n, input.getIntensity());
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
            this.selfSimilarity = spectralAlignment.score(this.spectrum, this.spectrum).similarity;
            this.selfSimilarityLosses = spectralAlignment.score(inverseSpectrum, inverseSpectrum).similarity;

        }
    }

}
