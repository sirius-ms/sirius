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
import de.unijena.bioinf.ChemistryBase.ms.lcms.CoelutingTraceSet;
import de.unijena.bioinf.ChemistryBase.ms.lcms.CompoundReport;
import de.unijena.bioinf.ChemistryBase.ms.lcms.Trace;
import de.unijena.bioinf.lcms.ProcessedSample;
import de.unijena.bioinf.lcms.peakshape.PeakShape;
import de.unijena.bioinf.lcms.quality.Quality;
import de.unijena.bionf.spectral_alignment.CosineQuerySpectrum;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TLongArrayList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class FragmentedIon extends IonGroup {

    protected final CosineQuerySpectrum msms;
    protected final Scan ms2Scan;
    protected final List<CorrelatedIon> adducts, inSourceFragments;
    protected PrecursorIonType detectedIonType;
    protected Set<PrecursorIonType> alternativeIonTypes;
    protected PeakShape peakShape;
    protected int alignments=0; // internal counter
    protected Quality ms2Quality;
    protected Polarity polarity;
    protected ArrayList<CompoundReport> additionalInfos;

    // might be useful for chimeric detection?
    protected double intensityAfterPrecursor;

    protected List<ChromatographicPeak> chimerics;
    private double chimericPollution;

    protected Scan[] mergedScans;

    public FragmentedIon(Polarity polarity, Scan ms2Scan, CosineQuerySpectrum msms, Quality ms2Quality, ChromatographicPeak chromatographicPeak,ChromatographicPeak.Segment segment, Scan[] mergedScans) {
        super(chromatographicPeak, segment, new ArrayList<>());
        this.polarity = polarity;
        this.msms = msms;
        this.ms2Scan = ms2Scan;
        this.adducts = new ArrayList<>();
        this.inSourceFragments = new ArrayList<>();
        this.ms2Quality = ms2Quality;
        this.alternativeIonTypes = Collections.emptySet();
        this.chimerics = new ArrayList<>();
        this.additionalInfos = new ArrayList<>();
        this.mergedScans = mergedScans;
    }

    public Scan[] getMergedScans() {
        return mergedScans;
    }

    public CoelutingTraceSet asLCMSSubtrace(ProcessedSample sample) {
        int[] scanNumberRange = new int[]{Integer.MAX_VALUE, Integer.MIN_VALUE};
        setMinMaxScanIndex(scanNumberRange, 10);
        final TLongArrayList retentionTimes = new TLongArrayList(scanNumberRange[1]-scanNumberRange[0]);
        final TIntArrayList scanIds = new TIntArrayList(scanNumberRange[1]-scanNumberRange[0]);
        for (Scan s : sample.run.getScans(scanNumberRange[0], scanNumberRange[1]).values()) {
            if (!s.isMsMs()) {
                retentionTimes.add(s.getRetentionTime());
                scanIds.add(s.getIndex());
            }
        }
        scanNumberRange[0] = scanIds.get(0);
        scanNumberRange[1] = scanIds.get(scanIds.size()-1);


return null;
    }

    private Trace getTrace(ChromatographicPeak.Segment segment, int[] range) {
        ChromatographicPeak P = segment.getPeak();
        int mindex = Math.max(P.getScanNumberAt(0), range[0]);
        int maxdex = Math.min(P.getScanNumberAt(P.numberOfScans()-1), range[1]);
        int offset = range[0]-mindex;
        final double[] mz = new double[maxdex-mindex+1];
        final float[] intensity = new float[maxdex-mindex+1];
        int i=0;
        for (int k=mindex; k <= maxdex; ++k) {
            mz[i] = P.getMzAt(k);
            intensity[i] = (float)P.getIntensityAt(k);
            ++i;
        }
        final int detectorOffset = segment.getStartIndex();

return null;
    }

    protected void setMinMaxScanIndex(int[] scanIndex, int surrounding) {
        super.setMinMaxScanIndex(scanIndex,surrounding);
        for (CorrelatedIon a : adducts) a.ion.setMinMaxScanIndex(scanIndex,surrounding);
        for (CorrelatedIon a : inSourceFragments) a.ion.setMinMaxScanIndex(scanIndex,surrounding);
    }

    public double getIntensityAfterPrecursor() {
        return intensityAfterPrecursor;
    }

    public void setIntensityAfterPrecursor(double intensityAfterPrecursor) {
        this.intensityAfterPrecursor = intensityAfterPrecursor;
    }

    public void setMs2Quality(Quality ms2Quality) {
        this.ms2Quality = ms2Quality;
    }

    public int getPolarity() {
        return polarity.charge;
    }

    public synchronized void incrementAlignments() {
        ++alignments;
    }
    public int alignmentCount() {
        return alignments;
    }

/*
    public double comparePeakWidthSmallToLarge(FragmentedIon other) {

        if (getIntensity() > other.getIntensity())
            return 1d/other.comparePeakWidthSmallToLarge(this);

        final double fwhm = segment.fwhm();
        final double otherFwhm = other.segment.fwhm(1d - 0.5d*getIntensity()/other.getIntensity());
        return fwhm / otherFwhm;
    }
*/

    public double comparePeakWidthSmallToLarge(FragmentedIon other) {

        if (getIntensity() > other.getIntensity()) {
            return other.getSegment().fwhm(0.5d) / getSegment().fwhm(0.5d);
        } else {
            return getSegment().fwhm(0.5d) / other.getSegment().fwhm(0.5d);
        }
    }

    public void setPeakShape(PeakShape peakShape) {
        this.peakShape = peakShape;
    }

    public PeakShape getPeakShape() {
        return peakShape;
    }

    public PrecursorIonType getDetectedIonType() {
        return detectedIonType;
    }

    public void setDetectedIonType(PrecursorIonType detectedIonType) {
        this.detectedIonType = detectedIonType;
    }

    public void setPossibleAdductTypes(Set<PrecursorIonType> possibleAdductTypes) {
        this.alternativeIonTypes = possibleAdductTypes;
    }

    public Set<PrecursorIonType> getPossibleAdductTypes() {
        return alternativeIonTypes;
    }

    public List<CorrelatedIon> getAdducts() {
        return adducts;
    }

    public List<CorrelatedIon> getInSourceFragments() {
        return inSourceFragments;
    }

    public double getMass() {
        return peak.getMzAt(segment.apex);
    }


    public long getRetentionTime() {
        return peak.getRetentionTimeAt(segment.apex);
    }

    public String toString() {
        return "MS/MS("+chargeState+") m/z = " + (msms==null ? "GAP FILLED" : ms2Scan.getPrecursor().getMass()) + ", apex = " + peak.getRetentionTimeAt(segment.getApexIndex())/60000d + " min";
    }

    public ArrayList<CompoundReport> getAdditionalInfos() {
        return additionalInfos;
    }

    public double getIntensity() {
        return peak.getIntensityAt(segment.apex);
    }

    public Quality getMsMsQuality() {
        return ms2Quality;
    }

    public CosineQuerySpectrum getMsMs() {
        return msms;
    }

    public Scan getMsMsScan() {
        return ms2Scan;
    }

    public void setChimerics(List<ChromatographicPeak> chimerics) {
        this.chimerics = chimerics;
    }

    public List<ChromatographicPeak> getChimerics() {
        return this.chimerics;
    }

    public void setChimericPollution(double chimericPollution) {
        this.chimericPollution = chimericPollution;
    }

    public double getChimericPollution() {
        return chimericPollution;
    }

    public boolean isCompound() {
        return true;
    }
}
