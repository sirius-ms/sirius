package de.unijena.bioinf.ms.utils;

import com.lexicalscope.jewel.cli.Option;
import com.lexicalscope.jewel.cli.Unparsed;

public interface MapOptions {

    @Option(longName = "ppm-max", description = "maximum allowed deviation in ppm for compounds to be considered to be the same.", defaultValue = "5")
    Double getPPMMax();

    @Option(longName = "rt-max", description = "maximum allowed retention time difference (in s) for compounds to be considered to be the same.", defaultValue = "20")
    Double getRTMax();

    @Option(longName = "input1", description = "dataset 1.")
    String getDataset1();

    @Option(longName = "input2", description = "dataset 2.")
    String getDataset2();

    @Option(longName = "output")
    String getOutput();

    @Option(shortName = "h", longName = "help", helpRequest = true)
    boolean isHelp();
}
