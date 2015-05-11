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

import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.*;

import java.util.List;

public class JenaGCMSExperiment implements Ms2Experiment {

    private List<JenaMsSpectrum> ms1Spectra;
    private Spectrum<Peak> mergedMs1Spectrum;
    private Ionization ionization;
    private double ionMass;
    private MolecularFormula compoundFormula;
    private String compoundName;
    private MeasurementProfile measurementProfile;
    private double collisionEnergy;


    public JenaGCMSExperiment(String compoundName, MolecularFormula compoundFormula, double ionMass, Ionization ionization, List<JenaMsSpectrum> ms1Spectra, double collisionEnergy) {
        this.compoundName = compoundName;
        this.compoundFormula = compoundFormula;
        this.ionMass = ionMass;
        this.ionization = ionization;
        this.ms1Spectra = ms1Spectra;
        this.collisionEnergy = collisionEnergy;
    }

    public String getCompoundName() {
        return compoundName;
    }

    public double getCollisionEnergy() {
        return collisionEnergy;
    }

    @Override
    public List<? extends Ms2Spectrum<? extends Peak>> getMs2Spectra() {
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

    @Override
    public double getMoleculeNeutralMass() {
        return compoundFormula != null ? compoundFormula.getMass() : Double.NaN;
    }

    @Override
    public MolecularFormula getMolecularFormula() {
        return compoundFormula;
    }

    public void setMeasurementProfile(MeasurementProfile measurementProfile) {
        this.measurementProfile = measurementProfile;
    }

    @Override
    public MeasurementProfile getMeasurementProfile() {
        return measurementProfile;
    }

    @Override
    public Ionization getIonization() {
        return ionization;
    }

    @Override
    public List<? extends Spectrum<Peak>> getMs1Spectra() {
        return ms1Spectra;
    }

    @Override
    public Spectrum<Peak> getMergedMs1Spectrum() {
        return mergedMs1Spectrum;
    }

    public void setMergedMs1Spectrum(Spectrum<Peak> mergedMs1Spectrum) {
        this.mergedMs1Spectrum = mergedMs1Spectrum;
    }



}
