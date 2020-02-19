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

package de.unijena.bioinf.ChemistryBase.ms;

import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ms.annotations.TreeAnnotation;

/**
 * An annotated peak after tree computation is done. Can be used as FragmentAnnotation in FTree
 */
public final class AnnotatedPeak implements TreeAnnotation  {

    /**
     * The molecular formula that is assigned to this peak
     */
    private final MolecularFormula molecularFormula;

    /**
     * the mass of the peak after peak merging
     */
    private final double mass;

    /**
     * the mass of the peak after recalibration
     */
    private final double recalibratedMass;

    /**
     * the relative intensity of the merged peak
     */
    private final double relativeIntensity;

    /**
     * the ionization of the peak
     */
    private final Ionization ionization;

    /**
     * the original peaks that were merged into this peak (maximal one per MS/MS spectrum)
     */
    private final Peak[] originalPeaks;

    /**
     * the collision energies of the original peaks. Should have the same size as the originalPeaks array. The i-th
     * collision energy belongs to the i-th peak.
     */
    private final CollisionEnergy[] collisionEnergies;

    /**
     * The index of the MS/MS spectrum this peak is derived from
     */
    private final int[] spectrumIds;

    private final static Peak[] NO_PEAKS = new Peak[0];
    private final static CollisionEnergy[] NO_ENERGY = new CollisionEnergy[0];
    private final static int[] NO_SPECTRUM = new int[0];

    private final static AnnotatedPeak NO_PEAK = new AnnotatedPeak(MolecularFormula.emptyFormula(), Double.NaN, Double.NaN, 0d, null, NO_PEAKS, new CollisionEnergy[0], new int[0]);

    public static AnnotatedPeak artificial(MolecularFormula formula, Ionization ionization) {
        return artificial(formula, ionization, ionization.addToMass(formula.getMass()));
    }
    public static AnnotatedPeak artificial(MolecularFormula formula, Ionization ionization, double mass) {
        return new AnnotatedPeak(formula, mass, mass, 0d, ionization, NO_PEAKS, NO_ENERGY, NO_SPECTRUM);
    }

    public static AnnotatedPeak none() {
        return NO_PEAK;
    }

    public boolean isArtificial() {
        return originalPeaks.length==0;
    }

    public AnnotatedPeak(MolecularFormula getFormula, double getMass, double recalibratedMass, double relativeIntensity, Ionization ionization, Peak[] originalPeaks, CollisionEnergy[] collisionEnergies, int[] spectrumIds) {
        this.molecularFormula = getFormula;
        this.mass = getMass;
        this.recalibratedMass = recalibratedMass;
        this.relativeIntensity = relativeIntensity;
        this.ionization = ionization;
        this.originalPeaks = originalPeaks;
        this.collisionEnergies = collisionEnergies;
        this.spectrumIds = spectrumIds;
    }

    /**
     * returns the mass error of this fragment after recalibration.
     * Returns NULL_DEVIATION if the fragment does not corresponds to any peak.
     */
    public Deviation getRecalibratedMassError() {
        if (isMeasured())
            return Deviation.fromMeasurementAndReference(recalibratedMass, ionization.addToMass(molecularFormula.getMass()));
        else return Deviation.NULL_DEVIATION;
    }
    /**
     * returns the mass error of this fragment. Returns NULL_DEVIATION if the fragment does not corresponds to any peak.
     */
    public Deviation getMassError() {
        if (isMeasured())
            return Deviation.fromMeasurementAndReference(mass, ionization.addToMass(molecularFormula.getMass()));
        else return Deviation.NULL_DEVIATION;
    }

    public int[] getSpectrumIds() {
        return spectrumIds;
    }

    public MolecularFormula getMolecularFormula() {
        return molecularFormula;
    }

    public double getMass() {
        return mass;
    }

    public double getRecalibratedMass() {
        return recalibratedMass;
    }

    public double getRelativeIntensity() {
        return relativeIntensity;
    }

    public Ionization getIonization() {
        return ionization;
    }

    public Peak[] getOriginalPeaks() {
        return originalPeaks;
    }

    public CollisionEnergy[] getCollisionEnergies() {
        return collisionEnergies;
    }

    public AnnotatedPeak withFormula(MolecularFormula newFormula) {
        return new AnnotatedPeak(newFormula, mass, recalibratedMass, relativeIntensity, ionization, originalPeaks, collisionEnergies, spectrumIds);
    }

    /**
     * @return true if peak does not correspond to a real peak in the spectrum
     */
    public boolean isSynthetic() {
        return relativeIntensity <= 0d;
    }

    public boolean isMeasured() {
        return relativeIntensity > 0d;
    }

    /**
     *
     * @return true if there does not exist any peak annotation
     */
    public boolean isNoPeak() {
        return Double.isNaN(mass);
    }

    public AnnotatedPeak withIonization(Ionization ion) {
        return new AnnotatedPeak(molecularFormula, mass, recalibratedMass, relativeIntensity, ion, originalPeaks, collisionEnergies, spectrumIds);
    }

    public double getMaximalIntensity() {
        double m = 0d;
        for (Peak p : originalPeaks) {
            if (p!=null) m = Math.max(p.getIntensity(), m);
        }
        return m;
    }
    public double getSumedIntensity() {
        double m = 0d;
        for (Peak p : originalPeaks) {
            if (p!=null) m += p.getIntensity();
        }
        return m;
    }
}
