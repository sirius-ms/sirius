package de.unijena.bioinf.ms.utils;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.chem.RetentionTime;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.OrderedSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.utils.clustering.CompleteLinkage;
import de.unijena.bioinf.utils.clustering.HierarchicalClustering;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TShortArrayList;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

/**
 * merge compounds ({@link de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment}s) between different (LC/MS/MS) runs.
 * //todo transform intensities for similarity computation or don't?
 * //todo how to merge isotope patterns
 */
public class Ms2CompoundMerger {

    protected final static boolean SIMPLY_SUM_INTENSITIES = true;
    protected final static boolean TAKE_MOST_INTENSE_ISOTOPE_PATTERN = true;

    private final Deviation maxMzDeviation;
    private final double maxRetentionTimeShift;
    private final double cosineSimilarity;
    private final boolean mergeWithinRuns;
    private final MeasurementProfile findIsotopesMeasurementProfile;

    private final CosineQueryUtils cosineUtils;

//    private Map<Ms2Experiment, Spectrum<Peak>> experimentToMergedMs2;

    public Ms2CompoundMerger(Deviation maxMzDeviation, double maxRetentionTimeShift, double cosineSimilarity, boolean mergeWithinRuns) {
        this.maxMzDeviation = maxMzDeviation;
        this.maxRetentionTimeShift = maxRetentionTimeShift;
        this.cosineSimilarity = cosineSimilarity;
        this.mergeWithinRuns = mergeWithinRuns;
//        //todo is hashmap necessary? or just array?
//        experimentToMergedMs2 = new HashMap<>();

        MutableMeasurementProfile mmp = new MutableMeasurementProfile();
        mmp.setAllowedMassDeviation(maxMzDeviation);
        mmp.setStandardMassDifferenceDeviation(maxMzDeviation.divide(2));//todo add hoc solution works with ms1merged from openMs features
        this.findIsotopesMeasurementProfile = mmp;

        this.cosineUtils = new CosineQueryUtils(new GaussianSpectralAlignment(maxMzDeviation));//todo what deviation? for library search using 10ppm for parent mz, but 20 ppm for cosine scoring on ms2 peaks
    }


