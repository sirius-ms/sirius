
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

package de.unijena.bioinf.MassDecomposer;

class Weight<T> implements Comparable<Weight<T>> {

    private final T owner;
    private final double mass;
    private long integerMass;
    private long l;
    private long lcm;

    public Weight(T owner, double mass) {
        this.owner = owner;
        this.mass = mass;
    }

    public T getOwner() {
        return owner;
    }

    public double getMass() {
        return mass;
    }

    public long getIntegerMass() {
        return integerMass;
    }

    public void setIntegerMass(long integerMass) {
        this.integerMass = integerMass;
    }

    public long getL() {
        return l;
    }

    public void setL(long l) {
        this.l = l;
    }

    public long getLcm() {
        return lcm;
    }

    public void setLcm(long lcm) {
        this.lcm = lcm;
    }

    @Override
    public int compareTo(Weight<T> tWeight) {
        return (int)Math.signum(mass - tWeight.mass);
    }
}
