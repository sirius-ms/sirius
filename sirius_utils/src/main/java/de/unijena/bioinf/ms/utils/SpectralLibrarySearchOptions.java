package de.unijena.bioinf.ms.utils;

import com.lexicalscope.jewel.cli.Option;
import com.lexicalscope.jewel.cli.Unparsed;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public interface SpectralLibrarySearchOptions {

    @Option(shortName = "h", longName = "help", helpRequest = true)
    boolean isHelp();

    @Option(longName = "library", description = "directory or file of annotated library hits.")
    String getLibraryHitsDir();

    @Option(longName = "cosine", description = "minimum cosine.", defaultValue = "0.7")
    double getMinCosine();

    @Option(longName = "method", description = "how to compute cosine. intensity weighted Gaussian (gaussian) or intensity weighted alignment (alignemnt)", defaultValue = "gaussian")
    String getMethod();

    @Option(longName = "biotransformations", description = "allow for mz difference between compound and library hit which matches a known biotransformation.")
    boolean isAllowBiotransformations();

    @Option(longName = "transformationlist", description = "molecular formulas of allowed biotransformation to override default list.", defaultToNull = true)
    List<String> getAllowedTransformations();

    @Option(longName = "max-difference", description = "compare compound with any library hit wiht precursor mass difference smaller or equal this value.", defaultToNull = true)
    Double getMaxDifference();

    @Option(longName = "search-transformed", description = "Changes scoring from using the mean of cosines of normal and inverted spectra to taking the max")
    boolean isSearchTransformed();

    @Option(longName = "force-mf-explanation", description = "if the mass difference between compound and library hit is not a known biotransformation try to decompose it")
    boolean isForceMFExplantion();


    @Option(longName = "ppm-ms2", description = "different meaning dependent on method", defaultValue = "20")
    double getPPMMs2();

    @Option(longName = "ppm-ms1", description = "different meaning dependent on method", defaultValue = "10")
    double getPPMMs1();

    @Option(longName = "min-matched-peaks", description = "minimum number of peaks to match against a library spectrum", defaultValue = "5")
    int getMinMatchedPeaks();

    @Option(longName = "output", description = "output file with hits")
    String getOutput();


    @Unparsed(description = "input spectra file")
    String getInput();
}
