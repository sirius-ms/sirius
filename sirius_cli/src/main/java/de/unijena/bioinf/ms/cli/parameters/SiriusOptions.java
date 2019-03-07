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

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.NumberOfCandidates;
import de.unijena.bioinf.ChemistryBase.ms.NumberOfCandidatesPerIon;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.IsotopeInMs2Handling;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.List;

/**
 * This is for SIRIUS specific parameters.
 * <p>
 * They may be annotated to the MS2 Experiment
 *
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
@Command(name = "sirius", aliases = {"S"}, description = "Identify molecular formula for each compound individually using fragmentation trees and isotope patterns.", defaultValueProvider = Provide.Defaults.class, versionProvider = Provide.Versions.class,  mixinStandardHelpOptions = true, sortOptions = false)
public class SiriusOptions extends AbstractMsExperimentOptions {



    @Option(names = "--ppm-max", description = "Maximum allowed mass deviation in ppm for decomposing masses.")
    public Double ppmMax;//todo config

    @Option(names = "--ppm-max-ms2", description = "Maximum allowed mass deviation in ppm for decomposing masses in MS2. If not specified, the same value as for the MS1 is used.")
    public Double ppmMaxMs2;//todo config

    @Option(names = "--tree-timeout", description = "Time out in seconds per fragmentation tree computations. 0 for an infinite amount of time. Default: 0"/*, defaultValue = "0"*/)
    public int treeTimeout; //todo config

    @Option(names = "--compound-timeout", description = "Maximal computation time in seconds for a single compound. 0 for an infinite amount of time. Default: 0"/*, defaultValue = "0"*/)
    public int instanceTimeout; //todo config

    @Option(names = "--no-recalibration", description = "Disable Recalibration of input Spectra")
    public boolean notRecalibrating; //todo config

    @Option(names = {"-p", "--profile"}, description = "Name of the configuration profile. Some of the default profiles are: 'qtof', 'orbitrap', 'fticr'.")
    public String profile;//todo config


    // candidates
    @Option(names = {"-c", "--candidates"}, description = "Number of formula candidates in the output.")
    public void setNumberOfCandidates(int value) {
        numberOfCandidates = new NumberOfCandidates(value);
    } //todo config

    private NumberOfCandidates numberOfCandidates;

    public NumberOfCandidates getNumberOfCandidates(final NumberOfCandidates defaultValue) {
        if (numberOfCandidates == null)
            return defaultValue;
        return numberOfCandidates;
    }


    @Option(names = "--candidates-per-ion", description = "Minimum number of candidates in the output for each ionization. Set to force output of results for each possible ionization, even if not part of highest ranked results.")
    public void setNumberOfCandidatesPerIon(int value) {
        numberOfCandidatesPerIon = new NumberOfCandidatesPerIon(value);
    } //todo config

    private NumberOfCandidatesPerIon numberOfCandidatesPerIon;

    public NumberOfCandidatesPerIon getNumberOfCandidatesPerIon(final NumberOfCandidatesPerIon defaultValue) {
        if (numberOfCandidatesPerIon == null)
            return defaultValue;
        return numberOfCandidatesPerIon;
    }

    // Elements
    @Option(names = {"-e", "--elements-considered"}, description = "Set the allowed elements for rare element detection. Write SBrClBSe to allow the elements S,Br,Cl,B and Se.")
    public void setDetectableElements(String elements){
        //todo FormulaSettings.detectable =
        //todo FormulaSettings.fallback =
    }

    @Option(names = {"-E", "--elements-enforced"}, description = "Enforce elements for molecular formula determination. Write CHNOPSCl to allow the elements C, H, N, O, P, S and Cl. Add numbers in brackets to restrict the minimal and maximal allowed occurrence of these elements: CHNOP[5]S[8]Cl[1-2]. When one number is given then it is interpreted as upper bound. Default is CHNOP")
    public void setEnforcedElements(String elements){
        //todo FormulaSettings.enforced =
    }

    @Option(names = {"-f", "--formula", "--formulas"}, description = "Specify the neutral molecular formula of the measured compound to compute its tree or a list of candidate formulas the method should discriminate. Omit this option if you want to consider all possible molecular formulas")
    public List<String> formula;


    @Option(names = {"--no-isotope-filter"}, description = "Disable molecular formula filter. When filtering is enabled, molecular formulas are excluded if their theoretical isotope pattern does not match the theoretical one, even if their MS/MS pattern has high score.")
    public void setIsotopeHandling(boolean diable){
        //todo disable IsotopeSettings.filter = false
    }

    @Option(names = {"--no-isotope-score"}, description = "Disable isotope pattern score.")
    public void setIsotopeHandling(IsotopeInMs2Handling handling){
        //todo IsotopeSettings.multiplier = 0
    }



    //Adducts
    @Option(names = {"-i", "--ions-considered"}, description = "the iontype/adduct of the MS/MS data. Example: [M+H]+, [M-H]-, [M+Cl]-, [M+Na]+, [M]+. You can also provide a comma separated list of adducts.")
    public void setIonsConsidered(List<PrecursorIonType> adducts){
        //todo set adductSettings.enforced
    }

    @Option(names = {"-I", "--ions-enforced"}, description = "the iontype/adduct of the MS/MS data. Example: [M+H]+, [M-H]-, [M+Cl]-, [M+Na]+, [M]+. You can also provide a comma separated list of adducts.")
    public void setIonsEnforced(List<PrecursorIonType> adducts){
        //todo set adductSettings.enforced
    }


    // some hidden parameters
    @Option(names = {"--mostintense-ms2"}, description = "Only use the fragmentation spectrum with the most intense precursor peak (for each compound).", hidden = true)
    public boolean mostIntenseMs2;

    @Option(names = "--disable-element-detection", hidden = true)
    public boolean disableElementDetection; //todo FormulaSettings.detectable empty

    @Option(names = "--enable-silicon-detection", hidden = true) //schliessen sich aus
    public boolean enableSiliconDetection; //todo FormulaSettings.detectable add Si

    @Option(names = "--trust-ion-prediction", description = "By default we use MS1 information to select additional ionizations ([M+Na]+,[M+K]+,[M+Cl]-,[M+Br]-) for considerations. With this parameter we trust the MS1 prediction and only consider these found ionizations.", hidden = true)
    public boolean trustGuessIonFromMS1; //todo manipulate adduct lists for marcus?????

    @Option(names = "--disable-fast-mode", hidden = true)
    public boolean disableFastMode;

}
