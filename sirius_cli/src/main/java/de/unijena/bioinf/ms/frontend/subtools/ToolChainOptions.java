package de.unijena.bioinf.ms.frontend.subtools;

import de.unijena.bioinf.projectspace.Instance;
import de.unijena.bioinf.projectspace.invalidation.Invalidator;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

public interface ToolChainOptions<O extends ToolChainJob<?>, F extends ToolChainJob.Factory<O>> extends Callable<F> {

    Consumer<Instance> getInvalidator();
    List<Class<? extends ToolChainOptions<?, ?>>> getSubCommands();
}
