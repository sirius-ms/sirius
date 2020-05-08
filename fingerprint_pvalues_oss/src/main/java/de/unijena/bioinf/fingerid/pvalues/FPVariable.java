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

package de.unijena.bioinf.fingerid.pvalues;

public final class FPVariable implements Cloneable {

    //incoming edge
    protected final int from, to;

    protected double oo, oI, Io, II;
    protected double frequency;
    protected double correlation;

    // PIo = child=1, parent=0
    protected Probability Poo, PoI, PIo, PII, o, I;

    public FPVariable(int from, int to) {
        this.from = from;
        this.to = to;
    }

    public void calculateProbabilities() {
        assert Math.abs(oo+Io+oI+II-1) < 1e-5;
        final double Mo = oo + Io, MI = oI + II;
        Poo = new Probability(oo / Mo);
        PoI = new Probability(oI / MI);
        PIo = new Probability(Io / Mo);
        PII = new Probability(II / MI);
        o = new Probability(1d-frequency);
        I = new Probability(frequency);
    }

    @Override
    public FPVariable clone() {
        try {
            return (FPVariable)super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e); // should never happen?
        }
    }
}