    public List<Ms2Experiment> mergeRuns(Deviation deviation, List<Ms2Experiment>... runs){
        //todo same deviation to merge and to score?
        List<Ms2Experiment> allExperimentsWithMs2 = new ArrayList<>();
        TShortArrayList expWithMs2RunIndices = new TShortArrayList();
        List<Ms2Experiment> allExperimentsWithoutMs2 = new ArrayList<>();
        short idx = 0;
        for (List<Ms2Experiment> run : runs) {
            for (Ms2Experiment experiment : run) {
                boolean hasMs2 = hasMs2(experiment);
                if (hasMs2){
                    allExperimentsWithMs2.add(experiment);
                    expWithMs2RunIndices.add(idx);
                }
                else allExperimentsWithoutMs2.add(experiment);
            }
            ++idx;
        }
//        OrderedSpectrum<Peak>[] mergedSpectra = new OrderedSpectrum[allExperimentsWithMs2.size()];
        CosineQuerySpectrum[] querySpectra = new CosineQuerySpectrum[allExperimentsWithMs2.size()];

        //create merged Ms2 to compare compounds (sqrt of intensity fo improve similarity comparison)
        SqrtIntensityTransformation sqrtIntensityTransformation = new SqrtIntensityTransformation();
        int numberOfMergedMs2 = numberOfMergedMs2(allExperimentsWithMs2);
        if (numberOfMergedMs2>0 && numberOfMergedMs2<allExperimentsWithMs2.size()){
            LoggerFactory.getLogger(Ms2CompoundMerger.class).warn("Not all but some compounds already contain a merged Ms2 spectrum. Recomputing all.");
        }
        if (numberOfMergedMs2==allExperimentsWithMs2.size()){
            int i = 0;
            for (Ms2Experiment experiment : allExperimentsWithMs2) {
                MergedMs2Spectrum mergedMs2Spectrum = experiment.getAnnotation(MergedMs2Spectrum.class);
                putSpectrum(querySpectra, i++, mergedMs2Spectrum);

            }

        } else {
            //compute merged
            int i = 0;
            for (Ms2Experiment experiment : allExperimentsWithMs2) {
                MergedMs2Spectrum mergedMs2Spectrum = mergeMs2Spectra(experiment, deviation);
                putSpectrum(querySpectra, i++, mergedMs2Spectrum);
            }
        }

        //now cluster similar compounds together
        //1. create Distance Matrix
        //todo stupid, since it stores all Infinity values
        double[][] distances = new double[allExperimentsWithMs2.size()][allExperimentsWithMs2.size()];
        for (int i = 0; i < allExperimentsWithMs2.size(); i++) {
            distances[i][i] = 0d;
            Ms2Experiment exp1 = allExperimentsWithMs2.get(i);
            final PrecursorIonType ionType1 = exp1.getPrecursorIonType();
            short runIdx1 = expWithMs2RunIndices.get(i);
            CosineQuerySpectrum querySpectrum1 = querySpectra[i];
            for (int j = i+1; j < allExperimentsWithMs2.size(); j++) {
                Ms2Experiment exp2 = allExperimentsWithMs2.get(j);
                final PrecursorIonType ionType2 = exp2.getPrecursorIonType();
                short runIdx2 = expWithMs2RunIndices.get(j);
                if (!mergeWithinRuns && runIdx1==runIdx2){
                    distances[j][i] = distances[i][j] = Double.POSITIVE_INFINITY;
                    continue;
                }
                //don't merge different ionizations
                if (!ionType1.isIonizationUnknown() && !ionType2.isIonizationUnknown() && !ionType1.equals(ionType2)){
                    distances[j][i] = distances[i][j] = Double.POSITIVE_INFINITY;
                    continue;
                }
                CosineQuerySpectrum querySpectrum2 = querySpectra[j];
                if (!maxMzDeviation.inErrorWindow(exp1.getIonMass(), exp2.getIonMass())){
                    distances[j][i] = distances[i][j] = Double.POSITIVE_INFINITY;
                    continue;
                }
                if (exp1.hasAnnotation(RetentionTime.class) && exp2.hasAnnotation(RetentionTime.class)){
                    RetentionTime time1 = exp1.getAnnotation(RetentionTime.class);
                    RetentionTime time2 = exp2.getAnnotation(RetentionTime.class);
                    if (Math.abs(time1.getMiddleTime()-time2.getMiddleTime())>maxRetentionTimeShift){
                        distances[j][i] = distances[i][j] = Double.POSITIVE_INFINITY;
                        continue;
                    }
                }
//                double cosine = Spectrums.cosineProduct(mergedMs2Spectrum1, mergedMs2Spectrum2, deviation);
                //changed
                double cosine = cosineUtils.cosineProductWithLosses(querySpectrum1, querySpectrum2).similarity;
                assert cosine<=1d;
                if (cosine<cosineSimilarity){
                    distances[j][i] = distances[i][j] = Double.POSITIVE_INFINITY;
                } else {
                    distances[j][i] = distances[i][j] = 1d-cosine;
                }

            }

        }


        //2. cluster
        HierarchicalClustering<Ms2Experiment> clustering = new HierarchicalClustering<>(new CompleteLinkage());
        clustering.cluster(allExperimentsWithMs2.toArray(new Ms2Experiment[0]), distances, 1d-cosineSimilarity);

        List<List<Ms2Experiment>> clusters = clustering.getClusters();


        //3. merge
        List<Ms2Experiment> mergedExperiments = new ArrayList<>();
        for (List<Ms2Experiment> cluster : clusters) {
            mergedExperiments.add(mergeExperiments(cluster, deviation));
        }

        //4. all all experiments without ms2 information without merging them
        mergedExperiments.addAll(allExperimentsWithoutMs2);

        return mergedExperiments;
    }

    @Deprecated
    private void putSpectrum(Spectrum<Peak>[] mergedSpectra, int index, MergedMs2Spectrum mergedMs2Spectrum) {
        if (Spectrums.isMassOrderedSpectrum(mergedMs2Spectrum)){
            mergedSpectra[index] = Spectrums.getAlreadyOrderedSpectrum(mergedMs2Spectrum);
        } else {
            mergedSpectra[index] = new SimpleSpectrum(mergedMs2Spectrum);
        }
    }

