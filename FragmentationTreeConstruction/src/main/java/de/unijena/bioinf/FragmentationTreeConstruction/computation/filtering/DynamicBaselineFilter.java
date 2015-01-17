package de.unijena.bioinf.FragmentationTreeConstruction.computation.filtering;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.math.NormalDistribution;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.FragmentationTreeConstruction.model.Ms2ExperimentImpl;
import de.unijena.bioinf.FragmentationTreeConstruction.model.Ms2SpectrumImpl;

import java.util.ArrayList;


public class DynamicBaselineFilter implements Preprocessor {

    private double standardDeviation = 0.05d;
    private double threshold = 0.25d;


    @Override
    public Ms2Experiment process(Ms2Experiment experiment) {
        final ArrayList<Ms2SpectrumImpl> list = new ArrayList<Ms2SpectrumImpl>();
        final ArrayList<Ms2Spectrum<? extends Peak>> newList = new ArrayList<Ms2Spectrum<? extends Peak>>();
        for (Ms2Spectrum<? extends Peak> spec : experiment.getMs2Spectra()) {
            if (spec instanceof Ms2SpectrumImpl) list.add((Ms2SpectrumImpl) spec);
            else list.add(new Ms2SpectrumImpl(spec));
        }
        final NormalDistribution dist = new NormalDistribution(0d, standardDeviation*standardDeviation);
        final double max = dist.getDensity(0d);
        for (Ms2SpectrumImpl spec : list) {
            final boolean[] remove = new boolean[spec.size()];
            int counter = 0;
            counter = cleanSpectrum(dist, max, spec, remove, counter);
            final SimpleMutableSpectrum newSpec = new SimpleMutableSpectrum(spec.size()-counter);
            for (int i=0; i < remove.length; ++i) {
                if (!remove[i]) newSpec.addPeak(spec.getMzAt(i), spec.getIntensityAt(i));
            }
            if (counter > 0) {
                newList.add(new Ms2SpectrumImpl(newSpec, spec.getCollisionEnergy(), spec.getPrecursorMz(), spec.getTotalIonCount()));
            } else newList.add(spec);
        }
        final Ms2ExperimentImpl exp = new Ms2ExperimentImpl(experiment);
        exp.setMs2Spectra(newList);
        return exp;
    }

    private int cleanSpectrum(NormalDistribution dist, double max, Ms2SpectrumImpl spec, boolean[] remove, int counter) {
        max/=threshold;
        for (int k=0; k < spec.size(); ++k) {
            final double intensity = spec.getIntensityAt(k);
            final double mz = spec.getMzAt(k);
            final double massLimit = 10*standardDeviation;
            final double a = mz-massLimit;
            final double b = mz+massLimit;
            for (int i = k-1; i >= 0 && spec.getMzAt(i) >= a; --i) {
                if (remove[i]) continue;
                final double limit = (dist.getDensity((mz-spec.getMzAt(i)))/max)*intensity;
                if (spec.getIntensityAt(i) < limit) {
                    remove[i] = true;
                    //System.err.println(String.format(Locale.ENGLISH, "remove %.4f (%.4f %%) due to %.4f (%.4f %%)", spec.getMzAt(i), spec.getIntensityAt(i), spec.getMzAt(k), spec.getIntensityAt(k)));
                    ++counter;
                }
            }
            for (int i = k+1; i < spec.size() && spec.getMzAt(i) <= b; ++i) {
                if (remove[i]) continue;
                final double limit = (dist.getDensity((mz-spec.getMzAt(i)))/max)*intensity;
                if (spec.getIntensityAt(i) < limit) {
                    remove[i] = true;
                    ++counter;
                    //System.err.println(String.format(Locale.ENGLISH, "remove %.4f (%.4f %%) due to %.4f (%.4f %%)", spec.getMzAt(i), spec.getIntensityAt(i), spec.getMzAt(k), spec.getIntensityAt(k)));
                }
            }
        }
        //System.out.println(counter + " (" + ((double)counter/spec.size()) + " %)");
        return counter;
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        this.standardDeviation = document.getDoubleFromDictionary(dictionary, "standardDeviation");
        this.threshold = document.getDoubleFromDictionary(dictionary, "threshold");
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        document.addToDictionary(dictionary, "standardDeviation", standardDeviation);
        document.addToDictionary(dictionary, "threshold", threshold);
    }
}
