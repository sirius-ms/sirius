package de.unijena.bioinf.sirius;

import de.unijena.bioinf.ChemistryBase.chem.RetentionTime;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.sirius.clustering.CompleteLinkage;
import de.unijena.bioinf.sirius.clustering.HierarchicalClustering;
import org.slf4j.LoggerFactory;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * merge compounds ({@link de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment}s) between different (LC/MS/MS) runs.
 */
public class Ms2CompoundMerger {

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
        List<Ms2Experiment> allExperiments = new ArrayList<>();
        for (List<Ms2Experiment> run : runs) {
            allExperiments.addAll(run);
        }

        //create merged Ms2 to compare compounds
        int numberOfMergedMs2 = numberOfMergedMs2(allExperiments);
        if (numberOfMergedMs2>0 && numberOfMergedMs2<allExperiments.size()){
            LoggerFactory.getLogger(Ms2CompoundMerger.class).warn("Not all but some compounds already contain a merged Ms2 spectrum. Recomputing all.");
        }
        if (numberOfMergedMs2==0){
            for (Ms2Experiment experiment : allExperiments) {
                MergedMs2Spectrum mergedMs2Spectrum = experiment.getAnnotation(MergedMs2Spectrum.class);
                experimentToMergedMs2.put(experiment, mergedMs2Spectrum);
            }

        } else {
            //compute merged
            for (Ms2Experiment experiment : allExperiments) {
                MergedMs2Spectrum mergedMs2Spectrum = mergeMs2Spectra(experiment, deviation);
                experimentToMergedMs2.put(experiment, mergedMs2Spectrum);
            }
        }

        //now cluster similar compounds together
        //1. create Distance Matrix
        //todo stupid, since it stores all Infinity values
        double[][] distances = new double[allExperiments.size()][allExperiments.size()];
        for (int i = 0; i < allExperiments.size(); i++) {
            distances[i][i] = 0d;
            Ms2Experiment exp1 = allExperiments.get(i);
            MergedMs2Spectrum mergedMs2Spectrum1 = experimentToMergedMs2.get(exp1);
            for (int j = i+1; j < allExperiments.size(); j++) {
                Ms2Experiment exp2 = allExperiments.get(j);
                MergedMs2Spectrum mergedMs2Spectrum2 = experimentToMergedMs2.get(exp2);
                if (!maxMzDeviation.inErrorWindow(exp1.getIonMass(), exp2.getIonMass())){
                    distances[i][j] = Double.POSITIVE_INFINITY;
                    continue;
                }
                if (exp1.hasAnnotation(RetentionTime.class) && exp2.hasAnnotation(RetentionTime.class)){
                    RetentionTime time1 = exp1.getAnnotation(RetentionTime.class);
                    RetentionTime time2 = exp2.getAnnotation(RetentionTime.class);
                    if (Math.abs(time1.getMiddleTime()-time2.getMiddleTime())>maxRetentionTimeShift){
                        distances[i][j] = Double.POSITIVE_INFINITY;
                        continue;
                    }
                }
                double cosine = Spectrums.dotProductPeaks(mergedMs2Spectrum1, mergedMs2Spectrum2, deviation);
                assert cosine<=1d;
                if (cosine<cosineSimilarity){
                    distances[i][j] = Double.POSITIVE_INFINITY;
                } else {
                    distances[i][j] = 1d-cosine;
                }

            }

        }

        //2. cluster
        HierarchicalClustering<Ms2Experiment> clustering = new HierarchicalClustering<>(new CompleteLinkage());
        clustering.cluster(allExperiments.toArray(new Ms2Experiment[0]), distances);

        List<List<Ms2Experiment>> clusters = clustering.getClusters();


        //....

        return null;
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

    private Ms2Experiment mergeExperiments(List<Ms2Experiment> experiments){
        //Todo merge isotopes in a robust way ....
        ///todo merge annotations
        MutableMs2Experiment merged = new MutableMs2Experiment(experiments.get(0));
        for (int i = 0; i < experiments.size(); i++) {
             Ms2Experiment experiment = experiments.get(i);
             merged.addAnnotationsFrom(experiment);
             merged.getMs2Spectra().addAll(experiment.getMs2Spectra());
             merged.getMs1Spectra().addAll(experiment.getMs1Spectra());
        }

        double meanMz = experiments.stream().mapToDouble(Ms2Experiment::getIonMass).average().getAsDouble();
        merged.setIonMass(meanMz);


        //....

        return null;
    }

    private SimpleSpectrum mergeMergedMs1(List<Ms2Experiment> experiments){
        if (true) throw new NotImplementedException();
        List<SimpleSpectrum> spectra = new ArrayList<>();
        for (Ms2Experiment experiment : experiments) {
            SimpleSpectrum mergedMs1 = experiment.getMergedMs1Spectrum();
            if (mergedMs1!=null && mergedMs1.size()>0) spectra.add(mergedMs1);
        }

        if (spectra.size()==0) return null;
        if (spectra.size()==1) return spectra.get(0);

        List<SimpleMutableSpectrum> mutableSpectra = spectra.stream().map(s->new SimpleMutableSpectrum(s)).collect(Collectors.toList());
        for (SimpleMutableSpectrum mutableSpectrum : mutableSpectra) {
            double maxInt = Spectrums.getMaximalIntensity(mutableSpectrum);
//            Spectrums.mostIntensivePeakWithin(mutableSpectrum, ....)
        }
        for (SimpleSpectrum spectrum : spectra) {
//            Spectrums.normalizeToMax(spectrum, 1d); ..of monoisotopic ...
        }


        //.....
        return null;
    }

}
