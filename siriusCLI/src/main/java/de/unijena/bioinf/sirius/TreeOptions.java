package de.unijena.bioinf.sirius;

import com.lexicalscope.jewel.cli.Option;
import de.unijena.bioinf.sirius.cli.InputOptions;

import java.io.File;

public interface TreeOptions extends InputOptions {

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

    @Option(shortName = "Z", longName = "auto-charge", description = "Use this option if the charge of your compounds is unknown and you do not want to assume [M+H]+ as default. With the auto charge option SIRIUS will not care about charges and allow arbitrary adducts for the precursor peak.")
    public boolean isAutoCharge();

    @Option(shortName = "t", description = "target directory/filename for the output", defaultValue = ".")
    public File getTarget();

    @Option(shortName = "o", description = "file format of the output. Available are 'dot' and 'json' for trees and 'txt' and 'csv' for spectra", defaultToNull = true)
    public String getFormat();

    @Option(longName = "no-html", description = "only for DOT/graphviz output: Do not use html for node labels")
    public boolean isNoHTML();

    @Option(longName = "no-ion", description = "only for DOT/graphviz output: Print node labels as neutral formulas instead of ions")
    public boolean isNoIon();

}
