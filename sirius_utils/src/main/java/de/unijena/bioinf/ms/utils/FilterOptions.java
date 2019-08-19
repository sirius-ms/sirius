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

    @Option(longName = "start-rt", description = "Remove all compounds below this specific rt (to remove start of LC).", defaultToNull = true)
    Double getStartRT();

    @Option(longName = "end-rt", description = "Remove all compounds above this specific rt (to remove end of LC).", defaultToNull = true)
    Double getEndRT();

    @Option(longName = "min-num-isotopes", description = "Minimum number of isotopes peaks which must be present to keep compound", defaultValue = "-1")
    int getMinNumberOfIsotopes();

    @Option(longName = "zero-intensity-filter", description = "Remove compounds whose precursor has zero intensity. uses ppm-max parameter")
    boolean isFilterZeroIntensity();

    @Option(longName = "no-ms2-filter", description = "Remove compounds without MS2. Note: other filters might remove all MS2 of a compound.")
    boolean isFilterCompoundsWithoutMs2();

    @Option(longName = "chimeric-filter", description = "Filter chimeric compounds. " +
            "A compound is chimeric if another peak in the isolation window is at least 33% intensity of the precursor" +
            " or if the sum of all peaks is at least 100% of the precursor's intensity. " +
            "Uses ppm-max-diff to filter isotopes in isolation window")
    boolean isFilterChimeric();

    //window used to filter chimerics
    @Option(
            longName = {"isolation-window-width"},
            description = "width of the isolation window to measure MS2",
            defaultToNull = true
    )
    Double getIsolationWindowWidth();

    @Option(
            longName = {"isolation-window-shift"},
            description = "The shift applied to the isolation window to measure MS2 in relation to the precursormass",
            defaultValue = "0"
    )
    double getIsolationWindowShift();


    @Option(longName = "min-precursor-intensity-abs", description = "Remove MS1 and corresponding MS2 where precursor peak intensity is below threshold", defaultToNull = true)
    Double getMinPrecursorIntAbs();

    @Option(longName = "min-precursor-intensity-rel", description = "Remove MS1 and corresponding MS2 where precursor peak intensity is below threshold, relative value in [0,1]", defaultToNull = true)
    Double getMinPrecursorIntRel();

    @Option(longName = "min-tic", description = "Remove MS2 by total ion count (summed intensities). Is applied AFTER applying baseline.", defaultToNull = true)
    Double getMinTic();

    @Option(longName = "total-min-tic", description = "Remove all compounds with sum of MS2 total ion count (summed intensities). Is applied AFTER applying baseline.", defaultToNull = true)
    Double getMinTicTotal();

    @Option(longName = "ms1-baseline", description = "Filter peaks in MS1 below this intensity (does not include merged MS1)", defaultToNull = true)
    Double getMs1Baseline();

    @Option(longName = "ms2-baseline", description = "Filter peaks in MS2 below this intensity.", defaultToNull = true)
    Double getMs2Baseline();

    @Option(longName = "remove-ms2-isotopes", description = "Remove isotope peaks from MS2.")
    boolean isRemoveIsotopes();

    @Option(longName = "chnops-only", description = "Assume CHNOPS when filtering isotopes.")
    boolean isCHNOPSOnly();

    @Option(longName = "output")
    String getOutput();

    @Unparsed
    String getInput();


    @Option(shortName = "h", longName = "help", helpRequest = true)
    boolean isHelp();
}
