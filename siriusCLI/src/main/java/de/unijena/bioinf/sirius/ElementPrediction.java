package de.unijena.bioinf.sirius;

import de.unijena.bioinf.ChemistryBase.chem.*;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.Peak;

import java.util.*;

/**
 * temporary class for predicting elements. Should be replaced by Marvins ML as soon as the
 * paper is published
 */
public class ElementPrediction {

    private static final String[] REPORTER_IONS = new String[]{"Br", "C10H7F", "C10H9FO", "C11H7FN2O", "C13H8ClN", "C14H9ClN", "C2H3Cl", "C2H3F", "C3HCl", "C5H2Cl2", "C5H3Cl", "C6H3Cl", "C6H3F", "C6H4ClN", "C6H5Cl", "C6H6ClN", "C7H2ClN", "C7H3Cl", "C7H3ClO", "C7H3FO", "C7H4Cl2", "C7H4ClN", "C7H5Cl", "C7H5F", "C8H3ClN2", "C8H4ClN", "C8H5Cl", "C8H5ClN", "C8H5F", "C8H6ClN", "C8H7Cl", "C8H8ClN", "C9H5Cl", "C9H5F", "C9H6ClN", "C9H7Cl", "C9H8ClN", "CClN", "CClO", "CF3", "CH3Cl", "CH3ClN2", "CHClO", "CHFO", "Cl", "HBr", "HCl", "HF", "HI", "I"};

    private final MolecularFormula[] reporterIons;
    private final double[] reporterIonMasses;
    private final double maxDiff;

    public ElementPrediction() {
        this.reporterIons = new MolecularFormula[REPORTER_IONS.length];
        double maxMass = 0d;
        for (int k=0; k < reporterIons.length; ++k) {
            reporterIons[k] = MolecularFormula.parse(REPORTER_IONS[k]);
            maxMass = Math.max(reporterIons[k].getMass(), maxMass);
        }
        this.maxDiff = maxMass+0.5;
        Arrays.sort(reporterIons);
        this.reporterIonMasses = new double[reporterIons.length];
        for (int k=0; k < reporterIons.length; ++k)
            reporterIonMasses[k] = reporterIons[k].getMass();
    }

    public FormulaConstraints extendConstraints(FormulaConstraints input, Ms2Experiment experiment, Deviation allowedDev) {
        // search for reporter ion
        final Set<MolecularFormula> detected = new HashSet<MolecularFormula>();
        for (Ms2Spectrum<? extends Peak> spec : experiment.getMs2Spectra()) {
            // first normalize
            double mx1 = 0d, mx2 = 0d;
            for (int k = 0; k < spec.size(); ++k) {
                final double f = spec.getIntensityAt(k);
                if (f > mx2) {
                    if (f > mx1) {
                        mx2 = mx1;
                        mx1 = f;
                    } else mx2 = f;
                }
            }
            final double intensityThreshold = mx2 * 0.01;
            for (int k = 0; k < spec.size(); ++k) {
                if (spec.getIntensityAt(k) < intensityThreshold) continue;
                final Ionization ion;
                if (spec.getIonization() != null) ion = spec.getIonization();
                else ion = experiment.getIonization();
                final double mz = spec.getMzAt(k);
                final int index = searchForMass(ion.subtractFromMass(mz), reporterIonMasses, allowedDev);
                if (index >= 0) {
                    detected.add(reporterIons[index]);
                }
                for (int l = k + 1; l < spec.size(); ++l) {
                    if (spec.getIntensityAt(l) < intensityThreshold) continue;
                    final double mzdiff = spec.getMzAt(l) - mz;
                    if (mzdiff > maxDiff) break;
                    final int index2 = searchForMass(mzdiff, reporterIonMasses, allowedDev);
                    if (index2 >= 0) {
                        detected.add(reporterIons[index2]);
                    }
                }
            }
        }
        final ChemicalAlphabet alphabet = input.getChemicalAlphabet();
        final List<Element> toExtend = new ArrayList<Element>();
        for (MolecularFormula det : detected) {
            for (Element e : det.elementArray()) {
                if (det.numberOf(e) > 0 && alphabet.indexOf(e)<0) {
                    toExtend.add(e);
                }
            }
        }
        if (toExtend.size() > 0) {
            final ArrayList<Element> newElements = new ArrayList<Element>(input.getChemicalAlphabet().getElements());
            for (Element e : toExtend)
                newElements.add(e);
            final ChemicalAlphabet newAlphabet = new ChemicalAlphabet(newElements.toArray(new Element[newElements.size()]));
            final FormulaConstraints newConstraints = new FormulaConstraints(newAlphabet, input.getFilters());
            for (Element e : alphabet) {
                newConstraints.setUpperbound(e, input.getUpperbound(e));
            }

            final PeriodicTable table = PeriodicTable.getInstance();
            final Element Chlorine = table.getByName("Cl");
            final Element Bromine = table.getByName("Br");
            final Element Iodine = table.getByName("I");
            final Element Florine = table.getByName("F");
            if (newAlphabet.indexOf(Chlorine)>=0 && alphabet.indexOf(Chlorine)<0)
                newConstraints.setUpperbound(Chlorine, 10);
            if (newAlphabet.indexOf(Chlorine)>=0 && alphabet.indexOf(Bromine)<0)
                newConstraints.setUpperbound(Bromine, 4);
            if (newAlphabet.indexOf(Chlorine)>=0 && alphabet.indexOf(Iodine)<0)
                newConstraints.setUpperbound(Iodine, 10);
            if (newAlphabet.indexOf(Chlorine)>=0 && alphabet.indexOf(Florine)<0)
                newConstraints.setUpperbound(Florine, 20);
            return newConstraints;
        } else return input;
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
