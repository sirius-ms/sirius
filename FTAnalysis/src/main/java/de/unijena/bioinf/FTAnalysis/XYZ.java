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
package de.unijena.bioinf.FTAnalysis;

public class XYZ implements Comparable<XYZ> {
    final double x;
    final double y;
    final double z;
    XYZ(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        XYZ xyz = (XYZ) o;

        if (Double.compare(xyz.x, x) != 0) return false;
        if (Double.compare(xyz.y, y) != 0) return false;
        if (Double.compare(xyz.z, z) != 0) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = x != +0.0d ? Double.doubleToLongBits(x) : 0L;
        result = (int) (temp ^ (temp >>> 32));
        temp = y != +0.0d ? Double.doubleToLongBits(y) : 0L;
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = z != +0.0d ? Double.doubleToLongBits(z) : 0L;
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public int compareTo(XYZ o) {
        return Double.compare(x, o.x);
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ", " + z + ")";
    }
}
