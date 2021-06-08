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

import de.unijena.bioinf.ms.frontend.core.SiriusPCS;

public class CompoundFilterModel implements SiriusPCS {
    private final MutableHiddenChangeSupport pcs = new MutableHiddenChangeSupport(this, true);

    private double currentMinMz;
    private double currentMaxMz;
    private double currentMinRt;
    private double currentMaxRt;

    private final double minMz;
    private final double maxMz;
    private final double minRt;
    private final double maxRt;


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

    public void setCurrentMinMz(double currentMinMz) {
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
        double oldValue = this.currentMaxMz;
        this.currentMaxMz = currentMaxMz;
        pcs.firePropertyChange("setMaxMz", oldValue, currentMaxMz);
    }

    public double getCurrentMinRt() {
        return currentMinRt;
    }

    public void setCurrentMinRt(double currentMinRt) {
        double oldValue = this.currentMinRt;
        this.currentMinRt = currentMinRt;
        pcs.firePropertyChange("setMinRt", oldValue, currentMinRt);

    }

    public double getCurrentMaxRt() {
        return currentMaxRt;
    }

    public void setCurrentMaxRt(double currentMaxRt) {
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
     * filter options are active
     * @return
     */
    public boolean isActive(){
        if (currentMinMz != minMz || currentMaxMz != maxMz ||
                currentMinRt != minRt || currentMaxRt != maxRt) return true;
        return false;
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
    }
}
