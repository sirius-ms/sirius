package de.unijena.bioinf.ms.utils;

import com.lexicalscope.jewel.cli.Option;
import com.lexicalscope.jewel.cli.Unparsed;

import java.util.List;

public interface MergingSpectraOptions {


    @Option(longName = "ppm-ms1", description = "maximum allowed deviation in ppm for peaks in MS1 to be merged", defaultValue = "5")
    Double getPPMMaxMs1();

    @Option(longName = "ppm-ms2", description = "maximum allowed deviation in ppm for peaks in MS2 to be merged", defaultValue = "10")
    Double getPPMMaxMs2();


    @Option(shortName = "h", longName = "help", helpRequest = true)
    boolean isHelp();


    @Option(longName = "output")
    String getOutput();

    @Unparsed
    List<String> getInput();
}