    private void putSpectrum(CosineQuerySpectrum[] cosineQuerySpectra, int index, MergedMs2Spectrum mergedMs2Spectrum) {
        assert mergedMs2Spectrum.getPrecursorMz()>0;
//        if (Spectrums.isMassOrderedSpectrum(mergedMs2Spectrum)){
//            cosineQuerySpectra[index] = cosineUtils.createQuery(Spectrums.getAlreadyOrderedSpectrum(mergedMs2Spectrum), mergedMs2Spectrum.getPrecursorMz());
//        } else {
//            cosineQuerySpectra[index] = cosineUtils.createQuery(new SimpleSpectrum(mergedMs2Spectrum), mergedMs2Spectrum.getPrecursorMz());
//        }
        cosineQuerySpectra[index] = cosineUtils.createQueryWithIntensityTransformation(mergedMs2Spectrum, mergedMs2Spectrum.getPrecursorMz(), true);
    }

    private boolean hasMs2(Ms2Experiment experiment) {
        if (experiment.getMs2Spectra()==null) return false;
        for (Ms2Spectrum<Peak> spectrum : experiment.getMs2Spectra()) {
            if (spectrum.size()>0){
                return true;
            }
        }
        return false;
    }

    private int numberOfMergedMs2(List<Ms2Experiment> allExperiments) {
        int numberOfMergedMs2 = 0;
        for (Ms2Experiment experiment : allExperiments) {
            if (experiment.hasAnnotation(MergedMs2Spectrum.class)) ++numberOfMergedMs2;
        }
        return numberOfMergedMs2;
    }

    private MergedMs2Spectrum mergeMs2Spectra(Ms2Experiment experiment, Deviation deviation){
        //todo best to merge spectra?
//        return new MergedMs2Spectrum(Spectrums.mergeSpectra(deviation, true, true, experiment.getMs2Spectra()));
        //changed because we are now using Gaussian cosine scoring
        //important to set precursor mass
        return new MergedMs2Spectrum(Spectrums.mergeSpectra(experiment.getMs2Spectra()), experiment.getIonMass(), null, 0);
    }

    private Ms2Experiment mergeExperiments(List<Ms2Experiment> experiments, Deviation deviation){
        MutableMs2Experiment merged = new MutableMs2Experiment(experiments.get(0));
        double meanMz = experiments.stream().mapToDouble(Ms2Experiment::getIonMass).average().getAsDouble();
        merged.setIonMass(meanMz);
        PrecursorIonType ionType = experiments.get(0).getPrecursorIonType();
        String filePaths = experiments.get(0).getSource().toString();
        for (int i = 1; i < experiments.size(); i++) {
             Ms2Experiment experiment = experiments.get(i);
             merged.addAnnotationsFrom(experiment); //todo merge annotations in a better way
             merged.getMs2Spectra().addAll(experiment.getMs2Spectra());
             merged.getMs1Spectra().addAll(experiment.getMs1Spectra());
             if (ionType==null || ionType.isIonizationUnknown()){
                 ionType = experiment.getPrecursorIonType();
             } else if (!experiment.getPrecursorIonType().isIonizationUnknown() && !ionType.equals(experiment.getPrecursorIonType())){
                 throw new RuntimeException("Cannot merge compounds: PrecursorIonTypes differ.");
             }
             filePaths += experiment.getSource().toString();
             if (i==experiments.size()) filePaths += ";";
        }
        merged.setMergedMs1Spectrum(mergeMergedMs1(experiments, meanMz, ionType.getCharge(), deviation));
        merged.setPrecursorIonType(ionType);
        //todo set name and sources
        //hack to write multiple sources into output file
        try {
            merged.setSource(new URL(filePaths));
        } catch (MalformedURLException e) {
            LoggerFactory.getLogger(Ms2CompoundMerger.class).warn("Could not set source paths");
        }


        return merged;
    }


