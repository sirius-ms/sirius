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

package de.unijena.bioinf.lcms;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.math.RealDistribution;
import de.unijena.bioinf.ChemistryBase.ms.IsolationWindow;
import de.unijena.bioinf.ChemistryBase.ms.NoiseInformation;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.lcms.noise.Ms2NoiseStatistics;
import de.unijena.bioinf.lcms.noise.NoiseModel;
import de.unijena.bioinf.lcms.quality.Quality;
import de.unijena.bioinf.lcms.quality.QualityAnnotation;
import de.unijena.bioinf.model.lcms.ChromatogramCache;
import de.unijena.bioinf.model.lcms.FragmentedIon;
import de.unijena.bioinf.model.lcms.LCMSRun;
import de.unijena.bioinf.model.lcms.Scan;
import de.unijena.bioinf.ms.annotations.Annotated;
import de.unijena.bioinf.ms.annotations.DataAnnotation;
import gnu.trove.list.array.TDoubleArrayList;
import org.apache.commons.lang3.Range;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.function.Identity;
import org.slf4j.LoggerFactory;

import java.util.*;

public class ProcessedSample implements Annotated<DataAnnotation> {

    public final LCMSRun run;
    public NoiseModel ms1NoiseModel, ms2NoiseModel;

    // ms2 noise model
    public NoiseInformation ms2NoiseInformation;

    public final ChromatogramCache chromatogramCache;
    public final SpectrumStorage storage;
    public final ChromatogramBuilder builder;
    protected double meanPeakWidthToHeightRatioStd;

    protected double meanPeakWidth, meanPeakWidthToHeightRatio;

    protected final Annotations<DataAnnotation> annotations;
    protected UnivariateFunction recalibrationFunction;
    protected double maxRT;
    public final ArrayList<FragmentedIon> ions;
    public final ArrayList<FragmentedIon> gapFilledIons;
    public final ArrayList<FragmentedIon> otherIons; // any ions used for alignment solely

    // can be used for multiple charge detection
    protected RealDistribution intensityAfterPrecursorDistribution;

    //the runs window to use (may be estimated), if an MS2 scan does not specify a window
    protected IsolationWindow defaultMs2IsolationWindow;

    ProcessedSample(LCMSRun run, NoiseModel ms1NoiseModel, Ms2NoiseStatistics ms2NoiseModel, ChromatogramCache chromatogramCache, SpectrumStorage storage) {
        this.run = run;
        this.ms1NoiseModel = ms1NoiseModel;
        this.ms2NoiseInformation = ms2NoiseModel.done();
        this.ms2NoiseModel = ms2NoiseModel.getGlobalNoiseModel();
        this.chromatogramCache = chromatogramCache;
        this.storage = storage;
        this.builder = new ChromatogramBuilder(this);
        this.ions = new ArrayList<>();
        this.maxRT = run.getScans().stream().max(Comparator.comparingLong(Scan::getRetentionTime)).map(x->x.getRetentionTime()).orElse(1l);
        this.recalibrationFunction = new Identity();
        this.annotations = new Annotations<>();
        this.gapFilledIons = new ArrayList<>();
        this.otherIons = new ArrayList<>();
    }

    public void setMs2NoiseModel(Ms2NoiseStatistics stats) {
        this.ms2NoiseInformation = stats.done();
        this.ms2NoiseModel = stats.getGlobalNoiseModel();
    }
    public void setMs2NoiseModel(NoiseModel model, NoiseInformation add) {
        this.ms2NoiseInformation = add;
        this.ms2NoiseModel = model;
    }

    public RealDistribution getIntensityAfterPrecursorDistribution() {
        return intensityAfterPrecursorDistribution;
    }

    public void setIntensityAfterPrecursorDistribution(RealDistribution intensityAfterPrecursorDistribution) {
        this.intensityAfterPrecursorDistribution = intensityAfterPrecursorDistribution;
    }

