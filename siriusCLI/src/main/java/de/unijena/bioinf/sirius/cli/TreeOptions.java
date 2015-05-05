package de.unijena.bioinf.sirius.cli;

import com.lexicalscope.jewel.cli.Option;
import de.unijena.bioinf.sirius.SiriusOptions;

import java.io.File;

public interface TreeOptions extends InputOptions, SiriusOptions {

    @Option(shortName = "h", longName = "help", helpRequest = true)
    public boolean isHelp();

    @Option(shortName = "o", description = "target directory/filename for the output", defaultToNull = true)
    public File getOutput();

    @Option(shortName = "f", description = "file format of the tree output. Available are 'dot' and 'json'", defaultToNull = true)
    public String getFormat();

    @Option(shortName = "a", longName = "annotate", description = "if set, a csv file is  created additional to the trees. It contains all annotated peaks together with their explanation ")
    public boolean isAnnotating();

    @Option(longName = "no-html", description = "only for DOT/graphviz output: Do not use html for node labels")
    public boolean isNoHTML();

    @Option(longName = "no-ion", description = "only for DOT/graphviz output: Print node labels as neutral formulas instead of ions")
    public boolean isNoIon();

    @Option(shortName = "p", description = "name of the configuration profile. Some of the default profiles are: 'qtof', 'orbitrap', 'fticr'.", defaultValue = "default")
    public String getProfile();

}
