package de.unijena.bioinf.sirius.cli;

import com.lexicalscope.jewel.cli.Option;
import com.lexicalscope.jewel.cli.Unparsed;
import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import de.unijena.bioinf.sirius.IsotopePatternHandling;

import java.io.File;
import java.util.List;

public interface SiriusOptions {

    @Option
    public boolean isVersion();

    @Option(shortName = "s", longName = "isotope", defaultValue = "score", description = "how to handle isotope pattern data. Use 'score' to use them for ranking or 'filter' if you just want to remove candidates with bad isotope pattern. Use 'omit' to ignore isotope pattern.")
    public IsotopePatternHandling getIsotopes();

    @Option(shortName = "c", longName = "candidates", description = "number of candidates in the output", defaultToNull = true)
    public Integer getNumberOfCandidates();

    @Option(shortName = "f", longName = {"formula", "formulas"}, description = "specify the neutral molecular formula of the measured compound to compute its tree or a list of candidate formulas the method should discriminate. Omit this option if you want to consider all possible molecular formulas", defaultToNull = true)
    public List<String> getFormula();

    public boolean isNotRecalibrating();

    @Option(longName = "ppm-max", description = "allowed ppm for decomposing masses", defaultToNull = true)
    public Double getPPMMax();

    @Option(longName = "noise", description = "median intensity of noise peaks", defaultToNull = true)
    public Double getMedianNoise();

    @Option(shortName = "Z", longName = "auto-charge", description = "Use this option if the charge of your compounds is unknown and you do not want to assume [M+H]+ as default. With the auto charge option SIRIUS will not care about charges and allow arbitrary adducts for the precursor peak.")
    public boolean isAutoCharge();

    @Option(shortName = "h", longName = "help", helpRequest = true)
    public boolean isHelp();

    @Option(shortName = "o", description = "target directory/filename for the output", defaultToNull = true)
    public File getOutput();

    @Option(shortName = "O", description = "file format of the tree output. Available are 'dot' and 'json'", defaultToNull = true)
    public String getFormat();

    @Option(shortName = "a", longName = "annotate", description = "if set, a csv file is  created additional to the trees. It contains all annotated peaks together with their explanation ")
    public boolean isAnnotating();

    @Option(longName = "no-html", description = "only for DOT/graphviz output: Do not use html for node labels")
    public boolean isNoHTML();

    @Option(longName = "no-ion", description = "only for DOT/graphviz output: Print node labels as neutral formulas instead of ions")
    public boolean isNoIon();

    @Option(shortName = "p", description = "name of the configuration profile. Some of the default profiles are: 'qtof', 'orbitrap', 'fticr'.", defaultValue = "default")
    public String getProfile();

    @Option(shortName = "1", longName = "ms1", description = "MS1 spectrum file name", minimum = 0, defaultToNull = true)
    public List<File> getMs1();

    @Option(shortName = "2", longName = "ms2", description = "MS2 spectra file names", minimum = 0, defaultToNull = true)
    public List<File> getMs2();

    @Option(shortName = "z", longName = {"parentmass", "precursor", "mz"}, description = "the mass of the parent ion", defaultToNull = true)
    public Double getParentMz();

    @Option(shortName = "i", longName = "ion", description = "the ionization/adduct of the MS/MS data. Example: [M+H]+, [M-H]-, [M+Cl]-, [M+Na]+, [M]+.", defaultToNull =true)
    public String getIon();

    @Unparsed
    public List<String> getInput();


    @Option(shortName = "e", longName = "elements", description = "The allowed elements. Write CHNOPSCl to allow the elements C, H, N, O, P, S and Cl. Add numbers in brackets to restrict the maximal allowed occurence of these elements: CHNOP[5]S[8]Cl[1]", defaultToNull = true)
    public FormulaConstraints getElements();




}
