/**
package de.unijena.bioinf.ms.frontend.utils;

import picocli.CommandLine;

class AutoComplete {

    CommandLine addAutocompletion() {
        CommandLine hierarchy = new CommandLine(new TopLevel())
                .addSubcommand("sub1", new Subcommand1())
                .addSubcommand("sub2", new Subcommand2());
        return hierarchy;
    }
}
**/