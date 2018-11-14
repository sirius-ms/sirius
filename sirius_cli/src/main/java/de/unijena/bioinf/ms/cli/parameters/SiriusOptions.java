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
package de.unijena.bioinf.ms.cli.parameters;

import com.google.common.collect.Iterables;
import de.unijena.bioinf.ChemistryBase.SimpleRectangularIsolationWindow;
import de.unijena.bioinf.ChemistryBase.chem.*;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.sirius.IsotopePatternHandling;
import de.unijena.bioinf.sirius.Sirius;
import org.jetbrains.annotations.Nullable;
import picocli.CommandLine.Option;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This is for SIRIUS specific parameters.
 * <p>
 * It will be annotated to the MS2 Experiment
 *
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class SiriusOptions extends AbstractMs2ExperimentOptions {
    @Option(names = {"-S", "--sirius"})
    public boolean sirius;

    @Option(names = {"-s", "--isotope"}, description = "how to handle isotope pattern data. Use 'score' to use them for ranking or 'filter' if you just want to remove candidates with bad isotope pattern. With 'both' you can use isotopes for filtering and scoring (default). Use 'omit' to ignore isotope pattern.")
    public IsotopePatternHandling isotopeHandling;

    @Option(names = {"-c", "--candidates"}, description = "Number of formula candidates in the output.")
    public void setNumberOfCandidates(int value) {
        numberOfCandidates = new NumberOfCandidates(value);
    }

    private NumberOfCandidates numberOfCandidates;

    public NumberOfCandidates getNumberOfCandidates(final NumberOfCandidates defaultValue) {
        if (numberOfCandidates == null)
            return defaultValue;
        return numberOfCandidates;
    }


    @Option(names = "--candidates-per-ion", description = "Minimum number of candidates in the output for each ionization. Set to force output of results for each possible ionization, even if not part of highest ranked results.")
    public void setNumberOfCandidatesPerIon(int value) {
        numberOfCandidatesPerIon = new NumberOfCandidatesPerIon(value);
    }

    private NumberOfCandidatesPerIon numberOfCandidatesPerIon;

    public NumberOfCandidatesPerIon getNumberOfCandidatesPerIon(final NumberOfCandidatesPerIon defaultValue) {
        if (numberOfCandidatesPerIon == null)
            return defaultValue;
        return numberOfCandidatesPerIon;
    }


    @Option(names = {"-f", "--formula", "--formulas"}, description = "specify the neutral molecular formula of the measured compound to compute its tree or a list of candidate formulas the method should discriminate. Omit this option if you want to consider all possible molecular formulas")
    public List<String> formula;

    @Option(names = "--no-recalibration")
    public boolean notRecalibrating;

    @Option(names = "--ppm-max", description = "allowed ppm for decomposing masses")
    public Double ppmMax;

    @Option(names = "--ppm-max-ms2", description = "allowed ppm for decomposing masses in MS2. If not specified, the same value as for the MS1")
    public Double ppmMaxMs2;

    @Option(names = "--noise", description = "median intensity of noise peaks")
    public Double medianNoise;

    @Option(names = "--disable-fast-mode", hidden = true)
    public boolean disableFastMode;

    @Option(names = "--tree-timeout", description = "Time out in seconds per fragmentation tree computations. 0 for an infinite amount of time. Default: 0"/*, defaultValue = "0"*/)
    public int treeTimeout;

    @Option(names = "--compound-timeout", description = "Maximal computation time in seconds for a single compound. 0 for an infinite amount of time. Default: 0"/*, defaultValue = "0"*/)
    public int instanceTimeout;

    @Option(names = {"-e", "--elements"}, description = "The allowed elements. Write CHNOPSCl to allow the elements C, H, N, O, P, S and Cl. Add numbers in brackets to restrict the minimal and maximal allowed occurence of these elements: CHNOP[5]S[8]Cl[1-2]. When one number is given then it is interpreted as upperbound.")
    public FormulaConstraints elements;

    @Option(names = {"--mostintense-ms2"}, description = "Only use the fragmentation spectrum with the most intense precursor peak (for each compound).")
    public boolean mostIntenseMs2;

    @Option(names = {"-z", "--parentmass", "precursor", "mz"}, description = "the mass of the parent ion")
    public Double parentMz;

    @Option(names = "--trust-ion-prediction", description = "By default we use MS1 information to select additional ionizations ([M+Na]+,[M+K]+,[M+Cl]-,[M+Br]-) for considerations. With this parameter we trust the MS1 prediction and only consider these found ionizations.")
    public boolean trustGuessIonFromMS1;

    @Option(names = {"-Z", "--auto-charge"}, description = "Use this option if the adduct type and/or ion mode of your compounds is unknown and you do not want to assume [M+H]+/[M-H]- as default. With the option enabled, SIRIUS will also search for other adduct types (e.g. [M+NH3+H]+ or even other ion modes (e.g. [M+Na]+) if no ion mode is specified.")
    public boolean autoCharge;

    @Option(names = {"-i", "--ion"}, description = "the ionization/adduct of the MS/MS data. Example: [M+H]+, [M-H]-, [M+Cl]-, [M+Na]+, [M]+. You can also provide a comma separated list of adducts.")
    public void setIon(List<String> ions) {
        if (ions == null || ions.isEmpty()) {
            possibleIonAdducts = new PossibleAdducts();
        } else {
            final List<PrecursorIonType> ionTypes = new ArrayList<>();
            for (String ion : ions) ionTypes.add(PrecursorIonType.getPrecursorIonType(ion));
            int ch = ionTypes.get(0).getCharge();
            for (PrecursorIonType pi : ionTypes)
                if (pi.getCharge() != ch)
                    throw new IllegalArgumentException("SIRIUS does not support different charge states for the same compound");
            possibleIonAdducts = new PossibleAdducts(ionTypes);
        }
    }

    private PossibleAdducts possibleIonAdducts;

    public @Nullable PossibleAdducts getPossibleIonAdducts() {
        return possibleIonAdducts;
    }

    public PrecursorIonType getPrecursorIonType() {
        if (possibleIonAdducts.isPrecursorIonType())
            return possibleIonAdducts.asPrecursorIonType();
        return PrecursorIonType.unknown();
    }


    @Option(names = "-p", description = "name of the configuration profile. Some of the default profiles are: 'qtof', 'orbitrap', 'fticr'.")
    public String profile;

    ///// some hidden parameters
    @Option(names = "--disable-element-detection", hidden = true)
    public boolean disableElementDetection;

    @Option(names = "--enable-silicon-detection", hidden = true)
    public boolean enableSiliconDetection;

    @Option(names = {"--assess-data-quality"}, description = "produce stats on quality of spectra and estimate isolation window. Needs to read all data at once.", hidden = true)
    public boolean assessDataQuality;


    @Option(names = {"--isolation-window-width"}, description = "width of the isolation window to measure MS2", hidden = true)
    public void setIsolationWindowWidth(Double isolationWindowWidth) {
        this.isolationWindowWidth = isolationWindowWidth;
        isolationWindow = null;
    }

    private Double isolationWindowWidth;

    @Option(names = {"--isolation-window-shift"}, description = "The shift applied to the isolation window to measure MS2 in relation to the precursormass", hidden = true)
    public void setIsolationWindowShift(double isolationWindowShift) {
        this.isolationWindowShift = isolationWindowShift;
        isolationWindow = null;
    }

    private double isolationWindowShift;

    private IsolationWindow isolationWindow = null;

    public IsolationWindow getIsolationWindow() {
        if (isolationWindow != null || isolationWindowWidth == null) return isolationWindow;
        final double right = Math.abs(isolationWindowWidth) / 2d + isolationWindowShift;
        final double left = -Math.abs(isolationWindowWidth) / 2d + isolationWindowShift;
        return new SimpleRectangularIsolationWindow(left, right);
    }


    private void configureAndAnnotateIonMode(MutableMs2Experiment exp) {
        //set precursor of from parameters if it is still unknown at this stage
        //so we do not have to check possibleIonAdducts.isPrecursorIonType() after this block again.
        if (exp.getPrecursorIonType() == null) {
            if (possibleIonAdducts.isPrecursorIonType())
                exp.setPrecursorIonType(possibleIonAdducts.asPrecursorIonType());
            else
                exp.setPrecursorIonType(PrecursorIonType.unknown(0));
        } else if (exp.getPrecursorIonType().isIonizationUnknown() && possibleIonAdducts.isPrecursorIonType()) {
            exp.setPrecursorIonType(possibleIonAdducts.asPrecursorIonType());
        }

        //now configure possible ion modes and guessing from ms1
        final PossibleIonModes.GuessingMode enabledGuessingMode = trustGuessIonFromMS1 ? PossibleIonModes.GuessingMode.SELECT : PossibleIonModes.GuessingMode.ADD_IONS;
        if (exp.getPrecursorIonType().isIonizationUnknown()) {
            if (autoCharge) {
                //merge default adduct and user adducts -> they already have the same charge
                final PossibleAdducts adductsToAdd = new PossibleAdducts(Iterables.toArray(PeriodicTable.getInstance().getKnownLikelyPrecursorIonizations(exp.getPrecursorIonType().getCharge()), PrecursorIonType.class));
                adductsToAdd.addAdducts(possibleIonAdducts);
                annotateIonMode(exp, adductsToAdd, enabledGuessingMode, true);
            } else if (possibleIonAdducts.size() > 1) {
                annotateIonMode(exp, possibleIonAdducts, enabledGuessingMode, false);
            } else {
                annotateIonMode(exp, new PossibleAdducts(exp.getPrecursorIonType().getCharge() >= 0 ? PrecursorIonType.getPrecursorIonType("[M+H]+") : PrecursorIonType.getPrecursorIonType("[M-H]-")), enabledGuessingMode, true); // TODO: ins MS1 gucken
            }
        } else {
            annotateIonMode(exp, new PossibleAdducts(exp.getPrecursorIonType()), PossibleIonModes.GuessingMode.DISABLED, false);
        }
    }

    private void annotateIonMode(MutableMs2Experiment exp, PossibleAdducts pa, PossibleIonModes.GuessingMode guessingMode, boolean preferProtonation) {
        PossibleIonModes im = exp.getAnnotation(PossibleIonModes.class, new PossibleIonModes());
        im.setGuessFromMs1(guessingMode);

        if (preferProtonation) {
            if (guessingMode.isEnabled())
                im.enableGuessFromMs1WithCommonIonModes(exp.getPrecursorIonType().getCharge());
            for (Ionization ion : pa.getIonModes()) {
                im.add(ion, 0.02);
            }
            if (exp.getPrecursorIonType().getCharge() > 0) {
                im.add(PrecursorIonType.getPrecursorIonType("[M+H]+").getIonization(), 1d);
            } else {
                im.add(PrecursorIonType.getPrecursorIonType("[M-H]-").getIonization(), 1d);
            }
        } else {
            pa.getIonModes().forEach((ion) -> im.add(ion, 1d));
        }

        Sirius.setAllowedAdducts(exp, pa);
        Sirius.setAllowedIonModes(exp, im);
    }

    protected Set<MolecularFormula> getFormulaWhitesetNoDB(Ms2Experiment experiment) {
        final Set<MolecularFormula> whiteset = new HashSet<MolecularFormula>();
        if (formula == null && (numberOfCandidates == null) && experiment.getMolecularFormula() != null) {
            whiteset.add(experiment.getMolecularFormula());
        } else if (formula != null) for (String s : formula) whiteset.add(MolecularFormula.parse(s));
        return whiteset.isEmpty() ? null : whiteset;
    }

    //////////////////////////////////////////////////////////////////////////////////
    @Override
    public void setParamatersToExperiment(MutableMs2Experiment exp) {
        configureAndAnnotateIonMode(exp);

        if (elements != null) {
            Sirius.setFormulaConstraints(exp, elements);
        }
        if (disableElementDetection) {
            Sirius.disableElementDetection(exp);
        }

        if (formula != null && formula.size() == 1)
            exp.setMolecularFormula(MolecularFormula.parse(formula.get(0)));

        Set<MolecularFormula> whiteSet = getFormulaWhitesetNoDB(exp);
        if (whiteSet != null) Sirius.setFormulaSearchList(exp, whiteSet);

        if (parentMz != null) exp.setIonMass(parentMz);

        final IsolationWindow window = getIsolationWindow();
        if (window != null) Sirius.setIsolationWindow(exp, window);

        Sirius.setTimeout(exp, instanceTimeout, treeTimeout);
        Sirius.enableRecalibration(exp, notRecalibrating);
        Sirius.setIsotopeMode(exp, isotopeHandling);

        //only keep most intense ms2 (hack used for bad data)
        if (mostIntenseMs2) {
            Sirius.onlyKeepMostIntenseMS2(exp);
        }
    }
}
