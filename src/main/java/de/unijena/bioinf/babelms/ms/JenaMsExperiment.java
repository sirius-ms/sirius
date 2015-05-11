/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2015 Kai Dührkop
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.unijena.bioinf.babelms.ms;

import de.unijena.bioinf.ChemistryBase.chem.Charge;
import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.MeasurementProfile;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;

import java.util.List;

public class JenaMsExperiment implements Ms2Experiment {

    private List<JenaMs2Spectrum> ms2Spectra;
    private List<JenaMsSpectrum> ms1Spectra;
    private Ionization ionization;
    private double ionMass;
    private int charge;
    private MolecularFormula compoundFormula;
    private String compoundName;
    private MeasurementProfile measurementProfile;

    JenaMsExperiment(String compoundName, MolecularFormula compoundFormula, double ionMass, int charge, Ionization ionization, List<JenaMsSpectrum> ms1Spectra, List<JenaMs2Spectrum> ms2Spectra) {
        this.compoundName = compoundName;
        this.compoundFormula = compoundFormula;
        this.ionMass = ionMass;
        this.charge = charge;
        this.ionization = ionization;
        this.ms1Spectra = ms1Spectra;
        this.ms2Spectra = ms2Spectra;
    }

    public String getCompoundName() {
        return compoundName;
    }

    public int getCharge() {
        return charge;
    }

    @Override
    public List<JenaMs2Spectrum> getMs2Spectra() {
        return ms2Spectra;
    }

    @Override
    public List<JenaMsSpectrum> getMs1Spectra() {
        return ms1Spectra;
    }

    @Override
    public JenaMsSpectrum getMergedMs1Spectrum() {
        return null;
    }

    @Override
    public double getIonMass() {
        return ionMass;
    }

    @Override
    public double getRetentionTime() {
        return 0;
    }

    public void setMeasurementProfile(MeasurementProfile measurementProfile) {
        this.measurementProfile = measurementProfile;
    }

    @Override
    public MeasurementProfile getMeasurementProfile() {
        return measurementProfile;
    }

    @Override
    public double getMoleculeNeutralMass() {
        return compoundFormula != null ? compoundFormula.getMass() : Double.NaN;
    }

    @Override
    public MolecularFormula getMolecularFormula() {
        return compoundFormula;
    }

    @Override
    public Ionization getIonization() {
        return ionization!=null ? ionization : new Charge(charge);
    }
}
