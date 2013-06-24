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
   ftc directory
   ftc file1.ms file2.ms
   ftc file*.ms

   # compute n best trees
   ftc --trees=5

   # compute only correct/optimal tree
   ftc --tree

   # set measurement profile instructions
   ftc --ppm.ms1 5 --ppm.ms2 10 --elements CHNOPSIBr[0-2] file.ms

   # load default profiles
   ftc --profile qTof file.ms

*/
public interface Options {

    @Unparsed
    public List<File> getFiles();

    @Option(description = "If set, the optimal tree is written on disk. If the correct molecular formula is given, the correct tree is written instead.")
    public boolean getTree();

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



}
