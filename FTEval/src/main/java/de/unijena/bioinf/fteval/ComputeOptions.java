package de.unijena.bioinf.fteval;

import com.lexicalscope.jewel.cli.Unparsed;
import de.unijena.bioinf.sirius.cli.ProfileOptions;

import java.util.List;

public interface ComputeOptions extends ProfileOptions, EvalBasicOptions {

    @Unparsed(defaultToNull = false)
    public List<String> getName();

}
