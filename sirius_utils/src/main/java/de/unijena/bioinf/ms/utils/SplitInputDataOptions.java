package de.unijena.bioinf.ms.utils;

import com.lexicalscope.jewel.cli.Option;
import com.lexicalscope.jewel.cli.Unparsed;

public interface SplitInputDataOptions {

    @Option(
            longName = {"number-of-files"},
            description = "number of files to create",
            defaultToNull = true
    )
    int getNumberOfFiles();

    @Option(shortName = "h", longName = "help", helpRequest = true)
    boolean isHelp();

    @Option(longName = "output-prefix", description = "prefix for split output files in ms format")
    String getOutputPrefix();

    @Unparsed(description = "input data in ms or mgf format")
    String getInput();
}
