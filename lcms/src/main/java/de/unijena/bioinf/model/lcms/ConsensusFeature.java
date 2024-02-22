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
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.lcms.quality.Quality;
import de.unijena.bioinf.ms.annotations.Annotated;
import de.unijena.bioinf.ms.annotations.DataAnnotation;
import gnu.trove.map.hash.TObjectDoubleHashMap;

import java.util.*;

public class ConsensusFeature implements Annotated<DataAnnotation> {

    protected final int featureId;
    protected final Feature[] features;
    // if MS/MS is given, this is the ID of the feature with the best MS/MS scan
    protected final int ms2RepresentativeFeature;
    protected final SimpleSpectrum[] coelutedPeaks;
    protected final SimpleSpectrum[] ms2;
    protected final long averageRetentionTime;
    protected final CollisionEnergy[] collisionEnergies;
    protected final double averageMass, totalIntensity;
    protected final PrecursorIonType ionType;
    protected final double chimericPollution;
    protected Annotated.Annotations<DataAnnotation> annotations = new Annotated.Annotations<>();

    protected ArrayList<IonConnection<ConsensusFeature>> connections = new ArrayList<>();

    public ConsensusFeature(int featureId, Feature[] features, int ms2RepresentativeFeature, SimpleSpectrum[] coelutedPeaks, SimpleSpectrum[] ms2, PrecursorIonType ionType,  long averageRetentionTime,CollisionEnergy[] collisionEnergies ,double averageMass, double totalIntensity, double chimericPollution) {
        this.featureId = featureId;
        this.features = features;
        this.ms2RepresentativeFeature = ms2RepresentativeFeature;
        this.coelutedPeaks = coelutedPeaks;
        this.ms2 = ms2;
        this.averageRetentionTime = averageRetentionTime;
        this.collisionEnergies=collisionEnergies;
        this.averageMass = averageMass;
        this.totalIntensity = totalIntensity;
        this.ionType = ionType;
        this.chimericPollution = chimericPollution;
    }

    public List<IonConnection<ConsensusFeature>> getConnections() {
        return connections;
    }

    public void addConnection(ConsensusFeature feature, IonConnection.ConnectionType type, float weight) {
        connections.add(new IonConnection<ConsensusFeature>(this, feature, weight,  type));
    }

    public LCMSPeakInformation getLCMSPeakInformation() {
        return new LCMSPeakInformation(Arrays.stream(features).map(f->f.traceset).toArray(CoelutingTraceSet[]::new));
    }

    private SimpleSpectrum getIsotopes() {
        for (SimpleSpectrum spec : coelutedPeaks) {
            if (Math.abs(spec.getMzAt(0)-averageMass)<0.1) {
                return spec;
            }
        }
        return Spectrums.empty();
    }

    public int getFeatureId() {
        return featureId;
    }

    public Feature[] getFeatures() {
        return features;
    }

    public SimpleSpectrum[] getCoelutedPeaks() {
        return coelutedPeaks;
    }

    public SimpleSpectrum[] getMs2() {
        return ms2;
    }

    public long getAverageRetentionTime() {
        return averageRetentionTime;
    }

    public double getAverageMass() {
        return averageMass;
    }

    public double getTotalIntensity() {
        return totalIntensity;
    }

    public PrecursorIonType getIonType() {
        return ionType;
    }

    public Set<PrecursorIonType> getPossibleAdductTypes() {
        final HashSet<PrecursorIonType> possibleIonTypes = new HashSet<>();
        for (Feature f : features) {
            possibleIonTypes.addAll(f.alternativeIonTypes);
        }
        return possibleIonTypes;
    }

    public Ms2Experiment toMs2Experiment() {

        final MutableMs2Experiment exp = new MutableMs2Experiment();
        exp.setName(String.valueOf(featureId));
        exp.setFeatureId(String.valueOf(featureId));
        exp.setPrecursorIonType(ionType);
        exp.setMergedMs1Spectrum(Spectrums.mergeSpectra(coelutedPeaks));
        final ArrayList<MutableMs2Spectrum> ms2Spectra = new ArrayList<>();
        for (int k=0; k < ms2.length; ++k) {
            ms2Spectra.add(new MutableMs2Spectrum(this.ms2[k], averageMass, collisionEnergies[k], 2));
        }
        exp.setMs2Spectra(ms2Spectra);
        exp.setIonMass(averageMass);
        exp.setAnnotation(RetentionTime.class, new RetentionTime(averageRetentionTime/1000d));

        /*
        if (ms2RepresentativeFeature>=0) {
            exp.setAnnotation(NoiseInformation.class, features[ms2RepresentativeFeature].ms2NoiseModel);
        }
         */

        final TObjectDoubleHashMap<String> map = new TObjectDoubleHashMap<>();
        boolean good = false;
        boolean goodMs1 = false, goodMs2 = false, goodPeakShape = false;
        for (Feature f : features) {
            map.put(f.origin.identifier, f.intensity);
            if (f.ms2Quality.betterThan(Quality.DECENT) && (f.ms1Quality.betterThan(Quality.DECENT) || f.peakShapeQuality.betterThan(Quality.DECENT)) && (f.ms1Quality.betterThan(Quality.BAD) && f.peakShapeQuality.betterThan(Quality.BAD))) {
                good = true;
            }
            if (f.ms2Quality.betterThan(Quality.DECENT))
                goodMs2 = true;
            if (f.ms1Quality.betterThan(Quality.DECENT))
                goodMs1 = true;
            if (f.peakShapeQuality.betterThan(Quality.DECENT) && (f.ms2Quality.betterThan(Quality.DECENT) || f.ms1Quality.betterThan(Quality.DECENT)))
                goodPeakShape = true;
        }
        final boolean chimeric = chimericPollution>0.33;

        // deprecated
        //exp.setAnnotation(Quantification.class, new Quantification(map));

        CompoundQuality quality = new CompoundQuality();
        if (!chimeric && good) quality = quality.updateQuality(CompoundQuality.CompoundQualityFlag.Good);

        if (!goodMs1) quality = quality.updateQuality(CompoundQuality.CompoundQualityFlag.BadIsotopePattern);
        if (!goodMs2) quality = quality.updateQuality(CompoundQuality.CompoundQualityFlag.FewPeaks);
        if (!goodPeakShape) quality = quality.updateQuality(CompoundQuality.CompoundQualityFlag.BadPeakShape);
        if (chimeric)
            quality = quality.updateQuality(CompoundQuality.CompoundQualityFlag.Chimeric);

        exp.setAnnotation(CompoundQuality.class, quality);

        final Set<PrecursorIonType> ionTypes = getPossibleAdductTypes();
//        if (!ionTypes.isEmpty())
            exp.computeAnnotationIfAbsent(DetectedAdducts.class, DetectedAdducts::new).put(DetectedAdducts.Source.LCMS_ALIGN, new PossibleAdducts(ionTypes));

        return exp;
    }

    @Override
    public Annotations<DataAnnotation> annotations() {
        return annotations;
    }
}
