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

import de.unijena.bioinf.lcms.ionidentity.AdductMassDifference;

public class CorrelationGroup {
    protected MutableChromatographicPeak left, right;
    protected ChromatographicPeak.Segment leftSegment, rightSegment;
    protected int startScanNumber, endScanNumber, length;
    protected double correlation, kl, cosine;

    protected String annotation;

    // for adduct types
    protected AdductMassDifference adductAssignment;

    public double score;

    public CorrelationGroup(MutableChromatographicPeak left, MutableChromatographicPeak right, ChromatographicPeak.Segment leftSegment, ChromatographicPeak.Segment rightSegment, int startScanNumber, int endScanNumber, int length, double correlation, double kl, double cosine, double score) {
        this.left = left;
        this.right = right;
        this.leftSegment = leftSegment;
        this.rightSegment = rightSegment;
        this.correlation = correlation;
        this.kl = kl;
        this.cosine = cosine;
        this.startScanNumber = startScanNumber;
        this.endScanNumber = endScanNumber;
        this.length = length;
        this.score = score;
    }

    public int getStartScanNumber() {
        return startScanNumber;
    }
    public int getEndScanNumber() {
        return endScanNumber;
    }

    public double getCosine() {
        return cosine;
    }

    public double getKullbackLeibler() {
        return kl;
    }

    public MutableChromatographicPeak getLeft() {
        return left;
    }

    public MutableChromatographicPeak getRight() {
        return right;
    }

    public ChromatographicPeak.Segment getLeftSegment() {
        return leftSegment;
    }

    public ChromatographicPeak.Segment getRightSegment() {
        return rightSegment;
    }

    public int getNumberOfCorrelatedPeaks() {
        return length;
    }

    public double getCorrelation() {
        return correlation;
    }

    public CorrelationGroup invert() {
        return new CorrelationGroup(right,left,rightSegment,leftSegment,startScanNumber,endScanNumber, length, correlation,kl,cosine, score);
    }

    public String getAnnotation() {
        return annotation;
    }

    public void setAnnotation(String annotation) {
        this.annotation = annotation;
    }

    public AdductMassDifference getAdductAssignment() {
        return adductAssignment;
    }

    public void setAdductAssignment(AdductMassDifference adductAssignment) {
        this.adductAssignment = adductAssignment;
    }

    @Override
    public String toString() {
        return getNumberOfCorrelatedPeaks() + " peaks with correlation = " + correlation;
    }

    public CorrelationGroup ensureLargeToSmall() {
        if (leftSegment.getStartScanNumber()>= rightSegment.getStartScanNumber() && leftSegment.getEndScanNumber() <= rightSegment.getEndScanNumber()) {
            return invert();
        } else return this;
    }
}
