
/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *  
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker, 
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

package de.unijena.bioinf.ftalign.view;

/**
 * Created by kaidu on 08.08.14.
 */
public class Pair {

    private DataElement left, right;
    private double tanimoto;

    public Pair(DataElement left, DataElement right) {
        this.left = left;
        this.right = right;
        this.tanimoto = left.tanimoto(right);
    }

    public DataElement getLeft() {
        return left;
    }

    public void setLeft(DataElement left) {
        this.left = left;
        this.tanimoto = left.tanimoto(right);

    }

    public DataElement getRight() {
        return right;
    }

    public void setRight(DataElement right) {
        this.right = right;
        this.tanimoto = left.tanimoto(right);
    }

    public double getTanimoto() {
        return tanimoto;
    }

    @Override
    public String toString() {
        return left.getName() + " vs. " + right.getName();
    }
}
