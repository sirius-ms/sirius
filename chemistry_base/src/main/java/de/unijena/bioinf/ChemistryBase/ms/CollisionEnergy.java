
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

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import de.unijena.bioinf.ChemistryBase.utils.SimpleSerializers;
import de.unijena.bioinf.ChemistryBase.utils.Utils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Collision Energy in eV
 */
@JsonSerialize(using = SimpleSerializers.CollisionEnergySerializer.class)
@JsonDeserialize(using = SimpleSerializers.CollisionEnergyDeserializer.class)
public class CollisionEnergy implements Serializable {


    /**
     * These are immutable uncorrected energies as imported from the data source
     */
    private final double minEnergySource, maxEnergySource;

    /**
     * These are the energies to use within algorithms (corrected energies).
     * Fallback to source energies if corrected NaN.
     */
    private double minEnergy, maxEnergy;

    public CollisionEnergy(double minSource, double maxSource) {
        if (minSource > maxSource)
            throw new IllegalArgumentException("minimal energy have to be smaller than maximum energy");
        this.minEnergy = Double.NaN;
        this.maxEnergy = Double.NaN;
        this.minEnergySource = minSource;
        this.maxEnergySource = minSource;
    }

    public CollisionEnergy(CollisionEnergy ce){
        this.minEnergy=ce.getMinEnergy();
        this.maxEnergy=ce.getMaxEnergy();
        this.minEnergySource=ce.minEnergySource;
        this.maxEnergySource=ce.maxEnergySource;
    }

    public CollisionEnergy(double min, double max, double minSource, double maxSource) {
        if (min > max) throw new IllegalArgumentException("minimal energy have to be smaller than maximum energy");
        this.minEnergy = min;
        this.maxEnergy = max;
        this.minEnergySource = minSource;
        this.maxEnergySource = maxSource;
    }

    public CollisionEnergy(double collisionEnergy) {
        this(collisionEnergy,collisionEnergy);
    }

    public static CollisionEnergy mergeAll(CollisionEnergy... others) {
        others = Arrays.stream(others).filter(x->!x.isUnknown()).toArray(CollisionEnergy[]::new);
        if (others.length == 0) return new CollisionEnergy(0, 0);
        double minSource = Arrays.stream(others).mapToDouble(CollisionEnergy::minEnergySource).min().orElse(Double.NaN);
        double maxSource = Arrays.stream(others).mapToDouble(CollisionEnergy::maxEnergySource).max().orElse(Double.NaN);
        double min = Double.NaN;
        double max = Double.NaN;

        if (!Arrays.stream(others).mapToDouble(CollisionEnergy::maxEnergy).allMatch(Double::isNaN))
            max = Arrays.stream(others).mapToDouble(CollisionEnergy::getMaxEnergy).max().orElse(Double.NaN);

        if (!Arrays.stream(others).mapToDouble(CollisionEnergy::minEnergy).allMatch(Double::isNaN))
            min = Arrays.stream(others).mapToDouble(CollisionEnergy::getMinEnergy).min().orElse(Double.NaN);

        return new CollisionEnergy(min, max, minSource, maxSource);
    }

    private boolean isUnknown() {
        return minEnergySource<0;
    }

    @Nullable
    public static CollisionEnergy fromStringOrNull(String value) {
        try {
            if (value == null || value.isBlank())
                return null;
            return fromString(value);
        } catch (NumberFormatException e) {
            LoggerFactory.getLogger(CollisionEnergy.class).error("Could not parse Collision Energy '" + value + "'. Try ignoring...");
            return null;
        }
    }

    private static final String[] PREFIXES = new String[]{"ramp", "ce"};
    private static final String[] SUFFIXES = new String[]{"ev", "(nce)", "nce", "cid", "hcd", "v", "(nominal)", "evft-ms", "evft-msii"};

    public static CollisionEnergy fromString(String value) {
        value = value.trim().toLowerCase().replaceAll("\\s+", "");

        if (value.isEmpty() || value.equals("none")) return NONE;

        final String corr = "(corrected";
        int split = value.indexOf(corr);
        if (split >= 0 && value.endsWith(")")) {
            double[] sourceCes = parseCE(value.substring(0, split));
            double[] correctedCes = parseCE(value.substring(split + corr.length(), value.length() - 1));
            return new CollisionEnergy(correctedCes[0], correctedCes[1],sourceCes[0], sourceCes[1]);
        } else {
            double[] ces = parseCE(value);
            return new CollisionEnergy(ces[0], ces[1]);
        }

    }

