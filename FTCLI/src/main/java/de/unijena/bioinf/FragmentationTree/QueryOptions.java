package de.unijena.bioinf.FragmentationTree;

import com.lexicalscope.jewel.cli.Option;
import com.lexicalscope.jewel.cli.Unparsed;
import de.unijena.bioinf.babelms.chemdb.Databases;

import java.io.File;
import java.util.List;

/**
 * TODO: Maybe export this code in an own module?
 */
public interface QueryOptions {

    @Option(shortName = "D", description = "use database", defaultValue="PUBCHEM")
    public Databases getDatabase();

    @Option(defaultToNull = true, description = "directory with cache file")
    public File getCachingDirectory();

    @Option(longName = "ppm", shortName = "p", defaultValue = "20", description = "relative mass error in ppm")
    public int getPPM();

    @Option(longName = "abs", shortName = "a", defaultValue = "0.001",description = "absolute mass error in Dalton")
    public double getAbsoluteDeviation();

    @Unparsed(description = "a list of masses and/or molecular formulas to query")
    public List<String> getQueries();





}
