package de.unijena.bioinf.FragmentationTreeConstruction.computation.recalibration;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.algorithm.Parameterized;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.MutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.recal.MzRecalibration;
import gnu.trove.list.array.TIntArrayList;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.function.Identity;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

/**
 * Recommended recalibration strategy.
 */
public class MedianSlope extends AbstractRecalibrationStrategy {

    public MedianSlope() {
    }

    public MedianSlope(Deviation epsilon, int minNumberOfPeaks, double threshold) {
        super(epsilon, minNumberOfPeaks, threshold);
    }

    @Override
    public UnivariateFunction recalibrate(MutableSpectrum<Peak> spectrum, Spectrum<Peak> referenceSpectrum) {
        spectrum = new SimpleMutableSpectrum(spectrum);
        final SimpleMutableSpectrum ref = new SimpleMutableSpectrum(referenceSpectrum);

        preprocess(spectrum, ref);
        final double[] eps = new double[spectrum.size()];
        for (int k=0; k < eps.length; ++k) eps[k] = this.epsilon.absoluteFor(spectrum.getMzAt(k));
        //final double[][] values = MzRecalibration.maxIntervalStabbing(spectrum, ref, eps, threshold);
        final double[][] values = getMedianSubset(spectrum, ref);//getMedianSubsetFairDistributed(spectrum, ref);
        // getMedianSubsetFairDistributed(spectrum, ref);
        if (values[0].length<minNumberOfPeaks) return new Identity();

        //if (forceParentPeakIn) forceParentPeakInRecalibration(spectrum,referenceSpectrum,values);

        final UnivariateFunction recalibration = MzRecalibration.getMedianLinearRecalibration(values[0], values[1]);
        MzRecalibration.recalibrate(spectrum, recalibration);
        return recalibration;
    }

    public double[][] getMedianSubset(Spectrum<Peak> measured, Spectrum<Peak> reference) {
        // assuming that all peaks are correct, choose all peaks for recalibration
        final double[][] peaks = new double[2][measured.size()];
        for (int k=0; k < measured.size(); ++k) {
            peaks[0][k] = measured.getMzAt(k);
            peaks[1][k] = reference.getMzAt(k);
        }
        return peaks;
    }

    public double[][] getMedianSubsetFairDistributed(final Spectrum<Peak> measured, final Spectrum<Peak> reference) {

        // for each mass range of 100 Da choose the most intensive peaks
        final SimpleSpectrum massOrderedSpectrum = new SimpleSpectrum(measured);
        final double highestMass = massOrderedSpectrum.getMzAt(massOrderedSpectrum.size()-1);
        final ArrayList<Integer>[] chosenPeaks = new ArrayList[(int)Math.ceil(highestMass/100)];
        for (int k=0; k< chosenPeaks.length; ++k) chosenPeaks[k] = new ArrayList<Integer>();
        for (int k=0; k< massOrderedSpectrum.size(); ++k) {
            final int bin = (int)Math.floor(massOrderedSpectrum.getMzAt(k)/100);
            chosenPeaks[bin].add(k);
        }
        for (int k=0; k< chosenPeaks.length; ++k) {
            Collections.sort(chosenPeaks[k], new Comparator<Integer>() {
                @Override
                public int compare(Integer o1, Integer o2) {
                    return new Double(measured.getIntensityAt(o2)).compareTo(measured.getIntensityAt(o1));
                }
            });
        }

        while (chosenPeaks[chosenPeaks.length-1].size() < 4) {
            chosenPeaks[chosenPeaks.length-1].add(massOrderedSpectrum.size()-1);
        }

        // take median of bin size
        Arrays.sort(chosenPeaks, new Comparator<ArrayList<Integer>>() {
            @Override
            public int compare(ArrayList<Integer> o1, ArrayList<Integer> o2) {
                return o2.size() - o1.size();
            }
        });
        final int median = Math.max(5, chosenPeaks[chosenPeaks.length/2].size());
        //System.err.println(median);

        final TIntArrayList allPeaks = new TIntArrayList();
        for (ArrayList<Integer> bin :chosenPeaks)
                allPeaks.addAll(bin.subList(0, Math.min(bin.size(), median)));

        // assuming that all peaks are correct, choose all peaks for recalibration
        final double[][] peaks = new double[2][allPeaks.size()];

        for (int k=0; k < allPeaks.size(); ++k) {
            peaks[0][k] = measured.getMzAt(allPeaks.get(k));
            peaks[1][k] = reference.getMzAt(allPeaks.get(k));
        }
        return peaks;
    }

}