    public IsolationWindow getMs2IsolationWindowOrLearnDefault(Scan scan, LCMSProccessingInstance instance) {
        if (!scan.getPrecursor().getIsolationWindow().isUndefined()) {
            return scan.getPrecursor().getIsolationWindow();
        }
        if (defaultMs2IsolationWindow == null) {
            LoggerFactory.getLogger(ProcessedSample.class).warn("No isolation window defined. We have to estimate it from data.");
            defaultMs2IsolationWindow = learnIsolationWindow(instance, this);
            LoggerFactory.getLogger(Ms2CosineSegmenter.class).warn("Estimate isolation window: " + defaultMs2IsolationWindow);
        }
        return defaultMs2IsolationWindow;
    }

    IsolationWindow learnIsolationWindow(LCMSProccessingInstance instance, ProcessedSample sample) {
        final TDoubleArrayList leftWidths = new TDoubleArrayList();
        final TDoubleArrayList rightWidths = new TDoubleArrayList();
        for (Scan s : sample.run.getScans()) {
            if (s.isMsMs()) {
                if (isMultipleCharged(s, sample)){
                    //multiple charged compounds will produce single-charged fragments with mass greater precursor
                    continue;
                }
                // check how many intensive peaks we see after the precursor
                final SimpleSpectrum spec = sample.storage.getScan(s);
                int K = Spectrums.getFirstPeakGreaterOrEqualThan(spec, s.getPrecursor().getMass());
                //use relatively high signal intensity as noise level, just to be sure not to use noise peaks
                double noiseLevel = sample.ms2NoiseModel.getSignalLevel(s.getIndex(), s.getPrecursor().getMass())*2d;

                detectRightBoundary(instance, rightWidths, s, spec, K, noiseLevel);
                detectLeftBoundary(instance, leftWidths, s, spec, K, noiseLevel);

            }
        }
        rightWidths.sort();
        leftWidths.sort();

        if (rightWidths.size()>3) {
            double estimatedWidthRight = rightWidths.get((int)(rightWidths.size()*0.9)) + 0.1;

            IsolationWindow window;
            if (leftWidths.size()>3){
                double estimatedWidthLeft = leftWidths.get((int)(leftWidths.size()*0.8)) + 0.1; //be more conservative in case of misidentifying losses as precursor ions
                window =  IsolationWindow.fromOffsets(estimatedWidthLeft, estimatedWidthRight);
            } else {
                if (estimatedWidthRight > 2) {
                    //larger windows are usually shifted to cover the isotope pattern, assume left side of window is 50% of right side (estimatedWidthRight)
                    double estimatedWidth = 1.5 * estimatedWidthRight;
                    double offset = estimatedWidthRight*0.25;
                    window =  new IsolationWindow(offset, estimatedWidth);
                } else {
                    window =  new IsolationWindow(0d, 2*estimatedWidthRight);
                }
            }

            if (window.getWindowWidth() > 6) {
                LoggerFactory.getLogger(Ms2CosineSegmenter.class).warn("The isolation window was estimated to be very large: " + window.getWindowWidth() + " m/z. SIRIUS does not support DIA data.");
            } else if (window.getWindowWidth() > 2) {
                LoggerFactory.getLogger(Ms2CosineSegmenter.class).warn("Larger isolation window width estimated: "+window.getWindowWidth()+" m/z. Larger windows favor chimeric compounds which will be removed from analysis.");
            }

            return window;
        } else return new IsolationWindow(0,0.5d);
    }

    private void detectRightBoundary(LCMSProccessingInstance instance, TDoubleArrayList widths, Scan s, SimpleSpectrum spec, int K, double noiseLevel) {
        double maxMass = 0d;
        for (int i = K; i < spec.size(); ++i) {
            if (spec.getIntensityAt(i)> noiseLevel) {
                final double mz = spec.getMzAt(i)- s.getPrecursor().getMass();
                if (mz>=17.5)
                    break; // we often see H2O gains...
                if (mz > 3) {
                    //will not work if target m/z is not the exact precursor m/z
                    if (!instance.getFormulaDecomposer().maybeDecomposable(mz-0.0004, mz+0.0004)) {
                        continue;
                    }
                }
                maxMass = Math.max(maxMass, mz);
            }
        }
        if (maxMass>10) {
            LoggerFactory.getLogger(getClass()).debug(")/");
        }
        if (maxMass>0.5) {
            widths.add(maxMass);
        }
    }

