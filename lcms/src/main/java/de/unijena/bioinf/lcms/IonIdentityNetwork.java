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

package de.unijena.bioinf.lcms;

import de.unijena.bioinf.model.lcms.ChromatographicPeak;
import de.unijena.bioinf.model.lcms.CorrelatedIon;
import de.unijena.bioinf.model.lcms.FragmentedIon;
import de.unijena.bioinf.model.lcms.ScanPoint;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.ListIterator;

/**
 * Iterates over all fragment ions and annotated peaks. Check if two ions are adduct or insource fragments of each other
 */
public class IonIdentityNetwork {


    public List<FragmentedIon> filterByIonIdentity(List<FragmentedIon> ions) {
        final BitSet delete = new BitSet(ions.size());
        foreachIon:
        for (int i=0; i < ions.size(); ++i) {
            final FragmentedIon left = ions.get(i);
            for (int j=0; j < ions.size(); ++j) {
                if (i==j) continue;
                final FragmentedIon right = ions.get(j);

                if (overlap(left,right)) {
                    ListIterator<CorrelatedIon> iter = right.getAdducts().listIterator();
                    while (iter.hasNext()) {
                        CorrelatedIon adduct = iter.next();
                        if (isSame(left, adduct.correlation.getRight(), adduct.correlation.getRightSegment())) {
                            delete.set(i);
                            iter.set(new CorrelatedIon(adduct.correlation, left));
                            //System.out.println(left + " is an adduct of " + right + ", see: " + adduct);
                            continue foreachIon;
                        }
                    }
                    iter = right.getInSourceFragments().listIterator();
                    while (iter.hasNext()) {
                        CorrelatedIon adduct = iter.next();
                        if (isSame(left, adduct.correlation.getRight(),adduct.correlation.getRightSegment())) {
                            delete.set(i);
                            //System.out.println(left + " is an adduct of " + right + ", see: " + adduct);
                            iter.set(new CorrelatedIon(adduct.correlation, left));
                            continue foreachIon;
                        }
                    }
                }


            }
        }
        final ArrayList<FragmentedIon> filtered = new ArrayList<>(ions.size());
        for (int k=0; k < ions.size(); ++k) {
            if (!delete.get(k))
                filtered.add(ions.get(k));
        }
        return filtered;
    }

    private boolean isSame(FragmentedIon left, ChromatographicPeak right, ChromatographicPeak.Segment rightSegment) {
        ScanPoint a = left.getPeak().getScanPointAt(left.getSegment().getApexIndex());
        ScanPoint b = right.getScanPointForScanId(a.getScanNumber());
        if (b==null || !a.equals(b)) return false;
        ScanPoint b2 = right.getScanPointAt(rightSegment.getApexIndex());
        ScanPoint a2 = left.getPeak().getScanPointForScanId(rightSegment.getApexScanNumber());
        if (a2==null || !a2.equals(b2)) return false;
        return true;
    }


    private boolean overlap(FragmentedIon left, FragmentedIon right) {
        int a1 = left.getSegment().getStartScanNumber();
        int a2 = right.getSegment().getStartScanNumber();
        int b1 = left.getSegment().getEndScanNumber();
        int b2 = right.getSegment().getEndScanNumber();
        return (a1 < b2 && b1 > a2) || (b1 < a2 && a1 > b2);
    }

}
