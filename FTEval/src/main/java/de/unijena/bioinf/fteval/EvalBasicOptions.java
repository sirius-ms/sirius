package de.unijena.bioinf.fteval;

import com.lexicalscope.jewel.cli.Option;

import java.io.File;

public interface EvalBasicOptions {

    @Option(shortName = "d", defaultValue = ".")
    public File getDataset();

}
