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

import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import de.unijena.bioinf.sirius.IsotopePatternHandling;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.util.List;


public class SiriusOptions {

    //////////////////
    // OUTPUT OPTIONS
    //////////////////

    @Option(names = "-q", description = "surpress shell output")
    public boolean quiet = false;

    @Option(names = "-o", description = "output directory.")
    public String output = null;


    @Option(names = {"--sirius", "--workspace", "-w"}, description = "store workspace into given file, such that it can be loaded by SIRIUS GUI afterwards")
    public String sirius = null;


    @Option(names = {"-s", "--isotope"}, description = "how to handle isotope pattern data. Use 'score' to use them for ranking or 'filter' if you just want to remove candidates with bad isotope pattern. With 'both' you can use isotopes for filtering and scoring (default). Use 'omit' to ignore isotope pattern.")
    IsotopePatternHandling isotopes = IsotopePatternHandling.both;

    @Option(names = {"-c", "--candidates"}, description = "Number of candidates in the output")
    Integer numberOfCandidates = null;

    @Option(names = {"-f", "--formula", "--formulas"}, description = "specify the neutral molecular formula of the measured compound to compute its tree or a list of candidate formulas the method should discriminate. Omit this option if you want to consider all possible molecular formulas")
    List<String> formula = null;

    @Option(names = "--no-recalibration")
    boolean notRecalibrating = false;

    @Option(names = "--ppm-max", description = "allowed ppm for decomposing masses")
    Double ppmMax = null;

    @Option(names = "--ppm-max-ms2", description = "allowed ppm for decomposing masses in MS2. If not specified, the same value as for the MS1")
    Double ppmMaxMs2 = null;

    @Option(names = "--noise", description = "median intensity of noise peaks")
    Double medianNoise = null;

    @Option(names = {"-Z", "--auto-charge"}, description = "Use this option if the adduct type and/or ion mode of your compounds is unknown and you do not want to assume [M+H]+/[M-H]- as default. With the option enabled, SIRIUS will also search for other adduct types (e.g. [M+NH3+H]+ or even other ion modes (e.g. [M+Na]+) if no ion mode is specified.")
    boolean autoCharge = false;


    @Option(names = "-p", description = "name of the configuration profile. Some of the default profiles are: 'qtof', 'orbitrap', 'fticr'.")
    String profile = "default";

    @Option(names = "--disable-fast-mode", hidden = true)
    public boolean disableFastMode = false;

    @Option(names = {"-1", "--ms1"}, description = "MS1 spectrum file name") //min0
            List<File> ms1 = null;

    @Option(names = {"-2", "--ms2"}, description = "MS2 spectra file names")//min0
            List<File> ms2 = null;

    @Option(names = {"-z", "--parentmass", "precursor", "mz"}, description = "the mass of the parent ion")
    Double parentMz = null;

    @Option(names = {"-i", "--ion"}, description = "the ionization/adduct of the MS/MS data. Example: [M+H]+, [M-H]-, [M+Cl]-, [M+Na]+, [M]+. You can also provide a comma separated list of adducts.")
    List<String> ion = null;

    @Option(names = "--tree-timeout", description = "Time out in seconds per fragmentation tree computations. 0 for an infinite amount of time. Default: 0", defaultValue = "0")
    int treeTimeout = 0;

    @Option(names = "--compound-timeout", description = "Maximal computation time in seconds for a single compound. 0 for an infinite amount of time. Default: 0", defaultValue = "0")
    int getInstanceTimeout = 0;

    @Option(names = {"-e", "--elements"}, description = "The allowed elements. Write CHNOPSCl to allow the elements C, H, N, O, P, S and Cl. Add numbers in brackets to restrict the minimal and maximal allowed occurence of these elements: CHNOP[5]S[8]Cl[1-2]. When one number is given then it is interpreted as upperbound.")
    FormulaConstraints elements = null;

    @Option(names = "--maxmz", description = "Just consider compounds with a precursor mz lower or equal this maximum mz. All other compounds in the input file are ignored.")
    Double maxMz = null;

    @Option(names = {"--mostintense-ms2"}, description = "Only use the fragmentation spectrum with the most intense precursor peak (for each compound).")
    boolean mostIntenseMs2 = false;

    @Option(names = "--trust-ion-prediction", description = "By default we use MS1 information to select additional ionizations ([M+Na]+,[M+K]+,[M+Cl]-,[M+Br]-) for considerations. With this parameter we trust the MS1 prediction and only consider these found ionizations.")
    boolean trustGuessIonFromMS1 = false;


    //naming
    @Option(names = "--naming-convention", description = "Specify a format for compounds' output directorys. Default %index_%filename_%compoundname")
    String namingConvention = null;


    //technical stuff
    @Option(names = {"--processors", "--cores"}, description = "Number of cpu cores to use. If not specified Sirius uses all available cores.")
    int numOfCores = 0;

    @Option(names = "--max-compound-buffer", description = "Maxmimal number of compounds that will be buffered in Memory. A larger buffer ensures that there are enough compounds available to use all cores efficiently during computation. A smaller buffer saves Memory. For Infinite buffer size set it to 0. Default: 2 * --initial_intance_buffer")
    Integer maxInstanceBuffer = null;

    @Option(names = "--initial-compound-buffer", description = "Number of compounds that will be loaded initially into the Memory. A larger buffer ensures that there are enough compounds available to use all cores efficiently during computation. A smaller buffer saves Memory. To load all compounds immediately set it to 0. Default: 2 * --cores")
    Integer minInstanceBuffer = null;

    ///// some hidden parameters

    @Option(names = "--disable-element-detection", hidden = true)
    public boolean disableElementDetection = false;

    @Option(names = "--enable-silicon-detection", hidden = true)
    public boolean enableSiliconDetection = false;

    @Option(names = {"--isolation-window-width"}, description = "width of the isolation window to measure MS2", hidden = true)
    Double isolationWindowWidth = null;

    @Option(names = {"--isolation-window-shift"}, description = "The shift applied to the isolation window to measure MS2 in relation to the precursormass", hidden = true)
    double isolationWindowShift = 0;

    @Option(names = {"--assess-data-quality"}, description = "produce stats on quality of spectra and estimate isolation window. Needs to read all data at once.", hidden = true)
    boolean assessDataQuality = false;


    //standard stuff
    @Option(names = {"-h", "--help"}, usageHelp = true)
    boolean help;

    @Option(names = "--version", versionHelp = true)
    boolean version = false;

    @Option(names = "--cite")
    boolean cite = false;

    @Parameters(hidden = true)
    List<String> input;

}
