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
package de.unijena.bioinf.ms.cli.parameters.sirius;

import de.unijena.bioinf.ms.cli.parameters.config.DefaultParameterOptionLoader;
import de.unijena.bioinf.ms.cli.parameters.InstanceJob;
import de.unijena.bioinf.ms.cli.parameters.Provide;
import de.unijena.bioinf.ms.properties.PropertyManager;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.List;
import java.util.concurrent.Callable;

/**
 * This is for SIRIUS specific parameters.
 * <p>
 * They may be annotated to the MS2 Experiment
 *
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */

//todo got descriprions from defaultConfigOptions
@Command(name = "sirius", aliases = {"S"}, description = "Identify molecular formula for each compound individually using fragmentation trees and isotope patterns.", defaultValueProvider = Provide.Defaults.class, versionProvider = Provide.Versions.class,  mixinStandardHelpOptions = true, sortOptions = false)
public class SiriusOptions implements Callable<InstanceJob.Factory> {
    protected final DefaultParameterOptionLoader defaultConfigOptions;

    public SiriusOptions(DefaultParameterOptionLoader defaultConfigOptions) {
        this.defaultConfigOptions = defaultConfigOptions;
    }

    @Option(names = "--ppm-max", description = "Maximum allowed mass deviation in ppm for decomposing masses.")
    public void setPpmMax(String value) throws Exception {
        defaultConfigOptions.changeOption("MS1MassDeviation.allowedMassDeviation", value);
    }

    @Option(names = "--ppm-max-ms2", description = "Maximum allowed mass deviation in ppm for decomposing masses in MS2. If not specified, the same value as for the MS1 is used.")
    public void setPpmMaxMs2(String value) throws Exception {
        defaultConfigOptions.changeOption("MS2MassDeviation.allowedMassDeviation", value);
    }

    @Option(names = "--tree-timeout", description = "Time out in seconds per fragmentation tree computations. 0 for an infinite amount of time. Default: 0"/*, defaultValue = "0"*/)
    public void setTreeTimeout(String value) throws Exception {
        defaultConfigOptions.changeOption("Timeout.secondsPerTree", value);
    }

    @Option(names = "--compound-timeout", description = "Maximal computation time in seconds for a single compound. 0 for an infinite amount of time. Default: 0"/*, defaultValue = "0"*/)
    public void setInstanceTimeout(String value) throws Exception {
        defaultConfigOptions.changeOption("Timeout.secondsPerInstance", value);
    }

    @Option(names = "--no-recalibration", description = "Disable Recalibration of input Spectra")
    public void disableRecalibration(boolean disable) throws Exception {
        defaultConfigOptions.changeOption("ForbidRecalibration", String.valueOf(disable));
    }

    @Option(names = {"-p", "--profile"}, description = "Name of the configuration profile. Some of the default profiles are: 'qtof', 'orbitrap', 'fticr'.")
    public void setProfile(String value) throws Exception {
        defaultConfigOptions.changeOption("AlgorithmProfile", value);
    }

    // candidates
    @Option(names = {"-c", "--candidates"}, description = "Number of formula candidates in the output.")
    public void setNumberOfCandidates(String value) throws Exception {
        defaultConfigOptions.changeOption("NumberOfCandidates", value);
    }

    @Option(names = "--candidates-per-ion", description = "Minimum number of candidates in the output for each ionization. Set to force output of results for each possible ionization, even if not part of highest ranked results.")
    public void setNumberOfCandidatesPerIon(String value) throws Exception {
        defaultConfigOptions.changeOption("NumberOfCandidatesPerIon", value);
    }

    // Elements
    @Option(names = {"-e", "--elements-considered"}, description = "Set the allowed elements for rare element detection. Write SBrClBSe to allow the elements S,Br,Cl,B and Se.")
    public void setDetectableElements(List<String> elements) throws Exception {
        defaultConfigOptions.changeOption("FormulaSettings.detectable", elements);
        defaultConfigOptions.changeOption("FormulaSettings.fallback", elements);
    }

