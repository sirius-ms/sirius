
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

package de.unijena.bioinf.ChemistryBase.ms;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.utils.SimpleSerializers;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

/**
 * Mutable spectrum with header information about MS/MS.
 * Allows shallow copy which do not copy all peak data and, thus,
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE)
@Builder
@Jacksonized
public class MutableMs2Spectrum implements Ms2Spectrum<Peak>, MutableSpectrum<Peak> {

    public static class Header implements Cloneable {
        private double precursorMz = 0d;
        private CollisionEnergy collisionEnergy = null;
        private double totalIoncount = 0d;
        @JsonSerialize(using = ToStringSerializer.class)
        @JsonDeserialize(using = SimpleSerializers.IonizationTypeDeserializer.class)
        private Ionization ionization = null;
        private int msLevel = 0;
        private int scanNumber = -1; // an arbitrary ID that is unique within the experiment object

        public Header() {

        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Header header = (Header) o;
            return scanNumber == header.scanNumber;
        }

        @Override
        public int hashCode() {
            return scanNumber;
        }

        public Header clone() {
            try {
                return (Header)super.clone();
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    protected final Header header;
    protected final SimpleMutableSpectrum peaks;

    public MutableMs2Spectrum() {
        this.header = new Header();
        this.peaks = new SimpleMutableSpectrum();
    }

    protected MutableMs2Spectrum(Header header, SimpleMutableSpectrum peaks) {
        this.header = header;
        this.peaks = peaks;
    }

    public <T extends Peak, S extends Spectrum<T>> MutableMs2Spectrum(S spec, double parentmass, CollisionEnergy energy, int mslevel, int scanNumber) {
        this(spec, parentmass, energy, mslevel);
        this.header.scanNumber = scanNumber;
    }
    public <T extends Peak, S extends Spectrum<T>> MutableMs2Spectrum(S spec, double parentmass, CollisionEnergy energy, int mslevel) {
        this.peaks = new SimpleMutableSpectrum(spec);
        this.header = new Header();
        header.precursorMz = parentmass;
        header.collisionEnergy = energy;
        header.msLevel = mslevel;
    }

    public MutableMs2Spectrum(Spectrum<? extends Peak> spec) {
        this.peaks = new SimpleMutableSpectrum(spec);
        if (spec instanceof MutableMs2Spectrum) {
            this.header = ((MutableMs2Spectrum) spec).header.clone();
        } else {
            this.header = new Header();
            header.msLevel = spec.getMsLevel();
            header.collisionEnergy = spec.getCollisionEnergy();
            if (spec instanceof Ms2Spectrum) {
                final Ms2Spectrum<Peak> ms2spec = (Ms2Spectrum<Peak>) spec;
                header.precursorMz = ms2spec.getPrecursorMz();

                header.totalIoncount = ms2spec.getTotalIonCount();
                header.ionization = ms2spec.getIonization();
            }
        }
    }

    public MutableMs2Spectrum shallowCopy() {
        return new MutableMs2Spectrum(header, peaks);
    }

    public int getScanNumber() {
        return header.scanNumber;
    }

    public void setScanNumber(int scanNumber) {
        header.scanNumber = scanNumber;
    }

    @Override
    public double getPrecursorMz() {
        return header.precursorMz;
    }

    @Override
    public double getMzAt(int index) {
        return peaks.getMzAt(index);
    }

    @Override
    public double getIntensityAt(int index) {
        return peaks.getIntensityAt(index);
    }

    @Override
    public Peak getPeakAt(int index) {
        return peaks.getPeakAt(index);
    }

    @Override
    public int size() {
        return peaks.size();
    }

    @Override
    public CollisionEnergy getCollisionEnergy() {
        return header.collisionEnergy;
    }

    @Override
    public double getTotalIonCount() {
        return header.totalIoncount;
    }

    @Override
    public Ionization getIonization() {
        return header.ionization;
    }

    @Override
    public int getMsLevel() {
        return header.msLevel;
    }

    public void setPrecursorMz(double precursorMz) {
        header.precursorMz = precursorMz;
    }

    public void setCollisionEnergy(CollisionEnergy collisionEnergy) {
        header.collisionEnergy = collisionEnergy;
    }

    public void setTotalIonCount(double totalIoncount) {
        header.totalIoncount = totalIoncount;
    }

    public void setIonization(Ionization ionization) {
        header.ionization = ionization;
    }

    public void setMsLevel(int msLevel) {
        header.msLevel = msLevel;
    }


    @Override
    public void addPeak(Peak peak) {
        peaks.addPeak(peak);
    }

    @Override
    public void addPeak(double mz, double intensity) {
        peaks.addPeak(mz,intensity);
    }

    @Override
    public void setPeakAt(int index, Peak peak) {
        peaks.setPeakAt(index,peak);
    }

    @Override
    public void setMzAt(int index, double mass) {
        peaks.setMzAt(index,mass);
    }

    @Override
    public void setIntensityAt(int index, double intensity) {
        peaks.setIntensityAt(index,intensity);
    }

    @Override
    public Peak removePeakAt(int index) {
        return peaks.removePeakAt(index);
    }

    @Override
    public void swap(int index1, int index2) {
        peaks.swap(index1, index2);
    }

    @NotNull
    @Override
    public Iterator<Peak> iterator() {
        return peaks.iterator();
    }
}
