package de.unijena.bioinf.sirius;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.chem.RetentionTime;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.utils.clustering.CompleteLinkage;
import de.unijena.bioinf.utils.clustering.HierarchicalClustering;
import gnu.trove.list.array.TDoubleArrayList;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

/**
 * merge compounds ({@link de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment}s) between different (LC/MS/MS) runs.
 */
public class Ms2CompoundMerger {

    protected final static boolean SIMPLY_SUM_INTENSITIES = false;

    private final Deviation maxMzDeviation;
    private final double maxRetentionTimeShift;
    private final double cosineSimilarity;

    private Map<Ms2Experiment, MergedMs2Spectrum> experimentToMergedMs2;

    public Ms2CompoundMerger(Deviation maxMzDeviation, double maxRetentionTimeShift, double cosineSimilarity) {
        this.maxMzDeviation = maxMzDeviation;
        this.maxRetentionTimeShift = maxRetentionTimeShift;
        this.cosineSimilarity = cosineSimilarity;
        //todo is hashmap necessary? or just array?
        experimentToMergedMs2 = new HashMap<>();
    }


    public List<Ms2Experiment> mergeRuns(Deviation deviation, List<Ms2Experiment>... runs){
        //todo same deviation to merge and to score?
        List<Ms2Experiment> allExperimentsWithMs2 = new ArrayList<>();
        List<Ms2Experiment> allExperimentsWithoutMs2 = new ArrayList<>();
        for (List<Ms2Experiment> run : runs) {
            for (Ms2Experiment experiment : run) {
                boolean hasMs2 = hasMs2(experiment);
                if (hasMs2) allExperimentsWithMs2.add(experiment);
                else allExperimentsWithoutMs2.add(experiment);
            }
        }

        //create merged Ms2 to compare compounds
        int numberOfMergedMs2 = numberOfMergedMs2(allExperimentsWithMs2);
        if (numberOfMergedMs2>0 && numberOfMergedMs2<allExperimentsWithMs2.size()){
            LoggerFactory.getLogger(Ms2CompoundMerger.class).warn("Not all but some compounds already contain a merged Ms2 spectrum. Recomputing all.");
        }
        if (numberOfMergedMs2==allExperimentsWithMs2.size()){
            for (Ms2Experiment experiment : allExperimentsWithMs2) {
                MergedMs2Spectrum mergedMs2Spectrum = experiment.getAnnotation(MergedMs2Spectrum.class);
                experimentToMergedMs2.put(experiment, mergedMs2Spectrum);
            }

        } else {
            //compute merged
            for (Ms2Experiment experiment : allExperimentsWithMs2) {
                MergedMs2Spectrum mergedMs2Spectrum = mergeMs2Spectra(experiment, deviation);
                experimentToMergedMs2.put(experiment, mergedMs2Spectrum);
            }
        }

        //now cluster similar compounds together
        //1. create Distance Matrix
        //todo stupid, since it stores all Infinity values
        double[][] distances = new double[allExperimentsWithMs2.size()][allExperimentsWithMs2.size()];
        for (int i = 0; i < allExperimentsWithMs2.size(); i++) {
            distances[i][i] = 0d;
            Ms2Experiment exp1 = allExperimentsWithMs2.get(i);
            MergedMs2Spectrum mergedMs2Spectrum1 = experimentToMergedMs2.get(exp1);
            for (int j = i+1; j < allExperimentsWithMs2.size(); j++) {
                Ms2Experiment exp2 = allExperimentsWithMs2.get(j);
                MergedMs2Spectrum mergedMs2Spectrum2 = experimentToMergedMs2.get(exp2);
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
                double cosine = Spectrums.cosineProduct(mergedMs2Spectrum1, mergedMs2Spectrum2, deviation);
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
        return new MergedMs2Spectrum(Spectrums.mergeSpectra(deviation, true, true, experiment.getMs2Spectra()));
    }

    private Ms2Experiment mergeExperiments(List<Ms2Experiment> experiments, Deviation deviation){
        MutableMs2Experiment merged = new MutableMs2Experiment(experiments.get(0));
        double meanMz = experiments.stream().mapToDouble(Ms2Experiment::getIonMass).average().getAsDouble();
        merged.setIonMass(meanMz);
        PrecursorIonType ionType = null;
        String filePaths = "";
        for (int i = 0; i < experiments.size(); i++) {
             Ms2Experiment experiment = experiments.get(i);
             merged.addAnnotationsFrom(experiment); //todo merge annotations in a better way
             merged.getMs2Spectra().addAll(experiment.getMs2Spectra());
             merged.getMs1Spectra().addAll(experiment.getMs1Spectra());
             if (ionType==null || ionType.isIonizationUnknown()){
                 ionType = experiment.getPrecursorIonType();
             } else if (!ionType.equals(experiment.getPrecursorIonType())){
                 throw new RuntimeException("Cannot merge compounds: PrecursorIonTypes differ.");
             }
             filePaths += experiment.getSource().toString();
             if (i==experiments.size()) filePaths += ";";
        }
        merged.setMergedMs1Spectrum(mergeMergedMs1(experiments, meanMz, deviation));
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


    private SimpleSpectrum mergeMergedMs1(List<Ms2Experiment> experiments, double precursorMass, Deviation deviation){
        List<SimpleSpectrum> spectra = new ArrayList<>();
        for (Ms2Experiment experiment : experiments) {
            SimpleSpectrum mergedMs1 = experiment.getMergedMs1Spectrum();
            if (mergedMs1!=null && mergedMs1.size()>0) spectra.add(mergedMs1);
        }

        if (spectra.size()==0) return null;
        if (spectra.size()==1) return spectra.get(0);

        if (SIMPLY_SUM_INTENSITIES){
            //don't normalize intensities. Idea: high intensities are reliable
            return Spectrums.mergeSpectra(deviation, true, true, spectra);
        }

        List<SimpleMutableSpectrum> mutableSpectra = spectra.stream().map(s->new SimpleMutableSpectrum(s)).collect(Collectors.toList());
        for (SimpleMutableSpectrum mutableSpectrum : mutableSpectra) {
            int peakIdx = Spectrums.mostIntensivePeakWithin(mutableSpectrum, precursorMass, deviation);
            Spectrums.normalizeByPeak(mutableSpectrum, peakIdx, 1d);
        }
        for (SimpleSpectrum spectrum : spectra) {
//            Spectrums.normalizeToMax(spectrum, 1d); ..of monoisotopic ...
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
                int peakIdx = Spectrums.binarySearch(mergedMs1, peak.getMass(), findPeakDeviation);
                masses[peakIdx].add(peak.getMass());
                intensities[peakIdx].add(peak.getIntensity());
            }
        }


        SimpleMutableSpectrum robustEstimateSpec = new SimpleMutableSpectrum();
        for (int i = 0; i < masses.length; i++) {
            TDoubleArrayList m = masses[i];
            TDoubleArrayList ints = intensities[i];
            m.sort();
            ints.sort();
            int l = m.size();
            double mass = l%2==1?m.get(l/2):(m.get(l/2)+m.get(l/2+1))/2;
            double intensity = l%2==1?ints.get(l/2):(ints.get(l/2)+ints.get(l/2+1))/2;
            robustEstimateSpec.addPeak(mass, intensity);
        }

        return new SimpleSpectrum(robustEstimateSpec);
    }

}
