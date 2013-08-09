package de.unijena.bioinf.fteval;

import com.lexicalscope.jewel.cli.Option;
import com.lexicalscope.jewel.cli.Unparsed;

import java.io.File;

public interface InitOptions {

    @Unparsed
    public String getName();

    @Option(defaultToNull = true)
    public File getSdf();

    @Option(defaultToNull = true)
    public File getMs();

}