    private void detectLeftBoundary(LCMSProccessingInstance instance, TDoubleArrayList widths, Scan s, SimpleSpectrum spec, int K, double noiseLevel) {
        // todo include MS1 information to be more sure that signals are no fragment peaks?
        double maxMass = 0d;
        //to distinguish precursor peaks of lower m/z from fragments, we have to assess their m/z difference to peaks close to the target m/z
        List<Double> potentialPrecursorsRelativeMz = new ArrayList<>(5);
        //add target m/z as "artificial" precursor m/z
        potentialPrecursorsRelativeMz.add(0d);
        for (int i = K; i < spec.size(); i++) {
            if (spec.getIntensityAt(i)> noiseLevel) {
                final double mz = s.getPrecursor().getMass() - spec.getMzAt(i);
                if (mz>-1) {
                    //add peaks with m/z still larger than a fragment with one H-loss (if this is possible at all)
                    potentialPrecursorsRelativeMz.add(mz);
                } else {
                    break;
                }
            }
        }
        for (int i = K-1; i >= 0; --i) {
            if (spec.getIntensityAt(i)> noiseLevel) {
                final double mz = s.getPrecursor().getMass() - spec.getMzAt(i);
                if (mz>=3)
                    break; //we will see normal fragments and don't want to confuse them for precursor ions.

                if (!instance.getFormulaDecomposer().maybeDecomposable(spec.getMzAt(i)-0.002, spec.getMzAt(i)+0.002)) {
                    continue;
                }

                if (mz<1) {
                    //add peaks with m/z still larger than a fragment with one H-loss (if this is possible at all)
                    potentialPrecursorsRelativeMz.add(mz);
                }

                //todo will probably also filter isotopes if target m/z is the +1 peak
                if (mayBeHLoss(mz, 0.005, 4, potentialPrecursorsRelativeMz)){
                    continue;
                }

                maxMass = Math.max(maxMass, mz);

            }
        }
        if (maxMass>2) {
            LoggerFactory.getLogger(getClass()).debug("large left isolation window boundary. Fragment?");
        }
        if (maxMass>0.5) {
            widths.add(maxMass);
        }
    }

    /**
     *
     * @param mz
     * @param absAllowedDeviation
     * @param maxCount
     * @param potentialPrecursorsRelativeMz mz of potential precursor peaks relative to target m/z. 0.0 is the m/z that matches target m/z.
     *                                      mz=1.0 corresponds to an m/z of (target_m/z - 1.0).
     * @return
     */
    private boolean mayBeHLoss(double mz, double absAllowedDeviation, int maxCount, List<Double> potentialPrecursorsRelativeMz) {
        //using Trove double collection is not necessary hre
        for (Double precursorMz : potentialPrecursorsRelativeMz) {
            if (precursorMz == mz) continue;
            if (mayBeHLoss(mz - precursorMz, absAllowedDeviation, maxCount)){
                return true;
            }
        }
        return false;
    }
    final double hMass = MolecularFormula.parseOrThrow("H").getMass();
    private boolean mayBeHLoss(double mz, double absAllowedDeviation, int maxCount) {
        int intMass = (int)mz; //implicit H-count; works because H mass is >1 (1.007..)
        if (intMass > maxCount || intMass==0) return false;
        return (Math.abs((hMass*intMass) - mz) <= absAllowedDeviation);
    }

    /**
     * detect multiple charges solely based on peaks that are larger than the precursor. This should be safer than relying on seeing an isotope peak.
     * This method should just filter obvious multiple charged compounds that hinder isolation window estimation.
     */
    private boolean isMultipleCharged(Scan s, ProcessedSample sample) {
        final SimpleSpectrum spec = sample.storage.getScan(s);
        double noiseLevel = sample.ms2NoiseModel.getSignalLevel(s.getIndex(), s.getPrecursor().getMass())/2d;
        int numberOfLargePeaks = 0;
        for (int i = spec.size() - 1; i >= 0; i--) {
            final double mz = spec.getMzAt(i)-s.getPrecursor().getMass();
            if (mz<50){ //some arbitrary constant
                return false;
            }
            if (spec.getIntensityAt(i)>noiseLevel) {
                ++numberOfLargePeaks;
                if (numberOfLargePeaks>=3){
                    return true;
                }
            }
        }
        return false;
    }

