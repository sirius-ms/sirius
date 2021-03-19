package de.unijena.bioinf.ms.utils;

import com.lexicalscope.jewel.cli.Option;
import com.lexicalscope.jewel.cli.Unparsed;

public interface ExtractChemicalNoiseOptions {

    @Option(
            longName = {"bin-size-ppm"},
            description = "MS1 peaks are binned. bins have equal size. size is the absolute value determined with given ppm at 200Da.",
            defaultToNull = true
    )
    Double getBinSizePPM();

    @Option(shortName = "h", longName = "help", helpRequest = true)
    boolean isHelp();

    @Option(longName = "output", description = "file with m/z values which are considered chemical noise.")
    String getOutput();

    @Unparsed(description = "mzML file of LC/MS/MS run.")
    String getInput();
}
