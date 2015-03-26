package de.unijena.bioinf.sirius.cli;

import com.lexicalscope.jewel.cli.Option;

import java.io.File;

public interface OutputOptions {

    @Option(shortName = "o", defaultToNull = true, description = "format of the output. If omitted, the format is estimated from the output file extension")
    public String getFormat();

    @Option(shortName = "O", defaultValue = ".", description = "target of the output. For single output, this is should be a file name, for multiple outputs this is a directory path")
    public File getOutput();

}
