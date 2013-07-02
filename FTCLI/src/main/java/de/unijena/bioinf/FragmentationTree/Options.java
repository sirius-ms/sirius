package de.unijena.bioinf.FragmentationTree;

import com.lexicalscope.jewel.cli.ArgumentValidationException;
import com.lexicalscope.jewel.cli.Option;
import com.lexicalscope.jewel.cli.Unparsed;
import de.unijena.bioinf.ChemistryBase.chem.*;
import de.unijena.bioinf.ChemistryBase.chem.utils.ValenceFilter;
import de.unijena.bioinf.MassDecomposer.Interval;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
   # compute optimal/correct tree
   tree directory
   tree file1.ms file2.ms
   tree file*.ms

   # compute n best trees (including correct tree if formula is given)
   ftc --trees=5

   # set measurement profile instructions
   ftc --ppm.ms1 5 --ppm.ms2 10 --elements CHNOPSIBr[0-2] file.ms

   # load default profiles
   ftc --profile qTof file.ms

*/
public interface Options {

    @Unparsed
    public List<File> getFiles();

    @Option(defaultValue = "CHNOPS")
    public String getElements();

    @Option(shortName = "t", defaultValue = ".")
    public File getTarget();

    @Option(shortName = "n", defaultValue = "1", description = "number of threads that should be used for computation")
    public int getThreads();

    @Option(description = "If set, the first <value> trees are written on disk.", defaultValue = "0")
    public int getTrees();

    @Option(description = "If correct formula is given, compute only trees with higher score than the correct one")
    public boolean getWrongPositive();

    @Option(description = "Compute only trees with higher score than <value>", defaultValue = "0")
    public double getLowerbound();

    @Option(shortName = "I", defaultValue = "0", description = "compute trees for the <value>th molecular formulas with best isotope pattern explanations")
    public int getFilterByIsotope();

    @Option(shortName = "i", description = "enable isotope pattern analysis")
    public boolean getMs1();

    @Option(shortName = "p", defaultToNull = true, description = "A profile contains all scoring and preprocessing information that is necessary for the given data. It is either a profile.json file or the name of a predefined profile")
    public String getProfile();

    /*
        ppm
     */
    @Option(longName = "ppm.max", defaultValue = "10", description = "maximal ppm of peaks (used for decompositions)")
    public Double getPPMMax();

    @Option(longName = "abs.max", defaultToNull = true, description = "maximal mass deviation of peaks (used for decomposition)")
    public Double getAbsMax();

    @Option(longName = "ppm.sd.ms2", defaultToNull = true, description = "ppm standard deviation of ms2 peaks")
    public Double getStandardDeviationOfMs1();

    @Option(longName = "ppm.sd.ms1", defaultToNull = true, description = "ppm standard deviation of ms1 peaks")
    public Double getStandardDeviationOfMs2();

    @Option(longName = "ppm.sd.diff", defaultToNull = true, description = "ppm standard deviation of ms1 peak differences (~recalibrated peaks)")
    public Double getStandardDeviationOfDiff();

    @Option(longName = "intensity.sd", defaultValue = "0.02", description = "intensity standard deviation of ms1 peaks")
    public Double getExpectedIntensityDeviation();

    @Option(longName = "noise.median", defaultValue = "0.02", description = "median intensity of noise peaks (above certain threshold)")
    public Double getNoiseMedian();

    @Option(longName = "treeSize", defaultToNull = true, description = "additional score bonus per explained peak. Higher values leads to bigger trees.")
    public Double getTreeSize();

    @Option(longName = "verbose", shortName = "v")
    public boolean getVerbose();

    @Option(shortName = "h", helpRequest = true)
    public boolean getHelp();

    @Option
    public boolean getVersion();

    @Option
    public boolean getCite();



}
