package de.unijena.bioinf.ms.utils;

import com.lexicalscope.jewel.cli.Option;

public interface UtilsOptions {

    @Option(longName = "merge", description = "merge compounds from multiple runs, which are likely the same molecular structure.")
    boolean isMerge();

    @Option(longName = "convert-to-mgf", description = "Converts the input into a mgf file.")
    boolean isConvertToMgf();

    @Option(longName = "filter", description = "Filter compounds.")
    boolean isFilter();

    @Option(longName = "map", description = "Map compound ids between different datasets by mz and rt.")
    boolean isMap();

    @Option(shortName = "h", longName = "help", helpRequest = true)
    boolean isHelp();

}
