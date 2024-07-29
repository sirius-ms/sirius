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

package de.unijena.bioinf.model.lcms;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.chem.RetentionTime;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.lcms.CoelutingTraceSet;
import de.unijena.bioinf.ChemistryBase.ms.lcms.LCMSPeakInformation;
import de.unijena.bioinf.ChemistryBase.ms.lcms.Trace;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.lcms.quality.Quality;
import de.unijena.bioinf.ms.annotations.Annotated;
import de.unijena.bioinf.ms.annotations.DataAnnotation;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Set;

public class Feature implements Annotated<DataAnnotation> {

    protected final LCMSRun origin;
    protected final double mz, intensity;
    protected final SimpleSpectrum[] correlatedFeatures; // isotopes of ion and all correlated ions
    protected final SimpleSpectrum isotopes; // isotopes of ion itself
    protected final SimpleSpectrum[] ms2Spectra;
    protected final PrecursorIonType ionType;
    protected final Set<PrecursorIonType> alternativeIonTypes;
    protected final UnivariateFunction rtRecalibration;
    protected Annotated.Annotations<DataAnnotation> annotations = new Annotations<>();
    protected final CollisionEnergy[] collisionEnergies;
    // quality terms
    protected final Quality peakShapeQuality, ms1Quality, ms2Quality;
    protected double chimericPollution;

    protected final CoelutingTraceSet traceset;
    protected final NoiseInformation ms2NoiseModel;

    /**
     *
     * @param origin lcms run this feature was extracted from
     * @param mz averaged m/z of the feature
     * @param intensity max apex intensity of the feature
     * @param traceset all correlated ion traces
     * @param correlatedFeatures all correlated ion features
     * @param isotope index of the isotope trace within the correlatedFeaturs array
     * @param ms2Spectra MS/MS spectra for each collision energy
     * @param noiseInformation noise statistics
     * @param collisionEnergies collision energy for each MS/MS spectrum
     * @param ionType detected precursor ion type. Might be unknown
     * @param alternativeIonTypes all possible precursor ion types
     * @param rtRecalibration recalibration function for retention time
     * @param peakShapeQuality peak shape quality estimate
     * @param ms1Quality MS1 quality estimate
     * @param ms2Quality MS2 quality estimate
     * @param chimericPollution ratio of chimeric pollution to precursor intensity
     */
    public Feature(LCMSRun origin, double mz, double intensity, CoelutingTraceSet traceset, SimpleSpectrum[] correlatedFeatures, int isotope, SimpleSpectrum[] ms2Spectra, NoiseInformation noiseInformation, CollisionEnergy[] collisionEnergies,PrecursorIonType ionType, Set<PrecursorIonType> alternativeIonTypes, UnivariateFunction rtRecalibration,Quality peakShapeQuality, Quality ms1Quality, Quality ms2Quality, double chimericPollution) {
        this.origin = origin;
        this.mz = mz;
        this.intensity = intensity;
        this.ms2NoiseModel = noiseInformation;
        this.traceset = traceset;
        this.correlatedFeatures = correlatedFeatures;
        this.isotopes = this.correlatedFeatures[isotope];
        this.ms2Spectra = ms2Spectra;
        this.ionType = ionType;
        this.rtRecalibration = rtRecalibration;
        this.peakShapeQuality = peakShapeQuality;
        this.ms1Quality = ms1Quality;
        this.ms2Quality = ms2Quality;
        this.alternativeIonTypes = alternativeIonTypes;
        this.chimericPollution = chimericPollution;
        this.collisionEnergies=collisionEnergies;
    }

    public CoelutingTraceSet getTraceset() {
        return traceset;
    }

    public Set<PrecursorIonType> getPossibleAdductTypes() {
        return alternativeIonTypes;
    }

    public Quality getPeakShapeQuality() {
        return peakShapeQuality;
    }

    public Quality getMs1Quality() {
        return ms1Quality;
    }

    public Quality getMs2Quality() {
        return ms2Quality;
    }

    public UnivariateFunction getRtRecalibration() {
        return rtRecalibration;
    }

    public LCMSRun getOrigin() {
        return origin;
    }

    public double getMz() {
        return mz;
    }

    public double getIntensity() {
        return intensity;
    }

