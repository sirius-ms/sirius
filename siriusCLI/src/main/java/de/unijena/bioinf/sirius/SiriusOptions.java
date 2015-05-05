package de.unijena.bioinf.sirius;

import com.lexicalscope.jewel.cli.Option;

/**
 * Created by kaidu on 04.05.2015.
 */
public interface SiriusOptions {

    @Option(longName = "no-recalibrate", description = "do not recalibrate the spectrum")
    public boolean isNotRecalibrating();

    @Option(longName = "ppm-max", description = "allowed ppm for decomposing masses", defaultToNull = true)
    public Double getPPMMax();

    @Option(longName = "noise", description = "median intensity of noise peaks", defaultToNull = true)
    public Double getMedianNoise();

    @Option(shortName = "Z", longName = "auto-charge", description = "Use this option if the charge of your compounds is unknown and you do not want to assume [M+H]+ as default. With the auto charge option SIRIUS will not care about charges and allow arbitrary adducts for the precursor peak.")
    public boolean isAutoCharge();

}
