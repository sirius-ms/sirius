package de.unijena.bioinf.FragmentationTree;

import com.lexicalscope.jewel.cli.Option;
import com.lexicalscope.jewel.cli.Unparsed;
import de.unijena.bioinf.babelms.chemdb.Databases;
import de.unijena.bioinf.sirius.cli.BasicOptions;
import de.unijena.bioinf.sirius.cli.ProfileOptions;
import de.unijena.bioinf.siriuscli.OutputOptions;

import java.io.File;
import java.util.List;

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
public interface Options extends BasicOptions, ProfileOptions,OutputOptions {

    @Unparsed
    public List<File> getFiles();

    @Option(shortName = "t", defaultValue = ".", description = "target directory for the output data")
    public File getTarget();

    @Option(shortName = "n", defaultValue = "1", description = "number of threads that should be used for computation")
    public int getThreads();

    @Option(shortName = "w", description = "If set, the first <value> trees are written on disk.", defaultValue = "0")
    public int getTrees();

    @Option(shortName = "D", description = "use database", defaultValue="NONE")
    public Databases getDatabase();

    @Option(defaultToNull = true, description = "directory with cache file")
    public File getCachingDirectory();

    @Option(shortName = "F", defaultValue = "0")
    public int getForceExplainedIntensity();

    @Option(hidden = true, shortName = "C")
    public boolean isIsotopeFilteringCheat();

    @Option(hidden = true)
    public boolean isWriteGraphInstances();

    @Option()
    public boolean getRecalibrate();

    @Option(description = "If correct formula is given, compute only trees with higher score than the correct one")
    public boolean getWrongPositive();

    @Option(hidden = true, defaultToNull = true)
    public File getRanking();

    @Option(hidden = true)
    public boolean isOldSirius();

    @Option(shortName = "I", defaultValue = "0", description = "compute trees for the <value>th molecular formulas with best isotope pattern explanations")
    public int getFilterByIsotope();

    @Option(shortName = "i", description = "enable isotope pattern analysis")
    public boolean getMs1();
}