    private SimpleSpectrum mergeMergedMs1(List<Ms2Experiment> experiments, double precursorMass, int charge, Deviation deviation){
        List<SimpleMutableSpectrum> spectra = new ArrayList<>();
        for (Ms2Experiment experiment : experiments) {
            SimpleSpectrum mergedMs1 = experiment.getMergedMs1Spectrum();
            if (mergedMs1!=null && mergedMs1.size()>0){
                SimpleMutableSpectrum zerosRemoved = removeZeroIntensityPeaks(mergedMs1);
                if (zerosRemoved.size()>0){
                    spectra.add(zerosRemoved);
                }
            }
        }

        boolean hasMergedMs1 = spectra.size()>0;
        //rather fix this at a different stage
//        if (!hasMergedMs1){
//            //use normal ms1 if no merged MS1 is available; //todo good idea?
//            for (Ms2Experiment experiment : experiments) {
//                List<SimpleSpectrum> ms1Spectra = experiment.getMs1Spectra();
//                for (SimpleSpectrum spectrum : ms1Spectra) {
//                    if (spectrum!=null && spectrum.size()>0){
//                        SimpleMutableSpectrum zerosRemoved = removeZeroIntensityPeaks(spectrum);
//                        if (zerosRemoved.size()>0){
//                            spectra.add(zerosRemoved);
//                        }
//                    }
//                }
//            }
//
//            if (spectra.size()<=1) return null; //nothing to merge
//        }

        if (spectra.size()<1){
            return null;
        }
        if (spectra.size()==1) return new SimpleSpectrum(spectra.get(0));


        //get spectra which have the longest isotope patterns

        spectra = extractMS1sWithMaxNumberOfIsotopePeaks(spectra, precursorMass, charge, findIsotopesMeasurementProfile);

        if (spectra.size()==1) return new SimpleSpectrum(spectra.get(0));


        if (TAKE_MOST_INTENSE_ISOTOPE_PATTERN){
            double highestIntensity = 0d;
            SimpleMutableSpectrum bestSpec = null;
            for (SimpleMutableSpectrum spectrum : spectra) {
                int peakIdx = Spectrums.mostIntensivePeakWithin(spectrum, precursorMass, deviation);

                if (peakIdx<0 || spectrum.getIntensityAt(peakIdx)<=0) continue;
                double intensity = spectrum.getIntensityAt(peakIdx);
                if (intensity>highestIntensity){
                    highestIntensity = intensity;
                    bestSpec = spectrum;
                }
            }
            return new SimpleSpectrum(bestSpec);
        }


        //todo first extract spectra with longest isotope patterns
        if (SIMPLY_SUM_INTENSITIES){
            //don't normalize intensities. Idea: high intensities are reliable
            return Spectrums.mergeSpectra(deviation, true, true, spectra);
        }


        List<SimpleMutableSpectrum> mutableSpectra = spectra;//spectra.stream().map(s->new SimpleMutableSpectrum(s)).collect(Collectors.toList());

        TDoubleArrayList precursorIntensities = new TDoubleArrayList();
        for (SimpleMutableSpectrum mutableSpectrum : mutableSpectra) {
            int peakIdx = Spectrums.mostIntensivePeakWithin(mutableSpectrum, precursorMass, deviation);
            if (peakIdx < 0 || mutableSpectrum.getIntensityAt(peakIdx) <= 0) continue;
            precursorIntensities.add(mutableSpectrum.getIntensityAt(peakIdx));
        }
        if (precursorIntensities.size()>1 && precursorIntensities.max()/precursorIntensities.min()>10d){
            //don't normalize intensities. Idea: high intensities are reliable
            LoggerFactory.getLogger(Ms2CompoundMerger.class).info("Sum isotope intensities for "+experiments.stream().map(Ms2Experiment::getName).collect(Collectors.joining(",")));
            return Spectrums.mergeSpectra(deviation, true, true, spectra);
        }



        Iterator<SimpleMutableSpectrum> iterator = mutableSpectra.iterator();
        while (iterator.hasNext()) {
            SimpleMutableSpectrum next = iterator.next();
            int peakIdx = Spectrums.mostIntensivePeakWithin(next, precursorMass, deviation);

            if (peakIdx<0 || next.getIntensityAt(peakIdx)<=0) iterator.remove();
            else Spectrums.normalizeByPeak(next, peakIdx, 1d);
        }


        if (mutableSpectra.size()==0){
            return null;
        } else if (mutableSpectra.size()==1){
            if (hasMergedMs1) return new SimpleSpectrum(mutableSpectra.get(0));
            else return null;
        }


        //use consensus spec to find medians as robust measure
        Spectrum<Peak> mergedMs1 = Spectrums.mergeSpectra(deviation, true, true, mutableSpectra);
        mergedMs1 = Spectrums.getMassOrderedSpectrum(mergedMs1);
        TDoubleArrayList[] intensities = new TDoubleArrayList[mergedMs1.size()];
        TDoubleArrayList[] masses = new TDoubleArrayList[mergedMs1.size()];
        for (int i = 0; i < intensities.length; i++) {
            intensities[i] = new TDoubleArrayList();
            masses[i] = new TDoubleArrayList();
        }

        Deviation findPeakDeviation = deviation.multiply(2);//todo necessary?
        for (SimpleMutableSpectrum mutableSpectrum : mutableSpectra) {
            for (Peak peak : mutableSpectrum) {
                if (peak.getIntensity()==0) continue;
                int peakIdx = Spectrums.binarySearch(mergedMs1, peak.getMass(), findPeakDeviation);
                masses[peakIdx].add(peak.getMass());
                intensities[peakIdx].add(peak.getIntensity());
            }
        }


        SimpleMutableSpectrum robustEstimateSpec = new SimpleMutableSpectrum();
        for (int i = 0; i < masses.length; i++) {
            TDoubleArrayList m = masses[i];
            TDoubleArrayList ints = intensities[i];

            //assume that every spectrum contributes to each merged peak. If not present, assume intensity 0.
            //to not run into index problems....
            int l = mutableSpectra.size();
            int numberOfAddedZeros = Math.min(l-m.size(), 2);
            for (int j = 0; j < numberOfAddedZeros; j++) {
                m.add(0d);
                ints.add(0d);
            }
            m.sort();
            ints.sort();

            int relPos = Math.max(l/2-(l-m.size()),1);
            double mass = l%2==1?m.get(relPos):(m.get(relPos)+m.get(relPos-1))/2;
            double intensity = l%2==1?ints.get(relPos):(ints.get(relPos)+ints.get(relPos-1))/2;
            if (intensity==0d) continue;
            robustEstimateSpec.addPeak(mass, intensity);
        }

        return new SimpleSpectrum(robustEstimateSpec);
    }


