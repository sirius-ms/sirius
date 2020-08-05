
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

package de.unijena.bioinf.IsotopePatternAnalysis;
import de.unijena.bioinf.ChemistryBase.chem.*;
import de.unijena.bioinf.ChemistryBase.chem.utils.IsotopicDistribution;
import de.unijena.bioinf.ChemistryBase.ms.Normalization;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.SimplePeak;
import de.unijena.bioinf.ChemistryBase.ms.utils.ArrayWrapperSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.ChargedSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;

import java.util.ArrayList;
import java.util.List;

@Deprecated
public class PatternGenerator {

	private final IsotopicDistribution distribution;
	private final Normalization mode;
	private final Ionization ion;

	public PatternGenerator(IsotopicDistribution dist, Ionization ion, Normalization mode) {
		this.distribution = dist;
		this.ion = ion;
		this.mode = mode;
		if (ion == null || mode == null || distribution == null) throw new NullPointerException("Expect non null parameters");
	}

	public PatternGenerator(Ionization ion) {
		this(PeriodicTable.getInstance().getDistribution(), ion, Normalization.Sum);
	}

	public PatternGenerator(Ionization ion, Normalization mode) {
		this(PeriodicTable.getInstance().getDistribution(), ion, mode);
	}

	public PatternGenerator(Normalization mode) {
		this(PeriodicTable.getInstance().getDistribution(), new Charge(1), mode);
	}

	public PatternGenerator() {
		this(Normalization.Max);
	}

    private static boolean isBitSet(int input, int pos) {
        if (pos >= Integer.SIZE) {
            return false;
        }
        return (input & (1 << pos)) > 0;
    }

    public ChargedSpectrum generatePattern(MolecularFormula formula) {
        return generatePattern(addAtomsToFormula(formula), Integer.MAX_VALUE);
    }

    public ChargedSpectrum generatePattern(MolecularFormula formula, int numberOfPeaks) {
        return peakList2Pattern(formula, foldFormula(addAtomsToFormula(formula), numberOfPeaks));
    }

    public ChargedSpectrum generatePatternWithTreshold(MolecularFormula formula, double treshold) {
        return peakList2Pattern(formula, foldFormula(addAtomsToFormula(formula), treshold));
    }

    protected MolecularFormula addAtomsToFormula(MolecularFormula formula) {
        final MolecularFormula f = ion.getAtoms();
        return (f == null) ? formula : f.add(formula);
    }

    protected ChargedSpectrum peakList2Pattern(MolecularFormula formula, List<Peak> peaks) {
        final double[] mzs = new double[peaks.size()];
        final double[] ints = new double[peaks.size()];
        final MolecularFormula adductAtoms = ion.getAtoms();
        // adductMass is already added to the peaks mass. ion.addToMass would add it twice, therefore, we have
        // to subtract it. ion.addToMass adds also the mass of protons and electrons which are not contained
        // in the adductAtoms mass.
        final double mass = formula.getIntMass() + (adductAtoms == null ? 0 : adductAtoms.getIntMass() - adductAtoms.getMass());
        for (int i = 0; i < peaks.size(); ++i) {
            mzs[i] = ion.addToMass(i + peaks.get(i).getMass() + mass);
            ints[i] = peaks.get(i).getIntensity();
        }
        final ArrayWrapperSpectrum s = new ArrayWrapperSpectrum(mzs, ints);
        Spectrums.normalize(s, mode);
        Spectrums.sortSpectrumByMass(s);
        return new ChargedSpectrum(mzs, ints, ion);
    }

	/**
     * look out: after folding the returned isotope distribution only contains the difference from the nominalmass
     * @param formula formula to be fold
     * @return list of isotopes of the folded formula
     */
    protected List<Peak> foldFormula(MolecularFormula formula) {
        return foldFormula(formula, Integer.MAX_VALUE);
    }

