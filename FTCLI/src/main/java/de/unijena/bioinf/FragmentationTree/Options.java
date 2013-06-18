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
   ftc --tree=5

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

    @Option
    public int getTree();

    @Option(defaultValue = "CHNOPS")
    public String getElements();

    public boolean isTree();



}
