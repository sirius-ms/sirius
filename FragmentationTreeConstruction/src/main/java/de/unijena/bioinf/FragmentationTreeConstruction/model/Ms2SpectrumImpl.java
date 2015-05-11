/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2015 Kai Dührkop
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.unijena.bioinf.FragmentationTreeConstruction.model;

import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;

import java.util.*;

/**
 * @author Kai Dührkop
 */
public class Ms2SpectrumImpl implements Ms2Spectrum<MS2Peak> {

    private final static Comparator<Ms2SpectrumImpl> ENERGY_COMPARATOR = new EnergyComparator();

    private final CollisionEnergy collisionEnergy;
    private final double parentMass, totalIonCount;
    private final List<MS2Peak> peaks;

    public Ms2SpectrumImpl(Spectrum<Peak> s, CollisionEnergy collisionEnergy, double parentMass, double totalIonCount) {
        this.collisionEnergy = collisionEnergy;
        this.parentMass = parentMass;
        this.peaks = new ArrayList<MS2Peak>();
        for (Peak p : s) {
            peaks.add(new MS2Peak(this, p.getMass(), p.getIntensity()));
        }
        Collections.sort(peaks);
        this.totalIonCount = totalIonCount;
    }

    public Ms2SpectrumImpl(CollisionEnergy collisionEnergy, double parentMass) {
        this.collisionEnergy = collisionEnergy;
        this.parentMass = parentMass;
        this.peaks = new ArrayList<MS2Peak>();
        this.totalIonCount = Double.NaN;
    }

    public Ms2SpectrumImpl(Ms2Spectrum<? extends Peak> spec) {
        this((Spectrum<Peak>) spec, spec.getCollisionEnergy(), spec.getPrecursorMz(), spec.getTotalIonCount());
    }

    public static Comparator<Ms2SpectrumImpl> getEnergyComparator() {
        return ENERGY_COMPARATOR;
    }

    public List<MS2Peak> getPeaks() {
        return peaks;
    }

    @Override
    public double getPrecursorMz() {
        return parentMass;
    }

    public CollisionEnergy getCollisionEnergy() {
        return collisionEnergy;
    }

    @Override
    public double getTotalIonCount() {
        return totalIonCount;
    }

    @Override
    public Ionization getIonization() {
        return null;
    }

    @Override
    public int getMsLevel() {
        return 2;
    }

    @Override
    public double getMzAt(int index) {
        return peaks.get(index).getMz();
    }

    @Override
    public double getIntensityAt(int index) {
        return peaks.get(index).getIntensity();
    }

    @Override
    public MS2Peak getPeakAt(int index) {
        return peaks.get(index);
    }

    @Override
    public int size() {
        return peaks.size();
    }

    @Override
    public Iterator<MS2Peak> iterator() {
        return peaks.iterator();
    }

    private final static class EnergyComparator implements Comparator<Ms2SpectrumImpl> {
        @Override
        public int compare(Ms2SpectrumImpl o1, Ms2SpectrumImpl o2) {
            final int c = Double.compare(o1.collisionEnergy.getMinEnergy(), o2.collisionEnergy.getMinEnergy());
            if (c==0) return Double.compare(o1.collisionEnergy.getMaxEnergy(), o2.collisionEnergy.getMaxEnergy());
            return c;
        }
    }
}