    public ScanPoint[] getTrace() {
        final Trace trace = traceset.getIonTrace().getMonoisotopicPeak();
        ScanPoint[] scans = new ScanPoint[trace.getDetectedFeatureLength()];
        for (int k=0; k < scans.length; ++k) {
            final int absoluteIndex = trace.getIndexOffset()+trace.getDetectedFeatureOffset();
            scans[k] = new ScanPoint(
                    traceset.getScanIds()[absoluteIndex],
                    traceset.getRetentionTimes()[absoluteIndex],
                    trace.getMasses()[k+trace.getDetectedFeatureOffset()],
                    trace.getIntensities()[k+trace.getDetectedFeatureOffset()]
            );
        }
        return scans;
    }

    public SimpleSpectrum[] getCorrelatedFeatures() {
        return correlatedFeatures;
    }

    public SimpleSpectrum[] getMs2Spectra() {
        return ms2Spectra;
    }

    public CollisionEnergy[] getCollisionEnergies() {return collisionEnergies;};

    public PrecursorIonType getIonType() {
        return ionType;
    }

    public Set<PrecursorIonType> getAlternativeIonTypes() {
        return alternativeIonTypes;
    }

    @Override
    public Annotations<DataAnnotation> annotations() {
        return annotations;
    }

    public Ms2Experiment toMsExperiment(String name, @Nullable String featureId) {
        final MutableMs2Experiment exp = new MutableMs2Experiment();
        final Trace peak = traceset.getIonTrace().getMonoisotopicPeak();
        int apex = traceset.getScanIds()[peak.getAbsoluteIndexApex()];
        exp.setName(name);
        exp.setFeatureId(featureId);
        exp.setPrecursorIonType(ionType);
        exp.setMergedMs1Spectrum(Spectrums.mergeSpectra(getCorrelatedFeatures()));
        final ArrayList<MutableMs2Spectrum> ms2Spectra = new ArrayList<>();
        for (int i=0; i < this.ms2Spectra.length; ++i) {
            ms2Spectra.add(new MutableMs2Spectrum(this.ms2Spectra[i], mz, this.collisionEnergies[i], 2));
        }
        for (MutableMs2Spectrum spec : ms2Spectra) spec.setIonization(ionType.getIonization());
        exp.setMs2Spectra(ms2Spectra);
        exp.setIonMass(mz);
        exp.setAnnotation(RetentionTime.class, new RetentionTime(traceset.getRetentionTimes()[peak.absoluteIndexLeft()]/1000d, traceset.getRetentionTimes()[peak.absoluteIndexRight()]/1000d, traceset.getRetentionTimes()[peak.getAbsoluteIndexApex()]/1000d));

        boolean chimeric = chimericPollution>=0.33;

        CompoundQuality quality = new CompoundQuality();
        if (chimeric) quality = quality.updateQuality(CompoundQuality.CompoundQualityFlag.Chimeric);
        if (getMs2Quality().notBetterThan(Quality.DECENT)) quality=quality.updateQuality(CompoundQuality.CompoundQualityFlag.FewPeaks);
        if (getMs1Quality().notBetterThan(Quality.DECENT)) quality=quality.updateQuality(CompoundQuality.CompoundQualityFlag.BadIsotopePattern);
        if (getPeakShapeQuality().notBetterThan(Quality.DECENT)) quality=quality.updateQuality(CompoundQuality.CompoundQualityFlag.BadPeakShape);
        if (quality.isNotBadQuality())
            quality=quality.updateQuality(CompoundQuality.CompoundQualityFlag.Good);

        final TObjectDoubleHashMap<String> map = new TObjectDoubleHashMap<>();
        // deprecated
        //exp.setAnnotation(Quantification.class, new Quantification(Collections.singletonMap(origin.identifier, intensity)));
        exp.setAnnotation(CompoundQuality.class,quality);
        exp.setSource(new SpectrumFileSource(origin.source.getURI()));

        final Set<PrecursorIonType> ionTypes = getPossibleAdductTypes();
        exp.computeAnnotationIfAbsent(DetectedAdducts.class, DetectedAdducts::new).put(DetectedAdducts.Source.LCMS_ALIGN,new PossibleAdducts(ionTypes));

        // add trace information
        exp.setAnnotation(LCMSPeakInformation.class, new LCMSPeakInformation(new CoelutingTraceSet[]{getTraceset()}));
        // add instrument annotation
        exp.setAnnotation(MsInstrumentation.class, origin.instrument);
        return exp;
    }
}
