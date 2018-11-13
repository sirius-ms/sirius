package de.unijena.bioinf.ms.utils;

import com.lexicalscope.jewel.cli.Option;
import com.lexicalscope.jewel.cli.Unparsed;

import java.util.List;

public interface ConvertToMgfOptions {

    @Option(longName = "write-ms1", description = "write MS1 spectra into file.")
    boolean isWriteMs1();

    @Option(longName = "merge-ms2", description = "merge all MS2 of a compound into one single spectrum.")
    boolean isMergeMs2();

    @Option(longName = "merge-ppm", description = "maximum allowed deviation (in ppm) for peaks of MS2 spectra to be merged.", defaultValue = "10")
    Double getPPMMerge();

    @Option(longName = "merge-abs", description = "maximum allowed absolute difference for peaks of MS2 spectra to be merged.", defaultValue = "0.005")
    Double getPPMMergeAbs();

    @Option(shortName = "h", longName = "help", helpRequest = true)
    boolean isHelp();

    @Option(longName = "output")
    String getOutput();

    @Unparsed
    List<String> getInput();

}
