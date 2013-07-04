package de.unijena.bioinf.FTAnalysis;

import com.lexicalscope.jewel.cli.Option;
import com.lexicalscope.jewel.cli.Unparsed;

import java.io.File;
import java.util.List;

public interface LearnOptions {

    @Unparsed
    public List<File> getTrainingdata();

    @Option(shortName = "p", defaultToNull = true, description = "initial profile to start learning")
    public String getProfile();

    @Option(shortName = "w", description = "write trees")
    public boolean isTrees();

    @Option(shortName = "i", defaultValue = "4", description = "number of iterations")
    public int getIterations();

    @Option(shortName = "I", defaultValue = "3", description = "number of iterations for common loss detection and loss size distribution estimation")
    public int getLossSizeIterations();

    @Option(shortName = "t", defaultValue = ".", description = "target directory")
    public File getTarget();

    @Option(shortName = "f", description = "use frequencies instead of intensities for common loss estimation")
    public boolean isFrequencyCounting();

    @Option(shortName = "l", defaultToNull = true, description = "limit number of peaks to the n-th most intensive peaks. This makes computation much faster")
    public Integer getPeakLimit();

    @Option(shortName = "v")
    public boolean isVerbose();

    @Option(shortName = "h", helpRequest = true)
    public boolean getHelp();

    @Option
    public boolean getVersion();

    @Option
    public boolean getCite();


}
