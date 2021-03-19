package de.unijena.bioinf.ms.utils;

import com.lexicalscope.jewel.cli.Option;
import com.lexicalscope.jewel.cli.Unparsed;

public interface FilterOptions {

    @Option(longName = "feature-filter", description = "tsv table with features to filter (e.g. blanks, chemical noise). File must contain rt,mz,intensity, with header.", defaultToNull=true)
    String getBlankFeaturesFile();

    @Option(longName = "min-fold-difference", description = "Minimum intensity fold change to keep a compound, even if mz and rt match a blank feature.", defaultValue = "2.0")
    Double getMinFoldDifference();

    @Option(longName = "ppm-max", description = "maximum allowed deviation in ppm for compound to be filtered against blank feature; or to find precursor peak in MS1 (for isotope filter)", defaultValue = "20")
    Double getPPMMax();

    @Option(longName = "ppm-max-diff", description = "maximum allowed difference in ppm between isotope peaks", defaultValue = "10")
    Double getPPMDiff();

    @Option(longName = "rt-max", description = "maximum allowed retention time difference (in s) for compound to be filtered against blank feature. If  below 0 rt differences are ignored.", defaultValue = "20")
    Double getRTMax();

    @Option(longName = "min-num-isotopes", description = "Minimum number of isotopes peaks which must be present to keep compound", defaultValue = "-1")
    int getMinNumberOfIsotopes();

    @Option(longName = "zero-intensity-filter", description = "Filter compounds whose precursor has zero intensity. uses ppm-max parameter")
    boolean isFilterZeroIntensity();

    @Option(longName = "no-ms2-filter", description = "Filter compounds without MS2. Note: other filters might remove all MS2 of a compound.")
    boolean isFilterCompoundsWithoutMs2();

    @Option(longName = "chimeric-filter", description = "Filter chimeric compounds. " +
            "A compound is chimeric if another peak in the isolation window is at least 33% intensity of the precursor" +
            " or if the sum of all peaks is at least 100% of the precursor's intensity. " +
            "Uses ppm-max-diff to filter isotopes in isolation window")
    boolean isFilterChimeric();

    @Option(longName = "ms1-baseline", description = "Filter peaks in MS1 below this intensity (does not include merged MS1)", defaultToNull = true)
    Double getMs1Baseline();

    @Option(longName = "ms2-baseline", description = "Filter peaks in MS2 below this intensity.", defaultToNull = true)
    Double getMs2Baseline();


    @Option(longName = "output")
    String getOutput();

    @Unparsed
    String getInput();


    @Option(shortName = "h", longName = "help", helpRequest = true)
    boolean isHelp();
}