    private SimpleMutableSpectrum removeZeroIntensityPeaks(Spectrum<Peak> spectrum){
        SimpleMutableSpectrum mutableSpectrum = new SimpleMutableSpectrum();
        for (Peak peak : spectrum) {
            if (peak.getIntensity()>0d) mutableSpectrum.addPeak(peak);
        }
        return mutableSpectrum;
    }

    private <S extends Spectrum<Peak>> List<S> extractMS1sWithMaxNumberOfIsotopePeaks(List<S> spectra, double ionMass, int charge, MeasurementProfile measurementProfile) {
        int absCharge = Math.abs(charge);

        int maxNumberIsotopes = 0;
        List<S> bestSpectra = null;
        for (S spectrum : spectra) {
            Spectrum<Peak> iso = Spectrums.extractIsotopePattern(spectrum, measurementProfile, ionMass, absCharge, true);
            if (iso.size()>maxNumberIsotopes){
                maxNumberIsotopes = iso.size();
                bestSpectra = new ArrayList<>();
                bestSpectra.add(spectrum);
            } else if (iso.size()==maxNumberIsotopes){
                bestSpectra.add(spectrum);
            }
        }


        return bestSpectra;
    }


    protected class SqrtIntensityTransformation implements Spectrums.Transformation<Peak, Peak> {
        @Override
        public Peak transform(Peak input) {
            final double intensity = input.getIntensity()<=0?0:Math.sqrt(input.getIntensity());
            return new Peak(input.getMass(), intensity);
        }
    }
}
