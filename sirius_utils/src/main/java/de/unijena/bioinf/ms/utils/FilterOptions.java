package de.unijena.bioinf.ms.utils;

import com.lexicalscope.jewel.cli.Option;
import com.lexicalscope.jewel.cli.Unparsed;

import java.util.List;

public interface FilterOptions {

    @Option(longName = "feature-filter", description = "tsv table with features to filter (e.g. blanks). File must contain rt,mz,intensity, with header.", defaultToNull=true)
    String getBlankFeaturesFile();

    @Option(longName = "min-fold-difference", description = "Minimum intensity fold change to keep a compound, even if mz and rt match a blank feature.", defaultValue = "2.0")
    Double getMinFoldDifference();

    @Option(longName = "ppm-max", description = "maximum allowed deviation in ppm for compound to be filtered against blank feature; or to find precursor peak in MS1 (for isotope filter)", defaultValue = "20")
    Double getPPMMax();

    @Option(longName = "ppm-max-diff", description = "maximum allowed difference in ppm between isotope peaks", defaultValue = "10")
    Double getPPMDiff();

    @Option(longName = "rt-max", description = "maximum allowed retention time difference (in s) for compound to be filtered against blank feature.", defaultValue = "20")
    Double getRTMax();

    @Option(longName = "min-num-isotopes", description = "Minimum number of isotopes peaks which must be present to keep compound", defaultValue = "-1")
    int getMinNumberOfIsotopes();

    @Option(longName = "zero-intensity-filter", description = "Filter compounds which precursor has zero intensity. uses ppm-max parameter", defaultToNull = true)
    boolean isFilterZeroIntensity();

    @Option(longName = "output")
    String getOutput();

    @Unparsed
    String getInput();


    @Option(shortName = "h", longName = "help", helpRequest = true)
    boolean isHelp();
}
