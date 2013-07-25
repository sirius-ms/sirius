package de.unijena.bioinf.siriuscli;

import com.lexicalscope.jewel.cli.Option;

public interface BasicOptions {
    @Option(longName = "verbose", shortName = "v")
    public boolean getVerbose();

    @Option(shortName = "h", helpRequest = true)
    public boolean getHelp();

    @Option
    public boolean getVersion();

    @Option
    public boolean getCite();
}