	/**
     * look out: after folding the returned isotope distribution only contains the difference from the nominalmass
     * @param formula formula to be fold
     * @param treshold threshold for smallest peak
     * @return list of isotopes of the folded formula
     */
    protected List<Peak> foldFormula(MolecularFormula formula, double treshold) {
        final int limit = 10; // TODO: sauberer lösen
        List<Peak> candidateDistribution = null;

		for(Element e : formula){
            final Isotopes iso = getIsotope(e);
            List<Peak> modIsoDist = new ArrayList<Peak>(iso.getNumberOfIsotopes());
            final int monoIsotopicMass = iso.getIntegerMass(0);
            int maxMass =iso.getIntegerMass(iso.getNumberOfIsotopes()-1)-monoIsotopicMass;
            final int n = Math.max(iso.getNumberOfIsotopes()-1, maxMass);
            int k=0;
            for (int i = 0; i <= n; i++){
                int diff =  iso.getIntegerMass(k) - monoIsotopicMass;
                while (diff > i) {modIsoDist.add(new SimplePeak(0,0)); ++i;}
                // Florian says: minus i is because the i-th isotope nominal mass is elemental nominal mass plus i!
                Peak peak = new SimplePeak(iso.getMass(k)-e.getIntegerMass()-i, iso.getAbundance(k));
                modIsoDist.add(peak);
                ++k;
            }

			//get the reverse binary string of the quantity of an element
			int exp = formula.numberOf(e),
					expLength = Integer.SIZE - Integer.numberOfLeadingZeros(exp);

			//folding of one element
			List<Peak> helper = modIsoDist;
			List<Peak> list = null;

			//if the first number of the binary exponent is 1,
			if(isBitSet(exp, 0)){
				list = helper;
			}

			//helper list is always folded twice
			//list is just folded if binary exponent is 1 at the current position
			for(int i=1;i<expLength;i++){
				helper = fold(helper,helper, limit);
				if(isBitSet(exp, i)){
					list = fold(list,helper, limit);
				}
			}
			// folding all elements to the candidate peaks
			// fold returns only list if candidatePeaks is still null
			candidateDistribution = fold(candidateDistribution, list, limit);
		}
        for (int k = candidateDistribution.size() - 1; k >= 0; --k) {
            if (candidateDistribution.get(k).getIntensity() < treshold) {
                candidateDistribution.remove(k);
            } else {
                break;
            }
        }
        return candidateDistribution;
	}

    /**
     * look out: after folding the returned isotope distribution only contains the difference from the nominalmass
     *
     * @param formula formula to be fold
     * @param limit   max number of isotopic peaks
     * @return list of isotopes of the folded formula
     */
    protected List<Peak> foldFormula(MolecularFormula formula, int limit) {
        List<Peak> candidateDistribution = null;

        for (Element e : formula) {
            final Isotopes iso = getIsotope(e);
            List<Peak> modIsoDist = new ArrayList<Peak>(iso.getNumberOfIsotopes());
            final int monoIsotopicMass = iso.getIntegerMass(0);
            int maxMass = iso.getIntegerMass(iso.getNumberOfIsotopes() - 1) - monoIsotopicMass;
            final int n = Math.max(iso.getNumberOfIsotopes() - 1, maxMass);
            int k = 0;
            for (int i = 0; i <= n; i++) {
                int diff = iso.getIntegerMass(k) - monoIsotopicMass;
                while (diff > i) {
                    modIsoDist.add(new SimplePeak(0, 0));
                    ++i;
                }
                // Florian says: minus i is because the i-th isotope nominal mass is elemental nominal mass plus i!
                Peak peak = new SimplePeak(iso.getMass(k) - e.getIntegerMass() - i, iso.getAbundance(k));
                modIsoDist.add(peak);
                ++k;
            }

            //get the reverse binary string of the quantity of an element
            int exp = formula.numberOf(e),
                    expLength = Integer.SIZE - Integer.numberOfLeadingZeros(exp);

            //folding of one element
            List<Peak> helper = modIsoDist;
            List<Peak> list = null;

            //if the first number of the binary exponent is 1,
            if (isBitSet(exp, 0)) {
                list = helper;
            }

            //helper list is always folded twice
            //list is just folded if binary exponent is 1 at the current position
            for (int i = 1; i < expLength; i++) {
                helper = fold(helper, helper, limit);
                if (isBitSet(exp, i)) {
                    list = fold(list, helper, limit);
                }
            }
            // folding all elements to the candidate peaks
            // fold returns only list if candidatePeaks is still null
            candidateDistribution = fold(candidateDistribution, list, limit);
        }
        return candidateDistribution;
    }

