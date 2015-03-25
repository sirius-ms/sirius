package de.unijena.bioinf.sirius.cli;

import com.lexicalscope.jewel.cli.Option;
import com.lexicalscope.jewel.cli.Unparsed;

import java.util.List;

public interface Options extends InputOptions, OutputOptions, ProfileOptions, IdentifyOptions {

    @Unparsed
    public List<String> getCommands();

}
