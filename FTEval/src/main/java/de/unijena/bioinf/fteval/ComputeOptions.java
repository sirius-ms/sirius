package de.unijena.bioinf.fteval;

import com.lexicalscope.jewel.cli.Unparsed;
import de.unijena.bioinf.sirius.cli.ProfileOptions;

public interface ComputeOptions extends ProfileOptions, EvalBasicOptions {

    @Unparsed(defaultToNull = true)
    public String getName();

}
