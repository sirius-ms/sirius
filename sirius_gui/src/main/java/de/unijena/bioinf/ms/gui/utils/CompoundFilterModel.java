package de.unijena.bioinf.ms.gui.utils;/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2021 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
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

import de.unijena.bioinf.lcms.LCMSCompoundSummary;
import de.unijena.bioinf.ms.frontend.core.SiriusPCS;

import java.util.Arrays;

/**
 * This model stores the filter criteria for a compound list
 */
public class CompoundFilterModel implements SiriusPCS {
    private final MutableHiddenChangeSupport pcs = new MutableHiddenChangeSupport(this, true);

    /*
    currently selected values
     */
    private double currentMinMz;
    private double currentMaxMz;
    private double currentMinRt;
    private double currentMaxRt;

    //
    private boolean[] peakShapeQualities = new boolean[]{true,true,true};

    private LipidFilter lipidFilter = LipidFilter.ALL;

    /*
    min/max possible values
     */
    private final double minMz;
    private final double maxMz;
    private final double minRt;
    private final double maxRt;


    public CompoundFilterModel() {
        this(0, 5000d, 0, 10000d);
    }


    /**
     * the filter model is initialized with the min / max possible values
     * MAX VALUES SHOULD BE USED FOR DISPLAY ONLY. AND IF SELECTED VALUES EQUAL THE MAXIMUM, INFINITY SHOULD BE ASSUMED, see {@link CompoundFilterMatcher} and is[...]Active() methods.
     * @param minMz
     * @param maxMz
     * @param minRt
     * @param maxRt
     */
    public CompoundFilterModel(double minMz, double maxMz, double minRt, double maxRt) {
        this.currentMinMz = minMz;
        this.currentMaxMz = maxMz;
        this.currentMinRt = minRt;
        this.currentMaxRt = maxRt;

        this.minMz = minMz;
        this.maxMz = maxMz;
        this.minRt = minRt;
        this.maxRt = maxRt;
    }

    public boolean isPeakShapeFilterEnabled() {
        for (boolean val : peakShapeQualities) {
            if (!val) return true;
        }
        return false;
    }

    public boolean isLipidFilterEnabled() {
        return lipidFilter != LipidFilter.ALL;
    }

    public LipidFilter getLipidFilter() {
        return lipidFilter;
    }

    public void setLipidFilter(LipidFilter value) {
        LipidFilter oldValue = lipidFilter;
        lipidFilter = value;
        pcs.firePropertyChange("setLipidFilter", oldValue, value);
    }

    public void setPeakShapeQuality(LCMSCompoundSummary.Quality quality, boolean value) {
        boolean oldValue = peakShapeQualities[quality.ordinal()];
        peakShapeQualities[quality.ordinal()] = value;
        pcs.firePropertyChange("setPeakShapeQuality", oldValue, value);
    }
    public void setPeakShapeQuality(int quality, boolean value) {
        boolean oldValue = peakShapeQualities[quality];
        peakShapeQualities[quality] = value;
        pcs.firePropertyChange("setPeakShapeQuality", oldValue, value);
    }
    public boolean getPeakShapeQuality(LCMSCompoundSummary.Quality quality) {
        return peakShapeQualities[quality.ordinal()];
    }
    public boolean getPeakShapeQuality(int quality) {
        return peakShapeQualities[quality];
    }

    public void setCurrentMinMz(double currentMinMz) {
        if (currentMinMz < minMz) throw new IllegalArgumentException("current value out of range: "+currentMinMz);
        double oldValue = this.currentMinMz;
        this.currentMinMz = currentMinMz;
        pcs.firePropertyChange("setMinMz", oldValue, currentMinMz);
    }

    public double getCurrentMinMz() {
        return currentMinMz;
    }

    public double getCurrentMaxMz() {
        return currentMaxMz;
    }

    public void setCurrentMaxMz(double currentMaxMz) {
        if (currentMaxMz > maxMz) throw new IllegalArgumentException("current value out of range: "+currentMaxMz);
        double oldValue = this.currentMaxMz;
        this.currentMaxMz = currentMaxMz;
        pcs.firePropertyChange("setMaxMz", oldValue, currentMaxMz);
    }

    public double getCurrentMinRt() {
        return currentMinRt;
    }

    public void setCurrentMinRt(double currentMinRt) {
        if (currentMinRt < minRt) throw new IllegalArgumentException("current value out of range: "+currentMinRt);
        double oldValue = this.currentMinRt;
        this.currentMinRt = currentMinRt;
        pcs.firePropertyChange("setMinRt", oldValue, currentMinRt);

    }

    public double getCurrentMaxRt() {
        return currentMaxRt;
    }

    public void setCurrentMaxRt(double currentMaxRt) {
        if (currentMaxRt > maxRt) throw new IllegalArgumentException("current value out of range: "+currentMaxRt);
        double oldValue = this.currentMaxRt;
        this.currentMaxRt = currentMaxRt;
        pcs.firePropertyChange("setMaxRt", oldValue, currentMaxRt);

    }

    public double getMinMz() {
        return minMz;
    }

    public double getMaxMz() {
        return maxMz;
    }

    public double getMinRt() {
        return minRt;
    }

    public double getMaxRt() {
        return maxRt;
    }

    /**
     * filter options are active. that means selected values differ from absolute min/max
     * @return
     */
    public boolean isActive(){
        if (currentMinMz != minMz || currentMaxMz != maxMz ||
                currentMinRt != minRt || currentMaxRt != maxRt) return true;
        if (isPeakShapeFilterEnabled() || isLipidFilterEnabled()) return true;
        return false;
    }

    public boolean isMaxMzFilterActive() {
        return currentMaxMz != maxMz;
    }

    public boolean isMaxRtFilterActive() {
        return currentMaxRt != maxRt;
    }

    @Override
    public HiddenChangeSupport pcs() {
        return pcs;
    }

    public void resetFilter() {
        //trigger events
        setCurrentMinMz(minMz);
        setCurrentMaxMz(maxMz);
        setCurrentMinRt(minRt);
        setCurrentMaxRt(maxRt);
        Arrays.fill(peakShapeQualities,true);
        lipidFilter = LipidFilter.ALL;
    }


    public enum LipidFilter {
        ALL, ANY_LIPID_CLASS_DETECTED, NO_LIPID_CLASS_DETECTED
    }
}
