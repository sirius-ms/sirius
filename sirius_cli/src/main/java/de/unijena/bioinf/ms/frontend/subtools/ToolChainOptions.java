package de.unijena.bioinf.ms.frontend.subtools;

import java.util.List;
import java.util.concurrent.Callable;

public interface ToolChainOptions<O extends ToolChainJob<?>, F extends ToolChainJob.Factory<O>> extends Callable<F> {

    ToolChainJob.Invalidator getInvalidator();
    List<Class<? extends ToolChainOptions<?, ?>>> getSubCommands();
}
