/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.gui.io.spectrum;

import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.ms.gui.ms_viewer.data.MSViewerDataModel;
import de.unijena.bioinf.ms.gui.ms_viewer.data.PeakInformation;

import java.util.TreeMap;

public class SpectrumContainer implements MSViewerDataModel {

    private final Spectrum<?> sourceSpectrum;
    private MutableMs2Spectrum modified = null;

    private TreeMap<Double, Integer> massToIndex;

    private SiriusMSViewerPeak[] peaks;

    protected double maxMass;

    public SpectrumContainer(Spectrum<?> sp) {
        this.sourceSpectrum = sp;

        //Zwischenschritt da Spektren denkbar bei denen es mehrere Peaks mit gleicher Masse geben kann...

        TreeMap<Double, SiriusMSViewerPeak> map = new TreeMap<>();
        double maxInt = 0;
        maxMass = 0d;
        for (int i = 0; i < sp.size(); i++) {
            SiriusMSViewerPeak peak = new SiriusMSViewerPeak();
            peak.setMass(sp.getMzAt(i));
            peak.setAbsoluteIntensity(sp.getIntensityAt(i));
            maxMass = Math.max(peak.getMass(), maxMass);
            if (maxInt < peak.getAbsoluteIntensity()) maxInt = peak.getAbsoluteIntensity();
            if (map.containsKey(peak.getMass())) {
                if (map.get(peak.getMass()).getAbsoluteIntensity() < peak.getAbsoluteIntensity()) {
                    map.put(peak.getMass(), peak);
                }
            } else {
                map.put(peak.getMass(), peak);
            }
        }

        peaks = new SiriusMSViewerPeak[map.size()];

        int index = 0;
        for (Double mass : map.keySet()) {
            peaks[index] = map.get(mass);
            peaks[index].setRelativeIntensity(peaks[index].getAbsoluteIntensity() / maxInt);
            index++;
        }

        massToIndex = new TreeMap<>();
        for (int i = 0; i < peaks.length; i++) {
            SiriusMSViewerPeak peak = peaks[i];
            massToIndex.put(peak.getMass(), i);
        }


    }

    public Spectrum<?> getSpectrum() {
        if (modified != null)
            return modified;
        return sourceSpectrum;
    }

    public MutableMs2Spectrum getModifiableSpectrum() {
        if (modified == null)
            modified = new MutableMs2Spectrum(sourceSpectrum);
        return modified;
    }

    public boolean isModified() {
        return modified != null;
    }


    @Override
    public int findIndexOfPeak(double mass, double tolerance) {
        double smallerMass = massToIndex.floorKey(mass);
        double biggerMass = massToIndex.ceilingKey(mass);
        if (smallerMass == mass) return massToIndex.get(mass);
        double diff1 = mass - smallerMass;
        double diff2 = biggerMass - mass;
        if (diff1 <= diff2) {
            if (diff1 > tolerance) return -1;
            else return massToIndex.get(smallerMass);
        } else {
            if (diff2 > tolerance) return -1;
            else return massToIndex.get(biggerMass);
        }
    }

    @Override
    public String getIonization(int index) {
        return null;
    }

    @Override
    public double getAbsoluteIntensity(int index) {
        return peaks[index].getAbsoluteIntensity();
    }

    @Override
    public int getIndexWithMass(double mass) {
        if (massToIndex.containsKey(mass)) {
            return massToIndex.get(mass);
        } else {
            return -1;
        }
    }

    @Override
    public PeakInformation getInformations(int index) {
        return peaks[index];
    }

    @Override
    public int[] getIsotopePeaks(int index) {
        return new int[0];
    }

    @Override
    public String getLabel() {
        return "";
    }

    @Override
    public double getMass(int index) {
        return peaks[index].getMass();
    }

    @Override
    public double getRelativeIntensity(int index) {
        return peaks[index].getRelativeIntensity();
    }


    @Override
    public double minMz() {
        return 0;
    }

    @Override
    public double maxMz() {
        return maxMass;
    }

    @Override
    public int getSize() {
        return peaks.length;
    }

    @Override
    public boolean isIsotope(int arg0) {
        return false;
    }

    @Override
    public boolean isMarked(int arg0) {
        return false;
    }

    @Override
    public boolean isPlusZeroPeak(int arg0) {
        return false;
    }

    @Override
    public String getMolecularFormula(int arg0) {
        return null;
    }

    @Override
    public boolean isImportantPeak(int index) {
        return false;
    }

    @Override
    public boolean isUnimportantPeak(int arg0) {
        // TODO Auto-generated method stub
        return false;
    }


}
