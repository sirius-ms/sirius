package de.unijena.bioinf.ms.utils;

import com.lexicalscope.jewel.cli.Option;
import com.lexicalscope.jewel.cli.Unparsed;

import java.util.List;

public interface CompoundQualityOptions {


    @Option(
            longName = {"ms2-median-noise"},
            description = "Set MS2 median noise intensity - else it is estimated. This is used to count the number of MS2 peaks to gauge spectrum quality.",
            defaultToNull = true
    )
    Double getMedianNoiseIntensity();

    @Option(
            longName = {"isolation-window-width"},
            description = "width of the isolation window to measure MS2",
            defaultToNull = true
    )
    Double getIsolationWindowWidth();

    @Option(
            longName = {"isolation-window-shift"},
            description = "The shift applied to the isolation window to measure MS2 in relation to the precursormass",
            defaultValue = "0"
    )
    double getIsolationWindowShift();

    @Option(shortName = "h", longName = "help", helpRequest = true)
    boolean isHelp();

    @Option(longName = "output")
    String getOutput();

    @Unparsed
    String getInput();
}
