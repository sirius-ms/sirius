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

package de.unijena.bioinf.ChemistryBase.ms.ft;

import de.unijena.bioinf.ms.annotations.TreeAnnotation;

public final class TreeStatistics implements TreeAnnotation {

    /**
     * an intensity ratio: intensity summed over all peaks explained by the tree normalized by total intensity. ignores FT root and artificial peaks
     */
    protected final double explainedIntensity;

    /**
     * an intensity ratio: intensity summed over all peaks explained by the tree normalized by total intensity of peaks with MF decomposition. ignores FT root and artificial peaks
     */
    protected final double explainedIntensityOfExplainablePeaks;

    /**
     * ratio of explained and total number of peaks. ignores FT root and artificial peaks
     */
    protected final double ratioOfExplainedPeaks;

    private final static TreeStatistics NONE = new TreeStatistics();

    public static TreeStatistics none() {
        return NONE;
    }

    protected TreeStatistics() {
        this(0d,0d,0d);
    }

    public TreeStatistics(double explainedIntensity, double explainedIntensityOfExplainablePeaks, double ratioOfExplainedPeaks) {
        this.explainedIntensity = explainedIntensity;
        this.explainedIntensityOfExplainablePeaks = explainedIntensityOfExplainablePeaks;
        this.ratioOfExplainedPeaks = ratioOfExplainedPeaks;
    }

    public double getExplainedIntensity() {
        return explainedIntensity;
    }

    public double getExplainedIntensityOfExplainablePeaks() {
        return explainedIntensityOfExplainablePeaks;
    }

    public double getRatioOfExplainedPeaks() {
        return ratioOfExplainedPeaks;
    }
}