    public NavigableMap<Integer,Scan> findScansByRT(Range<Long> rt) {
        // stupid java =/
        Scan a=null, b=null;
        for (Scan s : run.getScans()) {
            if (a==null && rt.contains(s.getRetentionTime())) {
                a = s;
            }
            if (a!=null && !rt.contains(s.getRetentionTime())) {
                b = s;
                break;
            }
        }
        if (b == null) b = run.getScanByNumber(run.scanRange().getMaximum()).get();
        return run.getScans(a.getIndex(), b.getIndex());
    }
    public NavigableMap<Integer,Scan> findScansByRecalibratedRT(Range<Double> rt) {
        // stupid java =/
        Scan a=null, b=null;
        for (Scan s : run.getScans()) {
            if (a==null && rt.contains(getRecalibratedRT(s.getRetentionTime()))) {
                a = s;
            }
            if (a!=null && !rt.contains(getRecalibratedRT(s.getRetentionTime()))) {
                b = s;
                break;
            }
        }
        if (a==null) return new TreeMap<>();
        if (b == null) b = run.getScanByNumber(run.scanRange().getMaximum()).get();
        return run.getScans(a.getIndex(), b.getIndex());
    }
/*
    public NavigableMap<Integer,Scan> findScansByNormalizedRecalibratedRT(Range<Double> rt) {
        // stupid java =/
        Scan a=null, b=null;
        for (Scan s : run.getScans()) {
            if (a==null && rt.contains(getNormalizedRecalibratedRT(s.getRetentionTime()))) {
                a = s;
            }
            if (a!=null && !rt.contains(getNormalizedRecalibratedRT(s.getRetentionTime()))) {
                b = s;
                break;
            }
        }
        if (b == null) b = run.getScanByNumber(run.scanRange().getMaximum()).get();
        if (a==null)
            return run.getScansAfter(run.scanRange().getMaximum());
        return run.getScans(a.getScanNumber(), b.getScanNumber());
    }

    public double getNormalizedRecalibratedRT(long retentionTime) {
        return recalibrationFunction.value(retentionTime / maxRT);
    }

    public double getNormalizedRT(long retentionTime) {
        return retentionTime / maxRT;
    }
    */


    public double getRecalibratedRT(long retentionTime) {
        return recalibrationFunction.value(retentionTime);
    }

    public UnivariateFunction getRecalibrationFunction() {
        return recalibrationFunction;
    }

    public void setRecalibrationFunction(UnivariateFunction recalibrationFunction) {
        this.recalibrationFunction = recalibrationFunction;
    }

    public Quality getBestQualityTerm() {
        Quality best = Quality.UNUSABLE;
        for (Class<DataAnnotation> a : annotations) {
            if (a.isAssignableFrom(QualityAnnotation.class)) {
                QualityAnnotation ano = (QualityAnnotation) getAnnotationOrThrow(a);
                if (ano.getQuality().betterThan(best))
                    best = ano.getQuality();
            }
        }
        return best;
    }
    public Quality getLowestQualityTerm() {
        Quality best = Quality.GOOD;
        for (Class<DataAnnotation> a : annotations) {
            if (a.isAssignableFrom(QualityAnnotation.class)) {
                QualityAnnotation ano = (QualityAnnotation) getAnnotationOrThrow(a);
                if (best.betterThan(ano.getQuality()))
                    best = ano.getQuality();
            }
        }
        return best;
    }

    public String toString() {
        return run.getIdentifier();
    }

    public double getMeanPeakWidth() {
        return meanPeakWidth;
    }

    public double getMeanPeakWidthToHeightRatio() {
        return meanPeakWidthToHeightRatio;
    }
    public double getMeanPeakWidthToHeightStd() {
        return meanPeakWidthToHeightRatio;
    }

    @Override
    public Annotations<DataAnnotation> annotations() {
        return annotations;
    }

}
