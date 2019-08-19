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

    @Option(longName = "id-list", description = "A file with compound ids and molecular formula candidates - one compound id per line. Only compounds with matching ids are selected. " +
            "If furthermore a line contains a list of molecular formulas (tab-separated), only candidates (fragmentation trees) with these formulas are selected.",  defaultToNull = true)
    String getIdList();

    @Unparsed(description = "input workspaces")
    List<String> getInput();
}
