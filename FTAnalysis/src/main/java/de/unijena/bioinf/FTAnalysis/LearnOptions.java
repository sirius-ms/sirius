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

    @Option(shortName = "t", defaultValue = ".", description = "target directory")
    public File getTarget();

    @Option(shortName = "f", description = "use intensities instead of frequencies for common loss estimation")
    public boolean isIntensityCounting();

    @Option(shortName = "v")
    public boolean isVerbose();

    @Option(shortName = "h", helpRequest = true)
    public boolean getHelp();

    @Option
    public boolean getVersion();

    @Option
    public boolean getCite();


}
