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
package de.unijena.bioinf.sirius.elementpred;

import de.unijena.bioinf.ChemistryBase.chem.*;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.MassDecomposer.Chemistry.MassToFormulaDecomposer;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class PredictFromMs2 implements Judge {

    private static final String[] REPORTER_IONS = new String[]{"C7H5Cl", "C8H6ClN", "C8H4ClN", "C9H5Cl", "C6H3Cl", "C7H3Cl", "C9H8ClN", "C8H5Cl", "C7H5F", "C9H5F", "C5H3Cl", "C8H7Cl", "C9H7Cl", "C8H8ClN", "C13H8ClN", "C10H9FO", "C11H7FN2O", "C6H3F", "C7H3FO", "C8H5F", "C7H4Cl2", "C7H4ClN", "C7H3ClO", "C6H4ClN"};

    private final static String[] REPORTER_LOSSES = new String[]{"HCl", "Cl", "Br", "HBr", "CClO", "CH3Cl", "HF", "CHClO", "C6H5Cl", "C2H3F", "CF3", "CHFO", "HI", "I"} ;

private final MassToFormulaDecomposer decomposer;
    private final MolecularFormula[] reporterIons, reporterLosses;
    private final double[] reporterIonMasses, reporterLossMasses;
    private final double maxDiff;

    public PredictFromMs2() {
        this.decomposer = new MassToFormulaDecomposer(new ChemicalAlphabet(MolecularFormula.parse("CHNOPSClBrIF").elementArray()));
        this.reporterIons = new MolecularFormula[REPORTER_IONS.length];
        this.reporterLosses = new MolecularFormula[REPORTER_LOSSES.length];
        this.reporterIonMasses = new double[REPORTER_IONS.length];
        double maxMass = 0d;
        for (int k=0; k < REPORTER_LOSSES.length; ++k) {
            reporterLosses[k] = MolecularFormula.parse(REPORTER_LOSSES[k]);
            maxMass = Math.max(reporterLosses[k].getMass(), maxMass);
        }
        for (int k=0; k < reporterIons.length; ++k) {
            reporterIons[k] = MolecularFormula.parse(REPORTER_IONS[k]);
        }
        this.maxDiff = maxMass+0.5;
        Arrays.sort(reporterLosses);
        Arrays.sort(reporterIons);
        this.reporterLossMasses = new double[reporterIons.length];
        for (int k=0; k < reporterLosses.length; ++k)
            reporterLossMasses[k] = reporterLosses[k].getMass();
        for (int k=0; k < reporterIons.length; ++k)
            reporterIonMasses[k] = reporterIons[k].getMass();
    }


    @Override
    public void vote(TObjectIntHashMap<Element> votes, Ms2Experiment experiment) {
        voteForReporterIons(votes, experiment);
    }

    private void voteForReporterIons(TObjectIntHashMap<Element> votes, Ms2Experiment experiment) {
        final PeriodicTable table = PeriodicTable.getInstance();
        final Element Cl = table.getByName("Cl"), Br = table.getByName("Br"), I = table.getByName("I"), F = table.getByName("F");
        final Element[] halogens = new Element[]{Cl,Br,I,F};
        final Deviation allowedDev =
                experiment.getAnnotationOrDefault(MS2MassDeviation.class).allowedMassDeviation.divide(2);
        // search for reporter ion
        final Set<MolecularFormula> detectedFragments = new HashSet<MolecularFormula>();
        final Set<MolecularFormula> detectedLosses = new HashSet<MolecularFormula>();
        final TObjectIntHashMap<MolecularFormula> detectedSubsets = new TObjectIntHashMap<MolecularFormula>();
        for (Ms2Spectrum<? extends Peak> ms2spec : experiment.getMs2Spectra()) {
            // only look at the 40 most intensive peaks
            final SimpleMutableSpectrum sms = new SimpleMutableSpectrum(ms2spec);
            Spectrums.sortSpectrumByDescendingIntensity(sms);
            final SimpleMutableSpectrum spec = new SimpleMutableSpectrum(40);
            for (int k=0; k < Math.min(40, sms.size()); ++k) {
                spec.addPeak(sms.getMzAt(k), sms.getIntensityAt(k));
            }
            Spectrums.sortSpectrumByMass(spec);
            final Ionization ion = ms2spec.getIonization()==null ? experiment.getPrecursorIonType().getIonization() : ms2spec.getIonization();
            for (int k = 0; k < spec.size(); ++k) {
                final double mz = spec.getMzAt(k);
                final double neutralMass = ion.subtractFromMass(mz);
                // search for fragments
                final int index = searchForMass(neutralMass, reporterIonMasses, allowedDev);
                if (index >= 0) {
                    detectedFragments.add(reporterIons[index]);
                }
                // search for losses
                for (int l = k + 1; l < spec.size(); ++l) {
                    final double mzdiff = spec.getMzAt(l) - mz;
                    if (mzdiff > maxDiff) break;
                    final int index2 = searchForMass(mzdiff, reporterLossMasses, allowedDev);
                    if (index2 >= 0) {
                        detectedLosses.add(reporterLosses[index2]);
                    }
                }
            }

            // now evaluate results:
            // Cl, Br, I, F

            final int[] votesNum = new int[]{0,0,0,0};
            // fragments
            for (MolecularFormula f : detectedFragments) {
                for (int i=0; i < halogens.length; ++i)
                    if (f.numberOf(halogens[i]) > 0)
                        ++votesNum[i];
            }
            for (int i=0; i < halogens.length; ++i) {
                final int value=Math.min(2, votesNum[i]);
                votes.adjustOrPutValue(halogens[i], value,value);
            }
            Arrays.fill(votesNum, 0);

            // losses
            for (MolecularFormula f : detectedLosses) {
                for (int i=0; i < halogens.length; ++i)
                    if (f.numberOf(halogens[i]) > 0)
                        ++votesNum[i];
            }
            for (int i=0; i < halogens.length; ++i) {
                final int value = Math.max(0, Math.min(2, votesNum[i]-3));
                votes.adjustOrPutValue(halogens[i], value, value);
            }
            Arrays.fill(votesNum, 0);

        }
    }

    private int searchForMass(final double mz, final double[] masses, Deviation allowedDev) {
        int k = Arrays.binarySearch(masses, mz);
        if (k>=0) return k;
        int i = -(k+1);
        for (int j=Math.max(0,i-1); j < Math.min(masses.length, i+1); ++j) {
            if (allowedDev.inErrorWindow(masses[j], mz)) {
                return j;
            }
        }
        return -1;
    }
}
