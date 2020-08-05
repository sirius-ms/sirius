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

package de.unijena.bioinf.model.lcms;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;

public class CorrelationGroup {
    protected ChromatographicPeak left, right;
    protected ChromatographicPeak.Segment leftSegment, rightSegment;
    protected int start, end;
    protected double correlation, kl, cosine;

    protected String annotation;

    // for adduct types
    protected PrecursorIonType leftType, rightType;

    public CorrelationGroup(ChromatographicPeak left, ChromatographicPeak right, ChromatographicPeak.Segment leftSegment, ChromatographicPeak.Segment rightSegment, int start, int end, double correlation, double kl, double cosine) {
        this.left = left;
        this.right = right;
        this.leftSegment = leftSegment;
        this.rightSegment = rightSegment;
        this.correlation = correlation;
        this.kl = kl;
        this.cosine = cosine;
        this.start = start;
        this.end = end;
    }

    public double getCosine() {
        return cosine;
    }

    public double getKullbackLeibler() {
        return kl;
    }

    public ChromatographicPeak getLeft() {
        return left;
    }

    public ChromatographicPeak getRight() {
        return right;
    }

    public ChromatographicPeak.Segment getLeftSegment() {
        return leftSegment;
    }

    public ChromatographicPeak.Segment getRightSegment() {
        return rightSegment;
    }

    public int getNumberOfCorrelatedPeaks() {
        return end-start+1;
    }

    public double getCorrelation() {
        return correlation;
    }

    public CorrelationGroup invert() {
        return new CorrelationGroup(right,left,rightSegment,leftSegment,start,end, correlation,kl,cosine);
    }

    public String getAnnotation() {
        return annotation;
    }

    public void setAnnotation(String annotation) {
        this.annotation = annotation;
    }

    public PrecursorIonType getLeftType() {
        return leftType;
    }

    public void setLeftType(PrecursorIonType leftType) {
        this.leftType = leftType;
    }

    public PrecursorIonType getRightType() {
        return rightType;
    }

    public void setRightType(PrecursorIonType rightType) {
        this.rightType = rightType;
    }

    @Override
    public String toString() {
        return getNumberOfCorrelatedPeaks() + " peaks with correlation = " + correlation;
    }
}