    private Isotopes getIsotope(Element e) {
        Isotopes iso = distribution.getIsotopesFor(e);
        if (iso == null) {
            iso = PeriodicTable.getInstance().getDistribution().getIsotopesFor(e);
            if (iso == null) throw new RuntimeException("No known isotopes for " + e);
        }
        return iso;
    }

	/*
	 *
	 * @param list1 first list to be fold
	 * @param list2 second list to be fold
	 * @param limit threshold for smallest peak
	 * @return null, if both lists are empty, just one list if the other is empty, the folding of both lists else
	 */
    /*
	protected List<Peak> fold(List<Peak> list1, List<Peak> list2, double limit){

		//tests if one of the lists is null, if so, it returns the other list or null
		if(list1 == null && list2 == null) return null;
		if(list1 == null) return list2;
		if(list2 == null) return list1;

		//folding 2 spectra with n and m non-monoisotopic peaks results in a new spectra with n+m non-monoisootopic peaks
		int len = (list1.size() + list2.size())-1;

		List<Peak> result = new ArrayList<Peak>(len);
		double m, mass, intensity;

		double maxint = Double.NEGATIVE_INFINITY;
		for(int n=0;n<len;n++){
			mass = 0;
			intensity = 0;
			//filling the lists
			while (list1.size()<n+1){
				list1.add(new SimplePeak(0,0));
			}
			while (list2.size()<n+1){
				list2.add(new SimplePeak(0,0));
			}
			for(int k=0;k<=n;k++){
				intensity += list1.get(k).getIntensity() * list2.get(n-k).getIntensity();
				mass += list1.get(k).getIntensity() * list2.get(n-k).getIntensity() * (list1.get(k).getMass() + list2.get(n-k).getMass());
			}
			if (intensity < maxint) {
				if (intensity < limit * maxint) {
                    System.out.println(intensity + " : " + limit);
                    break;               // TO/DO: bei manchen Isotopenmustern könnte das schief gehen
                }
			} else {
				maxint = intensity;
			}
			m = intensity != 0 ? mass/intensity : 0.0;
			result.add(new SimplePeak(m, intensity));
		}
		return result;
	}
*/

    /**
     * @param list1 first list to be fold
     * @param list2 second list to be fold
     * @param limit max number of isotopic peaks
     * @return null, if both lists are empty, just one list if the other is empty, the folding of both lists else
     */
    protected List<Peak> fold(List<Peak> list1, List<Peak> list2, int limit) {

        //tests if one of the lists is null, if so, it returns the other list or null
        if (list1 == null && list2 == null) return null;
        if (list1 == null) return list2;
        if (list2 == null) return list1;

        //folding 2 spectra with n and m non-monoisotopic peaks results in a new spectra with n+m non-monoisootopic peaks
        int len = Math.min((list1.size() + list2.size()) - 1, limit);
        //filling the lists
        while (list1.size() < len) {
            list1.add(new SimplePeak(0, 0));
        }
        while (list2.size() < len) {
            list2.add(new SimplePeak(0, 0));
        }

        List<Peak> result = new ArrayList<Peak>(len);
        double m, mass, intensity;

        for (int n = 0; n < len; n++) {
            mass = 0;
            intensity = 0;
            for (int k = 0; k <= n; k++) {
                intensity += list1.get(k).getIntensity() * list2.get(n - k).getIntensity();
                mass += list1.get(k).getIntensity() * list2.get(n - k).getIntensity() * (list1.get(k).getMass() + list2.get(n - k).getMass());
            }
            m = intensity != 0 ? mass / intensity : 0.0;
            result.add(new SimplePeak(m, intensity));
        }
		return result;
	}

}
