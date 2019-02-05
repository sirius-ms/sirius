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
package de.unijena.bioinf.ChemistryBase.ms;

import java.util.Comparator;

public class CollisionEnergy {

    private final double minEnergy, maxEnergy;

    public CollisionEnergy(double min, double max) {
        if (min > max) throw new IllegalArgumentException("minimal energy have to be smaller than maximum energy");
        this.minEnergy = min;
        this.maxEnergy = max;
    }

    public static CollisionEnergy mergeAll(CollisionEnergy... others) {
        if (others == null || others.length == 0) return new CollisionEnergy(0, 0);
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (int k = 0; k < others.length; ++k) {
            min = Math.min(min, others[k].minEnergy);
            max = Math.max(max, others[k].maxEnergy);
        }
        return new CollisionEnergy(min, max);
    }

    public static CollisionEnergy fromString(String value) {
        value = value.trim().toLowerCase();
        if (value.isEmpty() || value.equals("none")) return NONE;
        int ev =  value.indexOf("eV");
        if (ev > 0) value = value.substring(0,ev);
        final int k = value.indexOf('-');
        if (k > 0) {
            final double x = Double.parseDouble(value.substring(0, k));
            final double y = Double.parseDouble(value.substring(k + 1));
            return new CollisionEnergy(x, y);
        } else {
            final double x = Double.parseDouble(value);
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
        return minEnergy <= other.maxEnergy && maxEnergy >= other.minEnergy;
    }

    public double getMinEnergy() {
        return minEnergy;
    }

    public double getMaxEnergy() {
        return maxEnergy;
    }

    public boolean lowerThan(CollisionEnergy o) {
        return maxEnergy < o.minEnergy;
    }

    public boolean greaterThan(CollisionEnergy o) {
        return minEnergy > o.maxEnergy;
    }

    public CollisionEnergy merge(CollisionEnergy other) {
        return new CollisionEnergy(Math.min(minEnergy, other.minEnergy), Math.max(maxEnergy, other.maxEnergy));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CollisionEnergy) return equals((CollisionEnergy) obj);
        return false;
    }

    public boolean equals(CollisionEnergy obj) {
        if (this == obj) return true;
        return Math.abs(minEnergy - obj.minEnergy) < 1e-12 && Math.abs(maxEnergy - obj.maxEnergy) < 1e-12;
    }

    @Override
    public String toString() {
        if (this.equals(NONE)) return "none";
        if (minEnergy == maxEnergy) return stringify(minEnergy) + " eV";
        return stringify(minEnergy) + " - " + stringify(maxEnergy) + " eV";
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

    private static CollisionEnergy NONE = new CollisionEnergy(0,0);

    public static CollisionEnergy none() {
        return NONE;
    }
}
