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
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.middleware.formulas.model;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
@JsonIdentityReference(alwaysAsId = false)
public class FragmentNode {
    int id;
    String molecularFormula;
    String ionType;
    double massDeviationDa;
    double massErrorPpm;
    double score;

    double intensity;
    double mz;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getMolecularFormula() {
        return molecularFormula;
    }

    public void setMolecularFormula(String molecularFormula) {
        this.molecularFormula = molecularFormula;
    }

    public String getIonType() {
        return ionType;
    }

    public void setIonType(String ionType) {
        this.ionType = ionType;
    }

    public double getMassDeviationDa() {
        return massDeviationDa;
    }

    public void setMassDeviationDa(double massDeviationDa) {
        this.massDeviationDa = massDeviationDa;
    }

    public double getMassErrorPpm() {
        return massErrorPpm;
    }

    public void setMassErrorPpm(double massErrorPpm) {
        this.massErrorPpm = massErrorPpm;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public double getIntensity() {
        return intensity;
    }

    public void setIntensity(double intensity) {
        this.intensity = intensity;
    }

    public double getMz() {
        return mz;
    }

    public void setMz(double mz) {
        this.mz = mz;
    }
}
