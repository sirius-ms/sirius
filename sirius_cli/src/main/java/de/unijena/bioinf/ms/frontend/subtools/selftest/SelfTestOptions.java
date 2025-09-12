package de.unijena.bioinf.ms.frontend.subtools.selftest;

import de.unijena.bioinf.ms.frontend.subtools.Provide;
import de.unijena.bioinf.ms.frontend.subtools.RootOptions;
import de.unijena.bioinf.ms.frontend.subtools.StandaloneTool;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

@Slf4j
@CommandLine.Command(name = "selftest",  description = "<STANDALONE> Performs some basic tests to validate whether the application is working correctly independent from login, license and server connection. %n %n",  versionProvider = Provide.Versions.class, mixinStandardHelpOptions = true, showDefaultValues = true, hidden = true)
public class SelfTestOptions implements StandaloneTool<SelfTestWorkflow> {
    @Override
    public SelfTestWorkflow makeWorkflow(RootOptions<?> rootOptions, ParameterConfig config) {
        return new SelfTestWorkflow();
    }
}
