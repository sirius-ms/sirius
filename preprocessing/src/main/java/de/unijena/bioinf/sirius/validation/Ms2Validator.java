package de.unijena.bioinf.sirius.validation;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.Scored;
import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.chem.utils.UnknownElementException;
import de.unijena.bioinf.ChemistryBase.data.DataSource;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.inputValidators.InvalidException;
import de.unijena.bioinf.ChemistryBase.ms.inputValidators.Warning;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Ms2Validator extends Ms1Validator {

    @Override
    public boolean validate(MutableMs2Experiment input, Warning warn, boolean repair) throws InvalidException {
        checkInchi(warn, repair, input);
        if (input.getMs2Spectra() == null)
            throw new InvalidException("Missing MS2 spectra");
        removeEmptySpectra(warn, input);
        if (input.getMs2Spectra().isEmpty() && input.getMs1Spectra().isEmpty() && input.getMergedMs1Spectrum()==null)
            throw new InvalidException("Missing MS2 and MS1 spectra");
        if (input.getMs1Spectra() == null) input.setMs1Spectra(new ArrayList<SimpleSpectrum>());
        checkScanNumbers(warn, repair,input);
        checkIonization(warn, repair, input);
        checkMergedMs1(warn, repair, input);
        checkIonMass(warn, repair, input);
        return true;
    }

    private void checkScanNumbers(Warning warn, boolean repair, MutableMs2Experiment input) {
        for (int k=0; k < input.getMs2Spectra().size(); ++k) {
            input.getMs2Spectra().get(k).setScanNumber(k);
        }
    }

    private static String nameOf(MutableMs2Experiment exp) {
        final DataSource s = exp.getAnnotation(DataSource.class).orElse(new DataSource(exp.getSource()));
        if (s.getUrl()==null) return exp.getName();
        else return s.getUrl().getFile();
    }


    private static Pattern P_LAYER = Pattern.compile("/p([+-])(\\d+)");
    private void checkInchi(Warning warn, boolean repair, MutableMs2Experiment input) {
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


    protected void removeEmptySpectra(Warning warn, MutableMs2Experiment input) {
        final Iterator<MutableMs2Spectrum> iter = input.getMs2Spectra().iterator();
        while (iter.hasNext()) {
            final Ms2Spectrum spec = iter.next();
            if (spec.size() == 0) {
                warn.warn(nameOf(input) + ": Empty Spectrum at collision energy: " + spec.getCollisionEnergy());
                iter.remove();
            }
        }
    }

    protected void checkIonization(Warning warn, boolean repair, MutableMs2Experiment input) {

        double absError = 1e-2;
        Deviation dev = new Deviation(20, absError);
        //take maximum of default and particular experiment's deviation.
        Deviation dev2 = input.getAnnotationOrDefault(MS1MassDeviation.class).allowedMassDeviation;
        Deviation dev3 = input.getAnnotationOrDefault(MS2MassDeviation.class).allowedMassDeviation;
        dev = new Deviation(Math.max(Math.max(dev.getPpm(), dev2.getPpm()), dev3.getPpm()), Math.max(Math.max(dev.getAbsolute(), dev2.getAbsolute()), dev3.getAbsolute()));
        absError = Math.max(absError, dev.absoluteFor(input.getIonMass()));

        final double neutralmass = input.getMoleculeNeutralMass();
        if ((input.getMolecularFormula() != null || neutralmass > 0) && input.getIonMass() > 0 && !input.getPrecursorIonType().isIonizationUnknown()) {
            final double modification = input.getIonMass()-neutralmass;
            if (Math.abs(input.getPrecursorIonType().neutralMassToPrecursorMass(neutralmass)-input.getIonMass()) > absError) {
                final PrecursorIonType iontype = PeriodicTable.getInstance().ionByMass(modification, absError, input.getPrecursorIonType().getCharge());
                if (iontype != null) {
                    throwOrWarn(warn, true, nameOf(input) + ": PrecursorIonType is inconsistent with the data (" + input.getPrecursorIonType().toString() + " but " + iontype.toString() + " is estimated after looking at the data)");
                    input.setPrecursorIonType(iontype);
                } else {
                    throwOrWarn(warn, true, nameOf(input) + ": PrecursorIonType is inconsistent with the data (" + input.getPrecursorIonType().toString() + " with m/z " + input.getPrecursorIonType().getModificationMass() + " does not match ion mass m/z = " + input.getIonMass() + " and neutral mass m/z = " + neutralmass + ")" );
                    input.setPrecursorIonType(PeriodicTable.getInstance().getUnknownPrecursorIonType(input.getPrecursorIonType().getCharge()));
                }
            }
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

    protected void checkIonMass(Warning warn, boolean repair, MutableMs2Experiment input) {
        if (!validDouble(input.getIonMass(), false) || input.getIonMass() == 0) {
            if (input.getMolecularFormula() == null && !validDouble(input.getMoleculeNeutralMass(), false)) {
                final Spectrum<Peak> ms1 = input.getMergedMs1Spectrum();
                // maybe the ms2 spectra have a common precursor
                boolean found = true;
                double mz = input.getMs2Spectra().get(0).getPrecursorMz();
                for (Ms2Spectrum s : input.getMs2Spectra()) {
                    if (!validDouble(s.getPrecursorMz(), false) || s.getPrecursorMz() == 0) {
                        found = false;
                        break;
                    }
                    final double newMz = s.getPrecursorMz();
                    if (Math.abs(mz - newMz) > 1e-3) {
                        found = false;
                        break;
                    }
                }
                if (found) {
                    input.setIonMass(mz);
                } else {
                    if (ms1 == null || ms1.size()==0) {
                        // use the highest mass you find in the MS2 spectra with lowest collision energy as parent mass.
                        // (only if its intensity is higher than 10% and higher than peaks in its neighbourhood)
                        Ms2Spectrum spec = input.getMs2Spectra().get(0);
                        for (Ms2Spectrum spec2 : input.getMs2Spectra()) {
                            if (spec2.getCollisionEnergy().lowerThan(spec.getCollisionEnergy())) spec = spec2;
                        }
                        final Spectrum<Peak> normalized = Spectrums.getNormalizedSpectrum(spec, Normalization.Max(1d));
                        Peak parent = normalized.getPeakAt(Spectrums.getIndexOfPeakWithMaximalIntensity(normalized));
                        for (Peak p : normalized)
                            if (p.getIntensity() > 0.1d) {
                                if (Math.abs(p.getMass() - parent.getMass()) < 1e-2) {
                                    if (p.getIntensity() > parent.getIntensity()) parent = p;
                                } else if (p.getMass() > parent.getMass()) parent = p;
                            }
                        warn.warn(nameOf(input) + ": No ion mass is given. Choose m/z = " + parent.getMass() + " as parent peak.");
                        input.setIonMass(parent.getMass());
                    } else {
                        // take peak with highest intensity
                        int index = Spectrums.getIndexOfPeakWithMaximalIntensity(ms1);
                        // move backward, maybe you are in the middle of an isotope pattern
                        while (index > 0) {
                            if (Math.abs(ms1.getMzAt(index) - ms1.getMzAt(index - 1)) > 1.1d) break;
                            --index;
                        }
                        // hopefully, this is the correct isotope peak
                        warn.warn(nameOf(input) + ": Predict ion mass from MS1: " + ms1.getMzAt(index));
                        input.setIonMass(ms1.getMzAt(index));
                    }
                }
            } else {
                final double neutralMass = (input.getMolecularFormula()!=null ? input.getMolecularFormula().getMass() : input.getMoleculeNeutralMass());
                if (neutralMass <= 0) {
                    throwOrWarn(warn, false, nameOf(input) + ": Neither ionmass nor neutral mass nor molecular formula are given. Cannot determine parent peak!");
                }
                final double parentMz = input.getPrecursorIonType().neutralMassToPrecursorMass(neutralMass);
                input.setIonMass(parentMz);
                for (int i = 0; i < input.getMs2Spectra().size(); ++i) {
                    final Ms2Spectrum s = input.getMs2Spectra().get(i);
                    if (Math.abs(s.getPrecursorMz() - parentMz) > 0.1d) {
                        final MutableMs2Spectrum t = new MutableMs2Spectrum(s, parentMz, s.getCollisionEnergy(), 2);
                        input.getMs2Spectra().set(i, t);
                    }
                }
            }
        }
    }

    protected void throwOrWarn(Warning warn, boolean repair, String message) {
        if (repair) warn.warn(message);
        else throw new InvalidException(message);
    }

    private boolean validDouble(double val, boolean mayNegative) {
        return !Double.isInfinite(val) && !Double.isNaN(val) && (mayNegative || val > 0d);
    }
}