    @Option(names = {"-E", "--elements-enforced"}, description = "Enforce elements for molecular formula determination. Write CHNOPSCl to allow the elements C, H, N, O, P, S and Cl. Add numbers in brackets to restrict the minimal and maximal allowed occurrence of these elements: CHNOP[5]S[8]Cl[1-2]. When one number is given then it is interpreted as upper bound. Default is CHNOP")
    public void setEnforcedElements(List<String> elements) throws Exception {
        defaultConfigOptions.changeOption("FormulaSettings.enforced", elements);
    }

    @Option(names = {"-f", "--formula", "--formulas"}, description = "Specify the neutral molecular formula of the measured compound to compute its tree or a list of candidate formulas the method should discriminate. Omit this option if you want to consider all possible molecular formulas")
    public List<String> formulaWhiteList;


    @Option(names = {"--no-isotope-filter"}, description = "Disable molecular formula filter. When filtering is enabled, molecular formulas are excluded if their theoretical isotope pattern does not match the theoretical one, even if their MS/MS pattern has high score.")
    public void disableIsotopeFilter(boolean disable) throws Exception {
        defaultConfigOptions.changeOption("IsotopeSettings.filter", String.valueOf(!disable));
    }

    @Option(names = {"--no-isotope-score"}, description = "Disable isotope pattern score.")
    public void disableIsotopeScore(boolean disable) throws Exception {
        if (disable)
            defaultConfigOptions.changeOption("IsotopeSettings.multiplier", "0");
    }

    //Adducts
    @Option(names = {"-i", "--ions-considered"}, description = "the iontype/adduct of the MS/MS data. Example: [M+H]+, [M-H]-, [M+Cl]-, [M+Na]+, [M]+. You can also provide a comma separated list of adducts.")
    public void setIonsConsidered(List<String> adducts) throws Exception {
        defaultConfigOptions.changeOption("AdductSettings.detectable", adducts);
    }

    @Option(names = {"-I", "--ions-enforced"}, description = "the iontype/adduct of the MS/MS data. Example: [M+H]+, [M-H]-, [M+Cl]-, [M+Na]+, [M]+. You can also provide a comma separated list of adducts.")
    public void setIonsEnforced(List<String> adducts) throws Exception {
        defaultConfigOptions.changeOption("AdductSettings.enforced", adducts);
    }


    // some hidden parameters
    @Option(names = "--disable-element-detection", hidden = true)
    public void disableElementDetection(boolean disable) throws Exception {
        if (disable)
            defaultConfigOptions.changeOption("FormulaSettings.detectable", " , ");
    }

    @Option(names = "--enable-silicon-detection", hidden = true) //todo schliesst sich aus mit disable-element-detection
    public void enableSiliconDetection(boolean enable) throws Exception {
        if (enable) {
            String value = defaultConfigOptions.config.getConfigValue("FormulaSettings.detectable");
            if (value.isEmpty())
                defaultConfigOptions.changeOption("FormulaSettings.detectable", "Si");
            else if (!value.contains("Si"))
                defaultConfigOptions.changeOption("FormulaSettings.detectable", value + ",Si");
        }
    }

    @Option(names = "--trust-ion-prediction", description = "By default we use MS1 information to select additional ionizations ([M+Na]+,[M+K]+,[M+Cl]-,[M+Br]-) for considerations. With this parameter we trust the MS1 prediction and only consider these found ionizations.", hidden = true)
    public void setTrustGuessIonFromMS1(boolean trust) {
        throw new IllegalArgumentException("Parameter not implemented!");
        //todo manipulate adduct lists for marcus?????
    }

    @Option(names = {"--mostintense-ms2"}, description = "Only use the fragmentation spectrum with the most intense precursor peak (for each compound).", hidden = true)
    public boolean mostIntenseMs2;

    @Option(names = "--disable-fast-mode", hidden = true)
    public boolean disableFastMode;


    @Override
    public InstanceJob.Factory call() throws Exception {
        return SiriusSubToolJob::new;
    }

}
