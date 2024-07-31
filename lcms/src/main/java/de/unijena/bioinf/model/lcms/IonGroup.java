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

import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.lcms.LCMSProccessingInstance;
import de.unijena.bioinf.lcms.quality.Quality;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class IonGroup {

    protected MutableChromatographicPeak peak;
    protected List<CorrelationGroup> isotopes;
    protected SimpleSpectrum isotopeSpectrum;
    protected IonAnnotation ionAnnotation;
    protected int chargeState;

    //////// here we can add adducts which cannot be correlated but are still there ///////
    protected SimpleMutableSpectrum otherMzValues;
    //protected final ChromatographicPeak.Segment segment;
    // apex index of the segment
    protected int segmentApexIndex;

    public IonGroup(MutableChromatographicPeak peak, ChromatographicPeak.Segment segment, List<CorrelationGroup> isotopes) {
        this.peak = peak;
        this.isotopes = isotopes;
        this.otherMzValues = new SimpleMutableSpectrum();
        this.segmentApexIndex = segment.apexIndex;
        assert peak.getSegmentWithApexId(segmentApexIndex).isPresent();
        }

    public void assignIsotopePeaksWithoutCorrelation(SimpleSpectrum spec) {
        this.isotopeSpectrum = spec;
    }

    public boolean chargeStateIsNotDifferent(IonGroup other) {
        return chargeState == 0 || other.chargeState==0 || other.chargeState==chargeState;
    }

    public MutableChromatographicPeak getPeak() {
        return peak;
    }

    public List<CorrelationGroup> getIsotopes() {
        return isotopes;
    }

    public SimpleSpectrum getIsotopesAsSpectrum() {
        if (isotopeSpectrum==null) isotopeSpectrum = new SimpleSpectrum(LCMSProccessingInstance.toIsotopeSpectrum(this,getMass()));
        return isotopeSpectrum;
    }

    public void addIsotopes(List<CorrelationGroup> correlatedPeaks, SimpleMutableSpectrum spectrum) {
        this.isotopes.addAll(correlatedPeaks);
        this.isotopeSpectrum = new SimpleSpectrum(spectrum);
    }


    public IonAnnotation getIonAnnotation() {
        return ionAnnotation;
    }

    public void setIonAnnotation(IonAnnotation ionAnnotation) {
        this.ionAnnotation = ionAnnotation;
    }


    public int getChargeState() {
        return chargeState;
    }

    public void setChargeState(int chargeState) {
        this.chargeState = chargeState;
    }

    public ChromatographicPeak.Segment getSegment() {
        return peak.getSegmentWithApexId(segmentApexIndex).orElseThrow();
    }

    public String toString() {
        if (peak.getSegments().isEmpty()) return "";
        int apex = peak.getSegments().firstKey();
        String iso = isotopes.isEmpty() ? "" : (isotopes.size() + " isotopes with correlation " + Arrays.toString(isotopes.stream().mapToDouble(x->x.getCorrelation()).toArray()));
        return String.format(Locale.US, "ScanID: %d .. %d. m/z = %.5f, intensity = %.1f. %s", peak.getScanNumberAt(0), peak.getScanNumberAt(peak.numberOfScans()-1), peak.getMzAt(apex), peak.getIntensityAt(apex), iso);
    }

    public Quality getMsQuality() {
        if (isotopes.size()>=2) return Quality.GOOD;
        if (isotopes.size()>=1) return Quality.DECENT;
        return Quality.BAD;
    }

    public double getMass() {
        return peak.getMzAt(getSegment().apexIndex);
    }

    protected void setMinMaxScanIndex(int[] scanIndex, int surrounding) {
        getSegment().setMinMaxScanIndex(scanIndex,surrounding);
        for (CorrelationGroup iso : isotopes) {
            iso.rightSegment.setMinMaxScanIndex(scanIndex,surrounding);
        }
    }

    public int getApexIndex() {
        return segmentApexIndex;
    }
}
