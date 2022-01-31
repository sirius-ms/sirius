
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

import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.util.Comparator;

public class CollisionEnergy {


    private double minEnergy, maxEnergy, minEnergySource, maxEnergySource;

    public CollisionEnergy(double min, double max) {
        if (min > max) throw new IllegalArgumentException("minimal energy have to be smaller than maximum energy");
        this.minEnergy = min;
        this.maxEnergy = max;
        this.minEnergySource=Double.NaN;
        this.maxEnergySource=Double.NaN;
    }

    public CollisionEnergy(double min, double max,double minSource, double maxSource) {
        if (min > max) throw new IllegalArgumentException("minimal energy have to be smaller than maximum energy");
        this.minEnergy = min;
        this.maxEnergy = max;
        this.minEnergySource=minSource;
        this.maxEnergySource=maxSource;
    }

    public static CollisionEnergy mergeAll(CollisionEnergy... others) {
        if (others == null || others.length == 0) return new CollisionEnergy(0, 0);
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        double minSource = Double.POSITIVE_INFINITY;
        double maxSource = Double.NEGATIVE_INFINITY;
        for (int k = 0; k < others.length; ++k) {
            min = Math.min(min, others[k].minEnergy);
            max = Math.max(max, others[k].maxEnergy);
            minSource=Math.min(minSource,others[k].minEnergySource);
            maxSource=Math.max(maxSource,others[k].maxEnergySource);
        }
        return new CollisionEnergy(min, max,minSource,maxSource);
    }

    @Nullable
    public static CollisionEnergy fromStringOrNull(String value) {
        try {
            return fromString(value);
        } catch (NumberFormatException e) {
            LoggerFactory.getLogger(CollisionEnergy.class).error("Could not parse Collision Energy '" + value + "'. Try ignoring...");
            return null;
        }
    }

    private static final String[] PREFIXES = new String[]{"ramp", "ce"};
    private static final String[] SUFFIXES = new String[]{"ev", "(nce)", "nce", "cid", "hcd", "v", "(nominal)"};

    public static CollisionEnergy fromString(String value) {
        value = value.trim().toLowerCase().replaceAll("\\s+", "");

        if (value.isEmpty() || value.equals("none")) return NONE;


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
            final double x = Double.parseDouble(value.substring(0, k).replace(",", "."));
            final double y = Double.parseDouble(value.substring(k + 1).replace(">", "").replace(",", "."));
            return new CollisionEnergy(x, y);
        } else {
            final double x = Double.parseDouble(value.replace(",", "."));
            return new CollisionEnergy(x, x);
        }
    }

    private static String stringify(double minEnergy) {
        if (Math.abs((int) minEnergy - minEnergy) < 1e-12) return String.valueOf((int) minEnergy);
        return String.valueOf(minEnergy);
    }

    public static Comparator<CollisionEnergy> getMinEnergyComparator() {
        return new Comparator<CollisionEnergy>() {
            @Override
            public int compare(CollisionEnergy o1, CollisionEnergy o2) {
                return Double.compare(o1.minEnergy, o2.minEnergy);
            }
        };
    }

    public boolean isOverlapping(CollisionEnergy other) {
        if(Double.isNaN(minEnergySource))LoggerFactory.getLogger(CollisionEnergy.class).warn("Collision energy is not corrected by MS2 Input Validator");
        return minEnergy <= other.maxEnergy && maxEnergy >= other.minEnergy;
    }

    public double getMinEnergy() {

        if(Double.isNaN(minEnergySource))LoggerFactory.getLogger(CollisionEnergy.class).warn("Collision energy is not corrected by MS2 Input Validator");
        return minEnergy;
    }

    public double getMaxEnergy() {
        if(Double.isNaN(maxEnergySource))LoggerFactory.getLogger(CollisionEnergy.class).warn("Collision energy is not corrected by MS2 Input Validator");
        return maxEnergy;
    }

    public void setMinEnergy(double minEnergy) {
        this.minEnergy = minEnergy;
    }

    public void setMaxEnergy(double maxEnergy) {
        this.maxEnergy = maxEnergy;
    }


    public double getMinEnergySource() {
        return minEnergySource;
    }

    public void setMinEnergySource(double minEnergySource) {
        this.minEnergySource = minEnergySource;
    }

    public double getMaxEnergySource() {
        return maxEnergySource;
    }

    public void setMaxEnergySource(double maxEnergySource) {
        this.maxEnergySource = maxEnergySource;
    }


    public boolean lowerThan(CollisionEnergy o) {
        if(Double.isNaN(minEnergySource))LoggerFactory.getLogger(CollisionEnergy.class).warn("Collision energy is not corrected by MS2 Input Validator");
        return maxEnergy < o.minEnergy;
    }

    public boolean greaterThan(CollisionEnergy o) {
        if(Double.isNaN(minEnergySource))LoggerFactory.getLogger(CollisionEnergy.class).warn("Collision energy is not corrected by MS2 Input Validator");
        return minEnergy > o.maxEnergy;
    }

    public CollisionEnergy merge(CollisionEnergy other) {
        if(Double.isNaN(minEnergySource))LoggerFactory.getLogger(CollisionEnergy.class).warn("Collision energy is not corrected by MS2 Input Validator");
        return new CollisionEnergy(Math.min(minEnergy, other.minEnergy), Math.max(maxEnergy, other.maxEnergy),Math.min(minEnergySource,other.minEnergySource),Math.max(maxEnergySource,other.maxEnergySource));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CollisionEnergy) return equals((CollisionEnergy) obj);
        return false;
    }

    public boolean equals(CollisionEnergy obj) {
        if (this == obj) return true;
        if(Double.isNaN(minEnergySource))LoggerFactory.getLogger(CollisionEnergy.class).warn("Collision energy is not corrected by MS2 Input Validator");
        return Math.abs(minEnergy - obj.minEnergy) < 1e-12 && Math.abs(maxEnergy - obj.maxEnergy) < 1e-12;
    }

    @Override
    public String toString() {
        if(Double.isNaN(minEnergySource))LoggerFactory.getLogger(CollisionEnergy.class).warn("Collision energy is not corrected by MS2 Input Validator");
        if (this.equals(NONE)) return "none";
        if (minEnergy == maxEnergy && minEnergySource == maxEnergySource) return stringify(minEnergy) + " eV (corrected "+stringify(minEnergySource) + " eV)";
        if (minEnergy == maxEnergy && minEnergySource != maxEnergySource) return stringify(minEnergy) + " eV (corrected "+stringify(minEnergySource) + " - " +stringify(maxEnergySource)+ " eV)";

        return stringify(minEnergy) + " - " + stringify(maxEnergy) + " eV (corrected "+stringify(minEnergySource) + " - " +stringify(maxEnergySource)+ " eV)";
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = Double.doubleToLongBits(minEnergy);
        result = (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(maxEnergy);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    private static CollisionEnergy NONE = new CollisionEnergy(-1,-1);

    public static CollisionEnergy none() {
        return NONE;
    }
}
