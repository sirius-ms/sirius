package de.unijena.bioinf.ms.utils;

import com.lexicalscope.jewel.cli.Option;
import com.lexicalscope.jewel.cli.Unparsed;

public interface DefaultMSInputOutputOptions {

    @Option(shortName = "h", longName = "help", helpRequest = true)
    boolean isHelp();

    @Option(longName = "output", description = "output file in .ms format")
    String getOutput();

    @Unparsed(description = "input data in ms or mgf format")
    String getInput();
}
