package de.unijena.bioinf.ms.utils;

import com.lexicalscope.jewel.cli.Option;
import com.lexicalscope.jewel.cli.Unparsed;

import java.util.List;

public interface MergingOptions {


    @Option(longName = "ppm-max", description = "maximum allowed deviation in ppm for two compounds to be merged", defaultValue = "20")
    Double getPPMMax();

    @Option(longName = "ppm-ms2", description = "maximum allowed deviation in ppm for peaks of different MS2 to be merged or compared", defaultValue = "15")
    Double getPPMMerge();


    @Option(longName = "rt-max", description = "maximum allowed retention time (in s) difference for two compounds to be merged", defaultValue = "20")
    Double getRTMax();

    @Option(longName = "cosine", description = "minimum allowed MS2 cosine similarity for two compounds to be merged. [0.0-1.0]", defaultValue = "0.95")
    Double getMinCosine();

    @Option(longName = "within-run", description = "Merge compounds within a LC/MS/MS run.")
    boolean isMergeWithin();

    @Option(shortName = "h", longName = "help", helpRequest = true)
    boolean isHelp();


    @Option(longName = "output")
    String getOutput();

    @Unparsed
    List<String> getInput();
}
