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
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.lcms.LCMSProccessingInstance;
import de.unijena.bioinf.lcms.quality.Quality;
import de.unijena.bioinf.ms.annotations.Annotated;
import de.unijena.bioinf.ms.annotations.DataAnnotation;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import org.apache.commons.math3.analysis.UnivariateFunction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

public class Feature implements Annotated<DataAnnotation> {

    protected final LCMSRun origin;
    protected final double mz, intensity;
    protected final ScanPoint[] trace;
    protected final SimpleSpectrum[] correlatedFeatures; // isotopes of ion and all correlated ions
    protected final SimpleSpectrum isotopes; // isotopes of ion itself
    protected final SimpleSpectrum[] ms2Spectra;
    protected final PrecursorIonType ionType;
    protected final Set<PrecursorIonType> alternativeIonTypes;
    protected final UnivariateFunction rtRecalibration;
    protected Annotated.Annotations<DataAnnotation> annotations = new Annotations<>();
    protected final CollisionEnergy collisionEnergy;
    // quality terms
    protected final Quality peakShapeQuality, ms1Quality, ms2Quality;
    protected double chimericPollution;

    // debug
    public ScanPoint[] completeTraceDebug;

    public Feature(LCMSRun origin, double mz, double intensity, ScanPoint[] trace, SimpleSpectrum[] correlatedFeatures, int isotope, SimpleSpectrum[] ms2Spectra, CollisionEnergy collisionEnergy,PrecursorIonType ionType, Set<PrecursorIonType> alternativeIonTypes, UnivariateFunction rtRecalibration,Quality peakShapeQuality, Quality ms1Quality, Quality ms2Quality, double chimericPollution) {
        this.origin = origin;
        this.mz = mz;
        this.intensity = intensity;
        this.trace = trace;
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
        this.collisionEnergy=collisionEnergy;
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
        return trace;
    }

    public SimpleSpectrum[] getCorrelatedFeatures() {
        return correlatedFeatures;
    }

    public SimpleSpectrum[] getMs2Spectra() {
        return ms2Spectra;
    }

    public CollisionEnergy getCollisionEnergy() {return collisionEnergy;};

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

    public Ms2Experiment toMsExperiment() {
        final MutableMs2Experiment exp = new MutableMs2Experiment();
        int apex = 0;
        for (int k=0; k < trace.length; ++k) {
            if (trace[k].getIntensity()>trace[apex].getIntensity())
                apex = k;
        }
        exp.setName(String.valueOf(trace[apex].getScanNumber()));
        exp.setPrecursorIonType(ionType);
        exp.setMergedMs1Spectrum(Spectrums.mergeSpectra(getCorrelatedFeatures()));
        final ArrayList<MutableMs2Spectrum> ms2Spectra = new ArrayList<>();
        for (SimpleSpectrum s : getMs2Spectra()) {
            ms2Spectra.add(new MutableMs2Spectrum(s, mz, this.collisionEnergy, 2));
        }
        exp.setMs2Spectra(ms2Spectra);
        exp.setIonMass(mz);
        exp.setAnnotation(RetentionTime.class, new RetentionTime(trace[0].getRetentionTime()/1000d, trace[trace.length-1].getRetentionTime()/1000d, trace[apex].getRetentionTime()/1000d));

        boolean chimeric = chimericPollution>=0.33;

        CompoundQuality quality = new CompoundQuality();
        if (chimeric) quality = quality.updateQuality(CompoundQuality.CompoundQualityFlag.Chimeric);
        if (getMs2Quality().notBetterThan(Quality.DECENT)) quality=quality.updateQuality(CompoundQuality.CompoundQualityFlag.FewPeaks);
        if (getMs1Quality().notBetterThan(Quality.DECENT)) quality=quality.updateQuality(CompoundQuality.CompoundQualityFlag.BadIsotopePattern);
        if (getPeakShapeQuality().notBetterThan(Quality.DECENT)) quality=quality.updateQuality(CompoundQuality.CompoundQualityFlag.BadPeakShape);
        if (quality.isNotBadQuality())
            quality=quality.updateQuality(CompoundQuality.CompoundQualityFlag.Good);

        final TObjectDoubleHashMap<String> map = new TObjectDoubleHashMap<>();
        exp.setAnnotation(Quantification.class, new Quantification(Collections.singletonMap(origin.identifier, intensity)));
        exp.setAnnotation(CompoundQuality.class,quality);
        exp.setSource(new SpectrumFileSource(origin.source.getUrl()));

        final Set<PrecursorIonType> ionTypes = getPossibleAdductTypes();
        exp.computeAnnotationIfAbsent(DetectedAdducts.class, DetectedAdducts::new).put(LCMSProccessingInstance.POSSIBLE_ADDUCTS_KEY,new PossibleAdducts(ionTypes));

        return exp;
    }


}
