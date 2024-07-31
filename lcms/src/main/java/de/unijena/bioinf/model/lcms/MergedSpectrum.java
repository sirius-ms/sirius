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

import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.*;
import de.unijena.bioinf.lcms.quality.Quality;
import org.apache.commons.lang3.Range;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class MergedSpectrum extends PeaklistSpectrum<MergedPeak> implements OrderedSpectrum<MergedPeak> {

    protected Precursor precursor;
    protected List<Scan> scans;
    protected double noiseLevel;
    protected double dotProduct, cosine;
    protected double tic = Double.NaN;
    protected CollisionEnergy collisionEnergy;

    public MergedSpectrum(Scan scan, Spectrum<? extends Peak> spectrum, Precursor precursor, double noiseLevel) {
        super(new ArrayList<>());
        this.collisionEnergy = scan.getCollisionEnergy();
        this.noiseLevel = noiseLevel;
        for (int k=0; k < spectrum.size(); ++k) {
            peaks.add(new MergedPeak(new ScanPoint(scan, spectrum.getMzAt(k), spectrum.getIntensityAt(k))));
        }
        if (peaks.size()>100) {
            peaks.sort(Comparator.comparingDouble(MergedPeak::getIntensity).reversed());
            double cutoff = Math.min(noiseLevel, peaks.get(100).getIntensity());
            peaks.removeIf(x->x.getIntensity()<cutoff);
        }
        peaks.sort(Comparator.comparingDouble(Peak::getMass));
        this.precursor= precursor;
        scans = new ArrayList<>();
        scans.add(scan);
        this.dotProduct = 0d;
        for (MergedPeak peak : peaks) dotProduct += peak.getIntensity()*peak.getIntensity();
        this.cosine = 1d;
    }

    public MergedSpectrum(Precursor precursor, List<MergedPeak> peaks, List<Scan> scans, double cosine) {
        super(peaks);
        this.collisionEnergy = CollisionEnergy.mergeAll(scans.stream().map(Scan::getCollisionEnergy).toArray(CollisionEnergy[]::new));
        this.peaks.sort(Comparator.comparingDouble(Peak::getMass));
        this.scans = scans;
        this.precursor=precursor;
        this.dotProduct = 0d;
        for (MergedPeak peak : peaks) {
            if (peak.getMass() < precursor.getMass()-20 && peak.getIntensity()>noiseLevel) {
                dotProduct += peak.getIntensity() * peak.getIntensity();
            }
        }
        this.cosine = cosine;
    }

    public MergedSpectrumWithCollisionEnergies toCollisionEnergyGroup() {
        return new MergedSpectrumWithCollisionEnergies(this);
    }

    @Override
    public CollisionEnergy getCollisionEnergy() {
        return collisionEnergy;
    }

    // we have to do this. Otherwise, memory consumption is just too high
    public void applyNoiseFiltering() {
        int min = (int)Math.floor(scans.size()*0.2);
        this.peaks.removeIf(x->x.getIntensity()<noiseLevel || x.getSourcePeaks().length < min);
        tic = Double.NaN;
    }

    public double getNoiseLevel() {
        return noiseLevel;
    }

    public void setNoiseLevel(double noiseLevel) {
        this.noiseLevel = noiseLevel;
        tic = Double.NaN;
    }

    public List<Scan> getScans() {
        return scans;
    }

    public double totalTic() {
        if (Double.isFinite(tic)) return tic;
        tic = Spectrums.calculateTIC(this, Range.of(0d, precursor.getMass() - 20), noiseLevel);
        return tic;
    }

    public double getNorm() {
        return dotProduct;
    }

    public double getMergedCosine() {
        return cosine;
    }

    public Precursor getPrecursor() {
        return precursor;
    }

    public SimpleSpectrum finishMerging() {
        final int n = scans.size();
        int mostIntensive = scans.stream().max(Comparator.comparingDouble(Scan::getTIC)).map(x->x.getIndex()).orElse(-1);
        final SimpleMutableSpectrum buf = new SimpleMutableSpectrum();
        if (n>=6) {
            int min = (int)Math.ceil(n*0.25);
            for (MergedPeak p : peaks) {
                if (p.getMass() > (this.precursor.getMass()+10))
                    continue;
                if (p.getSourcePeaks().length >= min) {
                    buf.addPeak(p.getRobustAverageMass(noiseLevel), p.sumIntensity());
                } else if (p.getHighestIntensity() > 3*noiseLevel) {
                    for (ScanPoint q : p.getSourcePeaks()) {
                        if (q.getScanNumber()==mostIntensive) {
                            buf.addPeak(q.getMass(),q.getIntensity());
                            break;
                        }
                    }
                }
            }
        } else {
            for (MergedPeak peak : peaks) {
                if (peak.getIntensity()>2*noiseLevel && peak.getMass() < precursor.getMass()+10) {
                    buf.addPeak(peak.getRobustAverageMass(noiseLevel),peak.sumIntensity());
                }
            }
        }

        return new SimpleSpectrum(buf);
    }

    public Quality getQuality(SimpleSpectrum mergedSpectrum) {
        int peaksAboveNoise = 0;
        for (int k=0; k < mergedSpectrum.size(); ++k) {
            if (mergedSpectrum.getMzAt(k) < (getPrecursor().getMass()-20) && mergedSpectrum.getIntensityAt(k) >= noiseLevel*3)
                ++peaksAboveNoise;
        }
        if (peaksAboveNoise >= 5) return Quality.GOOD;
        if (peaksAboveNoise >= 3) return Quality.DECENT;
        return Quality.BAD;
    }
}
