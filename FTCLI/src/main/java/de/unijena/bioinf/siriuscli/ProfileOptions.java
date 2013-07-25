package de.unijena.bioinf.siriuscli;

import com.lexicalscope.jewel.cli.Option;

public interface ProfileOptions {

    @Option(shortName = "p", defaultValue = "default", description =
            "A profile contains all scoring and preprocessing information that is necessary for the given data. " +
                    "It is either a profile.json file or the name of a predefined profile. Predefined profiles are: " +
                    "default, qtof, qtof.high, orbitrap")
    public String getProfile();

    /*
        ppm
     */
    @Option(longName = "ppm.max", defaultToNull = true, description = "maximal ppm of peaks (used for decompositions)")
    public Double getPPMMax();

    @Option(longName = "abs.max", defaultToNull = true, description = "maximal mass deviation of peaks (used for decomposition)")
    public Double getAbsMax();

    @Option(longName = "ppm.sd.ms2", defaultToNull = true, description = "ppm standard deviation of ms2 peaks")
    public Double getStandardDeviationOfMs1();

    @Option(longName = "ppm.sd.ms1", defaultToNull = true, description = "ppm standard deviation of ms1 peaks")
    public Double getStandardDeviationOfMs2();

    @Option(longName = "ppm.sd.diff", defaultToNull = true, description = "ppm standard deviation of ms1 peak differences (~recalibrated peaks)")
    public Double getStandardDeviationOfDiff();

    @Option(longName = "intensity.sd", defaultToNull = true, description = "intensity standard deviation of ms1 peaks")
    public Double getExpectedIntensityDeviation();

    @Option(longName = "noise.median", defaultToNull = true, description = "median intensity of noise peaks (above certain threshold)")
    public Double getNoiseMedian();

    @Option(longName = "treeSize", defaultToNull = true, description = "additional score bonus per explained peak. Higher values leads to bigger trees.")
    public Double getTreeSize();

}