    private static double[] parseCE(String value) {
        //eliminate prefixes
        if (!Character.isDigit(value.charAt(0))) {
            for (String ext : PREFIXES) {
                if (value.startsWith(ext))
                    value = value.substring(ext.length());
            }
        }

        //eliminate suffixes
        if (!Character.isDigit(value.charAt(value.length() - 1))) {
            for (String ext : SUFFIXES) {
                if (value.endsWith(ext))
                    value = value.substring(0, value.length() - ext.length());
            }
        }


        int k = value.indexOf('-');
        if (k > 0) {
            final double x = Utils.parseDoubleWithUnknownDezSep(value.substring(0, k));
            final double y = Utils.parseDoubleWithUnknownDezSep(value.substring(k + 1).replace(">", ""));
            return new double[]{x, y};
        } else {
            final double x = Utils.parseDoubleWithUnknownDezSep(value);
            return new double[]{x, x};
        }
    }

    public static String stringify(double minEnergy) {
        if (Math.abs((int) minEnergy - minEnergy) < 1e-12) return String.valueOf((int) minEnergy);
        return String.valueOf(minEnergy);
    }

    public static Comparator<CollisionEnergy> getMinEnergyComparator() {
        return Comparator.comparingDouble(CollisionEnergy::getMinEnergy);
    }

    public boolean isOverlapping(CollisionEnergy other) {
        return getMinEnergy() <= other.getMaxEnergy() && getMaxEnergy() >= other.getMinEnergy();
    }


    public double getMinEnergy() {
        return getMinEnergy(true);
    }

    public double getMinEnergy(final boolean log) {
        if (Double.isNaN(minEnergy)) {
            if (log)
                LoggerFactory.getLogger(CollisionEnergy.class).debug("Collision energy '" + this + "' is not corrected by MS2 Input Validator ('minEnergy' field not set), falling back to 'minEnergySource'");
            return minEnergySource;
        }
        return minEnergy;
    }

    public double getMaxEnergy() {
        return getMaxEnergy(true);
    }

    public double getMaxEnergy(final boolean log) {
        if (Double.isNaN(maxEnergy)) {
            if (log)
                LoggerFactory.getLogger(CollisionEnergy.class).debug("Collision energy '" + this + "' is not corrected by MS2 Input Validator ('maxEnergy' field not set), falling back to 'maxEnergySource'");
            return maxEnergySource;
        }
        return maxEnergy;
    }

    public void setMinEnergy(double minEnergy) {
        this.minEnergy = minEnergy;
    }

    public void setMaxEnergy(double maxEnergy) {
        this.maxEnergy = maxEnergy;
    }


    public double minEnergySource() {
        return minEnergySource;
    }

    public double maxEnergySource() {
        return maxEnergySource;
    }

    protected double minEnergy() {
        return minEnergy;
    }

    protected double maxEnergy() {
        return maxEnergy;
    }

    public boolean isCorrected() {
        return !Double.isNaN(minEnergy()) && !Double.isNaN(maxEnergy());
    }

    public boolean lowerThan(CollisionEnergy o) {
        return getMaxEnergy() < o.getMinEnergy();
    }

    public boolean greaterThan(CollisionEnergy o) {
        return getMinEnergy() > o.getMaxEnergy();
    }

    public CollisionEnergy merge(CollisionEnergy other) {
        return new CollisionEnergy(Math.min(minEnergy, other.minEnergy), Math.max(maxEnergy, other.maxEnergy),Math.min(minEnergySource,other.minEnergySource),Math.max(maxEnergySource,other.maxEnergySource));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CollisionEnergy) return equals((CollisionEnergy) obj);
        return false;
    }

    public boolean equals(CollisionEnergy obj) {
        return Math.abs(getMinEnergy() - obj.getMinEnergy()) < 1e-12 && Math.abs(getMaxEnergy() - obj.getMaxEnergy()) < 1e-12;
    }

    @Override
    public String toString() {
        if (minEnergy == maxEnergy && minEnergySource == maxEnergySource)
            return stringify(minEnergySource) + " eV" +
                    (Double.isNaN(minEnergy) ? "" : " (corrected " + stringify(minEnergy) + " eV)");
        if (minEnergy != maxEnergy && minEnergySource == maxEnergySource)
            return stringify(minEnergySource) + " eV" +
                    (Double.isNaN(minEnergy) || Double.isNaN(maxEnergy) ? "" : " (corrected " + stringify(minEnergy) + " - " + stringify(maxEnergy) + " eV)");

        return stringify(minEnergySource) + " - " + stringify(maxEnergySource) + " eV" +
                (Double.isNaN(minEnergy) || Double.isNaN(maxEnergy) ? "" : " (corrected " + stringify(minEnergy) + " - " + stringify(maxEnergy) + " eV)");
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = Double.doubleToLongBits(getMinEnergy());
        result = (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(getMaxEnergy());
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    private static final CollisionEnergy NONE = new CollisionEnergy(-1, -1, -1, -1);

    public static CollisionEnergy none() {
        return NONE;
    }

    public static CollisionEnergy copyWithoutCorrection(CollisionEnergy ce){
        return new CollisionEnergy(ce.minEnergySource,ce.maxEnergySource);
    }

    public boolean isRamp() {
        return getMinEnergy(false) < getMaxEnergy();
    }
}
