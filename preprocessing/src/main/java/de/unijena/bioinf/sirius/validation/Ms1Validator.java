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

package de.unijena.bioinf.sirius.validation;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.Scored;
import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.chem.utils.UnknownElementException;
import de.unijena.bioinf.ChemistryBase.data.DataSource;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.Whiteset;
import de.unijena.bioinf.ChemistryBase.ms.inputValidators.InvalidException;
import de.unijena.bioinf.ChemistryBase.ms.inputValidators.Ms2ExperimentValidator;
import de.unijena.bioinf.ChemistryBase.ms.inputValidators.Warning;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Ms1Validator implements Ms2ExperimentValidator {

    @Override
    public boolean validate(MutableMs2Experiment input, Warning warn, boolean repair) throws InvalidException {
        if (input.getMs1Spectra().size()==0 && input.getMergedMs1Spectrum()==null && input.getMs2Spectra().size()==0)
            throw new InvalidException("Missing MS2 and MS1 spectra");
        if (input.getMs1Spectra().isEmpty() && input.getMergedMs1Spectrum()==null)
            throw new InvalidException("Missing MS1 spectra"); //this validator should be used for MS1-only  data. Hence, it requires some MS1 data.

        checkInchi(warn, repair, input);
        checkIntrinsicalCharged(warn,repair,input);
        checkIonization(warn, repair, input);
        checkMergedMs1(warn, repair, input);
        checkMolecularFormula(warn, repair, input);
        return true;

    }

    /**
     * if the molecule is intrinsical charged, we will neutralize its molecular formula (and neutral mass annotation)
     * because intrinsical charged ion types are, internally, represented via an protonation ionization.
     */
    protected void checkIntrinsicalCharged(Warning warn, boolean repair, MutableMs2Experiment input) {
        final Deviation dev = new Deviation(20,0.1);
        if (input.getMolecularFormula()==null)
            return;
        if (input.getPrecursorIonType().isIntrinsicalCharged() || dev.inErrorWindow(input.getMolecularFormula().getMass(), input.getIonMass())) {
            // compound is intrinsical charged
            if (dev.inErrorWindow(input.getMolecularFormula().getMass(), input.getIonMass())) {
                // and formula is ionized
                if (repair) {
                    if (input.getPrecursorIonType().getCharge()>0) {
                        input.setMolecularFormula(input.getMolecularFormula().subtract(MolecularFormula.getHydrogen()));
                    } else {
                        input.setMolecularFormula(input.getMolecularFormula().add(MolecularFormula.getHydrogen()));
                    }
                } else {
                    warn.warn("Molecular formula " + input.getMolecularFormula() + " is ionized, but should be converted into neutral form, even for intrinsical charged compounds.");
                }
            }


        }
    }

    protected static String nameOf(MutableMs2Experiment exp) {
        final DataSource s = exp.getAnnotation(DataSource.class).orElse(new DataSource(exp.getSource()));
        if (s.getURI()==null) return exp.getName();
        else return s.getURI().getPath();
    }

    private static Pattern P_LAYER = Pattern.compile("/p([+-])(\\d+)");
    protected void checkInchi(Warning warn, boolean repair, MutableMs2Experiment input) {
        final InChI inchi = input.getAnnotationOrNull(InChI.class);
        if (inchi==null || inchi.in3D == null) return;

        final MolecularFormula formula;
        try {
            formula = inchi.extractFormula();
            if (input.getMolecularFormula() != null && !input.getMolecularFormula().equals(formula)) {
                // check for p layer
                final Matcher m = P_LAYER.matcher(inchi.in3D);
                if (m.find()) {
                    MolecularFormula difference = MolecularFormula.parseOrThrow("H");
                    difference = difference.multiply(Integer.parseInt(m.group(2)));
                    if (m.group(1).equals("-")) difference = difference.negate();
                    if (formula.add(difference).equals(input.getMolecularFormula())) {
                        // everything is alright!
                        if (m.group(1).equals("+")) {
                            ensureIonType(input, PrecursorIonType.getPrecursorIonType("[M]+"), warn, repair);
                        } else {
                            ensureIonType(input, PrecursorIonType.getPrecursorIonType("[M]-"), warn, repair);
                        }
                        return;
                    }
                }

                warn.warn(nameOf(input) + ": InChI has different molecular formula than input formula (" + inchi.extractFormula() + " vs. " + input.getMolecularFormula() + ")");
            }

            if (input.getMoleculeNeutralMass() > 0 && Math.abs(formula.getMass() - input.getMoleculeNeutralMass()) > 0.01) {
                warn.warn(nameOf(input) + ": neutral mass does not match to InChI formula (" + input.getMoleculeNeutralMass() + " Da vs. exact mass " + formula.getMass() + ") ");
            }
            if (repair) {
                if (input.getMolecularFormula() == null) input.setMolecularFormula(formula);
            }

        } catch (UnknownElementException e) {
            warn.warn("Formula of Inchi is Not parsable! " + e.getMessage());
        }
    }

    private void ensureIonType(MutableMs2Experiment input, PrecursorIonType precursorIonType, Warning warn, boolean repair) {
        if (input.getPrecursorIonType().equals(precursorIonType)) return;
        else if (repair || input.getPrecursorIonType().isIonizationUnknown()) {
            if (!input.getPrecursorIonType().isIonizationUnknown())
                warn.warn(nameOf(input) + ": Set ion type to " + precursorIonType.toString());
            input.setPrecursorIonType(precursorIonType);
        } else throw new InvalidException("PrecursorIonType is expected to be " + precursorIonType.toString() + " but " + input.getPrecursorIonType() + " is given.");
    }

    protected void checkIonization(Warning warn, boolean repair, MutableMs2Experiment input) {
        Deviation devMs1 = input.getAnnotationOrDefault(MS1MassDeviation.class).allowedMassDeviation;
        checkIonization(warn, repair, input, devMs1);
    }

    /**
     * uses MS2 spectra if available
     * @param warn
     * @param repair
     * @param input
     * @param maxMs1AndOrMs2Dev
     */
    protected void checkIonization(Warning warn, boolean repair, MutableMs2Experiment input, Deviation maxMs1AndOrMs2Dev) {
        double absError = 1e-2;
        Deviation dev = new Deviation(20, absError);
        //take maximum of default and particular experiment's deviation.
        dev = new Deviation(Math.max(dev.getPpm(), maxMs1AndOrMs2Dev.getPpm()), Math.max(dev.getAbsolute(), maxMs1AndOrMs2Dev.getAbsolute()));
        absError = Math.max(absError, dev.absoluteFor(input.getIonMass()));

        final double neutralmass = input.getMoleculeNeutralMass();
        if ((input.getMolecularFormula() != null || neutralmass > 0) && input.getIonMass() > 0 && !input.getPrecursorIonType().isIonizationUnknown()) {
            final double modification = input.getIonMass()-neutralmass;
            final double error = input.getPrecursorIonType().neutralMassToPrecursorMass(neutralmass) - input.getIonMass();
            if (Math.abs(error) > absError) {
                // as we sometimes store the molecular formula of intrinsical charged ions inconsistently, check this possibility
                if (input.getPrecursorIonType().isIntrinsicalCharged()) {
                    // we always report a neutral formula, even if it should be intrinsical charged
                    // check if it works when we use protonation
                    if (input.getPrecursorIonType().getCharge()>0) {
                        if (Math.abs(PeriodicTable.getInstance().getPrecursorProtonation().neutralMassToPrecursorMass(neutralmass)-input.getIonMass()) <= absError) return;
                    } else {
                        if (Math.abs(PeriodicTable.getInstance().getPrecursorDeprotonation().neutralMassToPrecursorMass(neutralmass)-input.getIonMass()) <= absError) return;
                    }
                }
                final PrecursorIonType iontype = PeriodicTable.getInstance().ionByMass(modification, absError, input.getPrecursorIonType().getCharge());
                if (iontype != null) {
                    throwOrWarn(warn, true, nameOf(input) + ": PrecursorIonType is inconsistent with the data (" + input.getPrecursorIonType().toString() + " but " + iontype.toString() + " is estimated after looking at the data)");
                    input.setPrecursorIonType(iontype);
                } else {
                    throwOrWarn(warn, true, nameOf(input) + ": PrecursorIonType is inconsistent with the data (" + input.getPrecursorIonType().toString() + " with m/z " + input.getPrecursorIonType().getModificationMass() + " does not match ion mass m/z = " + input.getIonMass() + " and neutral mass m/z = " + neutralmass + ")" );
                    input.setPrecursorIonType(PeriodicTable.getInstance().getUnknownPrecursorIonType(input.getPrecursorIonType().getCharge()));
                }
            }
            return;
        }
        if (repair && input.getPrecursorIonType().isIonizationUnknown() && (input.getMolecularFormula() != null)) {
            if (input.getIonMass()==0 || Double.isNaN(input.getIonMass())) {
                // find matching ion mass
                final ArrayList<PrecursorIonType> ionTypes = new ArrayList<>();
                for (PrecursorIonType i : PeriodicTable.getInstance().getKnownLikelyPrecursorIonizations(input.getPrecursorIonType().getCharge())) {
                    ionTypes.add(i);
                }
                final List<Scored<PrecursorIonType>> scoredIonTypes = new ArrayList<>();
                final List<SimpleSpectrum> specs = new ArrayList<>();
                if (input.getMergedMs1Spectrum()!=null && input.getMergedMs1Spectrum().size()>0) {
                    specs.add(input.getMergedMs1Spectrum());
                } else {
                    for (SimpleSpectrum s : input.getMs1Spectra()) specs.add(s);
                }
                for (PrecursorIonType ionType : ionTypes) {
                    // search in MS1
                    final double peak = ionType.neutralMassToPrecursorMass(input.getMolecularFormula().getMass());
                    for (SimpleSpectrum s : specs) {
                        int i = Spectrums.mostIntensivePeakWithin(s, peak, dev);
                        if (i>=0) {
                            scoredIonTypes.add(new Scored<PrecursorIonType>(ionType, s.getIntensityAt(i)));
                        }
                    }
                }
                if (scoredIonTypes.size()==0) {
                    // repeat with MS2 spectrum
                    for (PrecursorIonType ionType : ionTypes) {
                        final double peak = ionType.neutralMassToPrecursorMass(input.getMolecularFormula().getMass());
                        for (Spectrum s : input.getMs2Spectra()) {
                            int i = Spectrums.mostIntensivePeakWithin(s, peak, dev);
                            if (i>=0) {
                                scoredIonTypes.add(new Scored<PrecursorIonType>(ionType, s.getIntensityAt(i)));
                            }
                        }
                    }
                }
                scoredIonTypes.sort(Comparator.reverseOrder());
                if (scoredIonTypes.size()>0) {
                    final PrecursorIonType ion = scoredIonTypes.get(0).getCandidate();
                    input.setPrecursorIonType(ion);
                    input.setIonMass(ion.neutralMassToPrecursorMass(input.getMolecularFormula().getMass()));
                    warn.warn("Set ion to " + ion.toString());
                    return;
                }
            }


            double modificationMass = input.getIonMass() - (input.getMolecularFormula() != null ? input.getMolecularFormula().getMass() : input.getMoleculeNeutralMass());
            PrecursorIonType ion = PeriodicTable.getInstance().ionByMass(modificationMass, absError, input.getPrecursorIonType().getCharge());
            if (ion != null) {
                warn.warn(nameOf(input) + ": Set ion to " + ion.toString());
                input.setPrecursorIonType(ion);
            } else {
                searchForIon(warn, input);
            }
        }
    }

    /**
     * also uses MS2 spectra, if available
     * @param warn
     * @param input
     */
    private void searchForIon(Warning warn, MutableMs2Experiment input) {
        final double neutral = (input.getMolecularFormula() != null) ? input.getMolecularFormula().getMass() : input.getMoleculeNeutralMass();
        final ArrayList<Spectrum<? extends Peak>> spectra = new ArrayList<Spectrum<? extends Peak>>(input.getMs1Spectra());
        for (Ms2Spectrum<? extends Peak> ms2 : input.getMs2Spectra()) spectra.add(ms2);
        // TODO: negative iondetection
        // search for [M+H]+
        final PrecursorIonType mhp = PeriodicTable.getInstance().ionByNameOrThrow("[M+H]+");
        final double mz = mhp.neutralMassToPrecursorMass(neutral);
        final Deviation dev = new Deviation(20);
        for (Spectrum<? extends Peak> spec : spectra) {
            final int peak = Spectrums.search(spec, mz, dev);
            if (peak >= 0) {
                warn.warn(nameOf(input) + ": Set ion to " + mhp.toString());
                input.setPrecursorIonType(mhp);
                input.setIonMass(spec.getMzAt(peak));
                return;
            }
        }
        // search for other iondetection
        final Collection<PrecursorIonType> ions = PeriodicTable.getInstance().getIons();
        for (Spectrum<? extends Peak> spec : spectra) {
            for (PrecursorIonType ion : ions) {
                if (Spectrums.search(spec, ion.neutralMassToPrecursorMass(neutral), dev) >= 0) {
                    warn.warn(nameOf(input) + ": Set ion to " + ion.toString());
                    input.setPrecursorIonType(ion);
                    return;
                }
            }
        }
        throw new InvalidException("Cannot find a proper ion mode/adduct type for the given spectrum. Please specify the correct ion/adduct type.");
    }

    protected void checkMergedMs1(Warning warn, boolean repair, MutableMs2Experiment input) {
        if (input.getMergedMs1Spectrum() == null && !input.getMs1Spectra().isEmpty()) {
            //warn.warn("No merged spectrum is given");
            if (repair) {
                if (input.getMs1Spectra().size() == 1)
                    input.setMergedMs1Spectrum(input.getMs1Spectra().get(0));
            }
        }
    }

    protected void checkMolecularFormula(Warning warn, boolean repair, MutableMs2Experiment input) {
        MolecularFormula mf = input.getMolecularFormula();
        if (mf != null) {
            Whiteset ws = input.getAnnotation(Whiteset.class).orElse(null);
            if (ws == null || ws.isEmpty() || ws.getNeutralFormulas().isEmpty()) {
                warn.warn("Adding missing formula candidate constrains for preset molecular formula.");
                input.setAnnotation(Whiteset.class, Whiteset.ofNeutralizedFormulas(List.of(mf), Ms1Validator.class));
            } else {
                if (ws.getNeutralFormulas().size() != 1 || !ws.getNeutralFormulas().iterator().next().equals(mf)) {
                    String warning = nameOf(input) + ": Preset molecular formula '" + mf
                            + "' does not match formula candidate constrains {"
                            + ws.getNeutralFormulas().stream().map(MolecularFormula::toString).collect(Collectors.joining(", "))
                            + "}.";
                    if (repair) {
                        warn.warn(warning + " Correcting constrains.");
                        input.setAnnotation(Whiteset.class, Whiteset.ofNeutralizedFormulas(List.of(mf), Ms1Validator.class));
                    } else {
                        warn.warn(warning + " No correction requested!.");

                    }
                }
            }
        }
    }

    protected void throwOrWarn(Warning warn, boolean repair, String message) {
        if (repair) warn.warn(message);
        else throw new InvalidException(message);
    }


}
