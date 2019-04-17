package de.unijena.bioinf.ms.utils;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.chem.utils.biotransformation.BioTransformation;
import de.unijena.bioinf.ChemistryBase.math.HighQualityRandom;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.OrderedSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.babelms.MsExperimentParser;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.array.TDoubleArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class SpectralLibrarySearch {
    public static final boolean TEST = false;

    //todo remove precursor mass?! or +-17Da or +-50Da?
    private final LibrarySpectrum[] librarySpectra;
    private final CosineQuerySpectrum[] libraryQueries;

    private final CosineQueryUtils.IntensityTransformation intensityTransformation;
    private final int minSharedPeaks;
    private final Deviation deviation;
    private static final Normalization NORMALIZATION = Normalization.Sum(100);

    private final boolean transformationSearch;

    private final AbstractSpectralAlignment spectralAlignment;
    private final CosineQueryUtils cosineUtils;

    private final Logger Log = LoggerFactory.getLogger(SpectralLibrarySearch.class);

    /**
     *
     * @param librarySpectra
     * @param spectralAlignment
     * @param ms2Deviation
     * @param transformSqrtIntensity
     * @param multiplyIntensityByMass
     * @param minSharedPeaks
     * @param transformationSearch search for similar compounds. For this the similiarity is not the mean of the cosines of the spectras and the inverse spectras but the maximum
     */
    public SpectralLibrarySearch(LibrarySpectrum[] librarySpectra, AbstractSpectralAlignment spectralAlignment, Deviation ms2Deviation, boolean transformSqrtIntensity, boolean multiplyIntensityByMass, int minSharedPeaks, boolean transformationSearch) {
        //todo remove parent peaks!?
        this.librarySpectra = librarySpectra.clone();
        this.minSharedPeaks = minSharedPeaks;
        this.deviation = ms2Deviation;
        this.spectralAlignment = spectralAlignment;
        this.cosineUtils = new CosineQueryUtils(spectralAlignment);
        this.transformationSearch = transformationSearch;


        //sort for binary mz search
        Arrays.sort(this.librarySpectra, new Comparator<LibrarySpectrum>() {
            @Override
            public int compare(LibrarySpectrum o1, LibrarySpectrum o2) {
                return Double.compare(o1.getIonMass(), o2.getIonMass());
            }
        });

        if (transformSqrtIntensity) {
            intensityTransformation = new CosineQueryUtils.IntensityTransformation(multiplyIntensityByMass, transformSqrtIntensity);
            for (int i = 0; i < this.librarySpectra.length; i++) {
                LibrarySpectrum ls = this.librarySpectra[i];
                Spectrum<Peak> transformedSpectrum =  Spectrums.transform(new SimpleMutableSpectrum(ls.getFragmentationSpectrum()), intensityTransformation);
                LibrarySpectrum transformedLibrarySpectrum = new LibrarySpectrum(ls.getName(), transformedSpectrum, ls.getMolecularFormula(), ls.getIonType(), ls.getSmiles(), ls.getInChI(), ls.getSource());
                this.librarySpectra[i] = transformedLibrarySpectrum;
            }
        } else {
            intensityTransformation = null;
        }

        //compute self-similarities
        this.libraryQueries = new CosineQuerySpectrum[this.librarySpectra.length];
        for (int i = 0; i < this.librarySpectra.length; i++) {
            LibrarySpectrum ls = this.librarySpectra[i];
            SimpleMutableSpectrum mutableSpectrum = new SimpleMutableSpectrum(ls.getFragmentationSpectrum());
            //filter out parent and every peak with mz greater precursor mz
            Spectrums.cutByMassThreshold(mutableSpectrum, ls.getIonMass()-0.5);
            //normalize
            Spectrums.normalize(mutableSpectrum, NORMALIZATION);

            OrderedSpectrum<Peak> orderedSpectrum = new SimpleSpectrum(mutableSpectrum);
            LibrarySpectrum librarySpectrum = new LibrarySpectrum(ls.getName(), orderedSpectrum, ls.getMolecularFormula(), ls.getIonType(), ls.getSmiles(), ls.getInChI(), ls.getSource());
            this.librarySpectra[i] = librarySpectrum;
            this.libraryQueries[i] = cosineUtils.createQuery(orderedSpectrum, librarySpectrum.getIonMass());//todo ionmass?vs measured
        }
    }

    public static SpectralLibrarySearch newInstance(Ms2Experiment[] library) {
        return newInstance(library, new GaussianSpectralAlignment(new Deviation(20, 005)), new Deviation(20, 0.005), true, true, 5, false);
    }

    public static SpectralLibrarySearch newInstance(Ms2Experiment[] library, AbstractSpectralAlignment spectralAlignment,  Deviation ms2MergeDeviation, boolean transformSqrtIntensity, boolean multiplyIntensityByMass, int minSharedPeaks, boolean transformationSearch) {
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

        return new SpectralLibrarySearch(librarySpectra, spectralAlignment, ms2MergeDeviation, transformSqrtIntensity, multiplyIntensityByMass, minSharedPeaks, transformationSearch);
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
        SimpleMutableSpectrum mutableSpectrum = new SimpleMutableSpectrum(spectrum);
        if (TEST){
            NoiseTransformation noiseTransformation = new NoiseTransformation();
            mutableSpectrum = Spectrums.transform(mutableSpectrum, noiseTransformation);
        }

        //filter out parent and every peak with mz greater precursor mz
        Spectrums.cutByMassThreshold(mutableSpectrum, precursorMass-0.5);

        if (intensityTransformation !=null){
            mutableSpectrum = Spectrums.transform(mutableSpectrum, intensityTransformation);
        }

        Spectrums.normalize(mutableSpectrum, NORMALIZATION);

        OrderedSpectrum<Peak> orderedSpectrum = new SimpleSpectrum(mutableSpectrum);

//        double dotProduct =
        LibrarySpectrum bestHit = null;
        SpectralSimilarity bestSimilarity = null;
        CosineQuerySpectrum query = cosineUtils.createQuery(orderedSpectrum, precursorMass);
        TIntIterator candidateIterator = canditateIterator(precursorMass, allowedMassDifference.maxAllowedShift());
        while (candidateIterator.hasNext()) {
            int current = candidateIterator.next();
//            if (ionType!=null && !librarySpectra[current].getIonType().equals(ionType)) continue; //only same or unknown iontype
            if (ionType!=null && !librarySpectra[current].getIonType().getIonization().equals(ionType.getIonization())) continue; //only same or unknown ionization
            if (!allowedMassDifference.isAllowed(precursorMass, librarySpectra[current].getIonMass(), deviation)) continue; //only specific mass differences allowed. e.g. 0 or biotransformation
            //i is the library spectrum
            SpectralSimilarity similarity = score(query, current);
//            findFormulaOfCompound(librarySpectra[current], precursorMass, deviation, allowedMassDifference);
            if (similarity.shardPeaks<=minSharedPeaks) continue;
            if (bestHit==null || bestSimilarity.similarity<similarity.similarity){
                bestHit = librarySpectra[current];
                bestSimilarity = similarity;
            }
        }
        if (bestHit==null) {
            return new SpectralLibraryHit(bestHit, null, 0, 0);
        }
        MolecularFormula estimatedMF = findFormulaOfCompound(bestHit, precursorMass, deviation, allowedMassDifference);
        if (!ionType.hasNeitherAdductNorInsource() && estimatedMF!=null) estimatedMF = ionType.measuredNeutralMoleculeToNeutralMolecule(estimatedMF);
        return new SpectralLibraryHit(bestHit, estimatedMF, bestSimilarity.similarity, bestSimilarity.shardPeaks);
    }

    /**
     * if library hit and compound have different mass, estimate the compounds MF using biotransformations
     */
    private MolecularFormula findFormulaOfCompound(LibrarySpectrum librarySpectrum, double compoundMass, Deviation deviation, AllowedMassDifference allowedMassDifference){
        //todo or rather transform libraryMz with adducts/in-source-losses??
        MolecularFormula libraryPrecursorMF = librarySpectrum.getIonType().neutralMoleculeToMeasuredNeutralMolecule(librarySpectrum.getMolecularFormula());

        double libraryMz = librarySpectrum.getIonMass();
        if (deviation.inErrorWindow(libraryMz, compoundMass)) return libraryPrecursorMF;

        List<MolecularFormula> possibleTransf = new ArrayList<>();
        for (MolecularFormula transformation : allowedMassDifference.getPossibleMassDifferenceExplanations()) {
            MolecularFormula transformedMF = explainMassDiffWithTransformation(libraryMz, libraryPrecursorMF, compoundMass, transformation, deviation);
            if (transformedMF!=null) possibleTransf.add(transformedMF);
        }
        if (possibleTransf.size()==0){
            Log.debug("no suitable biotransformation found for compounds mz "+compoundMass+" and library MF "+librarySpectrum.getMolecularFormula()+" | "+librarySpectrum.getIonType());
            return null;
        } else if (possibleTransf.size()>1){
            Log.debug("mass difference of compound and library hit is ambiguous. skipping");
            return null;
        }
        return possibleTransf.get(0);
    }

    private MolecularFormula explainMassDiffWithTransformation(double libMz, MolecularFormula libFormula, double compoundMz, MolecularFormula transformation, Deviation deviation){
        double min,max;
        if (libMz<compoundMz){
            min = libMz;
            max = compoundMz;
        } else {
            min = compoundMz;
            max = libMz;
        }

        if (deviation.inErrorWindow(min+transformation.getMass(), max)){
            //todo ignore condition or don't?
            if (libMz<compoundMz) return libFormula.add(transformation);
            else {
                MolecularFormula withoutTransf = libFormula.subtract(transformation);
                if (withoutTransf.isAllPositiveOrZero()) return withoutTransf;
                else return null;
            }
        }
        return null;
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

    private SpectralSimilarity score(CosineQuerySpectrum query, int librarySpectrumIdx) {
        if (transformationSearch){
            //take max
            SpectralSimilarity cos = cosineUtils.cosineProduct(query, libraryQueries[librarySpectrumIdx]);
            SpectralSimilarity cosInv = cosineUtils.cosineProductOfInverse(query, libraryQueries[librarySpectrumIdx]);
            if (cos.similarity>cosInv.similarity || (cos.similarity==cosInv.similarity && cos.shardPeaks>cosInv.shardPeaks)){
                return cos;
            } else {
                return cosInv;
            }
        } else {
            //take mean
            return cosineUtils.cosineProductWithLosses(query, libraryQueries[librarySpectrumIdx]);
        }

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

//    protected class IntensityTransformation implements Spectrums.Transformation<Peak, Peak> {
//        final boolean multiplyByMass;
//        final boolean sqrtIntensity;
//
//        public IntensityTransformation(boolean multiplyByMass, boolean sqrtIntensity) {
//            this.multiplyByMass = multiplyByMass;
//            this.sqrtIntensity = sqrtIntensity;
//        }
//
//        @Override
//        public Peak transform(Peak input) {
//            double intensity = input.getIntensity();
//            if (sqrtIntensity){
//                intensity = intensity<=0?0:Math.sqrt(intensity);
//            }
//            if (multiplyByMass){
//                intensity = input.getMass()*intensity;
//            }
//            return new Peak(input.getMass(), intensity);
//        }
//    }

    protected class NoiseTransformation implements Spectrums.Transformation<Peak, Peak> {
        final Deviation deviation = new Deviation(5,0.001);
        final Random random = new HighQualityRandom();

        @Override
        public Peak transform(Peak input) {
            double n = random.nextGaussian()*deviation.absoluteFor(input.getMass());
            return new Peak(input.getMass()+n, input.getIntensity());
        }
    }

//    private class QueryWithSelfSimilarity {
//        final OrderedSpectrum<Peak> spectrum;
//        final SimpleSpectrum inverseSpectrum;
//        final double selfSimilarity;
//        final double selfSimilarityLosses;
//        final double precursorMz;
//
//        public QueryWithSelfSimilarity(OrderedSpectrum<Peak> spectrum, double precursorMz) {
//            this.spectrum = spectrum;
//            this.precursorMz = precursorMz;
//            //todo remove parent from inversed!?
//            this.inverseSpectrum = Spectrums.getInversedSpectrum(this.spectrum, precursorMz);
//            this.selfSimilarity = spectralAlignment.score(this.spectrum, this.spectrum).similarity;
//            this.selfSimilarityLosses = spectralAlignment.score(inverseSpectrum, inverseSpectrum).similarity;
//
//        }
//    }

}
