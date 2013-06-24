package de.unijena.bioinf.MassDecomposer.cli;

import com.lexicalscope.jewel.cli.Option;
import com.lexicalscope.jewel.cli.Unparsed;

public interface Options {
    @Unparsed
    public double getMass();

    @Option(longName = "ppm", shortName = "p", defaultValue = "20", description = "relative mass error in ppm")
    public int getPPM();

    @Option(longName = "abs", shortName = "a", defaultValue = "0.001",description = "absolute mass error in Dalton")
    public double getAbsoluteDeviation();

    @Option(longName = "nofilter", shortName = "n", description = "if set, the molecular formulas are not filtered by their chemical properties")
    public boolean getDontUseRDBE();

    @Option(longName = "filter", shortName="f", defaultValue = "common", description = "set the strictness of the chemical filter. Allowed values are strict < common  < permissive < rdbe < none.")
    public String getFilter();

    @Option(longName = "errors", shortName = "r", description = "print mass errors")
    public boolean getMassErrors();

    @Option(longName = "elements", shortName = "e", defaultValue = "CHNOPS", description = "elements which should be used in decomposition. Use syntax 'CH[min-max]N[min-]O[-max]P[num]'")
    public AlphabetParser getAlphabet();

    @Option(longName = "parent", defaultToNull = true, description = "If set, the decompositions have to be subformulas of the given parent formula.")
    public String getParentFormula();

    @Option(longName = "ion", shortName = "i", defaultToNull = true, description = "Ionization mode, e.g. [M+H]+")
    public String getIonization();

    @Option(longName = "version", description = "")
    public boolean getVersion();

    @Option(description = "")
    public boolean getCite();

    @Option(longName = "help", shortName = "h", helpRequest = true, description = "")
    public boolean getHelp();

}
