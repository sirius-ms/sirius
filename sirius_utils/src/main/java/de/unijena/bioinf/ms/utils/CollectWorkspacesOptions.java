package de.unijena.bioinf.ms.utils;

import com.lexicalscope.jewel.cli.Option;
import com.lexicalscope.jewel.cli.Unparsed;

import java.util.List;

public interface CollectWorkspacesOptions {

    @Option(shortName = "h", longName = "help", helpRequest = true)
    boolean isHelp();

    @Option(longName = "output", description = "output workspace. if ends with '.workspace', '.zip' or '.sirius' workspace will be zipped.")
    String getOutput();

    //naming
    @Option(longName = "naming-convention", description = "Specify a format for compounds' output directories. Default %index_%filename_%compoundname",  defaultToNull = true)
    String getNamingConvention();

    @Unparsed(description = "input workspaces")
    List<String> getInput();
}
