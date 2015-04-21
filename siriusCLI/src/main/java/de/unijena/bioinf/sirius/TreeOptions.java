package de.unijena.bioinf.sirius;

import com.lexicalscope.jewel.cli.Option;
import com.lexicalscope.jewel.cli.Unparsed;

import java.io.File;
import java.util.List;

public interface TreeOptions {

    @Option(longName = "no-recalibrate", description = "do not recalibrate the spectrum")
    public boolean isNotRecalibrating();

    @Option(longName = "ppm-max", description = "allowed ppm for decomposing masses", defaultToNull = true)
    public Double getPPMMax();

    @Option(longName = "ppm-sd", description = "average difference between peak mass and exact mass", defaultToNull = true)
    public Double getPPMSd();

    @Option(longName = "noise", description = "median intensity of noise peaks", defaultToNull = true)
    public Double getMedianNoise();

    public String getChemicalAlphabet();

    @Option(shortName = "p", description = "name of the configuration profile. Some of the default profiles are: 'qtof', 'orbitrap', 'fticr'.", defaultValue = "default")
    public String getProfile();

    @Option(shortName = "h", longName = "help", helpRequest = true)
    public boolean isHelp();

    @Unparsed
    public List<String> getInput();

    @Option(shortName = "t", description = "target directory/filename for the output", defaultValue = ".")
    public File getTarget();

    @Option(shortName = "o", description = "file format of the output. Available are 'dot' and 'json' for trees and 'txt' and 'csv' for spectra", defaultToNull = true)
    public String getFormat();

    @Option(longName = "no-html", description = "only for DOT/graphviz output: Do not use html for node labels")
    public boolean isNoHTML();

    @Option(longName = "no-ion", description = "only for DOT/graphviz output: Print node labels as neutral formulas instead of ions")
    public boolean isNoIon();

}
